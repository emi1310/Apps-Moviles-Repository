package com.example.estudia.modelo

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

// Acciones que la IA puede proponer desde el chat ("anotame el TP para el
// viernes"). Gemini las pide como llamadas a funciones (function calling);
// la app las muestra con Confirmar y Descartar, y recién al confirmar las
// guarda. Acá están las funciones que se le declaran a la IA y la revisión de
// lo que pide (fechas válidas, textos no vacíos).

sealed interface AccionPropuesta {
    val materia: String

    data class CrearTarea(
        override val materia: String,
        val titulo: String,
        val fechaLimite: LocalDate?,
        val prioridad: Prioridad
    ) : AccionPropuesta

    data class CrearEvento(
        override val materia: String,
        val titulo: String,
        val tipo: TipoEvento,
        val fecha: LocalDate,
        val inicio: LocalTime,
        val fin: LocalTime
    ) : AccionPropuesta

    data class MarcarTema(
        override val materia: String,
        val examen: String,
        val tema: String
    ) : AccionPropuesta
}

enum class EstadoAccion { PENDIENTE, HECHA, DESCARTADA, FALLO }

// Lo que se le declara a Gemini en "tools" (formato OpenAPI de Gemini).
const val DECLARACION_FUNCIONES =
    """[{"functionDeclarations":[""" +
        """{"name":"crear_tarea","description":"Crea una tarea pendiente en una materia del estudiante (un TP, ejercicios, leer algo).","parameters":{"type":"object","properties":{"materia":{"type":"string","description":"Nombre de la materia, como figura en sus datos"},"titulo":{"type":"string"},"fecha_limite":{"type":"string","description":"AAAA-MM-DD; vacío si no tiene fecha"},"prioridad":{"type":"string","enum":["ALTA","MEDIA","BAJA"]}},"required":["materia","titulo"]}},""" +
        """{"name":"crear_evento","description":"Agenda en el calendario un examen, una entrega, una clase suelta o un bloque de estudio.","parameters":{"type":"object","properties":{"materia":{"type":"string"},"titulo":{"type":"string"},"tipo":{"type":"string","enum":["EXAMEN","ENTREGA","CLASE","BLOQUE_ESTUDIO"]},"fecha":{"type":"string","description":"AAAA-MM-DD"},"hora_inicio":{"type":"string","description":"HH:MM, 24 horas"},"hora_fin":{"type":"string","description":"HH:MM, 24 horas"}},"required":["materia","titulo","tipo","fecha","hora_inicio","hora_fin"]}},""" +
        """{"name":"marcar_tema_estudiado","description":"Marca como estudiado un tema del temario de un examen ya cargado.","parameters":{"type":"object","properties":{"materia":{"type":"string"},"examen":{"type":"string","description":"Título del examen"},"tema":{"type":"string","description":"Tema del temario"}},"required":["materia","examen","tema"]}}""" +
        """]}]"""

// Los tipos de evento como los nombra la IA.
private val TIPOS_EVENTO = mapOf(
    "EXAMEN" to TipoEvento.EXAMEN, "ENTREGA" to TipoEvento.ENTREGA, "CLASE" to TipoEvento.CLASE, "BLOQUE_ESTUDIO" to TipoEvento.BLOQUE_ESTUDIO
)

private fun nombreDeTipo(tipo: TipoEvento): String = TIPOS_EVENTO.entries.first { it.value == tipo }.key.lowercase().replace("_", " de ")

private fun fecha(texto: String?): LocalDate? =
    texto?.trim()?.takeIf { it.isNotEmpty() }?.let { try { LocalDate.parse(it) } catch (e: DateTimeParseException) { null } }

private fun hora(texto: String?): LocalTime? =
    texto?.trim()?.replace('.', ':')?.takeIf { it.isNotEmpty() }?.let { if (':' in it) it else "$it:00" }?.let { try { LocalTime.parse(it.padStart(5, '0')) } catch (e: DateTimeParseException) { null } }

