package com.example.estudia.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Funciones auxiliares para trabajar con fechas usando el formato
// "dd/MM/yyyy" que usamos en toda la app (ej: "17/06/2026").
// Se apoyan en java.util.Calendar, que viene incluido en el lenguaje,
// no es una librería externa.

private val formato = SimpleDateFormat("dd/MM/yyyy", Locale("es", "AR"))

private val nombresMeses = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

fun nombreDelMes(mes: Int): String = nombresMeses[mes]

// Devuelve la fecha de hoy como texto, ej: "14/08/2026"
fun obtenerFechaHoyComoTexto(): String {
    return formato.format(Calendar.getInstance().time)
}

// Devuelve la fecha de mañana como texto.
fun obtenerFechaMananaComoTexto(): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 1)
    return formato.format(calendar.time)
}

// Devuelve una fecha puntual (año, mes, día) como texto "dd/MM/yyyy".
// Ojo: "mes" acá va de 0 (Enero) a 11 (Diciembre), como maneja Calendar.
fun formatearFecha(anio: Int, mes: Int, dia: Int): String {
    val calendar = Calendar.getInstance()
    calendar.set(anio, mes, dia)
    return formato.format(calendar.time)
}

// Devuelve la lista de días de un mes para dibujar la grilla del
// calendario. Incluye "null" al principio para representar los
// espacios vacíos antes de que empiece el día 1 (ej: si el mes
// empieza un miércoles, los primeros 3 casilleros van vacíos).
fun obtenerDiasDelMes(anio: Int, mes: Int): List<Int?> {
    val calendar = Calendar.getInstance()
    calendar.set(anio, mes, 1)

    val primerDiaSemana = calendar.get(Calendar.DAY_OF_WEEK) // 1 = Domingo ... 7 = Sábado
    val diasEnElMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val dias = mutableListOf<Int?>()
    repeat(primerDiaSemana - 1) { dias.add(null) }
    for (dia in 1..diasEnElMes) dias.add(dia)
    return dias
}

// Año y mes actuales (mes de 0 a 11), para arrancar el calendario
// mostrando el mes de hoy por defecto.
fun obtenerAnioActual(): Int = Calendar.getInstance().get(Calendar.YEAR)
fun obtenerMesActual(): Int = Calendar.getInstance().get(Calendar.MONTH)
fun obtenerDiaActual(): Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)