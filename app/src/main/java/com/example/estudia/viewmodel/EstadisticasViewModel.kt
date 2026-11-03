package com.example.estudia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

// Minutos de estudio de una semana (de lunes a domingo).
data class SemanaEstudio(val lunes: LocalDate, val minutos: Long)

data class EstadisticasUiState(
    val cargando: Boolean = true,
    val rachaActual: Int = 0,                 // días seguidos estudiando, hasta hoy (o ayer)
    val mejorRacha: Int = 0,
    val estudioHoy: Boolean = false,          // si hoy ya hay un bloque de estudio
    val semanas: List<SemanaEstudio> = emptyList(),   // últimas 8, de la más vieja a la actual
    val metaHorasSemanales: Int? = null,
    val promedioGeneral: Double? = null,
    val promediosPorMateria: List<Pair<Materia, Double>> = emptyList(),
    val minutosUltimoMes: List<Pair<Materia, Long>> = emptyList()   // últimos 30 días, por materia
) {
    val minutosEstaSemana: Long get() = semanas.lastOrNull()?.minutos ?: 0
}

// ViewModel de la pantalla de Estadísticas: solo lee al usuario y calcula.
class EstadisticasViewModel(
    repositorio: RepositorioEstudia,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    val uiState: StateFlow<EstadisticasUiState> = repositorio.usuarioActual
        .map { usuario -> if (usuario == null) EstadisticasUiState() else calcularEstadisticas(usuario, hoy()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EstadisticasUiState())
}

const val SEMANAS_EN_GRAFICO = 8

// Función pura (sin Android ni corrutinas): fácil de testear.
// Solo cuenta los bloques de estudio que ya pasaron (hasta hoy inclusive):
// los planificados para más adelante todavía no se estudiaron.
fun calcularEstadisticas(usuario: Usuario, hoy: LocalDate): EstadisticasUiState {
    val activas = usuario.materias.filter { !it.archivada }
    val bloques = usuario.materias.flatMap { materia ->
        materia.eventos.filter { it.tipo == TipoEvento.BLOQUE_ESTUDIO && !it.fecha.isAfter(hoy) }
    }
    val diasEstudiados = bloques.map { it.fecha }.toSet()

    // Racha actual: si hoy todavía no estudió, la racha de ayer sigue viva.
    val rachaActual = usuario.rachaDeEstudio(hoy)

    // Mejor racha: el tramo más largo de días seguidos.
    var mejorRacha = 0
    var tramo = 0
    var anterior: LocalDate? = null
    for (fecha in diasEstudiados.sorted()) {
        tramo = if (anterior != null && fecha == anterior.plusDays(1)) tramo + 1 else 1
        mejorRacha = maxOf(mejorRacha, tramo)
        anterior = fecha
    }

    val lunesActual = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val semanas = (SEMANAS_EN_GRAFICO - 1 downTo 0).map { atras ->
        val lunes = lunesActual.minusWeeks(atras.toLong())
        val domingo = lunes.plusDays(6)
        SemanaEstudio(lunes, bloques.filter { !it.fecha.isBefore(lunes) && !it.fecha.isAfter(domingo) }.sumOf { it.duracionMinutos() })
    }

    val haceUnMes = hoy.minusDays(29)
    val minutosUltimoMes = activas.map { materia ->
        materia to materia.eventos
            .filter { it.tipo == TipoEvento.BLOQUE_ESTUDIO && !it.fecha.isBefore(haceUnMes) && !it.fecha.isAfter(hoy) }
            .sumOf { it.duracionMinutos() }
    }.filter { it.second > 0 }.sortedByDescending { it.second }

    val calificaciones = activas.flatMap { it.calificaciones }

    return EstadisticasUiState(
        cargando = false,
        rachaActual = rachaActual,
        mejorRacha = maxOf(mejorRacha, rachaActual),
        estudioHoy = hoy in diasEstudiados,
        semanas = semanas,
        metaHorasSemanales = usuario.metaHorasSemanales,
        promedioGeneral = calificaciones.takeIf { it.isNotEmpty() }?.map { it.nota }?.average(),
        promediosPorMateria = activas.mapNotNull { materia -> materia.promedio()?.let { materia to it } },
        minutosUltimoMes = minutosUltimoMes
    )
}
