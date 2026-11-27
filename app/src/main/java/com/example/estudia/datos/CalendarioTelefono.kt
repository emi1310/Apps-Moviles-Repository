package com.example.estudia.datos

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.provider.CalendarContract
import com.example.estudia.modelo.CambioDesdeCalendario
import com.example.estudia.modelo.CalendarioDisponible
import com.example.estudia.modelo.EventoDeCalendario
import com.example.estudia.modelo.FilaCalendario
import com.example.estudia.modelo.filaDeEvento
import com.example.estudia.modelo.planSincronizacion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.ZoneId

// Sincroniza los eventos de Estudia+ con un calendario del teléfono
// (Calendar Provider), en los dos sentidos (ver SincronizacionCalendario.kt).
//
// Permisos: READ_CALENDAR y WRITE_CALENDAR (los pide la pantalla antes).
// El destino puede ser:
//  - El calendario local "Estudia+" que crea la app (queda solo en el teléfono).
//  - Uno de Google Calendar que el usuario ya tiene en el teléfono: Google lo
//    sube a la nube y se ve en la web y en otros dispositivos.
// Cada evento que crea la app queda marcado con el paquete de la app y el id
// del evento (CUSTOM_APP_PACKAGE / CUSTOM_APP_URI): así solo se tocan esos y
// nunca los demás eventos del usuario.
class CalendarioTelefono(context: Context) {

    private val contexto = context.applicationContext
    private val resolver = contexto.contentResolver
    private val paquete = contexto.packageName
    private val fotos = contexto.getSharedPreferences("calendario_foto", Context.MODE_PRIVATE)

    data class Resultado(val cantidad: Int, val cambios: List<CambioDesdeCalendario>)

