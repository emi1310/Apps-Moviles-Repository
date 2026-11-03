package com.example.estudia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.SesionEstudio
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// Una tarea o un evento junto con los datos de su materia (Tarea y Evento
// por sí solos no saben de qué materia son).
data class TareaConMateria(val tarea: Tarea, val materiaId: Int, val nombreMateria: String, val colorMateria: Long)

data class EventoConMateria(val evento: Evento, val materiaId: Int, val nombreMateria: String, val colorMateria: Long)

// Todo lo que muestra la pantalla de Inicio, ya calculado.
data class InicioUiState(
    val cargando: Boolean = true,
    val nombre: String = "",
    val hoy: LocalDate = LocalDate.now(),
    val tareasPendientes: List<TareaConMateria> = emptyList(),
    val proximosEventos: List<EventoConMateria> = emptyList(),
    val notaDestacada: Nota? = null,
    val materiasActivas: List<Materia> = emptyList(),
    val minutosPorMateria: List<Pair<Materia, Long>> = emptyList(),
    val horasEstudio: Int = 0,
    // Para los tres datos del encabezado.
    val horasEstaSemana: Int = 0,
    val metaHorasSemanales: Int? = null,     // null = sin meta
    val rachaDias: Int = 0,
    val estudioHoy: Boolean = false          // si hoy ya hay un bloque de estudio
) {
    // Avance de la meta semanal entre 0 y 1 (null si no hay meta).
    val progresoMeta: Float?
        get() {
            val meta = metaHorasSemanales?.takeIf { it > 0 } ?: return null
            return (horasEstaSemana.toFloat() / meta).coerceIn(0f, 1f)
        }
}

// ViewModel de Inicio: arma el resumen a partir del usuario. Antes estos
// cálculos estaban dentro del Composable (lógica en la View); ahora la
// pantalla solo dibuja el UiState.
class InicioViewModel(
    private val repositorio: RepositorioEstudia,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    val uiState: StateFlow<InicioUiState> = repositorio.usuarioActual
        .map { usuario -> if (usuario == null) InicioUiState() else calcularResumen(usuario, hoy()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InicioUiState())

    // Sesión del temporizador en curso (null si no hay), para la tarjeta de Inicio.
    val sesionEstudio: StateFlow<SesionEstudio?> = repositorio.sesionEstudio
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun alternarTarea(item: TareaConMateria) {
        viewModelScope.launch {
            repositorio.actualizarTarea(item.materiaId, item.tarea.copy(completada = !item.tarea.completada))
        }
    }
}

// Función pura (sin Android ni corrutinas): fácil de testear.
fun calcularResumen(usuario: Usuario, hoy: LocalDate): InicioUiState {
    val activas = usuario.materias.filter { !it.archivada }

    // Pendientes de todas las materias activas: primero las vencidas y las
    // más próximas; las que no tienen fecha, al final.
    val pendientes = activas.flatMap { materia ->
        materia.tareas.filter { !it.completada }.map { TareaConMateria(it, materia.id, materia.nombre, materia.color) }
    }.sortedWith(compareBy<TareaConMateria> { it.tarea.fechaLimite == null }.thenBy { it.tarea.fechaLimite })

    // Exámenes y entregas desde hoy en adelante, en orden de fecha.
    val proximos = activas.flatMap { materia ->
        materia.eventos
            .filter { it.llevaRecordatorio && !it.fecha.isBefore(hoy) }
            .map { EventoConMateria(it, materia.id, materia.nombre, materia.color) }
    }.sortedBy { it.evento.inicio }

    val minutos = activas.map { materia ->
        materia to materia.eventos.filter { it.tipo == TipoEvento.BLOQUE_ESTUDIO }
            .sumOf { it.duracionMinutos() }
    }

    return InicioUiState(
        cargando = false,
        nombre = usuario.nombre.substringBefore(" "),
        hoy = hoy,
        tareasPendientes = pendientes,
        proximosEventos = proximos,
        notaDestacada = activas.flatMap { it.notas }.filter { it.esFavorita }.maxByOrNull { it.fechaCreacion },
        materiasActivas = activas,
        minutosPorMateria = minutos,
        horasEstudio = usuario.calcularHorasEstudio(),
        horasEstaSemana = usuario.horasEstudioDeLaSemana(hoy),
        metaHorasSemanales = usuario.metaHorasSemanales,
        rachaDias = usuario.rachaDeEstudio(hoy),
        estudioHoy = usuario.materias.any { materia ->
            materia.eventos.any { it.tipo == TipoEvento.BLOQUE_ESTUDIO && it.fecha == hoy }
        }
    )
}
