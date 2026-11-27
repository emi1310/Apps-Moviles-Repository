package com.example.estudia.widget

import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Una tarea del widget "Tareas", con lo necesario para completarla desde ahí.
data class TareaEnWidget(val tareaId: Int, val materiaId: Int, val texto: String)

// Lo que muestra el widget, armado sin nada de Android para poder probarlo
// con tests comunes. null en WidgetEstudia = no hay sesión iniciada.
data class ResumenWidget(
    val racha: Int,
    val pendientes: Int,
    // Hasta 3 líneas "Título · Materia", las más urgentes primero.
    val proximas: List<String>,
    // Las mismas tareas, con sus ids (para el botón de completar).
    val tareas: List<TareaEnWidget> = emptyList()
)

const val TAREAS_EN_WIDGET = 3

// Tareas sin hacer de las materias activas: primero las que tienen fecha
// (la más cercana arriba, las vencidas incluidas), después las sin fecha;
// a igual fecha, la de mayor prioridad.
fun armarResumenWidget(usuario: Usuario, hoy: LocalDate): ResumenWidget {
    val pendientes = usuario.materias
        .filter { !it.archivada }
        .flatMap { materia -> materia.tareas.filter { !it.completada }.map { it to materia } }
        .sortedWith(
            compareBy<Pair<com.example.estudia.modelo.Tarea, com.example.estudia.modelo.Materia>>(
                { it.first.fechaLimite == null },
                { it.first.fechaLimite },
                { -it.first.prioridad.peso }
            )
        )
    val primeras = pendientes.take(TAREAS_EN_WIDGET).map { (tarea, materia) ->
        TareaEnWidget(tarea.id, materia.id, "${tarea.titulo} · ${materia.nombre}")
    }
    return ResumenWidget(
        racha = usuario.rachaDeEstudio(hoy),
        pendientes = pendientes.size,
        proximas = primeras.map { it.texto },
        tareas = primeras
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

// ---------- Widget "Próximo examen" ----------

data class ResumenExamen(
    val titulo: String,
    val materia: String,
    val dias: Long,
    val temasEstudiados: Int,
    val temasTotal: Int
)

// El examen más cercano (de hoy en adelante) de las materias activas.
fun proximoExamen(usuario: Usuario, hoy: LocalDate): ResumenExamen? =
    usuario.materias.filter { !it.archivada }
        .flatMap { m -> m.eventos.filter { it.tipo == TipoEvento.EXAMEN && !it.fecha.isBefore(hoy) }.map { m to it } }
        .minByOrNull { it.second.fecha }
        ?.let { (m, e) ->
            ResumenExamen(
                titulo = e.titulo.trim(),
                materia = m.nombre,
                dias = ChronoUnit.DAYS.between(hoy, e.fecha),
                temasEstudiados = e.temas.count { it.estudiado },
                temasTotal = e.temas.size
            )
        }
