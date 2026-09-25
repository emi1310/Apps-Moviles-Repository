package com.example.estudia.modelo

data class Nota(
    val id: Int,
    val titulo: String,
    val contenido: String,
    val fechaCreacion: String,
    val esFavorita: Boolean = false
)
