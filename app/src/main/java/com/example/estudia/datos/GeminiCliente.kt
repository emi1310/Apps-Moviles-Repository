package com.example.estudia.datos

import com.example.estudia.modelo.ErrorIA
import com.example.estudia.modelo.INSTRUCCION_ASISTENTE
import com.example.estudia.modelo.MensajeIA
import com.example.estudia.modelo.ResultadoIA
import com.example.estudia.modelo.TarjetaSugerida
import com.example.estudia.modelo.contextoParaEnviar
import com.example.estudia.modelo.limpiarRespuesta
import com.example.estudia.modelo.limpiarTarjetas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// El asistente IA de la app. Es una interfaz para que los tests usen uno
// falso sin internet. La clave llega en cada pedido: es la del usuario que
// está usando la app (o la de la app, si él no conectó la suya).
interface AsistenteIA {
    // Responde al último mensaje de la conversación (que es del usuario).
    suspend fun responder(clave: String, conversacion: List<MensajeIA>): ResultadoIA<String>

    // Pide tarjetas de estudio (pregunta y respuesta) en JSON.
    suspend fun crearTarjetas(clave: String, pedido: String): ResultadoIA<List<TarjetaSugerida>>
}

// Asistente con la API de Gemini (plan gratis de cada usuario). Usa generateContent por
// HTTP, como los feriados, sin otra librería. La clave viaja en el
// encabezado x-goog-api-key (no en la URL).
class GeminiCliente(private val modelo: String) : AsistenteIA {

    override suspend fun responder(clave: String, conversacion: List<MensajeIA>): ResultadoIA<String> {
        val contenidos = JSONArray()
        contextoParaEnviar(conversacion).forEach { m ->
            contenidos.put(JSONObject().put("role", if (m.deUsuario) "user" else "model").put("parts", JSONArray().put(JSONObject().put("text", m.texto))))
        }
        return when (val r = pedir(clave, contenidos, null)) {
            is ResultadoIA.Ok -> ResultadoIA.Ok(limpiarRespuesta(r.valor))
            is ResultadoIA.Falla -> r
        }
    }

    override suspend fun crearTarjetas(clave: String, pedido: String): ResultadoIA<List<TarjetaSugerida>> {
        val contenidos = JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", pedido))))
        return when (val r = pedir(clave, contenidos, com.example.estudia.modelo.ESQUEMA_TARJETAS)) {
            is ResultadoIA.Falla -> r
            is ResultadoIA.Ok -> try {
                val lista = JSONObject(r.valor).getJSONArray("tarjetas")
                val tarjetas = (0 until lista.length()).map { i ->
                    val t = lista.getJSONObject(i)
                    TarjetaSugerida(t.optString("pregunta"), t.optString("respuesta"))
                }
                ResultadoIA.Ok(limpiarTarjetas(tarjetas))
            } catch (e: Exception) {
                ResultadoIA.Falla(ErrorIA.OTRO)
            }
        }
    }

    // esquemaJson != null pide la respuesta en JSON con ese esquema.
    private suspend fun pedir(clave: String, contenidos: JSONArray, esquemaJson: String?): ResultadoIA<String> = withContext(Dispatchers.IO) {
        if (clave.isBlank()) return@withContext ResultadoIA.Falla(ErrorIA.SIN_CLAVE)
        val configuracion = JSONObject()
        if (esquemaJson != null) {
            configuracion.put("responseMimeType", "application/json").put("responseJsonSchema", JSONObject(esquemaJson))
        }
        val cuerpo = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", INSTRUCCION_ASISTENTE))))
            .put("contents", contenidos)
            .put("generationConfig", configuracion)
        var conexion: HttpURLConnection? = null
        try {
            conexion = (URL("https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("x-goog-api-key", clave)
            }
            conexion.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
            val codigo = conexion.responseCode
            if (codigo != HttpURLConnection.HTTP_OK) {
                return@withContext ResultadoIA.Falla(
                    when (codigo) {
                        400, 401, 403 -> if (codigo == 400 && !errorDeClave(conexion)) ErrorIA.OTRO else ErrorIA.CLAVE_INVALIDA
                        429 -> ErrorIA.LIMITE
                        else -> ErrorIA.OTRO
                    }
                )
            }
            val respuesta = JSONObject(conexion.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            val candidato = respuesta.optJSONArray("candidates")?.optJSONObject(0)
                ?: return@withContext ResultadoIA.Falla(ErrorIA.BLOQUEADO)
            val partes = candidato.optJSONObject("content")?.optJSONArray("parts")
            val texto = (0 until (partes?.length() ?: 0))
                .mapNotNull { partes!!.optJSONObject(it) }
                .filter { !it.optBoolean("thought") }
                .joinToString("") { it.optString("text") }
            if (texto.isBlank()) ResultadoIA.Falla(if (candidato.optString("finishReason") == "SAFETY") ErrorIA.BLOQUEADO else ErrorIA.OTRO)
            else ResultadoIA.Ok(texto)
        } catch (e: IOException) {
            ResultadoIA.Falla(ErrorIA.SIN_INTERNET)
        } catch (e: Exception) {
            ResultadoIA.Falla(ErrorIA.OTRO)
        } finally {
            conexion?.disconnect()
        }
    }

    // Un 400 por clave mal copiada dice "API key" en el mensaje de error.
    private fun errorDeClave(conexion: HttpURLConnection): Boolean =
        runCatching { conexion.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty().contains("API key", ignoreCase = true) }.getOrDefault(false)
}
