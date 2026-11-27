package com.example.estudia.modelo

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

// Herramientas del Asistente IA que guardan algo en la app: un plan de
// estudio para un examen (bloques en el calendario), un mapa mental desde
// una nota, pasar la foto de un apunte a texto y un simulacro de examen.
// Son funciones puras (sin red): arman los pedidos y revisan lo que contesta
// la IA antes de guardarlo (fechas fuera de rango, textos vacíos, etc.).

// ---------- Plan de estudio para un examen ----------

// Un examen (o entrega) que viene, para elegirlo en el Asistente.
data class ExamenProximo(val evento: Evento, val materiaId: Int, val nombreMateria: String, val colorMateria: Long)

fun examenesProximos(usuario: Usuario, hoy: LocalDate): List<ExamenProximo> =
    usuario.materias.filter { !it.archivada }.flatMap { m ->
        m.eventos.filter { (it.tipo == TipoEvento.EXAMEN || it.tipo == TipoEvento.ENTREGA) && it.fecha.isAfter(hoy) }
            .map { ExamenProximo(it, m.id, m.nombre, m.color) }
    }.sortedBy { it.evento.fecha }

// Una sesión del plan: un bloque de estudio en el calendario.
data class SesionPlan(val fecha: LocalDate, val hora: LocalTime, val minutos: Int, val tema: String) {
    val horaFin: LocalTime get() = hora.plusMinutes(minutos.toLong())
}

// Lo que se le pide a la IA en JSON (fechas y horas como texto).
data class SesionPlanTexto(val fecha: String, val hora: String, val minutos: Int, val tema: String)

const val MAXIMO_SESIONES_PLAN = 20

const val ESQUEMA_PLAN =
    """{"type":"object","properties":{"sesiones":{"type":"array","items":{"type":"object","properties":{"fecha":{"type":"string","description":"AAAA-MM-DD"},"hora":{"type":"string","description":"HH:MM, 24 horas"},"minutos":{"type":"integer"},"tema":{"type":"string"}},"required":["fecha","hora","minutos","tema"]}}},"required":["sesiones"]}"""

fun pedidoPlanEstudio(examen: ExamenProximo, hoy: LocalDate): String {
    val e = examen.evento
    val temas = e.temas.joinToString("\n") { "- ${it.titulo}${if (it.estudiado) " (ya lo estudié)" else ""}" }
    return "Armame un plan de estudio para \"${e.titulo}\" de ${examen.nombreMateria}, que es el ${e.fecha} a las ${e.horaInicio}. " +
        "Hoy es $hoy. Repartí sesiones de estudio entre mañana (${hoy.plusDays(1)}) y el día anterior al examen (${e.fecha.minusDays(1)}), " +
        "de 30 a 120 minutos cada una, en horarios que no se pisen con mis clases ni con otros eventos de mi agenda. " +
        "Dejá el último día para un repaso general. Si el temario tiene temas que ya estudié, dales menos tiempo. " +
        "Máximo $MAXIMO_SESIONES_PLAN sesiones. En \"tema\" poné qué estudiar en esa sesión (corto, una línea).\n\n" +
        if (temas.isBlank()) "No cargué el temario: organizalo por etapas (lectura, ejercicios, repaso)." else "Temario:\n$temas"
}

// Se queda con las sesiones válidas: fecha entre mañana y el día anterior al
// examen, hora válida, duración razonable y tema no vacío; ordenadas.
fun limpiarPlan(sesiones: List<SesionPlanTexto>, hoy: LocalDate, fechaExamen: LocalDate): List<SesionPlan> =
    sesiones.mapNotNull { s ->
        val fecha = try { LocalDate.parse(s.fecha.trim()) } catch (e: DateTimeParseException) { null } ?: return@mapNotNull null
        val hora = try { LocalTime.parse(s.hora.trim().padStart(5, '0')) } catch (e: DateTimeParseException) { null } ?: return@mapNotNull null
        val tema = s.tema.trim().take(80)
        if (fecha.isBefore(hoy) || !fecha.isBefore(fechaExamen) || tema.isEmpty()) return@mapNotNull null
        val minutos = s.minutos.coerceIn(15, 180)
        // Que no pase de la medianoche.
        if (hora.plusMinutes(minutos.toLong()).isBefore(hora)) return@mapNotNull null
        SesionPlan(fecha, hora, minutos, tema)
    }.distinctBy { it.fecha to it.hora }.sortedWith(compareBy({ it.fecha }, { it.hora })).take(MAXIMO_SESIONES_PLAN)

// El bloque de estudio que se guarda en el calendario.
fun eventoDeSesion(sesion: SesionPlan, prefijo: String): Evento =
    Evento(
        id = 0,
        titulo = "$prefijo: ${sesion.tema}",
        tipo = TipoEvento.BLOQUE_ESTUDIO,
        fecha = sesion.fecha,
        horaInicio = sesion.hora,
        horaFin = sesion.horaFin
    )

