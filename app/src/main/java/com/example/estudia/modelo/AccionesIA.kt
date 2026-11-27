package com.example.estudia.modelo

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

// Acciones que la IA puede proponer desde el chat ("anotame el TP para el
// viernes", "borrá la nota de integrales", "cambiá el parcial al martes").
// Gemini las pide como llamadas a funciones (function calling); la app las
// muestra en una tarjeta con Confirmar y Descartar, y recién al confirmar las
// hace. Acá están las funciones que se le declaran a la IA y la revisión de lo
// que pide (fechas válidas, textos no vacíos, que cambie algo).
//
// Las cosas se nombran como figuran en los datos del estudiante (la materia,
// el título de la tarea, la nota o el evento); la app busca la más parecida.

sealed interface AccionPropuesta {
    // La materia a la que se refiere (en CrearMateria, el nombre de la nueva).
    val materia: String
    // Las que borran algo: la tarjeta se pinta de rojo y el aviso ofrece Deshacer.
    val destructiva: Boolean get() = false

    // ---------- Tareas ----------
    data class CrearTarea(override val materia: String, val titulo: String, val fechaLimite: LocalDate?, val prioridad: Prioridad) : AccionPropuesta
    data class EditarTarea(
        override val materia: String,
        val tarea: String,
        val nuevoTitulo: String? = null,
        val nuevaFecha: LocalDate? = null,
        val quitarFecha: Boolean = false,
        val nuevaPrioridad: Prioridad? = null
    ) : AccionPropuesta
    data class CompletarTarea(override val materia: String, val tarea: String, val hecha: Boolean = true) : AccionPropuesta
    data class BorrarTarea(override val materia: String, val tarea: String) : AccionPropuesta {
        override val destructiva get() = true
    }

    // ---------- Calendario ----------
    data class CrearEvento(
        override val materia: String,
        val titulo: String,
        val tipo: TipoEvento,
        val fecha: LocalDate,
        val inicio: LocalTime,
        val fin: LocalTime
    ) : AccionPropuesta
    data class EditarEvento(
        override val materia: String,
        val evento: String,
        val nuevoTitulo: String? = null,
        val nuevaFecha: LocalDate? = null,
        val nuevoInicio: LocalTime? = null,
        val nuevoFin: LocalTime? = null,
        val nuevoLugar: String? = null
    ) : AccionPropuesta
    data class BorrarEvento(override val materia: String, val evento: String) : AccionPropuesta {
        override val destructiva get() = true
    }
    data class MarcarTema(override val materia: String, val examen: String, val tema: String) : AccionPropuesta
    data class AgregarTema(override val materia: String, val examen: String, val tema: String) : AccionPropuesta

    // ---------- Notas (apuntes) ----------
    data class CrearNota(override val materia: String, val titulo: String, val contenido: String) : AccionPropuesta
    data class EditarNota(
        override val materia: String,
        val nota: String,
        val nuevoTitulo: String? = null,
        val nuevoContenido: String? = null,
        // Texto que se suma al final (sin borrar lo que ya tiene).
        val agregarTexto: String? = null
    ) : AccionPropuesta
    data class BorrarNota(override val materia: String, val nota: String) : AccionPropuesta {
        override val destructiva get() = true
    }

    // ---------- Materias ----------
    data class CrearMateria(override val materia: String) : AccionPropuesta
    data class EditarMateria(override val materia: String, val nuevoNombre: String? = null, val archivar: Boolean? = null) : AccionPropuesta
    data class BorrarMateria(override val materia: String) : AccionPropuesta {
        override val destructiva get() = true
    }

    // ---------- Repaso y notas de exámenes ----------
    data class CrearTarjeta(override val materia: String, val pregunta: String, val respuesta: String) : AccionPropuesta
    data class AgregarCalificacion(override val materia: String, val titulo: String, val nota: Double, val fecha: LocalDate?) : AccionPropuesta
}

enum class EstadoAccion { PENDIENTE, HECHA, DESCARTADA, FALLO }

// La función para buscar en internet: no es una acción de la app, la atiende
// GeminiCliente (hace la búsqueda con Google y le devuelve el resultado a la IA).
const val FUNCION_BUSCAR = "buscar_en_internet"

private const val M = """"materia":{"type":"string","description":"Nombre de la materia, como figura en sus datos"}"""
private const val FECHA = "AAAA-MM-DD"
private const val HORA = "HH:MM, 24 horas"

private fun funcion(nombre: String, descripcion: String, propiedades: String, requeridos: String) =
    """{"name":"$nombre","description":"$descripcion","parameters":{"type":"object","properties":{$propiedades},"required":[$requeridos]}}"""

