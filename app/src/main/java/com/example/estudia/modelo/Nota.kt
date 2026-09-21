package com.example.estudia.modelo

// Representa un apunte o nota de estudio dentro de una materia.
// esFavorita es val: para marcar/desmarcar se crea una copia con copy()
// y se reemplaza en la lista, así Compose se entera y redibuja.
data class Nota(
    val id: Int,
    val titulo: String,
    val contenido: String,
    val fechaCreacion: String,   // formato "dd/MM/yyyy"
    val esFavorita: Boolean = false
)
