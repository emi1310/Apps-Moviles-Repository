package com.example.estudia.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Funciones auxiliares para mostrar fechas y horas. Las fechas se guardan
// como LocalDate / LocalTime (java.time); acá solo se convierten a texto
// para mostrarlas, con el formato que usa la app: "17/06/2026" y "18:30".

private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

fun LocalDate.aTexto(): String = format(formatoFecha)

fun LocalTime.aTexto(): String = format(formatoHora)

// El DatePicker de Material 3 trabaja con milisegundos en UTC.
fun LocalDate.aMilisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun fechaDesdeMilisUtc(milis: Long): LocalDate = Instant.ofEpochMilli(milis).atZone(ZoneOffset.UTC).toLocalDate()
