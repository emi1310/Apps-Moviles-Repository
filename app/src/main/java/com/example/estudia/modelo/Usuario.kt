package com.example.estudia.modelo

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

// Representa al usuario de la app. Es dueño de todas sus Materias,
// y a través de ellas llega indirectamente a sus tareas, eventos y notas.
// Modelo de dominio inmutable: el Repository lo arma a partir de las
// entidades de Room (ver datos/local/Mapeos.kt).
// La contraseña NO forma parte del modelo: solo existe (como hash) en la
// base de datos, así nunca llega al ViewModel ni a la UI.
data class Usuario(
    val id: Int,
    val nombreUsuario: String,
    val metodoAuth: MetodoAuth,
    val nombre: String,
    val carrera: String? = null,
    val anio: Int? = null,
    val email: String? = null,
    val metaHorasSemanales: Int? = null,
    val materias: List<Materia> = emptyList()
) {

    // Cuenta el total de notas sumando las notas de todas las materias.
    fun contarNotas(): Int {
        return materias.sumOf { it.notas.size }
    }

    // Cuenta cuántas materias activas (no archivadas) tiene el usuario.
    fun contarMaterias(): Int {
        return materias.count { !it.archivada }
    }

    // Suma la duración de todos los bloques de estudio de todas las
    // materias y la devuelve en horas.
    fun calcularHorasEstudio(): Int {
        return (minutosDeEstudio { true } / 60).toInt()
    }

    // Horas de estudio planificadas en la semana (lunes a domingo) de "hoy".
    fun horasEstudioDeLaSemana(hoy: LocalDate): Int {
        val lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val domingo = lunes.plusDays(6)
        return (minutosDeEstudio { !it.isBefore(lunes) && !it.isAfter(domingo) } / 60).toInt()
    }

    // Días seguidos estudiando (con al menos un bloque de estudio) hasta hoy.
    // Si hoy todavía no estudió, cuenta la racha hasta ayer: sigue viva.
    fun rachaDeEstudio(hoy: LocalDate): Int {
        val dias = materias.flatMap { it.eventos }
            .filter { it.tipo == TipoEvento.BLOQUE_ESTUDIO && !it.fecha.isAfter(hoy) }
            .map { it.fecha }
            .toSet()
        var dia = if (hoy in dias) hoy else hoy.minusDays(1)
        var racha = 0
        while (dia in dias) {
            racha++
            dia = dia.minusDays(1)
        }
        return racha
    }

    private fun minutosDeEstudio(incluirFecha: (LocalDate) -> Boolean): Long {
        return materias.flatMap { it.eventos }
            .filter { it.tipo == TipoEvento.BLOQUE_ESTUDIO && incluirFecha(it.fecha) }
            .sumOf { it.duracionMinutos() }
    }
}
