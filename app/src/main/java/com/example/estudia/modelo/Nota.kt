package com.example.estudia.modelo

// Representa un apunte o nota de estudio dentro de una materia.
data class Nota(
    val id: Int,
    val titulo: String,
    val contenido: String,
    val fechaCreacion: String,
    var esFavorita: Boolean = false
) {
    // Marca esta nota como favorita (aparece con estrella llena).
    fun marcarFavorita() {
        esFavorita = true
    }

    // Le saca el estado de favorita.
    fun quitarFavorita() {
        esFavorita = false
    }
}