package com.example.estudia.datos

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import com.example.estudia.modelo.Feriado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

// De dónde se traen los feriados.
//
// 1) El calendario de feriados de la cuenta de Google del teléfono (el que
//    Google Calendar muestra como "Feriados en Argentina"). Se lee con el
//    Calendar Provider y necesita el permiso READ_CALENDAR. Google lo
//    sincroniza con un id que termina en "#holiday@group.v.calendar.google.com".
// 2) Si ese calendario no está (o no hay permiso), una API pública de
//    feriados por país (date.nager.at). Solo se manda el año y el país.
class FuenteFeriados(context: Context) {

    private val resolver = context.applicationContext.contentResolver

    // Feriados del calendario de Google del teléfono entre dos fechas.
    // null = no hay calendario de feriados o no se pudo leer (sin permiso).
    suspend fun delCalendario(desde: LocalDate, hasta: LocalDate): List<Feriado>? = withContext(Dispatchers.IO) {
        try {
            val calendarios = calendariosDeFeriados()
            if (calendarios.isEmpty()) return@withContext null
            val zona = ZoneId.systemDefault()
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
                ContentUris.appendId(it, desde.atStartOfDay(zona).toInstant().toEpochMilli())
                ContentUris.appendId(it, hasta.plusDays(1).atStartOfDay(zona).toInstant().toEpochMilli())
            }.build()
            val feriados = mutableListOf<Feriado>()
            resolver.query(
                uri,
                arrayOf(CalendarContract.Instances.CALENDAR_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.ALL_DAY),
                null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getLong(0) !in calendarios) continue
                    val titulo = cursor.getString(1)?.trim().orEmpty()
                    // Los eventos de todo el día se guardan a las 00:00 UTC.
                    val zonaDia = if (cursor.getInt(3) == 1) ZoneOffset.UTC else zona
                    val fecha = Instant.ofEpochMilli(cursor.getLong(2)).atZone(zonaDia).toLocalDate()
                    if (titulo.isNotEmpty()) feriados.add(Feriado(fecha, titulo))
                }
            }
            juntarPorDia(feriados)
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    // Ids de los calendarios del teléfono que son de feriados.
    private fun calendariosDeFeriados(): Set<Long> {
        val ids = mutableSetOf<Long>()
        resolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.OWNER_ACCOUNT, CalendarContract.Calendars.NAME, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val textos = (1..3).map { cursor.getString(it).orEmpty().lowercase(Locale.ROOT) }
                if (textos.any { esCalendarioDeFeriados(it) }) ids.add(cursor.getLong(0))
            }
        }
        return ids
    }

    // Feriados de un año desde internet. null = sin conexión o error.
    suspend fun deInternet(anio: Int, pais: String = paisDelTelefono()): List<Feriado>? = withContext(Dispatchers.IO) {
        var conexion: HttpURLConnection? = null
        try {
            conexion = (URL("https://date.nager.at/api/v3/PublicHolidays/$anio/$pais").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
            }
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val texto = conexion.inputStream.bufferedReader().use { it.readText() }
            juntarPorDia(leerJsonNager(texto))
        } catch (e: Exception) {
            null
        } finally {
            conexion?.disconnect()
        }
    }

    companion object {
        // El de Google Calendar tiene un id como
        // "es.ar#holiday@group.v.calendar.google.com"; por las dudas también
        // se reconocen por el nombre.
        fun esCalendarioDeFeriados(texto: String): Boolean {
            val t = texto.lowercase(Locale.ROOT)
            return "#holiday@" in t || "feriado" in t || "holiday" in t || "festivo" in t
        }

        // Respuesta de date.nager.at: [{"date":"2026-07-09","localName":"Día de la Independencia",...}]
        fun leerJsonNager(texto: String): List<Feriado> {
            val lista = JSONArray(texto)
            return (0 until lista.length()).mapNotNull { i ->
                val item = lista.getJSONObject(i)
                val fecha = runCatching { LocalDate.parse(item.getString("date")) }.getOrNull() ?: return@mapNotNull null
                val nombre = item.optString("localName").ifBlank { item.optString("name") }
                Feriado(fecha, nombre)
            }
        }

        // Si un día tiene dos nombres (pasa con los puentes), se juntan.
        fun juntarPorDia(feriados: List<Feriado>): List<Feriado> =
            feriados.groupBy { it.fecha }
                .map { (fecha, delDia) -> Feriado(fecha, delDia.map { it.nombre }.distinct().joinToString(" · ")) }
                .sortedBy { it.fecha }

        // País del teléfono (ej: "AR"); si no tiene, Argentina.
        fun paisDelTelefono(): String = Locale.getDefault().country.takeIf { it.length == 2 } ?: "AR"
    }
}