// Lo que se le declara a Gemini en "tools" (formato OpenAPI de Gemini).
val DECLARACION_FUNCIONES: String = "[{\"functionDeclarations\":[" + listOf(
    funcion(FUNCION_BUSCAR, "Busca en internet (Google) información actual o que no está en los datos del estudiante: definiciones, noticias, fechas de la facultad, datos, bibliografía. Usala cuando no sepas algo con seguridad o pidan algo actual.",
        """"consulta":{"type":"string","description":"Lo que hay que buscar, como en Google"}""", "\"consulta\""),
    // Tareas
    funcion("crear_tarea", "Crea una tarea pendiente (un TP, ejercicios, leer algo).",
        """$M,"titulo":{"type":"string"},"fecha_limite":{"type":"string","description":"$FECHA; vacío si no tiene"},"prioridad":{"type":"string","enum":["ALTA","MEDIA","BAJA"]}""", "\"materia\",\"titulo\""),
    funcion("editar_tarea", "Cambia una tarea existente: el título, la fecha límite o la prioridad. Mandá solo lo que cambia.",
        """$M,"tarea":{"type":"string","description":"Título actual de la tarea"},"nuevo_titulo":{"type":"string"},"nueva_fecha_limite":{"type":"string","description":"$FECHA, o SIN_FECHA para quitarla"},"nueva_prioridad":{"type":"string","enum":["ALTA","MEDIA","BAJA"]}""", "\"materia\",\"tarea\""),
    funcion("completar_tarea", "Marca una tarea como hecha (o como pendiente otra vez).",
        """$M,"tarea":{"type":"string"},"hecha":{"type":"boolean","description":"true = hecha (por defecto), false = pendiente"}""", "\"materia\",\"tarea\""),
    funcion("borrar_tarea", "Borra una tarea.", """$M,"tarea":{"type":"string"}""", "\"materia\",\"tarea\""),
    // Calendario
    funcion("crear_evento", "Agenda un examen, una entrega, una clase suelta o un bloque de estudio.",
        """$M,"titulo":{"type":"string"},"tipo":{"type":"string","enum":["EXAMEN","ENTREGA","CLASE","BLOQUE_ESTUDIO"]},"fecha":{"type":"string","description":"$FECHA"},"hora_inicio":{"type":"string","description":"$HORA"},"hora_fin":{"type":"string","description":"$HORA"}""",
        "\"materia\",\"titulo\",\"tipo\",\"fecha\",\"hora_inicio\",\"hora_fin\""),
    funcion("editar_evento", "Cambia un evento del calendario (examen, entrega, clase o bloque de estudio): título, fecha, horario o lugar. Mandá solo lo que cambia.",
        """$M,"evento":{"type":"string","description":"Título actual del evento"},"nuevo_titulo":{"type":"string"},"nueva_fecha":{"type":"string","description":"$FECHA"},"nueva_hora_inicio":{"type":"string","description":"$HORA"},"nueva_hora_fin":{"type":"string","description":"$HORA"},"nuevo_lugar":{"type":"string"}""", "\"materia\",\"evento\""),
    funcion("borrar_evento", "Borra un evento del calendario.", """$M,"evento":{"type":"string"}""", "\"materia\",\"evento\""),
    funcion("marcar_tema_estudiado", "Marca como estudiado un tema del temario de un examen.",
        """$M,"examen":{"type":"string","description":"Título del examen"},"tema":{"type":"string"}""", "\"materia\",\"examen\",\"tema\""),
    funcion("agregar_tema", "Agrega un tema al temario de un examen.",
        """$M,"examen":{"type":"string","description":"Título del examen"},"tema":{"type":"string"}""", "\"materia\",\"examen\",\"tema\""),
    // Notas
    funcion("crear_nota", "Crea una nota (apunte) en una materia.",
        """$M,"titulo":{"type":"string"},"contenido":{"type":"string","description":"Texto de la nota, en texto plano"}""", "\"materia\",\"titulo\""),
    funcion("editar_nota", "Cambia una nota: el título, todo el texto, o le suma texto al final. Mandá solo lo que cambia.",
        """$M,"nota":{"type":"string","description":"Título actual de la nota"},"nuevo_titulo":{"type":"string"},"nuevo_contenido":{"type":"string","description":"Reemplaza todo el texto"},"agregar_texto":{"type":"string","description":"Se suma al final"}""", "\"materia\",\"nota\""),
    funcion("borrar_nota", "Borra una nota.", """$M,"nota":{"type":"string"}""", "\"materia\",\"nota\""),
    // Materias
    funcion("crear_materia", "Agrega una materia nueva.", """"nombre":{"type":"string"}""", "\"nombre\""),
    funcion("editar_materia", "Cambia el nombre de una materia, o la archiva o desarchiva. Mandá solo lo que cambia.",
        """$M,"nuevo_nombre":{"type":"string"},"archivar":{"type":"boolean","description":"true = archivar, false = desarchivar"}""", "\"materia\""),
    funcion("borrar_materia", "Borra una materia con todo lo que tiene (tareas, eventos, notas).", M, "\"materia\""),
    // Repaso y calificaciones
    funcion("crear_tarjeta", "Crea una tarjeta de repaso (pregunta y respuesta).",
        """$M,"pregunta":{"type":"string"},"respuesta":{"type":"string"}""", "\"materia\",\"pregunta\",\"respuesta\""),
    funcion("agregar_calificacion", "Anota la nota que sacó en un examen o trabajo.",
        """$M,"titulo":{"type":"string","description":"Ej: Parcial 1"},"nota":{"type":"number","description":"De 1 a 10"},"fecha":{"type":"string","description":"$FECHA; vacío = hoy"}""", "\"materia\",\"titulo\",\"nota\"")
).joinToString(",") + "]}]"