    // Los calendarios de Google del teléfono en los que se puede escribir.
    suspend fun calendariosDeGoogle(): List<CalendarioDisponible> = withContext(Dispatchers.IO) {
        try {
            val lista = mutableListOf<CalendarioDisponible>()
            resolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                    CalendarContract.Calendars.ACCOUNT_NAME,
                    CalendarContract.Calendars.CALENDAR_COLOR
                ),
                "${CalendarContract.Calendars.ACCOUNT_TYPE} = ? AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND ${CalendarContract.Calendars.SYNC_EVENTS} = 1",
                arrayOf(TIPO_GOOGLE, CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
                "${CalendarContract.Calendars.ACCOUNT_NAME}, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME}"
            )?.use { c ->
                while (c.moveToNext()) {
                    lista.add(CalendarioDisponible(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getInt(3).toLong() and 0xFFFFFFFFL))
                }
            }
            lista
        } catch (e: Exception) {
            emptyList()
        }
    }

    // destino: id de un calendario de Google, o null para el local
    // "Estudia+". Devuelve null si falló (por ejemplo, sin permiso).
    suspend fun sincronizar(eventos: List<EventoDeCalendario>, nombreCalendario: String, destino: Long?): Resultado? = withContext(Dispatchers.IO) {
        try {
            val calendarioId = destino?.takeIf { existe(it) } ?: buscarOCrearCalendario(nombreCalendario) ?: return@withContext null
            val esLocal = destino == null || calendarioId != destino
            // Las versiones anteriores no marcaban los eventos: en el calendario
            // propio se borran (todos son de la app) y se vuelven a crear.
            if (esLocal) {
                resolver.delete(
                    CalendarContract.Events.CONTENT_URI,
                    "${CalendarContract.Events.CALENDAR_ID} = ? AND (${CalendarContract.Events.CUSTOM_APP_PACKAGE} IS NULL OR ${CalendarContract.Events.CUSTOM_APP_PACKAGE} != ?)",
                    arrayOf(calendarioId.toString(), paquete)
                )
            }
            val zona = ZoneId.systemDefault()
            val porId = eventos.associateBy { it.evento.id }
            val deseados = porId.mapValues { filaDeEvento(it.value, zona) }
            val (actuales, filas) = leerEventosPropios(calendarioId)
            val foto = leerFoto(calendarioId)
            val plan = planSincronizacion(deseados, actuales, foto)

            plan.insertar.forEach { id -> insertar(calendarioId, id, deseados.getValue(id), zona) }
            plan.actualizar.forEach { id ->
                resolver.update(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, filas.getValue(id)), valores(deseados.getValue(id), zona), null, null)
            }
            plan.borrar.forEach { id ->
                resolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, filas.getValue(id)), null, null)
            }
            // La foto nueva: lo que quedó en el calendario.
            val cambiados = plan.desdeCalendario.associate { it.eventoId to it.fila }
            guardarFoto(calendarioId, deseados.mapValues { (id, fila) -> cambiados[id] ?: fila })
            Resultado(deseados.size, plan.desdeCalendario)
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    // Deja de sincronizar: borra los eventos de la app del calendario de
    // Google (si se usaba uno) y el calendario local "Estudia+".
    suspend fun dejarDeSincronizar(destino: Long?): Boolean = withContext(Dispatchers.IO) {
        try {
            destino?.let { quitarEventosPropios(it) }
            borrarCalendarioLocal()
            fotos.edit().clear().apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Al cambiar de calendario se sacan los eventos del anterior.
    suspend fun quitarDe(destino: Long?) = withContext(Dispatchers.IO) {
        try {
            if (destino == null) borrarCalendarioLocal() else quitarEventosPropios(destino)
            fotos.edit().clear().apply()
        } catch (e: Exception) {
            // Sin permiso: no se puede limpiar, se sigue igual.
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

    // Los eventos de la app en ese calendario: id de Estudia+ -> cómo están,
    // y id de Estudia+ -> id de la fila (para cambiarla o borrarla). Si hay
    // repetidos, sobran y se borran.
    private fun leerEventosPropios(calendarioId: Long): Pair<Map<Int, FilaCalendario>, Map<Int, Long>> {
        val actuales = mutableMapOf<Int, FilaCalendario>()
        val filas = mutableMapOf<Int, Long>()
        resolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.CUSTOM_APP_URI,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.DURATION,
                CalendarContract.Events.RRULE
            ),
            "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.CUSTOM_APP_PACKAGE} = ? AND ${CalendarContract.Events.DELETED} = 0",
            arrayOf(calendarioId.toString(), paquete),
            null
        )?.use { c ->
            while (c.moveToNext()) {
                val fila = c.getLong(0)
                val id = c.getString(1)?.substringAfterLast('/')?.toIntOrNull()
                if (id == null || id in filas) {
                    resolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, fila), null, null)
                    continue
                }
                val duracion = c.getString(5)
                actuales[id] = FilaCalendario(
                    titulo = c.getString(2) ?: "",
                    inicioMillis = c.getLong(3),
                    finMillis = if (c.isNull(4) || duracion != null) null else c.getLong(4),
                    duracion = duracion,
                    regla = c.getString(6)
                )
                filas[id] = fila
            }
        }
        return actuales to filas
    }

    private fun valores(fila: FilaCalendario, zona: ZoneId) = ContentValues().apply {
        put(CalendarContract.Events.TITLE, fila.titulo)
        put(CalendarContract.Events.DTSTART, fila.inicioMillis)
        put(CalendarContract.Events.EVENT_TIMEZONE, zona.id)
        if (fila.regla != null) {
            // Los que se repiten usan DURATION en vez de DTEND.
            putNull(CalendarContract.Events.DTEND)
            put(CalendarContract.Events.DURATION, fila.duracion)
            put(CalendarContract.Events.RRULE, fila.regla)
        } else {
            put(CalendarContract.Events.DTEND, fila.finMillis)
            putNull(CalendarContract.Events.DURATION)
            putNull(CalendarContract.Events.RRULE)
        }
    }

    private fun insertar(calendarioId: Long, eventoId: Int, fila: FilaCalendario, zona: ZoneId) {
        val datos = valores(fila, zona).apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarioId)
            put(CalendarContract.Events.CUSTOM_APP_PACKAGE, paquete)
            put(CalendarContract.Events.CUSTOM_APP_URI, "estudia://evento/$eventoId")
        }
        resolver.insert(CalendarContract.Events.CONTENT_URI, datos)
    }

    private fun quitarEventosPropios(calendarioId: Long) {
        resolver.delete(
            CalendarContract.Events.CONTENT_URI,
            "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.CUSTOM_APP_PACKAGE} = ?",
            arrayOf(calendarioId.toString(), paquete)
        )
    }

    private fun existe(calendarioId: Long): Boolean =
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            "${CalendarContract.Calendars._ID} = ? AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
            arrayOf(calendarioId.toString(), CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            null
        )?.use { it.moveToFirst() } ?: false

    // ---------- Foto de la última sincronización (por calendario) ----------

    private fun leerFoto(calendarioId: Long): Map<Int, FilaCalendario> {
        val texto = fotos.getString("cal_$calendarioId", null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(texto)
            json.keys().asSequence().associate { clave ->
                val f = json.getJSONObject(clave)
                clave.toInt() to FilaCalendario(
                    f.getString("t"), f.getLong("i"),
                    if (f.has("f")) f.getLong("f") else null,
                    f.optString("d").ifEmpty { null },
                    f.optString("r").ifEmpty { null }
                )
            }
        }.getOrDefault(emptyMap())
    }

    private fun guardarFoto(calendarioId: Long, filas: Map<Int, FilaCalendario>) {
        val json = JSONObject()
        filas.forEach { (id, f) ->
            json.put(id.toString(), JSONObject().put("t", f.titulo).put("i", f.inicioMillis).apply {
                f.finMillis?.let { put("f", it) }
                f.duracion?.let { put("d", it) }
                f.regla?.let { put("r", it) }
            })
        }
        fotos.edit().putString("cal_$calendarioId", json.toString()).apply()
    }

    // ---------- Calendario local "Estudia+" ----------

    private fun uriComoAdaptador(): Uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CUENTA)
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        .build()

    private fun borrarCalendarioLocal() {
        resolver.delete(
            uriComoAdaptador(),
            "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
            arrayOf(CUENTA, CalendarContract.ACCOUNT_TYPE_LOCAL)
        )
    }

    // Si no existe, se crea. Se crea como "adaptador de sincronización" (así
    // lo pide Android para calendarios locales).
    private fun buscarOCrearCalendario(nombre: String): Long? {
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
            arrayOf(CUENTA, CalendarContract.ACCOUNT_TYPE_LOCAL),
            null
        )?.use { cursor -> if (cursor.moveToFirst()) return cursor.getLong(0) }

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
        return resolver.insert(uriComoAdaptador(), valores)?.lastPathSegment?.toLongOrNull()
    }

    private companion object {
        const val CUENTA = "Estudia+"
        const val TIPO_GOOGLE = "com.google"
    }
}
