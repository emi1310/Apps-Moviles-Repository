package com.example.estudia.modelo

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

// Resumen en texto de lo que el estudiante cargó en Estudia+, para que el
// Asistente IA pueda responder sobre "sus cosas" (qué clases tiene, qué
// entrega vence, cuándo rinde...). Es una función pura: se prueba sin red.
// No manda el contenido de las notas (solo sus títulos) ni datos de la cuenta.

// Días que se miran hacia adelante en la agenda.
const val DIAS_DE_AGENDA = 14

// Tope del resumen para no agrandar demasiado cada pedido.
const val LARGO_MAXIMO_CONTEXTO = 12_000

private val DIAS = mapOf(
    DayOfWeek.MONDAY to "lunes", DayOfWeek.TUESDAY to "martes", DayOfWeek.WEDNESDAY to "miércoles",
    DayOfWeek.THURSDAY to "jueves", DayOfWeek.FRIDAY to "viernes", DayOfWeek.SATURDAY to "sábado", DayOfWeek.SUNDAY to "domingo"
)

private fun dia(fecha: LocalDate) = "${DIAS.getValue(fecha.dayOfWeek)} ${fecha.dayOfMonth}/${fecha.monthValue}/${fecha.year}"
private fun corto(fecha: LocalDate) = "${DIAS.getValue(fecha.dayOfWeek)} ${fecha.dayOfMonth}/${fecha.monthValue}"
private fun hora(h: LocalTime) = "%02d:%02d".format(h.hour, h.minute)
private fun nota(n: Double) = if (n % 1.0 == 0.0) n.toInt().toString() else n.toString()

private fun tipo(t: TipoEvento) = when (t) {
    TipoEvento.CLASE -> "Clase"
    TipoEvento.EXAMEN -> "Examen"
    TipoEvento.ENTREGA -> "Entrega"
    TipoEvento.BLOQUE_ESTUDIO -> "Bloque de estudio"
    else -> "Evento"
}

private fun donde(e: Evento): String = when {
    e.virtual -> " (virtual)"
    !e.lugar.isNullOrBlank() -> " (${e.lugar.trim()})"
    else -> ""
}

