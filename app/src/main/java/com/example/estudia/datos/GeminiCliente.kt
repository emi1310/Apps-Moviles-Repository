package com.example.estudia.datos

import android.util.Base64
import com.example.estudia.modelo.ClaseHorarioTexto
import com.example.estudia.modelo.DECLARACION_FUNCIONES
import com.example.estudia.modelo.FUNCION_BUSCAR
import com.example.estudia.modelo.FuenteWeb
import com.example.estudia.modelo.MAXIMO_BUSQUEDAS
import com.example.estudia.modelo.MAXIMO_FUENTES_WEB
import com.example.estudia.modelo.pedidoBusqueda
import com.example.estudia.modelo.ESQUEMA_EVALUACION
import com.example.estudia.modelo.ESQUEMA_HORARIO
import com.example.estudia.modelo.ESQUEMA_MAPA
import com.example.estudia.modelo.ESQUEMA_PLAN
import com.example.estudia.modelo.ESQUEMA_TARJETAS
import com.example.estudia.modelo.ErrorIA
import com.example.estudia.modelo.EvaluacionSimulacro
import com.example.estudia.modelo.INSTRUCCION_ASISTENTE
import com.example.estudia.modelo.MensajeIA
import com.example.estudia.modelo.PiezaRespuesta
import com.example.estudia.modelo.RamaIA
import com.example.estudia.modelo.ResultadoIA
import com.example.estudia.modelo.SesionPlanTexto
import com.example.estudia.modelo.TarjetaSugerida
import com.example.estudia.modelo.accionDesdeLlamada
import com.example.estudia.modelo.contextoParaEnviar
import com.example.estudia.modelo.instruccionConDatos
import com.example.estudia.modelo.limpiarRespuesta
import com.example.estudia.modelo.limpiarTarjetas
import com.example.estudia.modelo.textoParaIA
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
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
    // Responde de una vez al último mensaje (se usa para probar una clave).
    suspend fun responder(clave: String, conversacion: List<MensajeIA>, contexto: String = ""): ResultadoIA<String>

    // La charla: la respuesta llega en vivo, de a pedazos. Con las fotos o el
    // PDF de los mensajes, y con las funciones para proponer acciones.
    // contexto: los datos cargados del estudiante (y fragmentos de sus notas).
    fun conversar(clave: String, conversacion: List<MensajeIA>, contexto: String): Flow<PiezaRespuesta>

    // Pide tarjetas de estudio (pregunta y respuesta) en JSON.
    suspend fun crearTarjetas(clave: String, pedido: String): ResultadoIA<List<TarjetaSugerida>>

    // Plan de estudio para un examen (sesiones con fecha, hora, minutos y
    // tema). contexto: la agenda del estudiante, para no pisar sus clases.
    suspend fun planEstudio(clave: String, pedido: String, contexto: String): ResultadoIA<List<SesionPlanTexto>>

    // Mapa mental: la idea central y sus ramas.
    suspend fun mapaMental(clave: String, pedido: String): ResultadoIA<Pair<String, List<RamaIA>>>

    // Lee la foto de un horario de cursada y devuelve sus clases.
    suspend fun leerHorario(clave: String, jpeg: ByteArray, pedido: String): ResultadoIA<List<ClaseHorarioTexto>>

    // Evalúa un simulacro terminado: nota, temas flojos y tarjetas.
    suspend fun evaluarSimulacro(clave: String, pedido: String): ResultadoIA<EvaluacionSimulacro>
}

// Asistente con la API de Gemini (plan gratis de cada usuario). Usa generateContent por
// HTTP, como los feriados, sin otra librería. La clave viaja en el
// encabezado x-goog-api-key (no en la URL).
class GeminiCliente(private val modelo: String) : AsistenteIA {

    override suspend fun responder(clave: String, conversacion: List<MensajeIA>, contexto: String): ResultadoIA<String> =
        when (val r = pedir(clave, contenidos(conversacion), null, instruccionConDatos(contexto))) {
            is ResultadoIA.Ok -> ResultadoIA.Ok(limpiarRespuesta(r.valor))
            is ResultadoIA.Falla -> r
        }

