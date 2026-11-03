package com.example.estudia.modelo

import java.time.LocalDate

// Nota de un examen (ej: "Primer parcial: 7,5"). Se llama Calificacion para
// no confundirla con Nota, que en la app son los apuntes.
// La escala es de 1 a 10, como en la facultad.
data class Calificacion(
    val id: Int,
    val titulo: String,
    val nota: Double,
    val fecha: LocalDate
) {
    val aprobada: Boolean get() = nota >= NOTA_APROBACION

    companion object {
        const val NOTA_MINIMA = 1.0
        const val NOTA_MAXIMA = 10.0
        const val NOTA_APROBACION = 4.0
    }
}