// Los tipos de evento como los nombra la IA.
private val TIPOS_EVENTO = mapOf(
    "EXAMEN" to TipoEvento.EXAMEN, "ENTREGA" to TipoEvento.ENTREGA, "CLASE" to TipoEvento.CLASE, "BLOQUE_ESTUDIO" to TipoEvento.BLOQUE_ESTUDIO
)

private fun nombreDeTipo(tipo: TipoEvento): String = TIPOS_EVENTO.entries.first { it.value == tipo }.key.lowercase().replace("_", " de ")

private fun fecha(texto: String?): LocalDate? =
    texto?.trim()?.takeIf { it.isNotEmpty() }?.let { try { LocalDate.parse(it) } catch (e: DateTimeParseException) { null } }

private fun hora(texto: String?): LocalTime? =
    texto?.trim()?.replace('.', ':')?.takeIf { it.isNotEmpty() }?.let { if (':' in it) it else "$it:00" }?.let { try { LocalTime.parse(it.padStart(5, '0')) } catch (e: DateTimeParseException) { null } }

private fun prioridad(texto: String?): Prioridad? = Prioridad.entries.firstOrNull { it.name == texto?.trim()?.uppercase() }

private fun siONo(texto: String?): Boolean? = when (texto?.trim()?.lowercase()) {
    "true", "si", "sí" -> true
    "false", "no" -> false
    else -> null
}

