package com.example.estudia.modelo

import java.time.LocalDate
import java.time.LocalDateTime

// Representa una tarea pendiente dentro de una materia.
// fechaLimite es una fecha real (o null si la tarea no tiene fecha),
// así se puede ordenar y saber si está vencida.
// Versión 6: cada tarea tiene una prioridad y puede tener subtareas (una
// lista de pasos, ej: "Leer el apunte", "Hacer los ejercicios").
data class Tarea(
    val id: Int,
    val titulo: String,
    val fechaLimite: LocalDate? = null,
    val completada: Boolean = false,
    val prioridad: Prioridad = Prioridad.MEDIA,
    val subtareas: List<Subtarea> = emptyList(),
    // Versión 7: cuántos días antes de la fecha límite avisar (0 = el mismo
    // día). null = sin aviso. Solo tiene sentido si hay fecha límite.
    val avisoDiasAntes: Int? = null
) {
    fun estaVencida(hoy: LocalDate): Boolean {
        val fecha = fechaLimite ?: return false
        return !completada && fecha.isBefore(hoy)
    }

    // Cuándo llega el aviso de esta tarea: a las 9:00 del día elegido.
    // null si no tiene aviso, no tiene fecha, ya está hecha o ya pasó.
    fun momentoDelAviso(ahora: LocalDateTime): LocalDateTime? {
        val dias = avisoDiasAntes ?: return null
        val fecha = fechaLimite ?: return null
        if (completada) return null
        return fecha.minusDays(dias.toLong()).atTime(HORA_AVISO_TAREA, 0).takeIf { it.isAfter(ahora) }
    }

    companion object {
        const val HORA_AVISO_TAREA = 9
        // Opciones de aviso que se ofrecen: el mismo día, 1, 2 y 7 días antes.
        val OPCIONES_AVISO = listOf(0, 1, 2, 7)
    }

    val subtareasHechas: Int get() = subtareas.count { it.completada }
}

// Un paso de una tarea. Se marca por separado de la tarea.
data class Subtarea(
    val id: Int,
    val titulo: String,
    val completada: Boolean = false
)

// Prioridad de una tarea. "peso" sirve para ordenar (alta primero) y es el
// número que se guarda en la base.
enum class Prioridad(val peso: Int) {
    BAJA(0), MEDIA(1), ALTA(2);

    companion object {
        fun desdePeso(peso: Int): Prioridad = entries.firstOrNull { it.peso == peso } ?: MEDIA
    }
}
