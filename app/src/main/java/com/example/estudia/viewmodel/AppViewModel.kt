package com.example.estudia.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.example.estudia.datos.RepositorioDatos
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.util.obtenerFechaHoyComoTexto

// Guarda el estado que se comparte entre pantallas (lista de usuarios
// y sesión actual) y concentra las operaciones que cambian los datos.
// Después de cada cambio le pide al Repository que guarde en el archivo.
// viewModel() hace que sobreviva a recomposiciones y a rotaciones de pantalla.
// Arquitectura: UI -> ViewModel -> Repository -> archivo.
class AppViewModel : ViewModel() {

    private var repositorio: RepositorioDatos? = null

    val usuariosRegistrados = mutableStateListOf<Usuario>()

    var usuarioActual by mutableStateOf<Usuario?>(null)

    // Colores que se van repartiendo entre las materias nuevas.
    private val coloresMaterias = listOf(
        Color(0xFF6C5CE7),
        Color(0xFF00B894),
        Color(0xFFFDA43C),
        Color(0xFFE74C3C),
        Color(0xFF0984E3)
    )

    // Se llama desde MainActivity. Carga los datos una sola vez: si el
    // ViewModel ya tiene repositorio (por ejemplo tras rotar la pantalla)
    // no vuelve a leer el archivo.
    fun iniciar(nuevoRepositorio: RepositorioDatos) {
        if (repositorio != null) return
        repositorio = nuevoRepositorio
        usuariosRegistrados.addAll(nuevoRepositorio.cargar())
    }

    fun guardarSesion(usuario: Usuario) {
        usuarioActual = usuario
    }

    fun cerrarSesion() {
        usuarioActual = null
    }

    fun registrarUsuario(usuario: Usuario) {
        usuariosRegistrados.add(usuario)
        usuarioActual = usuario
        guardar()
    }

    // ---------- Materias ----------

    fun agregarMateria(usuario: Usuario, nombre: String) {
        val posicion = usuario.materias.size
        // El id no puede volver a ser "size + 1": si se borró alguna materia
        // antes, ese cálculo generaría un id que ya existe. Usamos el mayor
        // id existente + 1 (0 si todavía no hay ninguna materia).
        val nuevoId = (usuario.materias.maxOfOrNull { it.id } ?: 0) + 1
        usuario.materias.add(
            Materia(
                id = nuevoId,
                nombre = nombre,
                color = coloresMaterias[posicion % coloresMaterias.size]
            )
        )
        guardar()
    }

    // Archiva o desarchiva una materia. Como Materia es una data class,
    // se hace una copia con copy() y se reemplaza en la lista (mismo
    // patrón que alternarFavorita), así Compose se entera y redibuja.
    fun alternarArchivadaMateria(usuario: Usuario, materia: Materia) {
        val indice = usuario.materias.indexOfFirst { it.id == materia.id }
        if (indice != -1) {
            usuario.materias[indice] = materia.copy(archivada = !materia.archivada)
        }
        guardar()
    }

    // Cambia el nombre de una materia (mismo patrón de copy() + reemplazo
    // por índice que alternarArchivadaMateria).
    fun editarMateria(usuario: Usuario, materia: Materia, nuevoNombre: String) {
        val indice = usuario.materias.indexOfFirst { it.id == materia.id }
        if (indice != -1) {
            usuario.materias[indice] = materia.copy(nombre = nuevoNombre)
        }
        guardar()
    }

    // Elimina una materia y, con ella, todas sus tareas, eventos y notas
    // (composición: no existen por separado de su materia dueña).
    fun eliminarMateria(usuario: Usuario, materia: Materia) {
        usuario.materias.removeAll { it.id == materia.id }
        guardar()
    }

    // ---------- Tareas ----------

    fun agregarTarea(materia: Materia, titulo: String, fechaLimite: String) {
        val nuevoId = (materia.tareas.maxOfOrNull { it.id } ?: 0) + 1
        materia.tareas.add(Tarea(id = nuevoId, titulo = titulo, fechaLimite = fechaLimite))
        guardar()
    }

    // Marca o desmarca una tarea como completada. Tarea.completada es val,
    // así que se reemplaza el elemento con una copia (mismo patrón que
    // alternarFavorita en Notas).
    fun alternarTareaCompletada(materia: Materia, tarea: Tarea) {
        val indice = materia.tareas.indexOfFirst { it.id == tarea.id }
        if (indice != -1) {
            materia.tareas[indice] = tarea.copy(completada = !tarea.completada)
        }
        guardar()
    }

    // ---------- Eventos ----------

    fun agregarEvento(
        materia: Materia,
        titulo: String,
        tipo: TipoEvento,
        fecha: String,
        horaInicio: String,
        horaFin: String
    ) {
        val nuevoId = (materia.eventos.maxOfOrNull { it.id } ?: 0) + 1
        materia.eventos.add(
            Evento(
                id = nuevoId,
                titulo = titulo,
                tipo = tipo,
                fecha = fecha,
                horaInicio = horaInicio,
                horaFin = horaFin
            )
        )
        guardar()
    }

    // ---------- Notas ----------

    fun agregarNota(materia: Materia, titulo: String, contenido: String) {
        val nuevoId = (materia.notas.maxOfOrNull { it.id } ?: 0) + 1
        materia.notas.add(
            Nota(
                id = nuevoId,
                titulo = titulo,
                contenido = contenido,
                fechaCreacion = obtenerFechaHoyComoTexto()
            )
        )
        guardar()
    }

    // Marca o desmarca una nota como favorita. Como Nota es una data class,
    // se hace una copia con copy() y se reemplaza en la lista, así Compose
    // se entera y redibuja.
    fun alternarFavorita(materia: Materia, nota: Nota) {
        val indice = materia.notas.indexOfFirst { it.id == nota.id }
        if (indice != -1) {
            materia.notas[indice] = nota.copy(esFavorita = !nota.esFavorita)
        }
        guardar()
    }

    // Cambia título y contenido de una nota existente (mismo patrón de
    // copy() + reemplazo por índice que alternarFavorita).
    fun editarNota(materia: Materia, nota: Nota, titulo: String, contenido: String) {
        val indice = materia.notas.indexOfFirst { it.id == nota.id }
        if (indice != -1) {
            materia.notas[indice] = nota.copy(titulo = titulo, contenido = contenido)
        }
        guardar()
    }

    fun eliminarNota(materia: Materia, nota: Nota) {
        materia.notas.removeAll { it.id == nota.id }
        guardar()
    }

    // ---------- Perfil ----------

    // Como Usuario es una data class, se arma una copia con los datos
    // nuevos y se reemplaza en usuariosRegistrados y, si corresponde,
    // en usuarioActual. Mutar los campos "var" del objeto original no
    // alcanza: usuarioActual guarda una referencia, y Compose solo
    // redibuja cuando ese estado recibe un valor nuevo, no cuando se
    // modifica en el lugar un campo del objeto que ya tenía.
    fun editarPerfil(usuario: Usuario, nombre: String, carrera: String?, anio: Int?, email: String?) {
        val usuarioActualizado = usuario.copy(
            nombre = nombre,
            carrera = carrera,
            anio = anio,
            email = email
        )

        val indice = usuariosRegistrados.indexOfFirst { it.id == usuario.id }
        if (indice != -1) {
            usuariosRegistrados[indice] = usuarioActualizado
        }

        if (usuarioActual?.id == usuario.id) {
            usuarioActual = usuarioActualizado
        }

        guardar()
    }

    private fun guardar() {
        val repo = repositorio
        if (repo != null) {
            repo.guardar(usuariosRegistrados)
        }
    }
}