// Arma la acción a partir de la llamada que pidió la IA. null si le falta
// algo o algo no tiene sentido (fecha inválida, termina antes de empezar...).
fun accionDesdeLlamada(nombre: String, argumentos: Map<String, String>): AccionPropuesta? {
    val materia = argumentos["materia"]?.trim().orEmpty()
    if (materia.isEmpty()) return null
    return when (nombre) {
        "crear_tarea" -> {
            val titulo = argumentos["titulo"]?.trim()?.take(100).orEmpty()
            if (titulo.isEmpty()) return null
            val textoFecha = argumentos["fecha_limite"]?.trim().orEmpty()
            val limite = fecha(textoFecha)
            if (textoFecha.isNotEmpty() && limite == null) return null
            val prioridad = Prioridad.entries.firstOrNull { it.name == argumentos["prioridad"]?.trim()?.uppercase() } ?: Prioridad.MEDIA
            AccionPropuesta.CrearTarea(materia, titulo, limite, prioridad)
        }
        "crear_evento" -> {
            val titulo = argumentos["titulo"]?.trim()?.take(100).orEmpty()
            val tipo = TIPOS_EVENTO[argumentos["tipo"]?.trim()?.uppercase()]
            val dia = fecha(argumentos["fecha"])
            val inicio = hora(argumentos["hora_inicio"])
            val fin = hora(argumentos["hora_fin"])
            if (titulo.isEmpty() || tipo == null || dia == null || inicio == null || fin == null || !fin.isAfter(inicio)) return null
            AccionPropuesta.CrearEvento(materia, titulo, tipo, dia, inicio, fin)
        }
        "marcar_tema_estudiado" -> {
            val examen = argumentos["examen"]?.trim().orEmpty()
            val tema = argumentos["tema"]?.trim().orEmpty()
            if (examen.isEmpty() || tema.isEmpty()) null else AccionPropuesta.MarcarTema(materia, examen, tema)
        }
        else -> null
    }
}

// Cómo queda la acción en la charla que se le vuelve a mandar a la IA (así
// sabe qué propuso y si el estudiante la confirmó).
fun descripcionParaIA(accion: AccionPropuesta, estado: EstadoAccion): String {
    val que = when (accion) {
        is AccionPropuesta.CrearTarea -> "crear la tarea \"${accion.titulo}\" en ${accion.materia}" + (accion.fechaLimite?.let { " para el $it" } ?: "")
        is AccionPropuesta.CrearEvento -> "agendar ${nombreDeTipo(accion.tipo)} \"${accion.titulo}\" de ${accion.materia} el ${accion.fecha} de ${accion.inicio} a ${accion.fin}"
        is AccionPropuesta.MarcarTema -> "marcar como estudiado \"${accion.tema}\" del examen \"${accion.examen}\" de ${accion.materia}"
    }
    val resultado = when (estado) {
        EstadoAccion.PENDIENTE -> "esperando que el estudiante confirme"
        EstadoAccion.HECHA -> "el estudiante lo confirmó y quedó guardado"
        EstadoAccion.DESCARTADA -> "el estudiante lo descartó"
        EstadoAccion.FALLO -> "no se pudo guardar"
    }
    return "[Propuse $que: $resultado.]"
}

// El texto de un mensaje tal como se le manda a la IA (con la acción, si hay).
fun textoParaIA(mensaje: MensajeIA): String {
    val accion = mensaje.accion?.let { descripcionParaIA(it, mensaje.estadoAccion) }
    return listOfNotNull(mensaje.texto.takeIf { it.isNotBlank() }, accion).joinToString("\n").ifEmpty { "…" }
}

// El tema del examen que corresponde a lo que dijo la IA (sin tildes ni mayúsculas;
// igual, o uno contenido en el otro).
fun buscarParecido(nombre: String, opciones: List<String>): Int? {
    val buscado = normalizarNombre(nombre)
    val normalizadas = opciones.map { normalizarNombre(it) }
    val exacta = normalizadas.indexOfFirst { it == buscado }
    if (exacta >= 0) return exacta
    return normalizadas.indexOfFirst { minOf(it.length, buscado.length) >= 4 && (it.contains(buscado) || buscado.contains(it)) }.takeIf { it >= 0 }
}