fun contextoDelEstudiante(usuario: Usuario, hoy: LocalDate): String {
    val materias = usuario.materias.filter { !it.archivada }
    val texto = StringBuilder()
    texto.appendLine("Hoy es ${dia(hoy)}.")
    usuario.carrera?.takeIf { it.isNotBlank() }?.let { c ->
        texto.appendLine("Carrera: ${c.trim()}${usuario.anio?.let { ", $it° año" } ?: ""}.")
    }

    // Materias que cursa, con sus horarios de clase y notas de exámenes.
    texto.appendLine()
    if (materias.isEmpty()) {
        texto.appendLine("Materias que cursa: ninguna cargada.")
    } else {
        texto.appendLine("Materias que cursa:")
        materias.forEach { m ->
            texto.appendLine("• ${m.nombre}")
            val clases = m.eventos.filter { it.tipo == TipoEvento.CLASE && it.semanal && (it.repetirHasta == null || !it.repetirHasta.isBefore(hoy)) }
                .sortedWith(compareBy({ it.fecha.dayOfWeek }, { it.horaInicio }))
            if (clases.isNotEmpty()) {
                texto.appendLine("  Clases semanales: " + clases.joinToString("; ") {
                    "${it.titulo.trim()} los ${DIAS.getValue(it.fecha.dayOfWeek)} de ${hora(it.horaInicio)} a ${hora(it.horaFin)}${donde(it)}"
                })
            }
            if (m.calificaciones.isNotEmpty()) {
                texto.appendLine("  Notas: " + m.calificaciones.sortedBy { it.fecha }.joinToString("; ") { "${it.titulo.trim()} ${nota(it.nota)}" })
            }
            if (m.tareas.any { it.completada }) {
                texto.appendLine("  Tareas ya hechas: " + m.tareas.filter { it.completada }.takeLast(10).joinToString("; ") { it.titulo.trim() })
            }
            // Exámenes y entregas que vienen (con su temario), aunque falte mucho.
            val proximos = m.eventos.filter { (it.tipo == TipoEvento.EXAMEN || it.tipo == TipoEvento.ENTREGA) && !it.fecha.isBefore(hoy) }.sortedBy { it.fecha }
            if (proximos.isNotEmpty()) {
                texto.appendLine("  Exámenes y entregas: " + proximos.take(8).joinToString("; ") { e ->
                    "${tipo(e.tipo)} \"${e.titulo.trim()}\" el ${corto(e.fecha)}" +
                        (if (e.temas.isEmpty()) "" else " (temario: " + e.temas.joinToString(", ") { t -> t.titulo.trim() + if (t.estudiado) " ✓" else "" } + ")")
                })
            }
            if (m.notas.isNotEmpty()) {
                texto.appendLine("  Apuntes (títulos): " + m.notas.sortedByDescending { it.fechaCreacion }.take(30).joinToString("; ") { it.titulo.trim() })
            }
        }
    }

    val archivadas = usuario.materias.filter { it.archivada }
    if (archivadas.isNotEmpty()) {
        texto.appendLine()
        texto.appendLine("Materias archivadas: " + archivadas.joinToString("; ") { it.nombre })
    }

    // Lo que tiene en el calendario los próximos días (incluye cada clase).
    texto.appendLine()
    texto.appendLine("Agenda de los próximos $DIAS_DE_AGENDA días:")
    var algunDia = false
    for (i in 0 until DIAS_DE_AGENDA) {
        val d = hoy.plusDays(i.toLong())
        val delDia = materias.flatMap { m -> m.eventos.filter { it.ocurreEl(d) }.map { m to it } }.sortedBy { it.second.horaInicio }
        if (delDia.isEmpty()) continue
        algunDia = true
        texto.appendLine("• ${corto(d)}${if (i == 0) " (hoy)" else if (i == 1) " (mañana)" else ""}: " + delDia.joinToString("; ") { (m, e) ->
            "${hora(e.horaInicio)}-${hora(e.horaFin)} ${tipo(e.tipo)} de ${m.nombre}: ${e.titulo.trim()}${donde(e)}"
        })
    }
    if (!algunDia) texto.appendLine("• Nada cargado.")

    // Tareas sin terminar: primero las que vencen antes.
    val tareas = materias.flatMap { m -> m.tareas.filter { !it.completada }.map { m to it } }
        .sortedWith(compareBy(nullsLast()) { it.second.fechaLimite })
    texto.appendLine()
    if (tareas.isEmpty()) {
        texto.appendLine("Tareas pendientes: ninguna.")
    } else {
        texto.appendLine("Tareas pendientes:")
        tareas.take(25).forEach { (m, t) ->
            val vence = t.fechaLimite?.let { f -> if (f.isBefore(hoy)) "venció el ${corto(f)}" else "vence el ${corto(f)}" } ?: "sin fecha"
            texto.appendLine("• ${t.titulo.trim()} (${m.nombre}), $vence, prioridad ${t.prioridad.name.lowercase()}")
        }
    }

    // Finales que le quedan por rendir.
    val finales = usuario.finales.filter { !it.rendido && !it.fechaMesa.isBefore(hoy) }.sortedBy { it.fechaMesa }
    if (finales.isNotEmpty()) {
        texto.appendLine()
        texto.appendLine("Próximos finales:")
        finales.take(10).forEach { f ->
            val inscripcion = when {
                f.inscripto -> "inscripto"
                f.cierreInscripcion != null -> "la inscripción cierra el ${corto(f.cierreInscripcion)}"
                else -> "sin inscribirse"
            }
            texto.appendLine("• ${f.materia} el ${corto(f.fechaMesa)} ($inscripcion)")
        }
    }

    // Cómo va la carrera, si cargó el plan.
    if (usuario.planEstudio.isNotEmpty()) {
        val avance = avanceCarrera(usuario.planEstudio)
        texto.appendLine()
        texto.appendLine(
            "Plan de estudios: ${avance.aprobadas} de ${avance.total} materias aprobadas, ${avance.regulares} regulares" +
                (avance.promedio?.let { ", promedio %.2f".format(it) } ?: "") + "."
        )
    }

    val resumen = texto.toString().trim()
    return if (resumen.length > LARGO_MAXIMO_CONTEXTO) resumen.take(LARGO_MAXIMO_CONTEXTO) + "\n[…recortado]" else resumen
}

// Lo que se agrega a la instrucción fija cuando se mandan los datos.
fun instruccionConDatos(contexto: String): String =
    if (contexto.isBlank()) INSTRUCCION_ASISTENTE
    else INSTRUCCION_ASISTENTE + "\n\n" +
        "Estos son los datos que el estudiante cargó en Estudia+. Usalos cuando te pregunte por sus clases, horarios, " +
        "tareas, exámenes, finales, notas o apuntes, y para ayudarlo a organizarse. Si algo no figura acá, decile que no " +
        "está cargado en la app (no inventes). No le pidas que te copie datos que ya están acá.\n\n" + contexto