// ---------- Mapa mental desde una nota ----------

data class RamaIA(val idea: String, val detalles: List<String>)

const val MAXIMO_RAMAS_MAPA = 7
const val MAXIMO_DETALLES_MAPA = 4

const val ESQUEMA_MAPA =
    """{"type":"object","properties":{"central":{"type":"string"},"ramas":{"type":"array","items":{"type":"object","properties":{"idea":{"type":"string"},"detalles":{"type":"array","items":{"type":"string"}}},"required":["idea","detalles"]}}},"required":["central","ramas"]}"""

fun pedidoMapa(titulo: String, contenido: String): String =
    "Armá un mapa mental de esta nota de estudio: una idea central (2 a 5 palabras), hasta $MAXIMO_RAMAS_MAPA ramas con las ideas " +
        "principales y, en cada rama, hasta $MAXIMO_DETALLES_MAPA detalles cortos. Cada texto de 1 a 6 palabras, sin oraciones largas. " +
        "Usá solo lo que dice la nota.\n\n" + "Título: ${titulo.trim()}\n\n${contenido.trim().take(LARGO_MAXIMO_NOTA)}"

// Arma el mapa con las ramas ubicadas solas (como si se agregaran a mano).
fun mapaDesdeIA(central: String, ramas: List<RamaIA>, materia: String, hoy: LocalDate): MapaMental? {
    val titulo = central.trim().take(60)
    val limpias = ramas.map { r -> RamaIA(r.idea.trim().take(60), r.detalles.map { it.trim().take(60) }.filter { it.isNotEmpty() }) }
        .filter { it.idea.isNotEmpty() }
        .take(MAXIMO_RAMAS_MAPA)
    if (titulo.isEmpty() || limpias.isEmpty()) return null
    var mapa = MapaMental.nuevo(0, materia, titulo, hoy)
    limpias.forEach { rama ->
        val (conRama, nodo) = mapa.agregarIdea(MapaMental.ID_RAIZ, rama.idea)
        mapa = conRama
        rama.detalles.take(MAXIMO_DETALLES_MAPA).forEach { detalle ->
            mapa = mapa.agregarIdea(nodo.id, detalle).first
        }
    }
    return mapa
}

// ---------- Foto de un apunte ----------

const val PEDIDO_FOTO_APUNTE =
    "Esta es la foto de un apunte (pizarrón, cuaderno o fotocopia). Pasalo a texto, tal cual lo que dice, ordenado y prolijo: " +
        "respetá títulos, listas y fórmulas (escribí las fórmulas en una línea, por ejemplo x^2 + 1). " +
        "Si hay partes que no se leen, marcá [no se lee]. No agregues explicaciones ni nada que no esté en la foto."

// ---------- Simulacro de examen ----------

const val PREGUNTAS_SIMULACRO = 5
const val MAXIMO_NOTAS_SIMULACRO = 5

fun pedidoSimulacro(notas: List<Pair<String, String>>): String {
    // El total de texto se reparte entre las notas elegidas.
    val porNota = LARGO_MAXIMO_NOTA / notas.size.coerceAtLeast(1)
    val material = notas.joinToString("\n\n---\n\n") { (titulo, contenido) ->
        val cuerpo = contenido.trim().let { if (it.length > porNota) it.take(porNota) + "\n[…recortada]" else it }
        "Título: ${titulo.trim()}\n\n$cuerpo"
    }
    return "Tomame un simulacro de examen sobre estas notas. Reglas: hacé $PREGUNTAS_SIMULACRO preguntas, DE A UNA: hacé la primera y " +
        "esperá mi respuesta. Cuando conteste, decime si está bien, regular o mal y por qué (corto), y recién ahí hacé la siguiente. " +
        "Mezclá preguntas de desarrollo corto, de verdadero o falso con justificación y de aplicar a un caso. " +
        "Al terminar la última, dame una nota del 1 al 10 y una lista de qué temas repasar. Empezá ya con la pregunta 1.\n\n" + material
}

// ---------- Horario de cursada desde una foto ----------

// Lo que devuelve la IA por cada clase (todo como texto).
data class ClaseHorarioTexto(val materia: String, val titulo: String, val dia: String, val inicio: String, val fin: String, val lugar: String)

// Una clase del horario ya revisada: se guarda como clase semanal.
data class ClaseHorario(
    val materia: String,
    val titulo: String,
    val dia: java.time.DayOfWeek,
    val inicio: LocalTime,
    val fin: LocalTime,
    val lugar: String? = null
)

const val MAXIMO_CLASES_HORARIO = 40

