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
    val repetirHasta: LocalDate? = null,
    // Versión 7: temas que entran en un examen (el temario), para ir
    // marcando los que ya se estudiaron.
    val temas: List<Tema> = emptyList(),
    // Versión 9: asistencia a las clases (una por día marcado).
    val asistencias: List<Asistencia> = emptyList(),
    // Versión 10: clase presencial (con aula o lugar) o virtual (con link).
    val virtual: Boolean = false,
    val lugar: String? = null,
    val enlace: String? = null,
    // Versión 14: las clases semanales pueden ser cada 1 semana o semana por
    // medio (2), para horarios de semana A y semana B.
    val cadaSemanas: Int = 1
) {
    val inicio: LocalDateTime get() = fecha.atTime(horaInicio)

    val temasEstudiados: Int get() = temas.count { it.estudiado }

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
        // Semana por medio: si cae en la semana que no toca, pasa a la siguiente.
        if (!enSemanaQueToca(dia)) dia = dia.plusWeeks(1)
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
            (hasta == null || !dia.isAfter(hasta)) &&
            enSemanaQueToca(dia)
    }

    // Para las de semana por medio: true si entre la primera clase y "dia"
    // pasó una cantidad de semanas que es múltiplo de cadaSemanas.
    private fun enSemanaQueToca(dia: LocalDate): Boolean {
        if (cadaSemanas <= 1) return true
        val semanas = java.time.temporal.ChronoUnit.WEEKS.between(fecha, dia)
        return Math.floorMod(semanas, cadaSemanas.toLong()) == 0L
    }
}

// Un tema del temario de un examen (ej: "Unidad 3: derivadas").
data class Tema(
    val id: Int,
    val titulo: String,
    val estudiado: Boolean = false
)

// Link de una clase virtual tal como se escribió: sin espacios y, si no
// tiene "http", con "https://" adelante (ej: "meet.google.com/abc" ->
// "https://meet.google.com/abc"). null si quedó vacío.
fun normalizarEnlace(texto: String?): String? {
    val limpio = texto?.trim()?.replace(" ", "").orEmpty()
    if (limpio.isEmpty()) return null
    return if (limpio.startsWith("http://", ignoreCase = true) || limpio.startsWith("https://", ignoreCase = true)) limpio
    else "https://$limpio"
}
