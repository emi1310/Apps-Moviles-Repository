package com.example.estudia.modelo

import java.time.LocalDate

// Un receso: días sin clases (vacaciones de invierno, semana de parciales,
// paro...). Como los feriados, esos días las clases no se avisan ni cuentan
// en la asistencia, y en el calendario se ven en rojo con el nombre.
data class Receso(
    val id: Int,
    val nombre: String,
    val desde: LocalDate,
    val hasta: LocalDate
) {
    fun incluye(dia: LocalDate): Boolean = !dia.isBefore(desde) && !dia.isAfter(hasta)
}

// Los días sin clase: los feriados y cada día de los recesos, con su nombre.
// Si un día es feriado y receso a la vez, queda el nombre del feriado.
fun diasSinClase(feriados: List<Feriado>, recesos: List<Receso>): Map<LocalDate, String> {
    val dias = mutableMapOf<LocalDate, String>()
    recesos.forEach { receso ->
        var dia = receso.desde
        // Tope de un año por receso, por si se cargó mal una fecha.
        var vueltas = 0
        while (!dia.isAfter(receso.hasta) && vueltas < 366) {
            dias[dia] = receso.nombre
            dia = dia.plusDays(1)
            vueltas++
        }
    }
    feriados.forEach { dias[it.fecha] = it.nombre }
    return dias
}
