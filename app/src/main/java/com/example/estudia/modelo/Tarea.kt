package com.example.estudia.modelo

// Representa una tarea pendiente dentro de una materia.
// Ejemplo en la app: "Ejercicios 3 y 4 - Matemática II - Hoy"
data class Tarea(
    val id: Int,
    val titulo: String,
    val fechaLimite: String,
    var completada: Boolean = false
) {
    // Invierte el estado de la tarea: si estaba pendiente pasa a completada, y viceversa.
    // Se llama cuando el usuario toca el círculo de la tarea en la pantalla.
    fun cambiarEstado() {
        completada = !completada
    }
}