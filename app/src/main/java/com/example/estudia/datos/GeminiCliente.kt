package com.example.estudia.datos

import android.util.Base64
import com.example.estudia.modelo.ClaseHorarioTexto
import com.example.estudia.modelo.ESQUEMA_HORARIO
import com.example.estudia.modelo.ESQUEMA_MAPA
import com.example.estudia.modelo.ESQUEMA_PLAN
import com.example.estudia.modelo.ErrorIA
import com.example.estudia.modelo.RamaIA
import com.example.estudia.modelo.SesionPlanTexto
import com.example.estudia.modelo.INSTRUCCION_ASISTENTE
import com.example.estudia.modelo.instruccionConDatos
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
    // contexto: los datos cargados del estudiante (vacío = no se mandan).
    suspend fun responder(clave: String, conversacion: List<MensajeIA>, contexto: String = ""): ResultadoIA<String>

    // Pide tarjetas de estudio (pregunta y respuesta) en JSON.
    suspend fun crearTarjetas(clave: String, pedido: String): ResultadoIA<List<TarjetaSugerida>>

    // Plan de estudio para un examen (sesiones con fecha, hora, minutos y
    // tema). contexto: la agenda del estudiante, para no pisar sus clases.
    suspend fun planEstudio(clave: String, pedido: String, contexto: String): ResultadoIA<List<SesionPlanTexto>>

    // Mapa mental: la idea central y sus ramas.
    suspend fun mapaMental(clave: String, pedido: String): ResultadoIA<Pair<String, List<RamaIA>>>

    // Lee una imagen (JPEG) y responde el pedido (por ejemplo, pasar un apunte a texto).
    suspend fun leerImagen(clave: String, jpeg: ByteArray, pedido: String): ResultadoIA<String>

    // Lee la foto de un horario de cursada y devuelve sus clases.
    suspend fun leerHorario(clave: String, jpeg: ByteArray, pedido: String): ResultadoIA<List<ClaseHorarioTexto>>
}

// Asistente con la API de Gemini (plan gratis de cada usuario). Usa generateContent por
// HTTP, como los feriados, sin otra librería. La clave viaja en el
// encabezado x-goog-api-key (no en la URL).
class GeminiCliente(private val modelo: String) : AsistenteIA {

    override suspend fun responder(clave: String, conversacion: List<MensajeIA>, contexto: String): ResultadoIA<String> {
        val contenidos = JSONArray()
        contextoParaEnviar(conversacion).forEach { m ->
            contenidos.put(JSONObject().put("role", if (m.deUsuario) "user" else "model").put("parts", JSONArray().put(JSONObject().put("text", m.texto))))
        }
        return when (val r = pedir(clave, contenidos, null, instruccionConDatos(contexto))) {
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

    override suspend fun planEstudio(clave: String, pedido: String, contexto: String): ResultadoIA<List<SesionPlanTexto>> =
        when (val r = pedir(clave, pedidoUsuario(pedido), ESQUEMA_PLAN, instruccionConDatos(contexto))) {
            is ResultadoIA.Falla -> r
            is ResultadoIA.Ok -> try {
                val lista = JSONObject(r.valor).getJSONArray("sesiones")
                ResultadoIA.Ok((0 until lista.length()).map { i ->
                    val s = lista.getJSONObject(i)
                    SesionPlanTexto(s.optString("fecha"), s.optString("hora"), s.optInt("minutos", 60), s.optString("tema"))
                })
            } catch (e: Exception) {
                ResultadoIA.Falla(ErrorIA.OTRO)
            }
        }

    override suspend fun mapaMental(clave: String, pedido: String): ResultadoIA<Pair<String, List<RamaIA>>> =
        when (val r = pedir(clave, pedidoUsuario(pedido), ESQUEMA_MAPA)) {
            is ResultadoIA.Falla -> r
            is ResultadoIA.Ok -> try {
                val json = JSONObject(r.valor)
                val ramas = json.getJSONArray("ramas")
                ResultadoIA.Ok(json.optString("central") to (0 until ramas.length()).map { i ->
                    val rama = ramas.getJSONObject(i)
                    val detalles = rama.optJSONArray("detalles") ?: JSONArray()
                    RamaIA(rama.optString("idea"), (0 until detalles.length()).map { detalles.optString(it) })
                })
            } catch (e: Exception) {
                ResultadoIA.Falla(ErrorIA.OTRO)
            }
        }

    override suspend fun leerImagen(clave: String, jpeg: ByteArray, pedido: String): ResultadoIA<String> {
        return when (val r = pedir(clave, conImagen(jpeg, pedido), null)) {
            is ResultadoIA.Ok -> ResultadoIA.Ok(limpiarRespuesta(r.valor))
            is ResultadoIA.Falla -> r
        }
    }

    override suspend fun leerHorario(clave: String, jpeg: ByteArray, pedido: String): ResultadoIA<List<ClaseHorarioTexto>> =
        when (val r = pedir(clave, conImagen(jpeg, pedido), ESQUEMA_HORARIO)) {
            is ResultadoIA.Falla -> r
            is ResultadoIA.Ok -> try {
                val lista = JSONObject(r.valor).getJSONArray("clases")
                ResultadoIA.Ok((0 until lista.length()).map { i ->
                    val c = lista.getJSONObject(i)
                    ClaseHorarioTexto(c.optString("materia"), c.optString("titulo"), c.optString("dia"), c.optString("inicio"), c.optString("fin"), c.optString("lugar"))
                })
            } catch (e: Exception) {
                ResultadoIA.Falla(ErrorIA.OTRO)
            }
        }

    // La foto (JPEG) y el pedido en un mismo mensaje.
    private fun conImagen(jpeg: ByteArray, pedido: String): JSONArray {
        val imagen = JSONObject().put("inlineData", JSONObject().put("mimeType", "image/jpeg").put("data", Base64.encodeToString(jpeg, Base64.NO_WRAP)))
        return JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(imagen).put(JSONObject().put("text", pedido))))
    }

    private fun pedidoUsuario(pedido: String) =
        JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", pedido))))

    // esquemaJson != null pide la respuesta en JSON con ese esquema.
    private suspend fun pedir(clave: String, contenidos: JSONArray, esquemaJson: String?, instruccion: String = INSTRUCCION_ASISTENTE): ResultadoIA<String> = withContext(Dispatchers.IO) {
        if (clave.isBlank()) return@withContext ResultadoIA.Falla(ErrorIA.SIN_CLAVE)
        val configuracion = JSONObject()
        if (esquemaJson != null) {
            configuracion.put("responseMimeType", "application/json").put("responseJsonSchema", JSONObject(esquemaJson))
        }
        val cuerpo = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instruccion))))
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
