package com.example.estudia.modelo

import java.time.LocalDate
import java.time.LocalDateTime

// "Silenciar en clase": el teléfono pasa a No molestar mientras hay una
// clase o un examen (según el horario cargado) y vuelve a sonar al terminar.
// Los feriados y recesos no tienen clase.

data class EstadoSilencioClase(
    val enClase: Boolean,
    // Cuándo hay que volver a revisar: el fin de la clase actual o el
    // comienzo de la próxima (null si no hay ninguna en la próxima semana).
    val proximoCambio: LocalDateTime?
)

const val DIAS_A_MIRAR_SILENCIO = 8L

// Franjas con clase o examen desde hoy, unidas si se superponen o están
// pegadas (dos clases seguidas son un solo silencio).
fun franjasDeClase(usuario: Usuario, desde: LocalDate, diasSinClase: Set<LocalDate>, dias: Long = DIAS_A_MIRAR_SILENCIO): List<ClosedRange<LocalDateTime>> {
    val activas = usuario.materias.filter { !it.archivada }
    val franjas = (0 until dias).map { desde.plusDays(it) }
        .filter { it !in diasSinClase }
        .flatMap { dia ->
            activas.flatMap { it.eventos }
                .filter { (it.tipo == TipoEvento.CLASE || it.tipo == TipoEvento.EXAMEN) && it.ocurreEl(dia) && it.horaFin.isAfter(it.horaInicio) }
                .map { dia.atTime(it.horaInicio)..dia.atTime(it.horaFin) }
        }
        .sortedBy { it.start }
    val unidas = mutableListOf<ClosedRange<LocalDateTime>>()
    for (franja in franjas) {
        val ultima = unidas.lastOrNull()
        if (ultima != null && !franja.start.isAfter(ultima.endInclusive)) {
            if (franja.endInclusive.isAfter(ultima.endInclusive)) unidas[unidas.lastIndex] = ultima.start..franja.endInclusive
        } else {
            unidas.add(franja)
        }
    }
    return unidas
}

fun estadoSilencioClase(usuario: Usuario, ahora: LocalDateTime, diasSinClase: Set<LocalDate>): EstadoSilencioClase {
    val franjas = franjasDeClase(usuario, ahora.toLocalDate(), diasSinClase)
    val actual = franjas.firstOrNull { !ahora.isBefore(it.start) && ahora.isBefore(it.endInclusive) }
    return if (actual != null) EstadoSilencioClase(true, actual.endInclusive)
    else EstadoSilencioClase(false, franjas.firstOrNull { it.start.isAfter(ahora) }?.start)
}
