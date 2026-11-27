package com.example.estudia.modelo

import java.time.LocalDate
import java.time.LocalDateTime

// Lo que la IA "recuerda" del estudiante más allá de una charla: las notas que
// tienen que ver con lo que pregunta, las conversaciones guardadas y los
// resultados de los simulacros (con los temas que le costaron).

// ---------- Buscar en el contenido de las notas ----------

// Una nota para buscar: título, materia y contenido.
data class NotaParaBuscar(val titulo: String, val materia: String, val contenido: String)

const val NOTAS_RELEVANTES = 3
const val LARGO_FRAGMENTO_NOTA = 2_500

// Palabras que no sirven para buscar (aparecen en cualquier texto).
private val PALABRAS_VACIAS = setOf(
    "como", "cual", "cuales", "cuando", "donde", "porque", "para", "pero", "sobre", "entre", "desde", "hasta", "tiene", "tienen",
    "esto", "esta", "este", "estos", "estas", "eso", "esos", "esas", "aquel", "algo", "nada", "todo", "todos", "toda", "todas",
    "hace", "hacer", "haceme", "explicame", "decime", "dame", "contame", "quiero", "puedo", "podes", "podrias", "sabes", "seria",
    "entonces", "tambien", "solo", "mismo", "misma", "cada", "otra", "otro", "otros", "otras", "muy", "mas", "menos", "bien",
    "ejemplo", "favor", "gracias", "hola", "tengo", "tenes", "estoy", "esta", "estan", "fue", "ser", "son", "hay", "que", "del",
    "los", "las", "una", "uno", "unos", "unas", "con", "sin", "por", "mis", "tus", "sus", "nota", "notas", "apunte", "apuntes"
)

// Las palabras que importan de un texto: sin tildes, en minúscula, de 4 letras o más.
fun palabrasClave(texto: String): Set<String> =
    normalizarNombre(texto).split(Regex("""[^\p{L}\p{N}]+"""))
        .filter { it.length >= 4 && it !in PALABRAS_VACIAS }
        .toSet()

// Las notas que más tienen que ver con la pregunta: cuenta cuántas veces
// aparecen sus palabras (las del título valen más). Solo las que coinciden.
fun notasRelevantes(pregunta: String, notas: List<NotaParaBuscar>, maximo: Int = NOTAS_RELEVANTES): List<NotaParaBuscar> {
    val claves = palabrasClave(pregunta)
    if (claves.isEmpty()) return emptyList()
    return notas.map { nota ->
        val titulo = normalizarNombre(nota.titulo)
        val contenido = normalizarNombre(nota.contenido)
        val puntaje = claves.sumOf { palabra ->
            3 * Regex.escape(palabra).toRegex().findAll(titulo).count() + minOf(Regex.escape(palabra).toRegex().findAll(contenido).count(), 5)
        }
        nota to puntaje
    }.filter { it.second > 0 && it.first.contenido.isNotBlank() }
        .sortedByDescending { it.second }
        .take(maximo)
        .map { it.first }
}

// Los fragmentos que se le pasan a la IA junto con la pregunta.
fun contextoDeNotas(notas: List<NotaParaBuscar>): String =
    if (notas.isEmpty()) "" else "Fragmentos de las notas del estudiante que pueden servir para esta pregunta:\n\n" +
        notas.joinToString("\n\n") { n ->
            val cuerpo = n.contenido.trim().let { if (it.length > LARGO_FRAGMENTO_NOTA) it.take(LARGO_FRAGMENTO_NOTA) + " […]" else it }
            "«${n.titulo.trim()}» (${n.materia}):\n$cuerpo"
        }

// ---------- Simulacros ----------

// Lo que devuelve la IA al terminar un simulacro.
data class EvaluacionSimulacro(
    val nota: Double,
    val resumen: String,
    val temasFlojos: List<String>,
    val tarjetas: List<TarjetaSugerida>
)

// Un simulacro hecho, guardado para ver la evolución y recordar qué repasar.
data class ResultadoSimulacro(
    val fecha: LocalDate,
    val materia: String,
    val notas: List<String>,
    val nota: Double,
    val temasFlojos: List<String>
)

const val MAXIMO_TARJETAS_SIMULACRO = 8

