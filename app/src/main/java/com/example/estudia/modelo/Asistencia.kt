package com.example.estudia.modelo

import java.time.LocalDate

// Asistencia a una clase: un registro por clase y por día (en las clases
// semanales, uno por cada semana en que se marcó).
data class Asistencia(
    val id: Int,
    val fecha: LocalDate,
    val presente: Boolean
)

// Porcentaje mínimo de asistencia por defecto (cada materia puede pedir otro:
// ver Materia.asistenciaMinima).
const val ASISTENCIA_MINIMA = 75

// Resumen de la asistencia a las clases de una materia.
data class ResumenAsistencia(
    val presentes: Int,
    val ausentes: Int,
    // Clases de todo el cuatrimestre, si todas las clases tienen fecha de fin
    // ("repetir hasta"); null si no se puede saber. No cuenta los feriados.
    val clasesTotales: Int?,
    // % mínimo que pide la materia.
    val minimo: Int = ASISTENCIA_MINIMA
) {
    val registradas: Int get() = presentes + ausentes

    // null si todavía no marcó ninguna clase.
    val porcentaje: Int? get() = if (registradas == 0) null else presentes * 100 / registradas

    // Cuántas veces más puede faltar sin bajar del mínimo. Si se conoce el
    // total del cuatrimestre se cuenta sobre ese total; si no, sobre las
    // clases ya marcadas (suponiendo que a las próximas va).
    val faltasDisponibles: Int
        get() {
            val total = clasesTotales
            return when {
                total != null && total >= registradas -> total * (100 - minimo) / 100 - ausentes
                // Si pide 0 %, puede faltar siempre.
                minimo <= 0 -> Int.MAX_VALUE
                // Mayor k tal que presentes / (registradas + k) >= mínimo.
                else -> presentes * 100 / minimo - registradas
            }
        }

    val enRiesgo: Boolean get() = registradas > 0 && faltasDisponibles <= 1
    val debajoDelMinimo: Boolean get() = faltasDisponibles < 0
}

// Las clases de una materia que ocurrieron entre dos fechas (inclusive),
// como pares (clase, día). Las semanales aparecen una vez por semana. Los
// feriados se saltean: ese día no hubo clase.
fun clasesEntre(
    materia: Materia,
    desde: LocalDate,
    hasta: LocalDate,
    feriados: Set<LocalDate> = emptySet()
): List<Pair<Evento, LocalDate>> {
    val resultado = mutableListOf<Pair<Evento, LocalDate>>()
    var dia = desde
    while (!dia.isAfter(hasta)) {
        if (dia !in feriados) {
            materia.eventos.filter { it.tipo == TipoEvento.CLASE && it.ocurreEl(dia) }.forEach { resultado.add(it to dia) }
        }
        dia = dia.plusDays(1)
    }
    return resultado
}

fun resumenAsistencia(materia: Materia, feriados: Set<LocalDate> = emptySet()): ResumenAsistencia {
    val clases = materia.eventos.filter { it.tipo == TipoEvento.CLASE }
    // Lo marcado un día que después resultó feriado no cuenta.
    val registros = clases.flatMap { it.asistencias }.filter { it.fecha !in feriados }
    // Total del cuatrimestre: solo si ninguna clase semanal es "sin fin".
    val total = if (clases.isNotEmpty() && clases.all { !it.semanal || it.repetirHasta != null }) {
        clases.sumOf { clase ->
            if (!clase.semanal) {
                if (clase.fecha in feriados) 0 else 1
            } else {
                val hasta = clase.repetirHasta!!
                if (hasta.isBefore(clase.fecha)) 0
                else {
                    val semanas = (java.time.temporal.ChronoUnit.WEEKS.between(clase.fecha, hasta) + 1).toInt()
                    semanas - feriados.count { clase.ocurreEl(it) }
                }
            }
        }
    } else null
    return ResumenAsistencia(
        presentes = registros.count { it.presente },
        ausentes = registros.count { !it.presente },
        clasesTotales = total,
        minimo = materia.asistenciaMinima
    )
}
