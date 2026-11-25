package com.example.estudia.modelo

import java.time.LocalDate

// Tarjeta de estudio (flashcard): una pregunta y su respuesta, de una materia.
//
// Repetición espaciada con el sistema de cajas de Leitner: cada tarjeta está
// en una caja del 1 al 5. Si al repasarla la sabías, pasa a la caja
// siguiente y vuelve a aparecer más adelante (1, 2, 4, 8 o 16 días); si no
// la sabías, vuelve a la caja 1 y aparece mañana. Así se repasan más las
// que cuestan y menos las que ya se saben.
data class Tarjeta(
    val id: Int,
    val pregunta: String,
    val respuesta: String,
    val caja: Int = 1,
    val proximoRepaso: LocalDate = LocalDate.now()
) {
    fun tocaRepasar(hoy: LocalDate): Boolean = !proximoRepaso.isAfter(hoy)

    // La tarjeta después de responderla hoy.
    fun responder(sabia: Boolean, hoy: LocalDate): Tarjeta {
        val nuevaCaja = if (sabia) (caja + 1).coerceAtMost(CAJA_MAXIMA) else 1
        val dias = if (sabia) DIAS_POR_CAJA.getValue(nuevaCaja) else 1L
        return copy(caja = nuevaCaja, proximoRepaso = hoy.plusDays(dias))
    }

    companion object {
        const val CAJA_MAXIMA = 5
        // Días hasta el próximo repaso según la caja a la que pasa.
        val DIAS_POR_CAJA = mapOf(1 to 1L, 2 to 2L, 3 to 4L, 4 to 8L, 5 to 16L)
    }
}