const val ESQUEMA_HORARIO =
    """{"type":"object","properties":{"clases":{"type":"array","items":{"type":"object","properties":{"materia":{"type":"string"},"titulo":{"type":"string","description":"Teórico, Práctico, Laboratorio o Clase"},"dia":{"type":"string","enum":["LUNES","MARTES","MIERCOLES","JUEVES","VIERNES","SABADO","DOMINGO"]},"inicio":{"type":"string","description":"HH:MM, 24 horas"},"fin":{"type":"string","description":"HH:MM, 24 horas"},"lugar":{"type":"string","description":"aula o sede; vacío si no figura"}},"required":["materia","titulo","dia","inicio","fin","lugar"]}}},"required":["clases"]}"""

// materias: las que ya tiene cargadas, para que la IA use esos nombres.
fun pedidoHorario(materias: List<String>): String =
    "Esta es la foto de mi horario de cursada. Pasá cada clase a la lista: materia, tipo de clase (Teórico, Práctico, " +
        "Laboratorio o, si no dice, Clase), día de la semana, hora de inicio y de fin (24 horas) y aula o sede si figura. " +
        "Si una materia tiene varios días u horarios, poné una entrada por cada uno. No pongas la comisión ni el código en el nombre " +
        "de la materia. Usá solo lo que se ve en la foto: si no es un horario, devolvé la lista vacía." +
        if (materias.isEmpty()) "" else "\n\nMaterias que ya tengo cargadas (si una clase es de alguna, usá exactamente ese nombre): " +
            materias.joinToString(", ")

private val DIAS_HORARIO = mapOf(
    "LUNES" to java.time.DayOfWeek.MONDAY, "MARTES" to java.time.DayOfWeek.TUESDAY, "MIERCOLES" to java.time.DayOfWeek.WEDNESDAY,
    "JUEVES" to java.time.DayOfWeek.THURSDAY, "VIERNES" to java.time.DayOfWeek.FRIDAY, "SABADO" to java.time.DayOfWeek.SATURDAY,
    "DOMINGO" to java.time.DayOfWeek.SUNDAY
)

// Se queda con las clases válidas (día y horas que existen, termina después
// de empezar, materia no vacía), sin repetidas y ordenadas por día y hora.
fun limpiarHorario(clases: List<ClaseHorarioTexto>): List<ClaseHorario> =
    clases.mapNotNull { c ->
        val dia = DIAS_HORARIO[normalizarNombre(c.dia).uppercase()] ?: return@mapNotNull null
        val inicio = horaDeTexto(c.inicio) ?: return@mapNotNull null
        val fin = horaDeTexto(c.fin) ?: return@mapNotNull null
        val materia = c.materia.trim().replace(Regex("""\s+"""), " ").take(60)
        if (materia.isEmpty() || !fin.isAfter(inicio)) return@mapNotNull null
        ClaseHorario(materia, c.titulo.trim().take(40).ifEmpty { "Clase" }, dia, inicio, fin, c.lugar.trim().take(60).ifEmpty { null })
    }.distinctBy { Triple(normalizarNombre(it.materia), it.dia, it.inicio) }
        .sortedWith(compareBy({ it.dia }, { it.inicio }))
        .take(MAXIMO_CLASES_HORARIO)

private fun horaDeTexto(texto: String): LocalTime? =
    try { LocalTime.parse(texto.trim().replace('.', ':').padStart(5, '0')) } catch (e: DateTimeParseException) { null }

// Para comparar nombres: minúsculas, sin tildes y sin espacios de más.
fun normalizarNombre(texto: String): String =
    java.text.Normalizer.normalize(texto.trim().lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("""\p{Mn}+"""), "")
        .replace(Regex("""\s+"""), " ")

// La materia ya cargada que corresponde a ese nombre: igual (sin tildes ni
// mayúsculas) o una contenida en la otra ("Análisis II" y "Análisis Matemático II" no;
// "Física" y "Física I" sí). null = hay que crearla.
fun materiaParecida(nombre: String, materias: List<Materia>): Materia? {
    val buscado = normalizarNombre(nombre)
    return materias.firstOrNull { normalizarNombre(it.nombre) == buscado }
        ?: materias.firstOrNull {
            val otro = normalizarNombre(it.nombre)
            minOf(otro.length, buscado.length) >= 4 && (otro.startsWith(buscado) || buscado.startsWith(otro))
        }
}

// La clase semanal que se guarda: empieza el primer día que toca desde "desde".
fun eventoDeClase(clase: ClaseHorario, desde: LocalDate): Evento =
    Evento(
        id = 0,
        titulo = clase.titulo,
        tipo = TipoEvento.CLASE,
        fecha = desde.with(java.time.temporal.TemporalAdjusters.nextOrSame(clase.dia)),
        horaInicio = clase.inicio,
        horaFin = clase.fin,
        semanal = true,
        lugar = clase.lugar
    )
