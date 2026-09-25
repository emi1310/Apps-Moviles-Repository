package com.example.estudia.modelo

data class Evento(
    val id: Int,
    val titulo: String,
    val tipo: TipoEvento,
    val fecha: String,
    val horaInicio: String,
    val horaFin: String
)
