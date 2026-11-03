package com.example.estudia.modelo

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

// Representa un evento del calendario: puede ser una clase, un examen,
// una entrega o un bloque de estudio (ver TipoEvento).
// La fecha y las horas son tipos reales de java.time (antes eran texto),
// así se pueden ordenar, comparar y calcular duraciones sin parsear.
//
// Las clases pueden repetirse cada semana (semanal = true): se guardan una
// sola vez, con la fecha de la primera clase, y se repiten el mismo día de
// la semana hasta repetirHasta (o sin fin, si es null).
data class Evento(
    val id: Int,
    val titulo: String,
    val tipo: TipoEvento,
    val fecha: LocalDate,
    val horaInicio: LocalTime,
    val horaFin: LocalTime,
    val semanal: Boolean = false,
    val repetirHasta: LocalDate? = null
) {
    val inicio: LocalDateTime get() = fecha.atTime(horaInicio)

    fun duracionMinutos(): Long {
        return Duration.between(horaInicio, horaFin).toMinutes().coerceAtLeast(0)
    }

    // Los exámenes y entregas se avisan la noche anterior (las clases, en
    // cambio, 15 minutos antes: ver ProgramadorRecordatorios).
    val llevaRecordatorio: Boolean
        get() = tipo == TipoEvento.EXAMEN || tipo == TipoEvento.ENTREGA

    // Inicio de la próxima vez que ocurre el evento después de "desde", o
    // null si ya no vuelve a ocurrir. Para las clases semanales recorre las
    // semanas hasta repetirHasta. Lo usa el aviso de antes de cada clase.
    fun proximoInicio(desde: LocalDateTime): LocalDateTime? {
        if (!semanal) return inicio.takeIf { it.isAfter(desde) }
        var dia = maxOf(fecha, desde.toLocalDate()).with(TemporalAdjusters.nextOrSame(fecha.dayOfWeek))
        if (!dia.atTime(horaInicio).isAfter(desde)) dia = dia.plusWeeks(1)
        val hasta = repetirHasta
        return if (hasta != null && dia.isAfter(hasta)) null else dia.atTime(horaInicio)
    }

    // ¿Hay que mostrar este evento el día "dia"? Para los que se repiten:
    // mismo día de la semana, desde la primera fecha y hasta repetirHasta.
    fun ocurreEl(dia: LocalDate): Boolean {
        if (!semanal) return dia == fecha
        val hasta = repetirHasta
        return dia.dayOfWeek == fecha.dayOfWeek &&
            !dia.isBefore(fecha) &&
            (hasta == null || !dia.isAfter(hasta))
    }
}
