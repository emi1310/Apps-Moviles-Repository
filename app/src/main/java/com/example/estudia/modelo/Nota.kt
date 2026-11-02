package com.example.estudia.modelo

import java.time.LocalDate

// Representa un apunte o nota de estudio dentro de una materia.
// Puede tener archivos adjuntos (por ejemplo, una foto del pizarrón).
// esFavorita es val: para marcar/desmarcar se crea una copia con copy().
data class Nota(
    val id: Int,
    val titulo: String,
    val contenido: String,
    val fechaCreacion: LocalDate,
    val esFavorita: Boolean = false,
    val archivos: List<Archivo> = emptyList()
)