// Arma la acción a partir de la llamada que pidió la IA. null si le falta
// algo o algo no tiene sentido (fecha inválida, termina antes de empezar, una
// edición que no cambia nada...).
fun accionDesdeLlamada(nombre: String, argumentos: Map<String, String>): AccionPropuesta? {
    fun texto(clave: String, largo: Int = 100): String = argumentos[clave]?.trim()?.take(largo).orEmpty()
    fun opcional(clave: String, largo: Int = 100): String? = texto(clave, largo).takeIf { it.isNotEmpty() }

    if (nombre == "crear_materia") return opcional("nombre", 60)?.let { AccionPropuesta.CrearMateria(it) }
    val materia = texto("materia")
    if (materia.isEmpty()) return null
    return when (nombre) {
        "crear_tarea" -> {
            val titulo = opcional("titulo") ?: return null
            val textoFecha = texto("fecha_limite")
            val limite = fecha(textoFecha)
            if (textoFecha.isNotEmpty() && limite == null) return null
            AccionPropuesta.CrearTarea(materia, titulo, limite, prioridad(argumentos["prioridad"]) ?: Prioridad.MEDIA)
        }
        "editar_tarea" -> {
            val tarea = opcional("tarea") ?: return null
            val textoFecha = texto("nueva_fecha_limite")
            val quitar = textoFecha.equals("SIN_FECHA", ignoreCase = true)
            val nuevaFecha = if (quitar) null else fecha(textoFecha)
            if (textoFecha.isNotEmpty() && !quitar && nuevaFecha == null) return null
            val cambio = AccionPropuesta.EditarTarea(materia, tarea, opcional("nuevo_titulo"), nuevaFecha, quitar, prioridad(argumentos["nueva_prioridad"]))
            if (cambio.nuevoTitulo == null && cambio.nuevaFecha == null && !cambio.quitarFecha && cambio.nuevaPrioridad == null) null else cambio
        }
        "completar_tarea" -> opcional("tarea")?.let { AccionPropuesta.CompletarTarea(materia, it, siONo(argumentos["hecha"]) ?: true) }
        "borrar_tarea" -> opcional("tarea")?.let { AccionPropuesta.BorrarTarea(materia, it) }
        "crear_evento" -> {
            val titulo = opcional("titulo") ?: return null
            val tipo = TIPOS_EVENTO[argumentos["tipo"]?.trim()?.uppercase()]
            val dia = fecha(argumentos["fecha"])
            val inicio = hora(argumentos["hora_inicio"])
            val fin = hora(argumentos["hora_fin"])
            if (tipo == null || dia == null || inicio == null || fin == null || !fin.isAfter(inicio)) return null
            AccionPropuesta.CrearEvento(materia, titulo, tipo, dia, inicio, fin)
        }
        "editar_evento" -> {
            val evento = opcional("evento") ?: return null
            val textoFecha = texto("nueva_fecha")
            val nuevaFecha = fecha(textoFecha)
            if (textoFecha.isNotEmpty() && nuevaFecha == null) return null
            val inicio = hora(argumentos["nueva_hora_inicio"])
            val fin = hora(argumentos["nueva_hora_fin"])
            if (texto("nueva_hora_inicio").isNotEmpty() && inicio == null) return null
            if (texto("nueva_hora_fin").isNotEmpty() && fin == null) return null
            if (inicio != null && fin != null && !fin.isAfter(inicio)) return null
            val cambio = AccionPropuesta.EditarEvento(materia, evento, opcional("nuevo_titulo"), nuevaFecha, inicio, fin, opcional("nuevo_lugar"))
            if (listOf(cambio.nuevoTitulo, cambio.nuevaFecha, cambio.nuevoInicio, cambio.nuevoFin, cambio.nuevoLugar).all { it == null }) null else cambio
        }
        "borrar_evento" -> opcional("evento")?.let { AccionPropuesta.BorrarEvento(materia, it) }
        "marcar_tema_estudiado", "agregar_tema" -> {
            val examen = opcional("examen") ?: return null
            val tema = opcional("tema") ?: return null
            if (nombre == "agregar_tema") AccionPropuesta.AgregarTema(materia, examen, tema) else AccionPropuesta.MarcarTema(materia, examen, tema)
        }
        "crear_nota" -> opcional("titulo")?.let { AccionPropuesta.CrearNota(materia, it, texto("contenido", LARGO_MAXIMO_NOTA)) }
        "editar_nota" -> {
            val nota = opcional("nota") ?: return null
            val cambio = AccionPropuesta.EditarNota(materia, nota, opcional("nuevo_titulo"), opcional("nuevo_contenido", LARGO_MAXIMO_NOTA), opcional("agregar_texto", LARGO_MAXIMO_NOTA))
            if (cambio.nuevoTitulo == null && cambio.nuevoContenido == null && cambio.agregarTexto == null) null else cambio
        }
        "borrar_nota" -> opcional("nota")?.let { AccionPropuesta.BorrarNota(materia, it) }
        "editar_materia" -> {
            val cambio = AccionPropuesta.EditarMateria(materia, opcional("nuevo_nombre", 60), siONo(argumentos["archivar"]))
            if (cambio.nuevoNombre == null && cambio.archivar == null) null else cambio
        }
        "borrar_materia" -> AccionPropuesta.BorrarMateria(materia)
        "crear_tarjeta" -> {
            val pregunta = opcional("pregunta", 300) ?: return null
            val respuesta = opcional("respuesta", 600) ?: return null
            AccionPropuesta.CrearTarjeta(materia, pregunta, respuesta)
        }
        "agregar_calificacion" -> {
            val titulo = opcional("titulo") ?: return null
            val nota = texto("nota").replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..10.0 } ?: return null
            val textoFecha = texto("fecha")
            val dia = fecha(textoFecha)
            if (textoFecha.isNotEmpty() && dia == null) return null
            AccionPropuesta.AgregarCalificacion(materia, titulo, nota, dia)
        }
        else -> null
    }
}

