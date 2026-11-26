package com.example.estudia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.Normalizer
import java.time.LocalDate

// Qué se encontró (define el ícono y a qué sección lleva al tocarlo).
enum class TipoResultado { MATERIA, TAREA, NOTA, EVENTO, TARJETA, CARRERA, FINAL, FUENTE, MAPA }

data class ResultadoBusqueda(
    val tipo: TipoResultado,
    val titulo: String,
    val detalle: String?,          // contenido de la nota, respuesta de la tarjeta...
    val nombreMateria: String?,
    val color: Long?,
    val fecha: LocalDate? = null
)

data class BusquedaUiState(
    val texto: String = "",
    val resultados: List<ResultadoBusqueda> = emptyList()
) {
    val buscando: Boolean get() = texto.trim().length >= LARGO_MINIMO_BUSQUEDA
}

const val LARGO_MINIMO_BUSQUEDA = 2
const val MAXIMO_RESULTADOS = 60

// ViewModel de la búsqueda global: busca a la vez en materias, tareas,
// notas, eventos, tarjetas, el plan de estudios, los finales, la bibliografía y los mapas mentales.
class BusquedaViewModel(repositorio: RepositorioEstudia) : ViewModel() {

    private val texto = MutableStateFlow("")

    val uiState: StateFlow<BusquedaUiState> = combine(repositorio.usuarioActual, texto) { usuario, t ->
        BusquedaUiState(t, if (usuario == null) emptyList() else buscarEnTodo(usuario, t))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusquedaUiState())

    fun cambiarTexto(nuevo: String) {
        texto.value = nuevo
    }
}

// Sin mayúsculas ni tildes: "matemática" encuentra "Matematica" y al revés.
fun normalizar(texto: String): String =
    Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

// Función pura: todos los resultados que contienen el texto, agrupados por tipo.
fun buscarEnTodo(usuario: Usuario, texto: String): List<ResultadoBusqueda> {
    val buscado = normalizar(texto.trim())
    if (buscado.length < LARGO_MINIMO_BUSQUEDA) return emptyList()
    fun coincide(vararg campos: String?) = campos.any { it != null && normalizar(it).contains(buscado) }

    val resultados = mutableListOf<ResultadoBusqueda>()
    usuario.materias.forEach { m ->
        if (coincide(m.nombre)) resultados += ResultadoBusqueda(TipoResultado.MATERIA, m.nombre, null, null, m.color)
    }
    usuario.materias.forEach { m ->
        m.tareas.filter { t -> coincide(t.titulo) || t.subtareas.any { coincide(it.titulo) } }
            .forEach { resultados += ResultadoBusqueda(TipoResultado.TAREA, it.titulo, null, m.nombre, m.color, it.fechaLimite) }
    }
    usuario.materias.forEach { m ->
        m.notas.filter { coincide(it.titulo, it.contenido) }
            .forEach { resultados += ResultadoBusqueda(TipoResultado.NOTA, it.titulo, extracto(it.contenido, buscado), m.nombre, m.color, it.fechaCreacion) }
    }
    usuario.materias.forEach { m ->
        m.eventos.filter { e -> e.tipo != TipoEvento.BLOQUE_ESTUDIO && (coincide(e.titulo) || e.temas.any { coincide(it.titulo) }) }
            .forEach { resultados += ResultadoBusqueda(TipoResultado.EVENTO, it.titulo, null, m.nombre, m.color, it.fecha) }
    }
    usuario.materias.forEach { m ->
        m.tarjetas.filter { coincide(it.pregunta, it.respuesta) }
            .forEach { resultados += ResultadoBusqueda(TipoResultado.TARJETA, it.pregunta, it.respuesta, m.nombre, m.color) }
    }
    usuario.planEstudio.filter { coincide(it.nombre) }
        .forEach { resultados += ResultadoBusqueda(TipoResultado.CARRERA, it.nombre, null, null, null) }
    usuario.finales.filter { coincide(it.materia) }
        .forEach { resultados += ResultadoBusqueda(TipoResultado.FINAL, it.materia, null, null, null, it.fechaMesa) }
    usuario.fuentes.filter { coincide(it.titulo, it.autores, it.origen, it.comentario) }
        .forEach { f -> resultados += ResultadoBusqueda(TipoResultado.FUENTE, f.titulo, f.listaAutores.joinToString(", ").ifEmpty { null }, f.materia.ifEmpty { null }, colorDeMateria(usuario, f.materia)) }
    usuario.mapas.forEach { m ->
        val idea = m.nodos.firstOrNull { it.padreId != null && coincide(it.texto) }
        if (coincide(m.titulo) || idea != null) {
            resultados += ResultadoBusqueda(TipoResultado.MAPA, m.titulo, idea?.texto, m.materia.ifEmpty { null }, colorDeMateria(usuario, m.materia), m.modificado)
        }
    }
    return resultados.take(MAXIMO_RESULTADOS)
}

// Un pedacito del contenido alrededor de lo encontrado (para las notas largas).
fun extracto(contenido: String, buscadoNormalizado: String, largo: Int = 80): String {
    val plano = contenido.replace('\n', ' ')
    val posicion = normalizar(plano).indexOf(buscadoNormalizado)
    if (posicion < 0 || plano.length <= largo) return plano.take(largo)
    val desde = (posicion - largo / 3).coerceAtLeast(0)
    val hasta = (desde + largo).coerceAtMost(plano.length)
    return (if (desde > 0) "…" else "") + plano.substring(desde, hasta).trim() + (if (hasta < plano.length) "…" else "")
}

// Color de una materia de la app por su nombre (la bibliografía y los
// finales guardan el nombre, no el id).
private fun colorDeMateria(usuario: Usuario, nombre: String): Long? =
    usuario.materias.firstOrNull { it.nombre.trim().equals(nombre.trim(), ignoreCase = true) }?.color
