package com.example.estudia.datos

import android.content.Context
import com.example.estudia.modelo.CONVERSACIONES_A_CONSERVAR
import com.example.estudia.modelo.ConversacionGuardada
import com.example.estudia.modelo.MensajeGuardado
import com.example.estudia.modelo.ResultadoSimulacro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

// Lo que el Asistente guarda de cada usuario: sus charlas y los resultados de
// sus simulacros. Es una interfaz para que los tests usen una en memoria.
interface HistorialIA {
    fun conversaciones(usuarioId: Int): Flow<List<ConversacionGuardada>>
    // Agrega o reemplaza (por id). Se conservan las más nuevas.
    suspend fun guardarConversacion(usuarioId: Int, conversacion: ConversacionGuardada)
    suspend fun borrarConversacion(usuarioId: Int, id: Long)

    fun simulacros(usuarioId: Int): Flow<List<ResultadoSimulacro>>
    suspend fun guardarSimulacro(usuarioId: Int, resultado: ResultadoSimulacro)
}

// Un archivo JSON por usuario en el almacenamiento interno de la app (no sale
// del teléfono y se borra al desinstalar). Es poco y se lee entero.
class HistorialIAEnArchivo(context: Context) : HistorialIA {

    private val carpeta = File(context.filesDir, "asistente").apply { mkdirs() }
    private val cerrojo = Mutex()
    // Lo leído de cada usuario, para que las pantallas se enteren de los cambios.
    private val cache = mutableMapOf<Int, MutableStateFlow<Datos?>>()

    private data class Datos(val conversaciones: List<ConversacionGuardada>, val simulacros: List<ResultadoSimulacro>)

    private fun estado(usuarioId: Int) = synchronized(cache) { cache.getOrPut(usuarioId) { MutableStateFlow(null) } }

    private fun archivo(usuarioId: Int) = File(carpeta, "u$usuarioId.json")

    private suspend fun datos(usuarioId: Int): Datos = cerrojo.withLock {
        estado(usuarioId).value ?: withContext(Dispatchers.IO) { leer(usuarioId) }.also { estado(usuarioId).value = it }
    }

    private suspend fun cambiar(usuarioId: Int, cambio: (Datos) -> Datos) {
        val nuevos = cambio(datos(usuarioId))
        cerrojo.withLock {
            withContext(Dispatchers.IO) { escribir(usuarioId, nuevos) }
            estado(usuarioId).value = nuevos
        }
    }

    override fun conversaciones(usuarioId: Int): Flow<List<ConversacionGuardada>> =
        estado(usuarioId).onStart { datos(usuarioId) }.map { it?.conversaciones.orEmpty().sortedByDescending { c -> c.actualizada } }

    override suspend fun guardarConversacion(usuarioId: Int, conversacion: ConversacionGuardada) = cambiar(usuarioId) { d ->
        d.copy(conversaciones = (d.conversaciones.filter { it.id != conversacion.id } + conversacion)
            .sortedByDescending { it.actualizada }.take(CONVERSACIONES_A_CONSERVAR))
    }

    override suspend fun borrarConversacion(usuarioId: Int, id: Long) = cambiar(usuarioId) { d ->
        d.copy(conversaciones = d.conversaciones.filter { it.id != id })
    }

    override fun simulacros(usuarioId: Int): Flow<List<ResultadoSimulacro>> =
        estado(usuarioId).onStart { datos(usuarioId) }.map { it?.simulacros.orEmpty() }

    override suspend fun guardarSimulacro(usuarioId: Int, resultado: ResultadoSimulacro) = cambiar(usuarioId) { d ->
        d.copy(simulacros = (d.simulacros + resultado).takeLast(100))
    }

    // ---------- JSON ----------

    private fun leer(usuarioId: Int): Datos {
        val texto = archivo(usuarioId).takeIf { it.exists() }?.readText() ?: return Datos(emptyList(), emptyList())
        return try {
            val json = JSONObject(texto)
            Datos(
                conversaciones = json.optJSONArray("conversaciones").objetos().map { c ->
                    ConversacionGuardada(
                        id = c.getLong("id"),
                        titulo = c.optString("titulo"),
                        materia = c.optString("materia").ifEmpty { null },
                        actualizada = LocalDateTime.parse(c.getString("actualizada")),
                        mensajes = c.optJSONArray("mensajes").objetos().map { m ->
                            MensajeGuardado(m.getBoolean("usuario"), m.optString("texto"), m.optString("mostrado"), m.optJSONArray("fuentes").textos())
                        }
                    )
                },
                simulacros = json.optJSONArray("simulacros").objetos().map { s ->
                    ResultadoSimulacro(
                        fecha = LocalDate.parse(s.getString("fecha")),
                        materia = s.optString("materia"),
                        notas = s.optJSONArray("notas").textos(),
                        nota = s.optDouble("nota", 1.0),
                        temasFlojos = s.optJSONArray("temas").textos()
                    )
                }
            )
        } catch (e: Exception) {
            // Archivo dañado: se empieza de nuevo antes que trabar el Asistente.
            Datos(emptyList(), emptyList())
        }
    }

    private fun escribir(usuarioId: Int, datos: Datos) {
        val json = JSONObject()
            .put("conversaciones", JSONArray().apply {
                datos.conversaciones.forEach { c ->
                    put(JSONObject().put("id", c.id).put("titulo", c.titulo).put("materia", c.materia.orEmpty())
                        .put("actualizada", c.actualizada.toString())
                        .put("mensajes", JSONArray().apply {
                            c.mensajes.forEach { m ->
                                put(JSONObject().put("usuario", m.deUsuario).put("texto", m.texto).put("mostrado", m.mostrado)
                                    .put("fuentes", JSONArray(m.fuentes)))
                            }
                        }))
                }
            })
            .put("simulacros", JSONArray().apply {
                datos.simulacros.forEach { s ->
                    put(JSONObject().put("fecha", s.fecha.toString()).put("materia", s.materia).put("notas", JSONArray(s.notas))
                        .put("nota", s.nota).put("temas", JSONArray(s.temasFlojos)))
                }
            })
        // Se escribe en uno temporal y se renombra: si se corta, no queda a medias.
        val temporal = File(carpeta, "u$usuarioId.tmp")
        temporal.writeText(json.toString())
        temporal.renameTo(archivo(usuarioId)) || run { archivo(usuarioId).writeText(json.toString()); true }
    }

    private fun JSONArray?.objetos(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
    private fun JSONArray?.textos(): List<String> = if (this == null) emptyList() else (0 until length()).map { optString(it) }
}
