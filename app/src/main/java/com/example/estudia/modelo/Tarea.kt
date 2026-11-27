package com.example.estudia.modelo

import java.time.LocalDate
import java.time.LocalDateTime

// Representa una tarea pendiente dentro de una materia.
// fechaLimite es una fecha real (o null si la tarea no tiene fecha),
// así se puede ordenar y saber si está vencida.
// Versión 6: cada tarea tiene una prioridad y puede tener subtareas (una
// lista de pasos, ej: "Leer el apunte", "Hacer los ejercicios").
data class Tarea(
    val id: Int,
    val titulo: String,
    val fechaLimite: LocalDate? = null,
    val completada: Boolean = false,
    val prioridad: Prioridad = Prioridad.MEDIA,
    val subtareas: List<Subtarea> = emptyList(),
    // Versión 7: cuántos días antes de la fecha límite avisar (0 = el mismo
    // día). null = sin aviso. Solo tiene sentido si hay fecha límite.
    val avisoDiasAntes: Int? = null,
    // Versión 14: tarea que se repite (ej: "TP cada lunes"). Al completarla
    // se crea sola la siguiente, con la próxima fecha. null = no se repite.
    val repeticion: Repeticion? = null
) {
    fun estaVencida(hoy: LocalDate): Boolean {
        val fecha = fechaLimite ?: return false
        return !completada && fecha.isBefore(hoy)
    }

    // Cuándo llega el aviso de esta tarea: a las 9:00 del día elegido.
    // null si no tiene aviso, no tiene fecha, ya está hecha o ya pasó.
    fun momentoDelAviso(ahora: LocalDateTime): LocalDateTime? {
        val dias = avisoDiasAntes ?: return null
        val fecha = fechaLimite ?: return null
        if (completada) return null
        return fecha.minusDays(dias.toLong()).atTime(HORA_AVISO_TAREA, 0).takeIf { it.isAfter(ahora) }
    }

    companion object {
        const val HORA_AVISO_TAREA = 9
        // Opciones de aviso que se ofrecen: el mismo día, 1, 2 y 7 días antes.
        val OPCIONES_AVISO = listOf(0, 1, 2, 7)
    }

    val subtareasHechas: Int get() = subtareas.count { it.completada }
}

// Un paso de una tarea. Se marca por separado de la tarea.
data class Subtarea(
    val id: Int,
    val titulo: String,
    val completada: Boolean = false
)

// Prioridad de una tarea. "peso" sirve para ordenar (alta primero) y es el
// número que se guarda en la base.
enum class Prioridad(val peso: Int) {
    BAJA(0), MEDIA(1), ALTA(2);

    companion object {
        fun desdePeso(peso: Int): Prioridad = entries.firstOrNull { it.peso == peso } ?: MEDIA
    }
}

// Cada cuánto se repite una tarea. Se guarda en la base con su nombre.
enum class Repeticion {
    DIARIA, SEMANAL, QUINCENAL, MENSUAL;

    fun siguiente(fecha: LocalDate): LocalDate = when (this) {
        DIARIA -> fecha.plusDays(1)
        SEMANAL -> fecha.plusWeeks(1)
        QUINCENAL -> fecha.plusWeeks(2)
        MENSUAL -> fecha.plusMonths(1)
    }

    companion object {
        fun desdeTexto(texto: String?): Repeticion? = entries.firstOrNull { it.name == texto }
    }
}

// La tarea que hay que crear cuando se completa una que se repite (null si
// no corresponde): mismo título, prioridad, aviso, pasos (sin marcar) y
// repetición, con la próxima fecha. Si se completó tarde, salta hasta la
// próxima fecha desde hoy (un TP de los lunes hecho el miércoles pasa al
// lunes que viene). Sin fecha límite, cuenta desde hoy.
fun siguienteTareaRepetida(anterior: Tarea?, nueva: Tarea, hoy: LocalDate): Tarea? {
    val repeticion = nueva.repeticion ?: return null
    if (anterior == null || anterior.completada || !nueva.completada) return null
    var fecha = repeticion.siguiente(nueva.fechaLimite ?: hoy)
    while (fecha.isBefore(hoy)) fecha = repeticion.siguiente(fecha)
    return nueva.copy(
        id = 0,
        fechaLimite = fecha,
        completada = false,
        subtareas = nueva.subtareas.map { it.copy(id = 0, completada = false) }
    )
}
