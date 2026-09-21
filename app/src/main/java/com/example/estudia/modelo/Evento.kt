package com.example.estudia.modelo

// Representa un evento del calendario: puede ser una clase, un examen,
// una entrega o un bloque de estudio (ver TipoEvento).
data class Evento(
    val id: Int,
    val titulo: String,
    val tipo: TipoEvento,
    val fecha: String,       // formato simple: "17/06/2026"
    val horaInicio: String,  // formato simple: "18:00"
    val horaFin: String
)
