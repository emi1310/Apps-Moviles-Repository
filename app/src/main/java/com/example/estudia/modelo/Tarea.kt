package com.example.estudia.modelo

import java.time.LocalDate

// Representa una tarea pendiente dentro de una materia.
// fechaLimite es una fecha real (o null si la tarea no tiene fecha),
// así se puede ordenar y saber si está vencida.
data class Tarea(
    val id: Int,
    val titulo: String,
    val fechaLimite: LocalDate? = null,
    val completada: Boolean = false
) {
    fun estaVencida(hoy: LocalDate): Boolean {
        val fecha = fechaLimite ?: return false
        return !completada && fecha.isBefore(hoy)
    }
}
