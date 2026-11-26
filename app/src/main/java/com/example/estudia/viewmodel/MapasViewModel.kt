package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.MapaMental
import com.example.estudia.modelo.cambiarColor
import com.example.estudia.modelo.cambiarTexto
import com.example.estudia.modelo.eliminarIdea
import com.example.estudia.modelo.moverRama
import com.example.estudia.modelo.agregarIdea
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

// ---------- Lista de mapas ----------

data class MapasUiState(
    val cargando: Boolean = true,
    val usuarioId: Int = 0,
    val mapas: List<MapaMental> = emptyList(),      // el último que se tocó, primero
    val materias: List<String> = emptyList(),       // para elegir al crear
    val colores: Map<String, Long> = emptyMap()     // nombre en minúscula -> color
)

// ViewModel de la pantalla "Mapas mentales": crear, renombrar y borrar mapas.
class MapasViewModel(
    private val repositorio: RepositorioEstudia,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModelConAvisos() {

    val uiState: StateFlow<MapasUiState> = repositorio.usuarioActual
        .map { u ->
            if (u == null) MapasUiState() else MapasUiState(
                cargando = false,
                usuarioId = u.id,
                mapas = u.mapas,
                materias = u.materias.filter { !it.archivada }.map { it.nombre },
                colores = u.materias.associate { it.nombre.trim().lowercase() to it.color }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MapasUiState())

    // Crea el mapa (la idea central es el título) y avisa su id para abrirlo.
    fun crear(titulo: String, materia: String, alCrear: (Int) -> Unit): Mensaje? {
        if (titulo.isBlank()) return Mensaje.ERROR_COMPLETAR_CAMPOS
        val usuarioId = uiState.value.usuarioId
        viewModelScope.launch {
            val guardado = repositorio.agregarMapa(usuarioId, MapaMental.nuevo(0, materia.trim(), titulo.trim(), hoy()))
            alCrear(guardado.id)
        }
        return null
    }

    // Cambia el título (y con él la idea central) y la materia.
    fun renombrar(mapa: MapaMental, titulo: String, materia: String): Mensaje? {
        if (titulo.isBlank()) return Mensaje.ERROR_COMPLETAR_CAMPOS
        val usuarioId = uiState.value.usuarioId
        viewModelScope.launch {
            repositorio.actualizarMapa(usuarioId, mapa.cambiarTexto(mapa.raiz.id, titulo).copy(materia = materia.trim(), modificado = hoy()))
        }
        return null
    }

    fun eliminar(mapa: MapaMental) {
        val usuarioId = uiState.value.usuarioId
        viewModelScope.launch {
            repositorio.eliminarMapa(usuarioId, mapa)
            avisar(Aviso(Mensaje.AVISO_MAPA_ELIMINADO, alDeshacer = { repositorio.restaurarMapa(usuarioId, mapa) }))
        }
    }
}

// ---------- Editor de un mapa ----------

data class MapaUiState(
    val cargando: Boolean = true,
    val mapa: MapaMental? = null,           // null sin cargar o si ya no existe
    val seleccionada: Int? = null           // la idea tocada (muestra sus acciones)
)

// ViewModel del editor: tiene el mapa en memoria y lo guarda en cada cambio.
// Mientras se arrastra una idea solo cambia la pantalla; se guarda al soltar.
class MapaViewModel(
    private val repositorio: RepositorioEstudia,
    private val mapaId: Int,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModelConAvisos() {

    private val estado = MutableStateFlow(MapaUiState())
    val uiState: StateFlow<MapaUiState> = estado.asStateFlow()
    private var usuarioId = 0

    init {
        viewModelScope.launch {
            val usuario = repositorio.usuarioActual.first { it != null }!!
            usuarioId = usuario.id
            estado.value = MapaUiState(cargando = false, mapa = usuario.mapas.firstOrNull { it.id == mapaId })
        }
    }

    fun seleccionar(id: Int?) = estado.update { it.copy(seleccionada = id) }

    // Agrega una idea que sale de la elegida y la deja elegida.
    fun agregarIdea(padreId: Int, texto: String) {
        val mapa = estado.value.mapa ?: return
        if (texto.isBlank()) return
        val (nuevo, idea) = mapa.agregarIdea(padreId, texto)
        cambiar(nuevo, seleccionada = idea.id)
    }

    fun editarTexto(id: Int, texto: String) {
        val mapa = estado.value.mapa ?: return
        cambiar(mapa.cambiarTexto(id, texto))
    }

    fun cambiarColor(id: Int, color: Int?) {
        val mapa = estado.value.mapa ?: return
        cambiar(mapa.cambiarColor(id, color))
    }

    // Borra la idea y lo que sale de ella; el aviso ofrece deshacer.
    fun eliminar(id: Int) {
        val antes = estado.value.mapa ?: return
        if (id == antes.raiz.id) return
        cambiar(antes.eliminarIdea(id), seleccionada = null)
        avisar(Aviso(Mensaje.AVISO_IDEA_ELIMINADA, alDeshacer = { cambiar(antes) }))
    }

    // Arrastrar: solo en pantalla. terminarArrastre() lo guarda.
    fun mover(id: Int, dx: Float, dy: Float) {
        estado.update { e -> e.copy(mapa = e.mapa?.moverRama(id, dx, dy)) }
    }

    fun terminarArrastre() {
        estado.value.mapa?.let { cambiar(it) }
    }

    private fun cambiar(mapa: MapaMental, seleccionada: Int? = estado.value.seleccionada) {
        val actualizado = mapa.copy(modificado = hoy())
        estado.update { it.copy(mapa = actualizado, seleccionada = seleccionada?.takeIf { id -> actualizado.nodos.any { n -> n.id == id } }) }
        viewModelScope.launch { repositorio.actualizarMapa(usuarioId, actualizado) }
    }
}