    // Con streamGenerateContent (alt=sse) Gemini manda la respuesta en eventos
    // "data: {...}" a medida que la escribe.
    //
    // Si la IA pide buscar en internet (la función buscar_en_internet), la
    // app hace la búsqueda con Google aparte, le devuelve lo que encontró y
    // la IA sigue respondiendo con eso (hasta 2 búsquedas por respuesta).
    override fun conversar(clave: String, conversacion: List<MensajeIA>, contexto: String): Flow<PiezaRespuesta> = channelFlow {
        if (clave.isBlank()) {
            send(PiezaRespuesta.Error(ErrorIA.SIN_CLAVE))
            return@channelFlow
        }
        val contenidos = contenidos(conversacion)
        val instruccion = instruccionConDatos(contexto)
        withContext(Dispatchers.IO) {
            var llegoAlgo = false
            var bloqueado = false
            var busquedas = 0
            try {
                while (true) {
                    val cuerpo = cuerpo(contenidos, null, instruccion).put("tools", JSONArray(DECLARACION_FUNCIONES))
                    var conexion: HttpURLConnection? = null
                    // Lo que dijo la IA en esta vuelta (para devolvérselo si pidió buscar).
                    val textoDeLaVuelta = StringBuilder()
                    var pedidoDeBusqueda: JSONObject? = null
                    var consulta = ""
                    try {
                        conexion = abrir(clave, "streamGenerateContent?alt=sse")
                        conexion.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
                        val codigo = conexion.responseCode
                        if (codigo != HttpURLConnection.HTTP_OK) {
                            send(PiezaRespuesta.Error(errorDeCodigo(codigo, conexion)))
                            return@withContext
                        }
                        conexion.inputStream.bufferedReader(Charsets.UTF_8).use { lector ->
                            while (true) {
                                val linea = lector.readLine() ?: break
                                if (!linea.startsWith("data:")) continue
                                val evento = JSONObject(linea.removePrefix("data:").trim())
                                val candidato = evento.optJSONArray("candidates")?.optJSONObject(0) ?: continue
                                if (candidato.optString("finishReason") == "SAFETY") bloqueado = true
                                val partes = candidato.optJSONObject("content")?.optJSONArray("parts") ?: continue
                                for (i in 0 until partes.length()) {
                                    val parte = partes.optJSONObject(i) ?: continue
                                    if (parte.optBoolean("thought")) continue
                                    val llamada = parte.optJSONObject("functionCall")
                                    if (llamada != null) {
                                        val args = llamada.optJSONObject("args") ?: JSONObject()
                                        val mapa = args.keys().asSequence().associateWith { args.opt(it)?.toString().orEmpty() }
                                        if (llamada.optString("name") == FUNCION_BUSCAR) {
                                            // Se guarda la parte entera (con su firma, si trae).
                                            if (pedidoDeBusqueda == null) {
                                                pedidoDeBusqueda = parte
                                                consulta = mapa["consulta"].orEmpty().trim()
                                            }
                                        } else {
                                            accionDesdeLlamada(llamada.optString("name"), mapa)?.let {
                                                llegoAlgo = true
                                                send(PiezaRespuesta.Accion(it))
                                            }
                                        }
                                    } else {
                                        val texto = parte.optString("text")
                                        if (texto.isNotEmpty()) {
                                            llegoAlgo = true
                                            textoDeLaVuelta.append(texto)
                                            send(PiezaRespuesta.Texto(texto))
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        conexion?.disconnect()
                    }
                    val pedido = pedidoDeBusqueda
                    if (pedido == null || consulta.isEmpty() || busquedas >= MAXIMO_BUSQUEDAS) break
                    busquedas++
                    send(PiezaRespuesta.Buscando(consulta))
                    val encontrado = buscarEnInternet(clave, consulta)
                    if (encontrado != null && encontrado.second.isNotEmpty()) send(PiezaRespuesta.FuentesWeb(encontrado.second))
                    // Se le devuelve a la IA su pedido y lo que se encontró.
                    val partesModelo = JSONArray()
                    if (textoDeLaVuelta.isNotEmpty()) partesModelo.put(JSONObject().put("text", textoDeLaVuelta.toString()))
                    partesModelo.put(pedido)
                    contenidos.put(JSONObject().put("role", "model").put("parts", partesModelo))
                    val resultado = encontrado?.first ?: "No se pudo buscar en internet en este momento. Respondé con lo que sepas y aclaralo."
                    contenidos.put(
                        JSONObject().put("role", "user").put(
                            "parts",
                            JSONArray().put(
                                JSONObject().put(
                                    "functionResponse",
                                    JSONObject().put("name", FUNCION_BUSCAR).put("response", JSONObject().put("resultado", resultado))
                                )
                            )
                        )
                    )
                }
                if (!llegoAlgo) send(PiezaRespuesta.Error(if (bloqueado) ErrorIA.BLOQUEADO else ErrorIA.OTRO))
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                send(PiezaRespuesta.Error(ErrorIA.SIN_INTERNET))
            } catch (e: Exception) {
                send(PiezaRespuesta.Error(ErrorIA.OTRO))
            }
        }
    }

    // Búsqueda en Google (la herramienta google_search de Gemini): devuelve un
    // resumen de lo encontrado y las páginas de donde sale. null si falló.
    private fun buscarEnInternet(clave: String, consulta: String): Pair<String, List<FuenteWeb>>? {
        val cuerpo = JSONObject()
            .put("contents", pedidoUsuario(pedidoBusqueda(consulta)))
            .put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))
        var conexion: HttpURLConnection? = null
        return try {
            conexion = abrir(clave, "generateContent")
            conexion.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) return null
            val respuesta = JSONObject(conexion.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            val candidato = respuesta.optJSONArray("candidates")?.optJSONObject(0) ?: return null
            val partes = candidato.optJSONObject("content")?.optJSONArray("parts")
            val texto = (0 until (partes?.length() ?: 0))
                .mapNotNull { partes!!.optJSONObject(it) }
                .filter { !it.optBoolean("thought") }
                .joinToString("") { it.optString("text") }
                .trim()
            val trozos = candidato.optJSONObject("groundingMetadata")?.optJSONArray("groundingChunks")
            val fuentes = (0 until (trozos?.length() ?: 0)).mapNotNull { i ->
                val web = trozos!!.optJSONObject(i)?.optJSONObject("web") ?: return@mapNotNull null
                val url = web.optString("uri")
                if (url.isBlank()) null else FuenteWeb(web.optString("title").ifBlank { url }, url)
            }.distinctBy { it.titulo }.take(MAXIMO_FUENTES_WEB)
            if (texto.isBlank()) null else texto to fuentes
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } finally {
            conexion?.disconnect()
        }
    }

    override suspend fun crearTarjetas(clave: String, pedido: String): ResultadoIA<List<TarjetaSugerida>> =
        when (val r = pedir(clave, pedidoUsuario(pedido), ESQUEMA_TARJETAS)) {
            is ResultadoIA.Falla -> r
            is ResultadoIA.Ok -> try {
                ResultadoIA.Ok(limpiarTarjetas(tarjetasDe(JSONObject(r.valor).getJSONArray("tarjetas"))))
            } catch (e: Exception) {
                ResultadoIA.Falla(ErrorIA.OTRO)
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

    override suspend fun leerHorario(clave: String, jpeg: ByteArray, pedido: String): ResultadoIA<List<ClaseHorarioTexto>> {
        val contenidos = JSONArray().put(
            JSONObject().put("role", "user").put("parts", JSONArray().put(datosEnLinea("image/jpeg", jpeg)).put(JSONObject().put("text", pedido)))
        )
        return when (val r = pedir(clave, contenidos, ESQUEMA_HORARIO)) {
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
    }

    override suspend fun evaluarSimulacro(clave: String, pedido: String): ResultadoIA<EvaluacionSimulacro> =
        when (val r = pedir(clave, pedidoUsuario(pedido), ESQUEMA_EVALUACION)) {
            is ResultadoIA.Falla -> r
            is ResultadoIA.Ok -> try {
                val json = JSONObject(r.valor)
                val temas = json.optJSONArray("temas_flojos") ?: JSONArray()
                ResultadoIA.Ok(
                    EvaluacionSimulacro(
                        nota = json.optDouble("nota", 1.0),
                        resumen = json.optString("resumen"),
                        temasFlojos = (0 until temas.length()).map { temas.optString(it) },
                        tarjetas = tarjetasDe(json.optJSONArray("tarjetas") ?: JSONArray())
                    )
                )
            } catch (e: Exception) {
                ResultadoIA.Falla(ErrorIA.OTRO)
            }
        }

    // ---------- Armado de los pedidos ----------

    // La charla como la pide Gemini: cada mensaje con sus archivos y su texto.
    private fun contenidos(conversacion: List<MensajeIA>): JSONArray {
        val contenidos = JSONArray()
        contextoParaEnviar(conversacion).forEach { m ->
            val partes = JSONArray()
            m.adjuntos.forEach { partes.put(datosEnLinea(it.tipoMime, it.bytes)) }
            partes.put(JSONObject().put("text", textoParaIA(m)))
            contenidos.put(JSONObject().put("role", if (m.deUsuario) "user" else "model").put("parts", partes))
        }
        return contenidos
    }

    private fun datosEnLinea(tipoMime: String, bytes: ByteArray) =
        JSONObject().put("inlineData", JSONObject().put("mimeType", tipoMime).put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)))

    private fun pedidoUsuario(pedido: String) =
        JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", pedido))))

    private fun tarjetasDe(lista: JSONArray): List<TarjetaSugerida> = (0 until lista.length()).map { i ->
        val t = lista.getJSONObject(i)
        TarjetaSugerida(t.optString("pregunta"), t.optString("respuesta"))
    }

    // esquemaJson != null pide la respuesta en JSON con ese esquema.
    private fun cuerpo(contenidos: JSONArray, esquemaJson: String?, instruccion: String): JSONObject {
        val configuracion = JSONObject()
        if (esquemaJson != null) {
            configuracion.put("responseMimeType", "application/json").put("responseJsonSchema", JSONObject(esquemaJson))
        }
        return JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instruccion))))
            .put("contents", contenidos)
            .put("generationConfig", configuracion)
    }

