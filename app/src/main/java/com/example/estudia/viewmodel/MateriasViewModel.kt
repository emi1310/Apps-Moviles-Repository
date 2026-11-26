package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.datos.TareaEncontrada
import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.Calificacion
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.CategoriaArchivo
import com.example.estudia.modelo.ReglasCursada
import com.example.estudia.modelo.conReglas
import com.example.estudia.modelo.Prioridad
import com.example.estudia.modelo.Subtarea
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.Tarjeta
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

    fun agregarMateria(nombre: String, reglas: ReglasCursada = ReglasCursada()) {
        val u = usuario.value ?: return
        val color = colorParaNuevaMateria(u)
        viewModelScope.launch {
            repositorio.agregarMateria(
                u.id, nombre.trim(), color,
                reglas.asistenciaMinima, reglas.promocional, reglas.notaPromocion, reglas.tieneFinal
            )
        }
    }

    fun renombrarMateria(materia: Materia, nombre: String) = actualizar(materia.copy(nombre = nombre.trim()))

    // Editar el nombre y las reglas de la cursada (asistencia, promoción, final).
    fun editarMateria(materia: Materia, nombre: String, reglas: ReglasCursada) =
        actualizar(materia.copy(nombre = nombre.trim()).conReglas(reglas))

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

    // Crea o edita una tarea con su prioridad, sus subtareas y su aviso. Las
    // subtareas vacías se descartan; las nuevas llegan con id 0. Sin fecha
    // límite no hay aviso. Después se (re)programa el aviso de la tarea.
    fun guardarTarea(
        materiaId: Int,
        existente: Tarea?,
        titulo: String,
        fechaLimite: LocalDate?,
        prioridad: Prioridad = Prioridad.MEDIA,
        subtareas: List<Subtarea> = emptyList(),
        avisoDiasAntes: Int? = null
    ) {
        val pasos = subtareas.map { it.copy(titulo = it.titulo.trim()) }.filter { it.titulo.isNotEmpty() }
        val aviso = avisoDiasAntes.takeIf { fechaLimite != null }
        viewModelScope.launch {
            val guardada = if (existente == null) {
                repositorio.agregarTarea(materiaId, titulo.trim(), fechaLimite, prioridad, pasos.map { it.titulo }, aviso)
            } else {
                val editada = existente.copy(titulo = titulo.trim(), fechaLimite = fechaLimite, prioridad = prioridad, avisoDiasAntes = aviso)
                repositorio.actualizarTarea(materiaId, editada)
                if (pasos != existente.subtareas) repositorio.guardarSubtareas(existente.id, pasos)
                editada
            }
            avisosSistema.programarTarea(guardada)
        }
    }

    fun alternarSubtarea(tarea: Tarea, subtarea: Subtarea) {
        viewModelScope.launch { repositorio.alternarSubtarea(tarea.id, subtarea) }
    }

    // Al completarla se cancela su aviso; al desmarcarla vuelve a programarse.
    fun alternarTarea(materiaId: Int, tarea: Tarea) {
        val cambiada = tarea.copy(completada = !tarea.completada)
        viewModelScope.launch {
            repositorio.actualizarTarea(materiaId, cambiada)
            if (cambiada.completada) avisosSistema.cancelarTarea(tarea.id) else avisosSistema.programarTarea(cambiada)
        }
    }

    fun eliminarTarea(materiaId: Int, tarea: Tarea) {
        viewModelScope.launch {
            repositorio.eliminarTarea(materiaId, tarea)
            avisosSistema.cancelarTarea(tarea.id)
            avisar(
                Aviso(
                    Mensaje.AVISO_TAREA_ELIMINADA,
                    alDeshacer = {
                        repositorio.restaurarTarea(materiaId, tarea)
                        avisosSistema.programarTarea(tarea)
                    }
                )
            )
        }
    }

    // ---------- Tarjetas de estudio ----------

    // Crea o edita una tarjeta. false si falta la pregunta o la respuesta.
    fun guardarTarjeta(materiaId: Int, existente: Tarjeta?, pregunta: String, respuesta: String): Boolean {
        if (pregunta.isBlank() || respuesta.isBlank()) return false
        viewModelScope.launch {
            if (existente == null) repositorio.agregarTarjeta(materiaId, pregunta.trim(), respuesta.trim())
            else repositorio.actualizarTarjeta(materiaId, existente.copy(pregunta = pregunta.trim(), respuesta = respuesta.trim()))
        }
        return true
    }

    fun eliminarTarjeta(materiaId: Int, tarjeta: Tarjeta) {
        viewModelScope.launch {
            repositorio.eliminarTarjeta(materiaId, tarjeta)
            avisar(Aviso(Mensaje.AVISO_TARJETA_ELIMINADA, alDeshacer = { repositorio.restaurarTarjeta(materiaId, tarjeta) }))
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

    // Si ya se le explicó al usuario para qué es el permiso de fotos y
    // archivos (se explica una sola vez; después el permiso se pide directo).
    // Arranca en true para no mostrar la explicación antes de saber qué hay guardado.
    val permisoArchivosPedido: StateFlow<Boolean> = repositorio.permisoArchivosPedido
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun marcarPermisoArchivosPedido() {
        viewModelScope.launch { repositorio.marcarPermisoArchivosPedido() }
    }

    fun adjuntarArchivo(materiaId: Int, uri: String, categoria: CategoriaArchivo = CategoriaArchivo.OTROS) {
        viewModelScope.launch {
            val ok = repositorio.adjuntarArchivo(materiaId, null, uri, categoria)
            avisar(Aviso(if (ok) Mensaje.AVISO_ARCHIVO_GUARDADO else Mensaje.AVISO_ARCHIVO_ERROR))
        }
    }

    fun cambiarCategoriaArchivo(materiaId: Int, archivo: Archivo, categoria: CategoriaArchivo) {
        if (archivo.categoria == categoria) return
        viewModelScope.launch { repositorio.cambiarCategoriaArchivo(materiaId, archivo, categoria) }
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
