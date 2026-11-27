package com.example.estudia.datos

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.provider.CalendarContract
import com.example.estudia.modelo.EventoDeCalendario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Pasa los eventos de Estudia+ al calendario del teléfono (Calendar
// Provider), para verlos en la app de calendario junto con lo demás.
//
// Permisos: READ_CALENDAR y WRITE_CALENDAR (los pide la pantalla antes).
// Para no tocar los calendarios del usuario, la app crea uno propio llamado
// "Estudia+" (un calendario local del teléfono) y trabaja solo sobre ese:
// cada vez que se sincroniza borra sus eventos y los vuelve a cargar.
// Las clases que se repiten se cargan una vez, con una regla semanal (RRULE).
class CalendarioTelefono(context: Context) {

    private val resolver = context.applicationContext.contentResolver

    // Devuelve cuántos eventos quedaron en el calendario, o null si falló
    // (por ejemplo, si se quitó el permiso).
    suspend fun sincronizar(eventos: List<EventoDeCalendario>, nombreCalendario: String): Int? = withContext(Dispatchers.IO) {
        try {
            val calendarioId = buscarOCrearCalendario(nombreCalendario) ?: return@withContext null
            resolver.delete(
                CalendarContract.Events.CONTENT_URI,
                "${CalendarContract.Events.CALENDAR_ID} = ?",
                arrayOf(calendarioId.toString())
            )
            eventos.count { insertar(calendarioId, it) }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    // Deja de usar el calendario del teléfono: borra el calendario "Estudia+"
    // (con sus eventos). Devuelve false si no se pudo.
    suspend fun borrarCalendario(): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CUENTA)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build()
            resolver.delete(
                uri,
                "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
                arrayOf(CUENTA, CalendarContract.ACCOUNT_TYPE_LOCAL)
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    // Guarda el texto del archivo .ics en el archivo que eligió el usuario
    // (content:// del selector del sistema).
    suspend fun guardarIcs(uri: String, texto: String): Boolean = withContext(Dispatchers.IO) {
        try {
            resolver.openOutputStream(Uri.parse(uri), "wt")?.use { it.write(texto.toByteArray(Charsets.UTF_8)) } != null
        } catch (e: Exception) {
            false
        }
    }

    private fun insertar(calendarioId: Long, item: EventoDeCalendario): Boolean {
        val evento = item.evento
        val zona = ZoneId.systemDefault()
        val inicio = evento.inicio.atZone(zona).toInstant().toEpochMilli()
        val valores = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarioId)
            put(CalendarContract.Events.TITLE, "${evento.titulo} · ${item.nombreMateria}")
            put(CalendarContract.Events.DTSTART, inicio)
            put(CalendarContract.Events.EVENT_TIMEZONE, zona.id)
            if (evento.semanal) {
                // Los eventos que se repiten usan DURATION en vez de DTEND.
                put(CalendarContract.Events.DURATION, "PT${evento.duracionMinutos()}M")
                val hasta = evento.repetirHasta?.let {
                    ";UNTIL=" + it.atTime(23, 59, 59).atZone(zona).withZoneSameInstant(ZoneId.of("UTC"))
                        .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))
                } ?: ""
                put(CalendarContract.Events.RRULE, "FREQ=WEEKLY$hasta")
            } else {
                put(CalendarContract.Events.DTEND, evento.fecha.atTime(evento.horaFin).atZone(zona).toInstant().toEpochMilli())
            }
        }
        return resolver.insert(CalendarContract.Events.CONTENT_URI, valores) != null
    }

    // El calendario local "Estudia+": si no existe, se crea. Se crea como
    // "adaptador de sincronización" (así lo pide Android para calendarios locales).
    private fun buscarOCrearCalendario(nombre: String): Long? {
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
            arrayOf(CUENTA, CalendarContract.ACCOUNT_TYPE_LOCAL),
            null
        )?.use { cursor -> if (cursor.moveToFirst()) return cursor.getLong(0) }

        val uri: Uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CUENTA)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
        val valores = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, CUENTA)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, nombre)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, nombre)
            put(CalendarContract.Calendars.CALENDAR_COLOR, Color.rgb(0x6C, 0x5C, 0xE7))
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, CUENTA)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, ZoneId.systemDefault().id)
        }
        return resolver.insert(uri, valores)?.lastPathSegment?.toLongOrNull()
    }

    private companion object {
        const val CUENTA = "Estudia+"
    }
}
