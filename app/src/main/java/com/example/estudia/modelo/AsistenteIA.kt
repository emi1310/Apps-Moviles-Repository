package com.example.estudia.modelo

// Lo que el asistente IA sabe hacer y cómo se le pide. Son funciones puras
// (sin red): arman el texto que se manda a la IA. La llamada la hace
// datos/GeminiCliente.

// Un mensaje de la conversación. "texto" es lo que se manda a la IA y
// "mostrado" lo que se ve en la pantalla (ej: se muestra "Resumir: Guía 7"
// pero se manda la nota entera).
data class MensajeIA(
    val deUsuario: Boolean,
    val texto: String,
    val mostrado: String = texto
)

data class TarjetaSugerida(val pregunta: String, val respuesta: String)

// Por qué no se pudo responder.
enum class ErrorIA { SIN_CLAVE, CLAVE_INVALIDA, SIN_INTERNET, LIMITE, BLOQUEADO, OTRO }

sealed class ResultadoIA<out T> {
    data class Ok<T>(val valor: T) : ResultadoIA<T>()
    data class Falla(val error: ErrorIA) : ResultadoIA<Nothing>()
}

// Las acciones rápidas de la pantalla.
enum class AccionIA { RESUMIR, EXPLICAR, PREGUNTAS, TARJETAS }

// Instrucción fija para la IA: que ayude a aprender sin hacer el trabajo
// por el estudiante, y que responda en texto plano (la app no muestra Markdown).
const val INSTRUCCION_ASISTENTE =
    "Sos el asistente de estudio de Estudia+, una app para estudiantes universitarios de Argentina. " +
        "Respondé en español rioplatense (usá \"vos\"), claro y breve. " +
        "Tu objetivo es que el estudiante aprenda, no hacerle el trabajo: explicá con pasos y ejemplos simples, " +
        "hacé preguntas que lo hagan pensar y, si te pide resolver un trabajo práctico, un examen o una tarea completa, " +
        "guialo paso a paso y proponé que intente cada paso él mismo en vez de darle la solución final entera. " +
        "Si no estás seguro de algo, decilo y sugerí revisarlo en la bibliografía de la materia. " +
        "Escribí en texto plano: usá viñetas con \"•\" y números \"1.\", sin asteriscos, numerales ni otro formato Markdown."

// Las notas largas se recortan para no pasarse del tamaño de un pedido.
const val LARGO_MAXIMO_NOTA = 12_000

// Cuántos mensajes anteriores se mandan (para que recuerde la charla sin
// que el pedido crezca sin fin).
const val MENSAJES_DE_CONTEXTO = 16

private fun textoDeNota(titulo: String, contenido: String): String {
    val cuerpo = contenido.trim().let { if (it.length > LARGO_MAXIMO_NOTA) it.take(LARGO_MAXIMO_NOTA) + "\n[…nota recortada]" else it }
    return "Título: ${titulo.trim()}\n\n${cuerpo.ifEmpty { "(la nota no tiene contenido, solo el título)" }}"
}

fun pedidoResumir(titulo: String, contenido: String): String =
    "Resumí esta nota de estudio en sus ideas clave (como máximo 8 viñetas) y terminá con una síntesis de 2 o 3 líneas. " +
        "No agregues información que no esté en la nota.\n\n" + textoDeNota(titulo, contenido)

fun pedidoExplicar(tema: String): String =
    "Explicame paso a paso, como si fuera la primera vez que lo veo, y con un ejemplo simple: ${tema.trim()}"

fun pedidoPreguntas(titulo: String, contenido: String): String =
    "Haceme 5 preguntas para autoevaluarme sobre esta nota, de la más fácil a la más difícil. " +
        "No pongas las respuestas: te las voy a contestar yo en el chat y después me corregís.\n\n" + textoDeNota(titulo, contenido)

fun pedidoTarjetas(titulo: String, contenido: String, cantidad: Int = 8): String =
    "Armá hasta $cantidad tarjetas de estudio (pregunta y respuesta corta) con lo más importante de esta nota, " +
        "para repasar con repetición espaciada. Usá solo lo que dice la nota.\n\n" + textoDeNota(titulo, contenido)

// Esquema JSON que se le pide a la IA para las tarjetas.
const val ESQUEMA_TARJETAS =
    """{"type":"object","properties":{"tarjetas":{"type":"array","items":{"type":"object","properties":{"pregunta":{"type":"string"},"respuesta":{"type":"string"}},"required":["pregunta","respuesta"]}}},"required":["tarjetas"]}"""

// Los últimos mensajes, empezando siempre por uno del usuario (la IA lo exige).
fun contextoParaEnviar(conversacion: List<MensajeIA>): List<MensajeIA> =
    conversacion.takeLast(MENSAJES_DE_CONTEXTO).dropWhile { !it.deUsuario }

// Limpia lo que pueda quedar de Markdown si la IA no hizo caso.
fun limpiarRespuesta(texto: String): String = texto
    .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
    .replace(Regex("(?m)^#{1,6}\\s*"), "")
    .replace(Regex("(?m)^\\s*[*-]\\s+"), "• ")
    .trim()

// Saca las tarjetas repetidas o vacías.
fun limpiarTarjetas(tarjetas: List<TarjetaSugerida>): List<TarjetaSugerida> = tarjetas
    .map { TarjetaSugerida(it.pregunta.trim(), it.respuesta.trim()) }
    .filter { it.pregunta.isNotEmpty() && it.respuesta.isNotEmpty() }
    .distinctBy { it.pregunta.lowercase() }

// Pedido mínimo para probar que una clave anda (gasta lo menos posible).
const val PEDIDO_PRUEBA = "Respondé solamente: OK"
