package com.example.estudia.modelo

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// Sincronización en los dos sentidos con un calendario del teléfono (el
// local "Estudia+" o uno de Google Calendar, que Google sube a la nube):
//  - Lo que se crea, cambia o borra en Estudia+ se pasa al calendario.
//  - Si en el calendario se cambia el título, el día o el horario de un
//    evento que vino de Estudia+, el cambio vuelve a la app.
// Para saber de qué lado fue el cambio se guarda una "foto" de cómo quedó
// cada evento la última vez que se sincronizó: si el calendario ya no
// coincide con la foto, lo cambió el usuario en el calendario (y ese cambio
// gana); si lo que cambió es Estudia+, se actualiza el calendario.
// Si un evento se borra en el calendario, se vuelve a crear: para borrarlo
// hay que hacerlo en Estudia+ (así nunca se pierde nada sin querer).

// Un evento tal como está (o debería estar) en el calendario del teléfono.
data class FilaCalendario(
    val titulo: String,
    val inicioMillis: Long,
    val finMillis: Long?,          // null en los que se repiten (usan duración)
    val duracion: String?,         // "PT90M" en los que se repiten
    val regla: String?             // RRULE ("FREQ=WEEKLY;INTERVAL=2...")
) {
    // Lo que se compara: el usuario solo puede cambiar título, día y horario.
    fun mismaFecha(otra: FilaCalendario) =
        inicioMillis == otra.inicioMillis && finMillis == otra.finMillis && duracion == otra.duracion

    fun igual(otra: FilaCalendario) = titulo == otra.titulo && mismaFecha(otra) && regla == otra.regla
}

// Un evento que el usuario cambió en el calendario y hay que pasar a Estudia+.
data class CambioDesdeCalendario(val eventoId: Int, val fila: FilaCalendario)

data class PlanSincronizacion(
    val insertar: List<Int>,                 // ids de eventos de Estudia+ que faltan en el calendario
    val actualizar: List<Int>,               // los que cambiaron en Estudia+
    val borrar: List<Int>,                   // los que ya no están en Estudia+
    val desdeCalendario: List<CambioDesdeCalendario>
)

// deseados: cómo tendrían que quedar (salen de Estudia+). actuales: cómo
// están en el calendario. foto: cómo quedaron la última vez.
fun planSincronizacion(
    deseados: Map<Int, FilaCalendario>,
    actuales: Map<Int, FilaCalendario>,
    foto: Map<Int, FilaCalendario>
): PlanSincronizacion {
    val insertar = mutableListOf<Int>()
    val actualizar = mutableListOf<Int>()
    val desdeCalendario = mutableListOf<CambioDesdeCalendario>()
    for ((id, deseado) in deseados) {
        val actual = actuales[id]
        val anterior = foto[id]
        when {
            actual == null -> insertar.add(id)
            // Cambió en el calendario (título u horario distinto de la foto): gana el calendario.
            anterior != null && (actual.titulo != anterior.titulo || !actual.mismaFecha(anterior)) ->
                if (actual.titulo != deseado.titulo || !actual.mismaFecha(deseado)) desdeCalendario.add(CambioDesdeCalendario(id, actual))
            !actual.igual(deseado) -> actualizar.add(id)
        }
    }
    val borrar = actuales.keys.filter { it !in deseados }
    return PlanSincronizacion(insertar, actualizar, borrar, desdeCalendario)
}

// Cómo queda un evento de Estudia+ en el calendario. Las clases que se
// repiten llevan una regla semanal (cada 1 o 2 semanas, hasta el último día).
fun filaDeEvento(item: EventoDeCalendario, zona: ZoneId): FilaCalendario {
    val evento = item.evento
    val inicio = evento.inicio.atZone(zona).toInstant().toEpochMilli()
    val titulo = "${evento.titulo} · ${item.nombreMateria}"
    return if (evento.semanal) {
        val intervalo = if (evento.cadaSemanas > 1) ";INTERVAL=${evento.cadaSemanas}" else ""
        val hasta = evento.repetirHasta?.let {
            ";UNTIL=" + it.atTime(23, 59, 59).atZone(zona).withZoneSameInstant(ZoneId.of("UTC"))
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
        } ?: ""
        FilaCalendario(titulo, inicio, null, "PT${evento.duracionMinutos()}M", "FREQ=WEEKLY$intervalo$hasta")
    } else {
        FilaCalendario(titulo, inicio, evento.fecha.atTime(evento.horaFin).atZone(zona).toInstant().toEpochMilli(), null, null)
    }
}

// Pasa a un evento de Estudia+ lo que se cambió en el calendario: el título
// (sin el " · Materia" que se le agrega al pasarlo), el día y el horario.
// null si el horario que quedó no sirve (termina otro día o antes de empezar).
fun aplicarCambioDeCalendario(evento: Evento, nombreMateria: String, fila: FilaCalendario, zona: ZoneId): Evento? {
    val inicio = LocalDateTime.ofInstant(Instant.ofEpochMilli(fila.inicioMillis), zona)
    val fin = when {
        fila.finMillis != null -> LocalDateTime.ofInstant(Instant.ofEpochMilli(fila.finMillis), zona)
        fila.duracion != null -> duracionEnMinutos(fila.duracion)?.let { inicio.plusMinutes(it) }
        else -> null
    } ?: return null
    if (fin.toLocalDate() != inicio.toLocalDate() || !fin.toLocalTime().isAfter(inicio.toLocalTime())) return null
    val titulo = fila.titulo.removeSuffix(" · $nombreMateria").trim().ifEmpty { evento.titulo }
    return evento.copy(titulo = titulo, fecha = inicio.toLocalDate(), horaInicio = inicio.toLocalTime(), horaFin = fin.toLocalTime())
}

// "PT90M", "PT1H30M", "P0DT1H" (formato RFC 5545 que usa Android) -> minutos.
fun duracionEnMinutos(texto: String): Long? = runCatching { java.time.Duration.parse(texto.replace("P0D", "P")).toMinutes() }
    .getOrNull()
    ?: Regex("^P(\\d+)S$").find(texto)?.groupValues?.get(1)?.toLongOrNull()?.div(60)

// Un calendario de Google del teléfono donde se pueden pasar los eventos.
data class CalendarioDisponible(val id: Long, val nombre: String, val cuenta: String, val color: Long)

// Lo que pasó al sincronizar: cuántos eventos hay en el calendario y cuáles
// de Estudia+ se cambiaron con lo que el usuario editó en el calendario.
data class ResultadoCalendario(val cantidad: Int, val cambiados: List<Evento>)
