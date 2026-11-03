package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.datos.TareaEncontrada
import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.Calificacion
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.Usuario
import com.example.estudia.notificaciones.AvisosSistema
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MateriasUiState(
    val cargando: Boolean = true,
    val usuarioId: Int = 0,
    val todas: List<Materia> = emptyList(),
    val mostrarArchivadas: Boolean = false,
    val busqueda: String = "",
    val resultadosBusqueda: List<TareaEncontrada> = emptyList()
) {
    // Materias del filtro elegido (Activas o Archivadas).
    val materias: List<Materia> get() = todas.filter { it.archivada == mostrarArchivadas }
    val buscando: Boolean get() = busqueda.isNotBlank()
}

// ViewModel de "Mis Materias": materias, tareas y archivos adjuntos.
class MateriasViewModel(
    private val repositorio: RepositorioEstudia,
    private val avisosSistema: AvisosSistema
) : ViewModelConAvisos() {

    private val usuario = repositorio.usuarioActual
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val mostrarArchivadas = MutableStateFlow(false)
    private val busqueda = MutableStateFlow("")

    // El buscador consulta la base (LIKE en TareaDao) cada vez que cambia el texto.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val resultados = combine(usuario, busqueda) { u, texto -> u to texto.trim() }
        .flatMapLatest { (u, texto) ->
            if (u == null || texto.isEmpty()) flowOf(emptyList()) else repositorio.buscarTareas(u.id, texto)
        }

    val uiState: StateFlow<MateriasUiState> =
        combine(usuario, mostrarArchivadas, busqueda, resultados) { u, archivadas, texto, encontradas ->
            MateriasUiState(
                cargando = u == null,
                usuarioId = u?.id ?: 0,
                todas = u?.materias ?: emptyList(),
                mostrarArchivadas = archivadas,
                busqueda = texto,
                resultadosBusqueda = encontradas
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MateriasUiState())

    fun mostrarArchivadas(archivadas: Boolean) {
        mostrarArchivadas.value = archivadas
    }

    fun cambiarBusqueda(texto: String) {
        busqueda.value = texto
    }

    // ---------- Materias ----------

    // false si ya hay otra materia con ese nombre (sin importar mayúsculas).
    fun nombreDisponible(nombre: String, exceptoId: Int? = null): Boolean {
        return uiState.value.todas.none { it.id != exceptoId && it.nombre.equals(nombre.trim(), ignoreCase = true) }
    }

    fun agregarMateria(nombre: String) {
        val u = usuario.value ?: return
        val color = colorParaNuevaMateria(u)
        viewModelScope.launch {
            repositorio.agregarMateria(u.id, nombre.trim(), color)
        }
    }

    fun renombrarMateria(materia: Materia, nombre: String) = actualizar(materia.copy(nombre = nombre.trim()))

    fun alternarArchivada(materia: Materia) {
        actualizar(materia.copy(archivada = !materia.archivada))
        avisar(Aviso(if (materia.archivada) Mensaje.AVISO_MATERIA_DESARCHIVADA else Mensaje.AVISO_MATERIA_ARCHIVADA))
    }

    // Eliminar una materia pide confirmación (borra todo su contenido) y,
    // por si acaso, también ofrece "Deshacer". Los archivos del disco se
    // borran recién cuando pasa el aviso.
    fun eliminarMateria(materia: Materia) {
        val u = usuario.value ?: return
        viewModelScope.launch {
            repositorio.eliminarMateria(u.id, materia)
            materia.eventos.forEach { avisosSistema.cancelarEvento(it.id) }
            avisar(
                Aviso(
                    Mensaje.AVISO_MATERIA_ELIMINADA,
                    argumentos = listOf(materia.nombre),
                    alDeshacer = {
                        repositorio.restaurarMateria(u.id, materia)
                        avisosSistema.programarEventos(materia.eventos)
                    },
                    alConfirmar = { repositorio.borrarArchivosDelDisco(materia.todosLosArchivos()) }
                )
            )
        }
    }

    private fun actualizar(materia: Materia) {
        val u = usuario.value ?: return
        viewModelScope.launch { repositorio.actualizarMateria(u.id, materia) }
    }

    // ---------- Tareas ----------

    fun guardarTarea(materiaId: Int, existente: Tarea?, titulo: String, fechaLimite: LocalDate?) {
        viewModelScope.launch {
            if (existente == null) {
                repositorio.agregarTarea(materiaId, titulo.trim(), fechaLimite)
            } else {
                repositorio.actualizarTarea(materiaId, existente.copy(titulo = titulo.trim(), fechaLimite = fechaLimite))
            }
        }
    }

    fun alternarTarea(materiaId: Int, tarea: Tarea) {
        viewModelScope.launch {
            repositorio.actualizarTarea(materiaId, tarea.copy(completada = !tarea.completada))
        }
    }

    fun eliminarTarea(materiaId: Int, tarea: Tarea) {
        viewModelScope.launch {
            repositorio.eliminarTarea(materiaId, tarea)
            avisar(
                Aviso(
                    Mensaje.AVISO_TAREA_ELIMINADA,
                    alDeshacer = { repositorio.restaurarTarea(materiaId, tarea) }
                )
            )
        }
    }

    // ---------- Calificaciones (notas de los exámenes) ----------

    // Crea o edita una nota de examen. La nota llega como texto ("7,5" o
    // "7.5"). Devuelve el error a mostrar en el diálogo, o null si se guardó.
    fun guardarCalificacion(materiaId: Int, existente: Calificacion?, titulo: String, notaTexto: String, fecha: LocalDate): Mensaje? {
        if (titulo.isBlank()) return Mensaje.ERROR_TITULO_CALIFICACION
        val nota = notaTexto.trim().replace(',', '.').toDoubleOrNull()
        if (nota == null || nota < Calificacion.NOTA_MINIMA || nota > Calificacion.NOTA_MAXIMA) {
            return Mensaje.ERROR_NOTA_INVALIDA
        }
        viewModelScope.launch {
            if (existente == null) {
                repositorio.agregarCalificacion(materiaId, titulo.trim(), nota, fecha)
            } else {
                repositorio.actualizarCalificacion(materiaId, existente.copy(titulo = titulo.trim(), nota = nota, fecha = fecha))
            }
        }
        return null
    }

    fun eliminarCalificacion(materiaId: Int, calificacion: Calificacion) {
        viewModelScope.launch {
            repositorio.eliminarCalificacion(materiaId, calificacion)
            avisar(
                Aviso(
                    Mensaje.AVISO_CALIFICACION_ELIMINADA,
                    alDeshacer = { repositorio.restaurarCalificacion(materiaId, calificacion) }
                )
            )
        }
    }

    // ---------- Archivos ----------

    // El permiso de fotos y archivos se pide una sola vez, la primera vez que
    // el usuario adjunta algo. Arranca en true para no preguntar antes de
    // saber qué hay guardado.
    val permisoArchivosPedido: StateFlow<Boolean> = repositorio.permisoArchivosPedido
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun marcarPermisoArchivosPedido() {
        viewModelScope.launch { repositorio.marcarPermisoArchivosPedido() }
    }

    fun adjuntarArchivo(materiaId: Int, uri: String) {
        viewModelScope.launch {
            val ok = repositorio.adjuntarArchivo(materiaId, null, uri)
            avisar(Aviso(if (ok) Mensaje.AVISO_ARCHIVO_GUARDADO else Mensaje.AVISO_ARCHIVO_ERROR))
        }
    }

    // La fila se borra ya; el archivo del disco recién cuando pasa el
    // "Deshacer" (si no, no habría cómo recuperarlo).
    fun eliminarArchivo(materiaId: Int, archivo: Archivo) {
        viewModelScope.launch {
            repositorio.eliminarArchivo(materiaId, archivo)
            avisar(
                Aviso(
                    Mensaje.AVISO_ARCHIVO_ELIMINADO,
                    alDeshacer = { repositorio.restaurarArchivo(materiaId, archivo) },
                    alConfirmar = { repositorio.borrarArchivosDelDisco(listOf(archivo)) }
                )
            )
        }
    }

    fun uriParaAbrir(archivo: Archivo): String = repositorio.uriParaAbrir(archivo)

    companion object {
        // Colores (ARGB) que se van repartiendo entre las materias nuevas.
        val COLORES = listOf(0xFF6C5CE7, 0xFF00B894, 0xFFFDA43C, 0xFFE74C3C, 0xFF0984E3)

        fun colorParaNuevaMateria(usuario: Usuario): Long = COLORES[usuario.materias.size % COLORES.size]
    }
}
