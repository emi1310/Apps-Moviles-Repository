package com.example.estudia.modelo

import java.time.LocalDate
import java.time.LocalDateTime

// Representa un apunte o nota de estudio dentro de una materia.
// Puede tener archivos adjuntos (por ejemplo, una foto del pizarrón).
// esFavorita es val: para marcar/desmarcar se crea una copia con copy().
// - fijada: va arriba de todo en la lista.
// - color: color propio de la tarjeta (ARGB); null = el de la materia.
data class Nota(
    val id: Int,
    val titulo: String,
    val contenido: String,
    val fechaCreacion: LocalDate,
    val esFavorita: Boolean = false,
    val archivos: List<Archivo> = emptyList(),
    val fijada: Boolean = false,
    val color: Long? = null
)

// Cómo era una nota antes de un cambio (historial de versiones).
data class VersionNota(
    val id: Int,
    val titulo: String,
    val contenido: String,
    val fecha: LocalDateTime
)

// Cuántas versiones viejas se guardan de cada nota.
const val MAXIMO_VERSIONES_NOTA = 20
