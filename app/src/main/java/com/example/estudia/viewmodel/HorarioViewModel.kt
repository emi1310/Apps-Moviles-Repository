package com.example.estudia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

// Una clase del horario: el evento junto con los datos de su materia.
data class ClaseHorario(
    val titulo: String,
    val nombreMateria: String,
    val colorMateria: Long,
    val inicio: LocalTime,
    val fin: LocalTime
) {
    val minutos: Long get() = java.time.Duration.between(inicio, fin).toMinutes().coerceAtLeast(0)
}

// Las clases de un día de la semana, ordenadas por hora.
data class DiaHorario(val dia: DayOfWeek, val clases: List<ClaseHorario>)

data class HorarioUiState(
    val cargando: Boolean = true,
    val hoy: LocalDate = LocalDate.now(),
    val dias: List<DiaHorario> = emptyList()
) {
    val totalClases: Int get() = dias.sumOf { it.clases.size }
    val minutosPorSemana: Long get() = dias.sumOf { dia -> dia.clases.sumOf { it.minutos } }
}

// ViewModel del horario semanal: solo lee al usuario y arma la grilla.
class HorarioViewModel(
    repositorio: RepositorioEstudia,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    val uiState: StateFlow<HorarioUiState> = repositorio.usuarioActual
        .map { usuario -> if (usuario == null) HorarioUiState() else armarHorario(usuario, hoy()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HorarioUiState())
}

// Función pura (sin Android ni corrutinas): fácil de testear.
// Toma las clases de las materias activas que caen en la semana de "hoy"
// (de lunes a domingo): las que se repiten cada semana (Evento.ocurreEl) y
// las sueltas de esta semana. De lunes a viernes siempre aparecen (aunque
// no tengan clases); sábado y domingo, solo si tienen.
fun armarHorario(usuario: Usuario, hoy: LocalDate): HorarioUiState {
    val lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val activas = usuario.materias.filter { !it.archivada }
    val dias = DayOfWeek.entries.map { diaSemana ->
        val fecha = lunes.plusDays((diaSemana.value - 1).toLong())
        val clases = activas.flatMap { materia ->
            materia.eventos
                .filter { it.tipo == TipoEvento.CLASE && it.ocurreEl(fecha) }
                .map { ClaseHorario(it.titulo, materia.nombre, materia.color, it.horaInicio, it.horaFin) }
        }.sortedBy { it.inicio }
        DiaHorario(diaSemana, clases)
    }.filter { it.dia.value <= 5 || it.clases.isNotEmpty() }
    return HorarioUiState(cargando = false, hoy = hoy, dias = dias)
}
