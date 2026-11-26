package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.Fuente
import com.example.estudia.modelo.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BibliografiaUiState(
    val cargando: Boolean = true,
    val usuarioId: Int = 0,
    // Las fuentes que se ven (según la materia elegida), por apellido.
    val fuentes: List<Fuente> = emptyList(),
    val total: Int = 0,
    // Materias para filtrar: las que tienen fuentes cargadas.
    val materiasConFuentes: List<String> = emptyList(),
    val filtro: String? = null,          // null = todas
    // Materias para elegir al cargar una fuente (las de la app sin archivar).
    val materiasParaElegir: List<String> = emptyList(),
    // Color de cada materia de la app (nombre en minúscula).
    val colores: Map<String, Long> = emptyMap()
)

// ViewModel de la bibliografía: libros, artículos, páginas web y apuntes de
// cátedra, agrupados por materia, con la cita en normas APA lista para copiar.
class BibliografiaViewModel(private val repositorio: RepositorioEstudia) : ViewModelConAvisos() {

    private val filtro = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BibliografiaUiState> = repositorio.usuarioActual
        .combine(filtro) { usuario, elegido -> if (usuario == null) BibliografiaUiState() else armarBibliografia(usuario, elegido) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BibliografiaUiState())

    fun filtrar(materia: String?) {
        filtro.value = materia
    }

    // Guarda la fuente nueva (id 0) o los cambios. Necesita título y, si no
    // es una web, el autor (en una web puede faltar: va el título primero).
    fun guardar(fuente: Fuente): Mensaje? {
        val f = fuente.copy(
            materia = fuente.materia.trim(),
            autores = fuente.autores.trim(),
            titulo = fuente.titulo.trim(),
            origen = fuente.origen.trim(),
            enlace = fuente.enlace.trim(),
            paginas = fuente.paginas.trim(),
            comentario = fuente.comentario.trim()
        )
        if (f.titulo.isEmpty()) return Mensaje.ERROR_COMPLETAR_CAMPOS
        if (f.anio != null && f.anio !in 1000..2200) return Mensaje.ERROR_ANIO_INVALIDO
        val usuarioId = uiState.value.usuarioId
        viewModelScope.launch {
            if (f.id == 0) repositorio.agregarFuente(usuarioId, f) else repositorio.actualizarFuente(usuarioId, f)
        }
        return null
    }

    fun eliminar(fuente: Fuente) {
        val usuarioId = uiState.value.usuarioId
        viewModelScope.launch {
            repositorio.eliminarFuente(usuarioId, fuente)
            avisar(Aviso(Mensaje.AVISO_FUENTE_ELIMINADA, alDeshacer = { repositorio.restaurarFuente(usuarioId, fuente) }))
        }
    }
}

fun armarBibliografia(usuario: Usuario, filtro: String?): BibliografiaUiState {
    val materias = usuario.fuentes.map { it.materia }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
    // Si la materia elegida ya no tiene fuentes, se vuelve a "Todas".
    val elegido = filtro?.takeIf { f -> materias.any { it.equals(f, ignoreCase = true) } }
    return BibliografiaUiState(
        cargando = false,
        usuarioId = usuario.id,
        fuentes = usuario.fuentes.filter { elegido == null || it.materia.equals(elegido, ignoreCase = true) },
        total = usuario.fuentes.size,
        materiasConFuentes = materias,
        filtro = elegido,
        materiasParaElegir = usuario.materias.filter { !it.archivada }.map { it.nombre },
        colores = usuario.materias.associate { it.nombre.trim().lowercase() to it.color }
    )
}
