package com.example.estudia.modelo

import java.time.LocalDate
import java.time.LocalTime

// Reacomodar el plan de estudio: si pasaron sesiones del plan (bloques de
// estudio que guardó la IA) y el examen todavía no llegó, la app pregunta si
// se hicieron. Las que no, se mueven a los días que quedan antes del examen,
// en un horario libre. Es un cálculo propio (sin IA): rápido y sin gastar pedidos.

// Una sesión del plan que ya pasó, con el examen para el que era.
data class SesionAtrasada(val evento: Evento, val materiaId: Int, val nombreMateria: String, val examen: Evento)

// Cuántos días hacia atrás se miran.
const val DIAS_SESIONES_ATRASADAS = 14

// Sesiones del plan (título "prefijo: ...") que ya pasaron, después de la
// última revisión, y cuyo examen sigue por venir (mañana o después).
fun sesionesAtrasadas(usuario: Usuario, hoy: LocalDate, prefijo: String, revisadoHasta: LocalDate?): List<SesionAtrasada> {
    val desde = maxOf(hoy.minusDays(DIAS_SESIONES_ATRASADAS.toLong()), revisadoHasta?.plusDays(1) ?: LocalDate.MIN)
    return usuario.materias.filter { !it.archivada }.flatMap { m ->
        val examenes = m.eventos.filter { it.tipo == TipoEvento.EXAMEN || it.tipo == TipoEvento.ENTREGA }.sortedBy { it.fecha }
        m.eventos.filter {
            it.tipo == TipoEvento.BLOQUE_ESTUDIO && !it.semanal && it.titulo.startsWith("$prefijo:") &&
                it.fecha.isBefore(hoy) && !it.fecha.isBefore(desde)
        }.mapNotNull { sesion ->
            // El examen para el que era: el primero después de la sesión, y que todavía no pasó.
            val examen = examenes.firstOrNull { it.fecha.isAfter(sesion.fecha) } ?: return@mapNotNull null
            if (!examen.fecha.isAfter(hoy)) null else SesionAtrasada(sesion, m.id, m.nombre, examen)
        }
    }.sortedWith(compareBy({ it.evento.fecha }, { it.evento.horaInicio }))
}

// Horario en el que se puede estudiar.
private val PRIMERA_HORA = LocalTime.of(8, 0)
private val ULTIMA_HORA = LocalTime.of(23, 0)

// Mueve cada sesión al primer hueco libre entre mañana y el día anterior a su
// examen: primero a la misma hora y, si está ocupada, de hora en hora. No se
// pisa con clases, exámenes ni otros bloques (ni con las ya movidas). Las que
// no entran no se devuelven.
fun reacomodarSesiones(atrasadas: List<SesionAtrasada>, usuario: Usuario, hoy: LocalDate): List<SesionAtrasada> {
    val ocupados = usuario.materias.filter { !it.archivada }.flatMap { it.eventos }
        .filter { e -> atrasadas.none { it.evento.id == e.id } }
        .toMutableList()
    val movidas = mutableListOf<SesionAtrasada>()
    atrasadas.forEach { s ->
        val minutos = s.evento.duracionMinutos()
        var dia = hoy.plusDays(1)
        var lugar: Pair<LocalDate, LocalTime>? = null
        while (lugar == null && dia.isBefore(s.examen.fecha)) {
            // La misma hora primero; después, de hora en hora desde la mañana.
            val horas = listOf(s.evento.horaInicio) + generateSequence(PRIMERA_HORA) { it.plusHours(1) }.takeWhile { it.isBefore(ULTIMA_HORA) && it != LocalTime.of(0, 0) }
            lugar = horas.firstOrNull { inicio ->
                val fin = inicio.plusMinutes(minutos)
                !fin.isBefore(inicio) && !fin.isAfter(ULTIMA_HORA) &&
                    ocupados.none { e -> e.ocurreEl(dia) && e.horaInicio < fin && inicio < e.horaFin }
            }?.let { dia to it }
            dia = dia.plusDays(1)
        }
        lugar?.let { (nuevoDia, inicio) ->
            val movido = s.evento.copy(fecha = nuevoDia, horaInicio = inicio, horaFin = inicio.plusMinutes(minutos))
            ocupados += movido
            movidas += s.copy(evento = movido)
        }
    }
    return movidas
}
