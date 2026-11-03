package com.example.estudia.modelo

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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

    // Solo los exámenes y entregas generan un recordatorio propio.
    val llevaRecordatorio: Boolean
        get() = tipo == TipoEvento.EXAMEN || tipo == TipoEvento.ENTREGA

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
