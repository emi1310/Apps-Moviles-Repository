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
import java.time.temporal.TemporalAdjusters

// Minutos estudiados en una materia durante la semana.
data class EstudioMateria(val nombre: String, val color: Long, val minutos: Long)

// Un examen o entrega de la semana que viene.
data class ProximoEvento(val titulo: String, val nombreMateria: String, val color: Long, val fecha: LocalDate)

data class ResumenSemanal(
    val lunes: LocalDate,
    val hasta: LocalDate,                // hoy (la semana todavía puede seguir)
    val minutos: Long,
    val minutosSemanaAnterior: Long,
    val diasEstudiados: Int,
    val sesiones: Int,
    val porMateria: List<EstudioMateria>, // la más estudiada primero
    val tareasHechas: Int,                // de las que vencían esta semana
    val tareasDeLaSemana: Int,
    val racha: Int,
    val metaHoras: Int?,
    val proximos: List<ProximoEvento>
) {
    // Diferencia con la semana anterior, en %. null si la anterior fue 0.
    val variacion: Int?
        get() = if (minutosSemanaAnterior == 0L) null
        else ((minutos - minutosSemanaAnterior) * 100 / minutosSemanaAnterior).toInt()

    val metaCumplida: Boolean get() = metaHoras != null && minutos >= metaHoras * 60L
}

data class ResumenSemanalUiState(val cargando: Boolean = true, val resumen: ResumenSemanal? = null)

class ResumenSemanalViewModel(
    repositorio: RepositorioEstudia,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    val uiState: StateFlow<ResumenSemanalUiState> = repositorio.usuarioActual
        .map { usuario -> ResumenSemanalUiState(cargando = false, resumen = usuario?.let { armarResumenSemanal(it, hoy()) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ResumenSemanalUiState())
}

// Función pura: la semana de "hoy" (de lunes a hoy) comparada con la
// semana anterior completa. También la usa el aviso de los domingos.
fun armarResumenSemanal(usuario: Usuario, hoy: LocalDate): ResumenSemanal {
    val lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val domingo = lunes.plusDays(6)
    val lunesAnterior = lunes.minusWeeks(1)
    val activas = usuario.materias.filter { !it.archivada }

    fun bloquesEntre(desde: LocalDate, hasta: LocalDate) = usuario.materias.flatMap { m ->
        m.eventos.filter { it.tipo == TipoEvento.BLOQUE_ESTUDIO && !it.fecha.isBefore(desde) && !it.fecha.isAfter(hasta) }
            .map { m to it }
    }

    val bloques = bloquesEntre(lunes, hoy)
    val anteriores = bloquesEntre(lunesAnterior, lunes.minusDays(1))
    val tareas = activas.flatMap { it.tareas }.filter { t -> t.fechaLimite?.let { !it.isBefore(lunes) && !it.isAfter(domingo) } ?: false }
    val proximos = activas.flatMap { m ->
        m.eventos.filter {
            (it.tipo == TipoEvento.EXAMEN || it.tipo == TipoEvento.ENTREGA) && it.fecha.isAfter(hoy) && !it.fecha.isAfter(hoy.plusDays(7))
        }.map { ProximoEvento(it.titulo, m.nombre, m.color, it.fecha) }
    }.sortedBy { it.fecha }

    return ResumenSemanal(
        lunes = lunes,
        hasta = hoy,
        minutos = bloques.sumOf { it.second.duracionMinutos() },
        minutosSemanaAnterior = anteriores.sumOf { it.second.duracionMinutos() },
        diasEstudiados = bloques.map { it.second.fecha }.distinct().size,
        sesiones = bloques.size,
        porMateria = bloques.groupBy { it.first.id }
            .map { (_, lista) -> EstudioMateria(lista.first().first.nombre, lista.first().first.color, lista.sumOf { it.second.duracionMinutos() }) }
            .sortedByDescending { it.minutos },
        tareasHechas = tareas.count { it.completada },
        tareasDeLaSemana = tareas.size,
        racha = usuario.rachaDeEstudio(hoy),
        metaHoras = usuario.metaHorasSemanales,
        proximos = proximos
    )
}
