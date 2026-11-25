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