const val ESQUEMA_EVALUACION =
    """{"type":"object","properties":{"nota":{"type":"number","description":"Del 1 al 10"},"resumen":{"type":"string","description":"Dos o tres líneas: cómo le fue y qué repasar"},"temas_flojos":{"type":"array","items":{"type":"string"}},"tarjetas":{"type":"array","items":{"type":"object","properties":{"pregunta":{"type":"string"},"respuesta":{"type":"string"}},"required":["pregunta","respuesta"]}}},"required":["nota","resumen","temas_flojos","tarjetas"]}"""

// Arma el pedido de evaluación con la charla del simulacro (sin las notas
// del principio, que son largas: la IA ya las usó para preguntar).
fun pedidoEvaluarSimulacro(conversacion: List<MensajeIA>): String {
    val charla = conversacion.drop(1).joinToString("\n\n") { m -> (if (m.deUsuario) "Estudiante: " else "Profesor: ") + m.texto.trim() }
    return "Esta es la charla de un simulacro de examen: el profesor pregunta y el estudiante responde. " +
        "Evaluá cómo le fue en lo que llegó a contestar: poné una nota del 1 al 10, un resumen corto, los temas en los que " +
        "flojeó (máximo 5, cada uno en pocas palabras) y hasta $MAXIMO_TARJETAS_SIMULACRO tarjetas de repaso (pregunta y " +
        "respuesta corta) justo sobre lo que respondió mal o no supo. Si no contestó nada, nota 1 y tarjetas de lo preguntado.\n\n" +
        charla.takeLast(LARGO_MAXIMO_NOTA)
}

fun limpiarEvaluacion(evaluacion: EvaluacionSimulacro): EvaluacionSimulacro =
    evaluacion.copy(
        nota = (Math.round(evaluacion.nota.coerceIn(1.0, 10.0) * 2) / 2.0),
        resumen = evaluacion.resumen.trim(),
        temasFlojos = evaluacion.temasFlojos.map { it.trim().take(60) }.filter { it.isNotEmpty() }.distinct().take(5),
        tarjetas = limpiarTarjetas(evaluacion.tarjetas).take(MAXIMO_TARJETAS_SIMULACRO)
    )

// Lo que se le cuenta a la IA de los últimos simulacros (para que insista en
// lo que le costó).
fun contextoSimulacros(resultados: List<ResultadoSimulacro>): String {
    val recientes = resultados.sortedByDescending { it.fecha }.take(5)
    if (recientes.isEmpty()) return ""
    return "Simulacros recientes en la app:\n" + recientes.joinToString("\n") { r ->
        val nota = if (r.nota % 1.0 == 0.0) r.nota.toInt().toString() else r.nota.toString()
        "• ${r.fecha} (${r.materia}): nota $nota" + if (r.temasFlojos.isEmpty()) "" else ", le costó: ${r.temasFlojos.joinToString(", ")}"
    }
}

// ---------- Conversaciones guardadas ----------

data class ConversacionGuardada(
    val id: Long,
    val titulo: String,
    // La materia de la nota o del PDF con que se trabajó (null = charla general).
    val materia: String?,
    val actualizada: LocalDateTime,
    val mensajes: List<MensajeGuardado>
)

// Un mensaje guardado: sin los archivos (fotos o PDF), que pesan mucho; queda
// su nombre en lo que se muestra.
data class MensajeGuardado(val deUsuario: Boolean, val texto: String, val mostrado: String, val fuentes: List<String> = emptyList())

const val CONVERSACIONES_A_CONSERVAR = 50

fun guardarMensajes(conversacion: List<MensajeIA>): List<MensajeGuardado> =
    conversacion.filter { it.texto.isNotBlank() || it.accion != null }.map { m ->
        val archivos = if (m.adjuntos.isEmpty()) "" else " (" + m.adjuntos.joinToString(", ") { it.nombre } + ")"
        MensajeGuardado(m.deUsuario, textoParaIA(m), m.mostrado.let { if (archivos.isNotEmpty() && !it.contains(m.adjuntos.first().nombre)) it + archivos else it }, m.fuentes)
    }

fun mensajesDeGuardados(guardados: List<MensajeGuardado>): List<MensajeIA> =
    guardados.map { MensajeIA(it.deUsuario, it.texto, it.mostrado, fuentes = it.fuentes) }

// El título de la charla: lo primero que escribió el estudiante.
fun tituloDeConversacion(conversacion: List<MensajeIA>): String =
    conversacion.firstOrNull { it.deUsuario }?.mostrado?.lineSequence()?.firstOrNull()?.trim()?.take(60).orEmpty().ifEmpty { "Charla" }
