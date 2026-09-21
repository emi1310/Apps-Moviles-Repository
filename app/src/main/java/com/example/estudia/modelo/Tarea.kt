package com.example.estudia.modelo

// Representa una tarea pendiente dentro de una materia.
// Ejemplo en la app: "Ejercicios 3 y 4 - Matemática II - Hoy"
data class Tarea(
    val id: Int,
    val titulo: String,
    val fechaLimite: String,
    val completada: Boolean = false
)
