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

class AppViewModel : ViewModel() {

    private var repositorio: RepositorioDatos? = null

    val usuariosRegistrados = mutableStateListOf<Usuario>()

    var usuarioActual by mutableStateOf<Usuario?>(null)

    private val coloresMaterias = listOf(
        Color(0xFF6C5CE7),
        Color(0xFF00B894),
        Color(0xFFFDA43C),
        Color(0xFFE74C3C),
        Color(0xFF0984E3)
    )

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

    fun agregarMateria(usuario: Usuario, nombre: String) {
        val posicion = usuario.materias.size
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

    fun alternarArchivadaMateria(usuario: Usuario, materia: Materia) {
        val indice = usuario.materias.indexOfFirst { it.id == materia.id }
        if (indice != -1) {
            usuario.materias[indice] = materia.copy(archivada = !materia.archivada)
        }
        guardar()
    }

    fun editarMateria(usuario: Usuario, materia: Materia, nuevoNombre: String) {
        val indice = usuario.materias.indexOfFirst { it.id == materia.id }
        if (indice != -1) {
            usuario.materias[indice] = materia.copy(nombre = nuevoNombre)
        }
        guardar()
    }

    fun eliminarMateria(usuario: Usuario, materia: Materia) {
        usuario.materias.removeAll { it.id == materia.id }
        guardar()
    }

    fun agregarTarea(materia: Materia, titulo: String, fechaLimite: String) {
        val nuevoId = (materia.tareas.maxOfOrNull { it.id } ?: 0) + 1
        materia.tareas.add(Tarea(id = nuevoId, titulo = titulo, fechaLimite = fechaLimite))
        guardar()
    }

    fun alternarTareaCompletada(materia: Materia, tarea: Tarea) {
        val indice = materia.tareas.indexOfFirst { it.id == tarea.id }
        if (indice != -1) {
            materia.tareas[indice] = tarea.copy(completada = !tarea.completada)
        }
        guardar()
    }

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

    fun alternarFavorita(materia: Materia, nota: Nota) {
        val indice = materia.notas.indexOfFirst { it.id == nota.id }
        if (indice != -1) {
            materia.notas[indice] = nota.copy(esFavorita = !nota.esFavorita)
        }
        guardar()
    }

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
