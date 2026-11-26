package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.NotaEncontrada
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Fuente
import com.example.estudia.modelo.Nota
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FiltroNotas { TODAS, RECIENTES, FAVORITAS }

// Una nota junto con su materia (Nota no sabe a qué materia pertenece).
data class NotaConMateria(val nota: Nota, val materiaId: Int, val nombreMateria: String, val colorMateria: Long)

data class NotasUiState(
    val cargando: Boolean = true,
    val notas: List<NotaConMateria> = emptyList(),
    val filtro: FiltroNotas = FiltroNotas.TODAS,
    val busqueda: String = "",
    val materiasActivas: List<Materia> = emptyList(),
    // La bibliografía, para "Citar" desde una nota.
    val fuentes: List<Fuente> = emptyList()
)

// ViewModel de "Mis Notas": filtros, buscador y adjuntos de cada nota.
class NotasViewModel(
    private val repositorio: RepositorioEstudia
) : ViewModelConAvisos() {

    private val usuario = repositorio.usuarioActual
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val filtro = MutableStateFlow(FiltroNotas.TODAS)
    private val busqueda = MutableStateFlow("")

    // Ids de las notas que coinciden con la búsqueda (LIKE en NotaDao), o
    // null si no se está buscando.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val idsEncontrados = combine(usuario, busqueda) { u, texto -> u to texto.trim() }
        .flatMapLatest { (u, texto) ->
            if (u == null || texto.isEmpty()) flowOf<List<Int>?>(null)
            else repositorio.buscarNotas(u.id, texto).map<List<NotaEncontrada>, List<Int>?> { lista -> lista.map { it.nota.id } }
        }

    val uiState: StateFlow<NotasUiState> =
        combine(usuario, filtro, busqueda, idsEncontrados) { u, filtroActual, texto, ids ->
            val todas = u?.materias.orEmpty().flatMap { materia ->
                materia.notas.map { NotaConMateria(it, materia.id, materia.nombre, materia.color) }
            }
            val buscadas = if (ids == null) todas else ids.mapNotNull { id -> todas.firstOrNull { it.nota.id == id } }
            val filtradas = when (filtroActual) {
                FiltroNotas.TODAS -> buscadas
                FiltroNotas.RECIENTES -> buscadas.sortedByDescending { it.nota.fechaCreacion }
                FiltroNotas.FAVORITAS -> buscadas.filter { it.nota.esFavorita }
            }
            NotasUiState(
                cargando = u == null,
                notas = filtradas,
                filtro = filtroActual,
                busqueda = texto,
                materiasActivas = u?.materias.orEmpty().filter { !it.archivada },
                fuentes = u?.fuentes.orEmpty()
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotasUiState())

    fun cambiarFiltro(nuevo: FiltroNotas) {
        filtro.value = nuevo
    }

    fun cambiarBusqueda(texto: String) {
        busqueda.value = texto
    }

    fun agregarNota(materiaId: Int, titulo: String, contenido: String) {
        viewModelScope.launch {
            repositorio.agregarNota(materiaId, titulo.trim(), contenido.trim())
        }
    }

    fun editarNota(item: NotaConMateria, titulo: String, contenido: String) {
        viewModelScope.launch {
            repositorio.actualizarNota(item.materiaId, item.nota.copy(titulo = titulo.trim(), contenido = contenido.trim()))
        }
    }

    fun alternarFavorita(item: NotaConMateria) {
        viewModelScope.launch {
            repositorio.actualizarNota(item.materiaId, item.nota.copy(esFavorita = !item.nota.esFavorita))
        }
    }

    // Se borra la nota (con sus adjuntos) y se ofrece "Deshacer". Los
    // archivos del disco se borran recién si no se deshace.
    fun eliminarNota(item: NotaConMateria) {
        viewModelScope.launch {
            repositorio.eliminarNota(item.materiaId, item.nota)
            avisar(
                Aviso(
                    Mensaje.AVISO_NOTA_ELIMINADA,
                    alDeshacer = { repositorio.restaurarNota(item.materiaId, item.nota) },
                    alConfirmar = { repositorio.borrarArchivosDelDisco(item.nota.archivos) }
                )
            )
        }
    }

    // Si ya se le explicó al usuario para qué es el permiso de fotos y
    // archivos (se explica una sola vez; después el permiso se pide directo).
    // Arranca en true para no mostrar la explicación antes de saber qué hay guardado.
    val permisoArchivosPedido: StateFlow<Boolean> = repositorio.permisoArchivosPedido
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun marcarPermisoArchivosPedido() {
        viewModelScope.launch { repositorio.marcarPermisoArchivosPedido() }
    }

    fun adjuntarArchivo(item: NotaConMateria, uri: String) {
        viewModelScope.launch {
            val ok = repositorio.adjuntarArchivo(item.materiaId, item.nota.id, uri)
            avisar(Aviso(if (ok) Mensaje.AVISO_ARCHIVO_GUARDADO_NOTA else Mensaje.AVISO_ARCHIVO_ERROR))
        }
    }

    fun eliminarArchivo(item: NotaConMateria, archivo: Archivo) {
        viewModelScope.launch {
            repositorio.eliminarArchivo(item.materiaId, archivo)
            avisar(
                Aviso(
                    Mensaje.AVISO_ARCHIVO_ELIMINADO,
                    alDeshacer = { repositorio.restaurarArchivo(item.materiaId, archivo) },
                    alConfirmar = { repositorio.borrarArchivosDelDisco(listOf(archivo)) }
                )
            )
        }
    }

    fun uriParaAbrir(archivo: Archivo): String = repositorio.uriParaAbrir(archivo)

    // ---------- Exportar a PDF ----------
    // El ViewModel arma el archivo (con el Repository) y deja su uri acá; la
    // pantalla abre "Compartir" con esa uri y después avisa con pdfCompartido().
    private val _pdfParaCompartir = MutableStateFlow<String?>(null)
    val pdfParaCompartir: StateFlow<String?> = _pdfParaCompartir.asStateFlow()

    fun exportarPdf(item: NotaConMateria) {
        viewModelScope.launch {
            val uri = repositorio.exportarNotaPdf(item.nota, item.nombreMateria)
            if (uri == null) avisar(Aviso(Mensaje.AVISO_PDF_ERROR)) else _pdfParaCompartir.value = uri
        }
    }

    fun pdfCompartido() {
        _pdfParaCompartir.value = null
    }

    // OCR: lee el texto de una foto adjunta (ej: el pizarrón) y lo suma al
    // final del contenido de la nota. Se puede deshacer.
    fun pasarATexto(item: NotaConMateria, archivo: Archivo) {
        viewModelScope.launch {
            val texto = repositorio.leerTextoDeImagen(archivo)
            when {
                texto == null -> avisar(Aviso(Mensaje.AVISO_OCR_ERROR))
                texto.isBlank() -> avisar(Aviso(Mensaje.AVISO_OCR_SIN_TEXTO))
                else -> {
                    val anterior = item.nota.contenido.trimEnd()
                    val contenido = if (anterior.isEmpty()) texto else "$anterior\n\n$texto"
                    repositorio.actualizarNota(item.materiaId, item.nota.copy(contenido = contenido))
                    avisar(Aviso(Mensaje.AVISO_OCR_AGREGADO, alDeshacer = { repositorio.actualizarNota(item.materiaId, item.nota) }))
                }
            }
        }
    }
}
