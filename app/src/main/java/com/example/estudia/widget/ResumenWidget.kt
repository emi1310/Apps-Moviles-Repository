package com.example.estudia.widget

import com.example.estudia.modelo.Usuario
import java.time.LocalDate

// Lo que muestra el widget, armado sin nada de Android para poder probarlo
// con tests comunes. null en WidgetEstudia = no hay sesión iniciada.
data class ResumenWidget(
    val racha: Int,
    val pendientes: Int,
    // Hasta 3 líneas "Título · Materia", las más urgentes primero.
    val proximas: List<String>
)

const val TAREAS_EN_WIDGET = 3

// Tareas sin hacer de las materias activas: primero las que tienen fecha
// (la más cercana arriba, las vencidas incluidas), después las sin fecha;
// a igual fecha, la de mayor prioridad.
fun armarResumenWidget(usuario: Usuario, hoy: LocalDate): ResumenWidget {
    val pendientes = usuario.materias
        .filter { !it.archivada }
        .flatMap { materia -> materia.tareas.filter { !it.completada }.map { it to materia.nombre } }
        .sortedWith(
            compareBy<Pair<com.example.estudia.modelo.Tarea, String>>(
                { it.first.fechaLimite == null },
                { it.first.fechaLimite },
                { -it.first.prioridad.peso }
            )
        )
    return ResumenWidget(
        racha = usuario.rachaDeEstudio(hoy),
        pendientes = pendientes.size,
        proximas = pendientes.take(TAREAS_EN_WIDGET).map { (tarea, materia) -> "${tarea.titulo} · $materia" }
    )
}

// ---------- Widget "Hoy" ----------

// Lo que muestra el widget "Hoy": las clases, exámenes, entregas y bloques
// de estudio de hoy (por hora) y las tareas que vencen hoy.
data class ResumenHoy(val lineas: List<String>, val mas: Int)

const val LINEAS_WIDGET_HOY = 5

fun armarResumenHoy(usuario: Usuario, hoy: LocalDate): ResumenHoy {
    val activas = usuario.materias.filter { !it.archivada }
    val eventos = activas.flatMap { m -> m.eventos.filter { it.ocurreEl(hoy) }.map { m to it } }
        .sortedBy { it.second.horaInicio }
        .map { (m, e) -> "%02d:%02d  %s · %s".format(e.horaInicio.hour, e.horaInicio.minute, e.titulo.trim(), m.nombre) }
    val tareas = activas.flatMap { m -> m.tareas.filter { !it.completada && it.fechaLimite == hoy }.map { "☐  ${it.titulo.trim()} · ${m.nombre}" } }
    val todas = eventos + tareas
    return ResumenHoy(todas.take(LINEAS_WIDGET_HOY), (todas.size - LINEAS_WIDGET_HOY).coerceAtLeast(0))
}
