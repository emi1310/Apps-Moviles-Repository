package com.example.estudia.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val formato = SimpleDateFormat("dd/MM/yyyy", Locale("es", "AR"))

private val nombresMeses = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

fun nombreDelMes(mes: Int): String = nombresMeses[mes]

fun obtenerFechaHoyComoTexto(): String {
    return formato.format(Calendar.getInstance().time)
}

fun obtenerFechaMananaComoTexto(): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_MONTH, 1)
    return formato.format(calendar.time)
}

fun formatearFecha(anio: Int, mes: Int, dia: Int): String {
    val calendar = Calendar.getInstance()
    calendar.set(anio, mes, dia)
    return formato.format(calendar.time)
}

fun obtenerDiasDelMes(anio: Int, mes: Int): List<Int?> {
    val calendar = Calendar.getInstance()
    calendar.set(anio, mes, 1)

    val primerDiaSemana = calendar.get(Calendar.DAY_OF_WEEK)
    val diasEnElMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val dias = mutableListOf<Int?>()
    repeat(primerDiaSemana - 1) { dias.add(null) }
    for (dia in 1..diasEnElMes) dias.add(dia)
    return dias
}

fun obtenerAnioActual(): Int = Calendar.getInstance().get(Calendar.YEAR)
fun obtenerMesActual(): Int = Calendar.getInstance().get(Calendar.MONTH)
fun obtenerDiaActual(): Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

fun parsearFechaAMilisegundos(fecha: String): Long {
    return try {
        formato.parse(fecha)?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}

fun minutosDelDia(hora: String): Int {
    return hora.substringBefore(':').toInt() * 60 + hora.substringAfter(':').toInt()
}