// Cómo queda la acción en la charla que se le vuelve a mandar a la IA (así
// sabe qué propuso y si el estudiante la confirmó).
fun descripcionParaIA(accion: AccionPropuesta, estado: EstadoAccion): String {
    val de = "de ${accion.materia}"
    val que = when (accion) {
        is AccionPropuesta.CrearTarea -> "crear la tarea \"${accion.titulo}\" en ${accion.materia}" + (accion.fechaLimite?.let { " para el $it" } ?: "")
        is AccionPropuesta.EditarTarea -> "cambiar la tarea \"${accion.tarea}\" $de" + cambios(
            accion.nuevoTitulo?.let { "título \"$it\"" },
            if (accion.quitarFecha) "sin fecha" else accion.nuevaFecha?.let { "fecha $it" },
            accion.nuevaPrioridad?.let { "prioridad ${it.name.lowercase()}" }
        )
        is AccionPropuesta.CompletarTarea -> "marcar la tarea \"${accion.tarea}\" $de como ${if (accion.hecha) "hecha" else "pendiente"}"
        is AccionPropuesta.BorrarTarea -> "borrar la tarea \"${accion.tarea}\" $de"
        is AccionPropuesta.CrearEvento -> "agendar ${nombreDeTipo(accion.tipo)} \"${accion.titulo}\" $de el ${accion.fecha} de ${accion.inicio} a ${accion.fin}"
        is AccionPropuesta.EditarEvento -> "cambiar el evento \"${accion.evento}\" $de" + cambios(
            accion.nuevoTitulo?.let { "título \"$it\"" },
            accion.nuevaFecha?.let { "fecha $it" },
            accion.nuevoInicio?.let { "empieza $it" },
            accion.nuevoFin?.let { "termina $it" },
            accion.nuevoLugar?.let { "lugar $it" }
        )
        is AccionPropuesta.BorrarEvento -> "borrar el evento \"${accion.evento}\" $de"
        is AccionPropuesta.MarcarTema -> "marcar como estudiado \"${accion.tema}\" del examen \"${accion.examen}\" $de"
        is AccionPropuesta.AgregarTema -> "agregar el tema \"${accion.tema}\" al examen \"${accion.examen}\" $de"
        is AccionPropuesta.CrearNota -> "crear la nota \"${accion.titulo}\" en ${accion.materia}"
        is AccionPropuesta.EditarNota -> "cambiar la nota \"${accion.nota}\" $de" + cambios(
            accion.nuevoTitulo?.let { "título \"$it\"" },
            accion.nuevoContenido?.let { "texto nuevo" },
            accion.agregarTexto?.let { "texto agregado al final" }
        )
        is AccionPropuesta.BorrarNota -> "borrar la nota \"${accion.nota}\" $de"
        is AccionPropuesta.CrearMateria -> "agregar la materia ${accion.materia}"
        is AccionPropuesta.EditarMateria -> "cambiar la materia ${accion.materia}" + cambios(
            accion.nuevoNombre?.let { "nombre \"$it\"" },
            accion.archivar?.let { if (it) "archivarla" else "desarchivarla" }
        )
        is AccionPropuesta.BorrarMateria -> "borrar la materia ${accion.materia}"
        is AccionPropuesta.CrearTarjeta -> "crear una tarjeta de repaso $de: \"${accion.pregunta}\""
        is AccionPropuesta.AgregarCalificacion -> "anotar un ${accion.nota} en \"${accion.titulo}\" $de"
    }
    val resultado = when (estado) {
        EstadoAccion.PENDIENTE -> "esperando que el estudiante confirme"
        EstadoAccion.HECHA -> "el estudiante lo confirmó y quedó guardado"
        EstadoAccion.DESCARTADA -> "el estudiante lo descartó"
        EstadoAccion.FALLO -> "no se pudo hacer (no se encontró o hubo un error)"
    }
    return "[Propuse $que: $resultado.]"
}

private fun cambios(vararg partes: String?): String = partes.filterNotNull().joinToString(", ").let { if (it.isEmpty()) "" else " ($it)" }

// El texto de un mensaje tal como se le manda a la IA (con la acción, si hay).
fun textoParaIA(mensaje: MensajeIA): String {
    val accion = mensaje.accion?.let { descripcionParaIA(it, mensaje.estadoAccion) }
    return listOfNotNull(mensaje.texto.takeIf { it.isNotBlank() }, accion).joinToString("\n").ifEmpty { "…" }
}

// Lo que corresponde a lo que dijo la IA (sin tildes ni mayúsculas; igual, o
// uno contenido en el otro). Devuelve la posición en la lista, o null.
fun buscarParecido(nombre: String, opciones: List<String>): Int? {
    val buscado = normalizarNombre(nombre)
    val normalizadas = opciones.map { normalizarNombre(it) }
    val exacta = normalizadas.indexOfFirst { it == buscado }
    if (exacta >= 0) return exacta
    return normalizadas.indexOfFirst { minOf(it.length, buscado.length) >= 4 && (it.contains(buscado) || buscado.contains(it)) }.takeIf { it >= 0 }
}
