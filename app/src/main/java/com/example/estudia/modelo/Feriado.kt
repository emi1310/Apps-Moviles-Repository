package com.example.estudia.modelo

import java.time.LocalDate

// Un feriado (ej: "Día de la Independencia"). Se traen del calendario de
// feriados de la cuenta de Google del teléfono o, si no está, de internet.
// Los días feriados no hay clase: no se marcan ni cuentan en la asistencia.
data class Feriado(
    val fecha: LocalDate,
    val nombre: String
)
