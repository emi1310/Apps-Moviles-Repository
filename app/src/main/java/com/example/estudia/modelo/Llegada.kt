package com.example.estudia.modelo

import java.time.LocalDate

// Lo que se avisa al llegar a la facultad: las clases de hoy (si no es
// feriado ni receso) y las tareas que vencen hoy o mañana (o ya vencieron).
data class ResumenLlegada(
    val clases: List<String>,
    val tareas: List<String>
) {
    val vacio: Boolean get() = clases.isEmpty() && tareas.isEmpty()
}

fun resumenDeLlegada(usuario: Usuario, hoy: LocalDate, diasSinClase: Set<LocalDate>): ResumenLlegada {
    val activas = usuario.materias.filter { !it.archivada }
    val clases = if (hoy in diasSinClase) emptyList() else activas.flatMap { materia ->
        materia.eventos.filter { it.tipo == TipoEvento.CLASE && it.ocurreEl(hoy) }.map { materia to it }
    }.sortedBy { it.second.horaInicio }.map { (materia, clase) -> "${clase.horaInicio} ${materia.nombre}" }
    val tareas = activas.flatMap { materia ->
        materia.tareas.filter { tarea ->
            val fecha = tarea.fechaLimite
            !tarea.completada && fecha != null && !fecha.isAfter(hoy.plusDays(1))
        }.map { "${it.titulo} (${materia.nombre})" }
    }
    return ResumenLlegada(clases, tareas)
}
