package com.example.estudia.modelo

data class Tarea(
    val id: Int,
    val titulo: String,
    val fechaLimite: String,
    val completada: Boolean = false
)
