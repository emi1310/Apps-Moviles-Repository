package com.example.estudia.modelo

import kotlin.math.ceil

// "¿Qué nota necesito?": cómo se compone la nota de una materia. Cada
// evaluación pesa un porcentaje (ej: Parcial 1 40 %, Parcial 2 40 %, TP
// 20 %); las que todavía no se rindieron tienen nota null.
data class Evaluacion(
    val id: Int,
    val nombre: String,
    val peso: Int,              // porcentaje, de 1 a 100
    val nota: Double? = null
)

// Nota para promocionar (sin final) por defecto; cada materia puede pedir
// otra (Materia.notaPromocion).
const val NOTA_PROMOCION = 7.0

// Resultado del cálculo para una meta (aprobar o promocionar).
sealed class NotaNecesaria {
    // Ya llegó a la meta, aunque saque la mínima en lo que falta.
    object Asegurada : NotaNecesaria()
    // Necesita este promedio en las evaluaciones que faltan.
    data class Necesita(val nota: Double) : NotaNecesaria()
    // Ni con 10 en todo lo que falta llega.
    object NoAlcanza : NotaNecesaria()
    // Ya rindió todo: llegó o no llegó.
    data class Terminada(val alcanzo: Boolean) : NotaNecesaria()
}

data class CalculoNotas(
    val pesoTotal: Int,
    val pesoRendido: Int,
    // Nota acumulada sobre 10 con lo rendido (ya ponderada).
    val acumulado: Double,
    // Promedio ponderado de lo rendido (null si no rindió nada).
    val promedioActual: Double?,
    val paraAprobar: NotaNecesaria,
    // null si la materia no es promocional.
    val paraPromocionar: NotaNecesaria?
) {
    val pesoValido: Boolean get() = pesoTotal == 100
}

// notaPromocion: la de la materia, o null si no se puede promocionar.
fun calcularNotas(evaluaciones: List<Evaluacion>, notaPromocion: Double? = NOTA_PROMOCION): CalculoNotas {
    val pesoTotal = evaluaciones.sumOf { it.peso }
    val rendidas = evaluaciones.filter { it.nota != null }
    val pesoRendido = rendidas.sumOf { it.peso }
    val acumulado = rendidas.sumOf { it.nota!! * it.peso } / 100.0
    val promedio = if (pesoRendido == 0) null else rendidas.sumOf { it.nota!! * it.peso } / pesoRendido
    val pesoFaltante = (100 - pesoRendido).coerceAtLeast(0)
    return CalculoNotas(
        pesoTotal = pesoTotal,
        pesoRendido = pesoRendido,
        acumulado = acumulado,
        promedioActual = promedio,
        paraAprobar = notaNecesaria(acumulado, pesoFaltante, Calificacion.NOTA_APROBACION),
        paraPromocionar = notaPromocion?.let { notaNecesaria(acumulado, pesoFaltante, it) }
    )
}

// Despeja: acumulado + x * pesoFaltante / 100 >= meta.
fun notaNecesaria(acumulado: Double, pesoFaltante: Int, meta: Double): NotaNecesaria {
    if (pesoFaltante == 0) return NotaNecesaria.Terminada(acumulado >= meta - 1e-9)
    val x = (meta - acumulado) * 100.0 / pesoFaltante
    return when {
        x <= Calificacion.NOTA_MINIMA -> NotaNecesaria.Asegurada
        x > Calificacion.NOTA_MAXIMA + 1e-9 -> NotaNecesaria.NoAlcanza
        // Se redondea hacia arriba a una décima: 5,01 -> 5,1.
        else -> NotaNecesaria.Necesita(ceil(x * 10 - 1e-9) / 10)
    }
}
