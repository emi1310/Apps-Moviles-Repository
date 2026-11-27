package com.example.estudia.modelo

// Lo que el asistente IA sabe hacer y cómo se le pide. Son funciones puras
// (sin red): arman el texto que se manda a la IA. La llamada la hace
// datos/GeminiCliente.

// Un mensaje de la conversación. "texto" es lo que se manda a la IA y
// "mostrado" lo que se ve en la pantalla (ej: se muestra "Resumir: Guía 7"
// pero se manda la nota entera).
// adjuntos: fotos o un PDF que van con el mensaje (se mandan a la IA mientras
// dure la charla, para poder seguir preguntando). accion: lo que la IA propuso
// hacer en la app (crear una tarea...), esperando que el usuario lo confirme.
// fuentes: las notas del estudiante que se le pasaron para esta respuesta.
data class MensajeIA(
    val deUsuario: Boolean,
    val texto: String,
    val mostrado: String = texto,
    val adjuntos: List<AdjuntoIA> = emptyList(),
    val accion: AccionPropuesta? = null,
    val estadoAccion: EstadoAccion = EstadoAccion.PENDIENTE,
    val fuentes: List<String> = emptyList(),
    // Páginas de internet que usó (si buscó) y lo que está buscando ahora.
    val fuentesWeb: List<FuenteWeb> = emptyList(),
    val buscando: String? = null
)

// Un archivo que va con un mensaje: una foto (JPEG) o un PDF. No es data class
// porque compara un ByteArray (se compara por identidad).
class AdjuntoIA(val tipoMime: String, val bytes: ByteArray, val nombre: String)

// Tope de un PDF: los pedidos a Gemini con archivos adentro tienen un límite de tamaño.
const val TAMANO_MAXIMO_PDF = 15 * 1024 * 1024

data class TarjetaSugerida(val pregunta: String, val respuesta: String)

// Por qué no se pudo responder.
enum class ErrorIA { SIN_CLAVE, CLAVE_INVALIDA, SIN_INTERNET, LIMITE, BLOQUEADO, ARCHIVO_GRANDE, OTRO }

sealed class ResultadoIA<out T> {
    data class Ok<T>(val valor: T) : ResultadoIA<T>()
    data class Falla(val error: ErrorIA) : ResultadoIA<Nothing>()
}

// Las acciones rápidas de la pantalla.
enum class AccionIA { RESUMIR, EXPLICAR, PREGUNTAS, TARJETAS, PLAN, MAPA, FOTO, SIMULACRO, HORARIO, PDF }

// Instrucción fija para la IA: que ayude a aprender sin hacer el trabajo
// por el estudiante. La app muestra Markdown simple y las fórmulas entre $...$.
const val INSTRUCCION_ASISTENTE =
    "Sos el asistente de estudio de Estudia+, una app para estudiantes universitarios de Argentina. " +
        "Respondé en español rioplatense (usá \"vos\"), claro y breve. " +
        "Tu objetivo es que el estudiante aprenda, no hacerle el trabajo: explicá con pasos y ejemplos simples, " +
        "hacé preguntas que lo hagan pensar y, si te pide resolver un trabajo práctico, un examen o una tarea completa, " +
        "guialo paso a paso y proponé que intente cada paso él mismo en vez de darle la solución final entera. " +
        "Si no estás seguro de algo, decilo y sugerí revisarlo en la bibliografía de la materia. " +
        "Podés usar Markdown simple: **negrita**, listas con \"-\" o \"1.\" y títulos cortos con \"##\" (sin tablas). " +
        "Escribí las fórmulas entre signos \$, con notación LaTeX simple (por ejemplo \$x^2 + \\frac{1}{2}\$). " +
        "Podés hacer cosas en la app con las funciones que tenés: crear, cambiar, completar o borrar tareas; agendar, " +
        "mover o borrar exámenes, entregas, clases y bloques de estudio; agregar o marcar temas del temario; crear, editar " +
        "o borrar notas; crear, renombrar, archivar o borrar materias; crear tarjetas de repaso y anotar calificaciones. " +
        "Cuando el estudiante te pida algo así, llamá a la función (una vez por cada cosa, aunque sean varias) con los " +
        "nombres tal como figuran en sus datos: la app le muestra cada acción y la hace cuando la confirma. " +
        "No digas que ya está hecho: decí que lo confirme. " +
        "Si te pregunta algo que no está en sus datos y no sabés con seguridad (algo actual, una fecha, un dato, " +
        "una noticia), usá buscar_en_internet y respondé con lo que encuentres. " +
        "Si te pasan fragmentos de sus notas y usás algo de ellos, decí de qué nota sale, con el título entre « »."

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
// Si el último mensaje con fotos o un PDF quedó más atrás, va igual adelante:
// así se puede seguir preguntando sobre el archivo en una charla larga.
fun contextoParaEnviar(conversacion: List<MensajeIA>): List<MensajeIA> {
    val ultimos = conversacion.takeLast(MENSAJES_DE_CONTEXTO).dropWhile { !it.deUsuario }
    val conArchivo = conversacion.lastOrNull { it.adjuntos.isNotEmpty() }
    return if (conArchivo == null || ultimos.any { it === conArchivo }) ultimos else listOf(conArchivo) + ultimos
}

// Ordena la respuesta: saltos de línea comunes y sin espacios de más al final.
fun limpiarRespuesta(texto: String): String = texto.replace("\r\n", "\n").replace(Regex("""\n{3,}"""), "\n\n").trim()

// Saca las tarjetas repetidas o vacías.
fun limpiarTarjetas(tarjetas: List<TarjetaSugerida>): List<TarjetaSugerida> = tarjetas
    .map { TarjetaSugerida(it.pregunta.trim(), it.respuesta.trim()) }
    .filter { it.pregunta.isNotEmpty() && it.respuesta.isNotEmpty() }
    .distinctBy { it.pregunta.lowercase() }

// Pedido mínimo para probar que una clave anda (gasta lo menos posible).
const val PEDIDO_PRUEBA = "Respondé solamente: OK"

// Lo que va llegando de una respuesta en vivo: un pedazo de texto, una acción
// que propone la IA o el error que cortó la respuesta.
sealed interface PiezaRespuesta {
    data class Texto(val texto: String) : PiezaRespuesta
    data class Accion(val accion: AccionPropuesta) : PiezaRespuesta
    // La IA pidió buscar en internet: se muestra "Buscando…" mientras tanto.
    data class Buscando(val consulta: String) : PiezaRespuesta
    // Las páginas de donde sale lo que encontró.
    data class FuentesWeb(val fuentes: List<FuenteWeb>) : PiezaRespuesta
    data class Error(val error: ErrorIA) : PiezaRespuesta
}

// Una página de internet de donde la IA sacó algo (al buscar con Google).
data class FuenteWeb(val titulo: String, val url: String)

// Cuántas veces puede buscar en internet en una misma respuesta, y cuántas
// páginas se muestran como fuentes.
const val MAXIMO_BUSQUEDAS = 2
const val MAXIMO_FUENTES_WEB = 5

// Lo que se le pide a Google (por Gemini) cuando la IA quiere buscar algo.
fun pedidoBusqueda(consulta: String): String =
    "Buscá en internet y resumí en español lo que encuentres sobre esto, con datos concretos, fechas y cifras si las hay. " +
        "Si hay información contradictoria o desactualizada, decilo.\n\n$consulta"