    private fun abrir(clave: String, metodo: String): HttpURLConnection =
        (URL("https://generativelanguage.googleapis.com/v1beta/models/$modelo:$metodo").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", clave)
        }

    private suspend fun pedir(clave: String, contenidos: JSONArray, esquemaJson: String?, instruccion: String = INSTRUCCION_ASISTENTE): ResultadoIA<String> = withContext(Dispatchers.IO) {
        if (clave.isBlank()) return@withContext ResultadoIA.Falla(ErrorIA.SIN_CLAVE)
        val cuerpo = cuerpo(contenidos, esquemaJson, instruccion)
        var conexion: HttpURLConnection? = null
        try {
            conexion = abrir(clave, "generateContent")
            conexion.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
            val codigo = conexion.responseCode
            if (codigo != HttpURLConnection.HTTP_OK) return@withContext ResultadoIA.Falla(errorDeCodigo(codigo, conexion))
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

    private fun errorDeCodigo(codigo: Int, conexion: HttpURLConnection): ErrorIA = when (codigo) {
        400, 401, 403 -> if (codigo == 400 && !errorDeClave(conexion)) ErrorIA.OTRO else ErrorIA.CLAVE_INVALIDA
        413 -> ErrorIA.ARCHIVO_GRANDE
        429 -> ErrorIA.LIMITE
        else -> ErrorIA.OTRO
    }

    // Un 400 por clave mal copiada dice "API key" en el mensaje de error.
    private fun errorDeClave(conexion: HttpURLConnection): Boolean =
        runCatching { conexion.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty().contains("API key", ignoreCase = true) }.getOrDefault(false)
}
