package com.example.estudia.modelo

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Los eventos de Estudia+ fuera de la app: en el calendario del teléfono
// (CalendarioTelefono) o en un archivo .ics, el formato estándar que
// importan Google Calendar, Outlook o el calendario del iPhone.

// Un evento con el nombre de su materia (va en el título: "Parcial · Física I").
data class EventoDeCalendario(val evento: Evento, val nombreMateria: String)

// Qué eventos salen de la app: los de las materias activas, menos los bloques
// de estudio que ya pasaron (son un registro de lo estudiado). Los que vienen,
// como los del plan de estudio de la IA, sí salen.
fun eventosParaCalendario(usuario: Usuario, hoy: LocalDate): List<EventoDeCalendario> =
    usuario.materias.filter { !it.archivada }.flatMap { materia ->
        materia.eventos
            .filter { it.tipo != TipoEvento.BLOQUE_ESTUDIO || !it.fecha.isBefore(hoy) }
            .map { EventoDeCalendario(it, materia.nombre) }
    }

private val FORMATO_LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
private val FORMATO_UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")

// Arma el archivo .ics (RFC 5545). Las horas van en la zona del teléfono
// (TZID) y las clases semanales llevan una regla de repetición (RRULE).
fun calendarioIcs(eventos: List<EventoDeCalendario>, nombre: String, zona: ZoneId, ahora: Instant): String {
    val sello = ahora.atOffset(ZoneOffset.UTC).format(FORMATO_UTC)
    val lineas = mutableListOf(
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//Estudia+//ES",
        "CALSCALE:GREGORIAN",
        "METHOD:PUBLISH",
        "X-WR-CALNAME:${escaparIcs(nombre)}",
        "X-WR-TIMEZONE:${zona.id}"
    )
    eventos.forEach { (evento, materia) ->
        lineas += "BEGIN:VEVENT"
        lineas += "UID:estudia-${evento.id}-${evento.fecha}@estudia.app"
        lineas += "DTSTAMP:$sello"
        lineas += "DTSTART;TZID=${zona.id}:${evento.fecha.atTime(evento.horaInicio).format(FORMATO_LOCAL)}"
        lineas += "DTEND;TZID=${zona.id}:${evento.fecha.atTime(evento.horaFin).format(FORMATO_LOCAL)}"
        if (evento.semanal) {
            val hasta = evento.repetirHasta?.let {
                ";UNTIL=" + it.atTime(23, 59, 59).atZone(zona).withZoneSameInstant(ZoneOffset.UTC).format(FORMATO_UTC)
            } ?: ""
            lineas += "RRULE:FREQ=WEEKLY$hasta"
        }
        lineas += "SUMMARY:${escaparIcs("${evento.titulo} · $materia")}"
        (evento.lugar ?: evento.enlace)?.let { lineas += "LOCATION:${escaparIcs(it)}" }
        evento.enlace?.let { lineas += "URL:$it" }
        if (evento.temas.isNotEmpty()) {
            lineas += "DESCRIPTION:${escaparIcs(evento.temas.joinToString("\n") { "- ${it.titulo}" })}"
        }
        lineas += "END:VEVENT"
    }
    lineas += "END:VCALENDAR"
    // Cada línea termina en CRLF y las largas se cortan (máximo 75 bytes).
    return lineas.joinToString("") { plegarLineaIcs(it) + "\r\n" }
}

// En los textos se escapan la barra, el punto y coma, la coma y los saltos de línea.
fun escaparIcs(texto: String): String =
    texto.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r\n", "\\n").replace("\n", "\\n")

// Corta en pedazos de hasta 75 bytes (UTF-8, sin partir caracteres); los
// que siguen empiezan con un espacio.
fun plegarLineaIcs(linea: String): String {
    val partes = mutableListOf<String>()
    val actual = StringBuilder()
    var bytes = 0
    var limite = 75
    var i = 0
    while (i < linea.length) {
        val largo = Character.charCount(linea.codePointAt(i))
        val caracter = linea.substring(i, i + largo)
        val tamano = caracter.toByteArray(Charsets.UTF_8).size
        if (bytes + tamano > limite) {
            partes += actual.toString()
            actual.clear()
            bytes = 0
            // Las siguientes llevan un espacio adelante, que cuenta.
            limite = 74
        }
        actual.append(caracter)
        bytes += tamano
        i += largo
    }
    partes += actual.toString()
    return partes.joinToString("\r\n ")
}
