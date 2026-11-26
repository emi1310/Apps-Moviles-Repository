package com.example.estudia.modelo

// Representa una materia del usuario (ej: "Matemática II").
// Es dueña de sus propias tareas, eventos y notas: si esta Materia
// se elimina, sus tareas/eventos/notas dejan de existir con ella
// (esto refleja la composición que definimos en el diagrama UML;
// en Room se resuelve con ForeignKey + CASCADE).
//
// Es un modelo de dominio: no depende de Compose ni de Room. Las listas
// son List comunes (inmutables); cuando algo cambia, Room emite una
// Materia nueva por el Flow y la pantalla se redibuja.
data class Materia(
    val id: Int,
    val nombre: String,
    val color: Long,         // color en formato ARGB, ej: 0xFF6C5CE7
    val archivada: Boolean = false,
    val tareas: List<Tarea> = emptyList(),
    val eventos: List<Evento> = emptyList(),
    val notas: List<Nota> = emptyList(),
    val archivos: List<Archivo> = emptyList(),  // adjuntos de la materia (no de sus notas)
    val calificaciones: List<Calificacion> = emptyList(),  // notas de los exámenes
    val tarjetas: List<Tarjeta> = emptyList(),             // tarjetas de estudio (versión 8)
    val evaluaciones: List<Evaluacion> = emptyList(),      // cómo se compone la nota (versión 9)
    // Versión 10: reglas de la cursada de cada materia.
    val asistenciaMinima: Int = ASISTENCIA_MINIMA,  // % de asistencia que pide
    val promocional: Boolean = true,                // ¿se puede promocionar (sin final)?
    val notaPromocion: Double = NOTA_PROMOCION,     // nota para promocionar
    val tieneFinal: Boolean = true                  // ¿hay que rendir final si no promociona?
) {
    // Tarjetas que tocan repasar hoy (o que se atrasaron).
    fun tarjetasParaHoy(hoy: java.time.LocalDate): List<Tarjeta> = tarjetas.filter { it.tocaRepasar(hoy) }

    // Archivos de la materia y de todas sus notas (para borrarlos del disco).
    fun todosLosArchivos(): List<Archivo> = archivos + notas.flatMap { it.archivos }

    // Promedio de las notas de los exámenes, o null si todavía no hay ninguna.
    fun promedio(): Double? = calificaciones.takeIf { it.isNotEmpty() }?.map { it.nota }?.average()

    // Calcula el % de tareas completadas sobre el total.
    // Si no hay tareas cargadas, devuelve 100 (para que no rompa el porcentaje).
    fun calcularProgreso(): Int {
        if (tareas.isEmpty()) return 100
        val completadas = tareas.count { it.completada }
        return (completadas * 100) / tareas.size
    }
}

// Reglas de la cursada de una materia (versión 10): cuánta asistencia pide,
// si se puede promocionar y con qué nota, y si tiene final.
data class ReglasCursada(
    val asistenciaMinima: Int = ASISTENCIA_MINIMA,
    val promocional: Boolean = true,
    val notaPromocion: Double = NOTA_PROMOCION,
    val tieneFinal: Boolean = true
)

val Materia.reglas: ReglasCursada get() = ReglasCursada(asistenciaMinima, promocional, notaPromocion, tieneFinal)

fun Materia.conReglas(r: ReglasCursada): Materia =
    copy(asistenciaMinima = r.asistenciaMinima, promocional = r.promocional, notaPromocion = r.notaPromocion, tieneFinal = r.tieneFinal)

enum class ErrorReglas { ASISTENCIA, NOTA_PROMOCION }

data class LecturaReglas(val reglas: ReglasCursada? = null, val error: ErrorReglas? = null)

// Lee lo que se escribió en el diálogo de la materia. La asistencia va de 0
// a 100 (puede tener "%"); la nota de promoción, entre la de aprobación (4)
// y 10, y solo importa si la materia es promocional.
fun leerReglas(asistenciaTexto: String, promocional: Boolean, notaTexto: String, tieneFinal: Boolean): LecturaReglas {
    val asistencia = asistenciaTexto.trim().removeSuffix("%").trim().toIntOrNull()
    if (asistencia == null || asistencia !in 0..100) return LecturaReglas(error = ErrorReglas.ASISTENCIA)
    val nota = if (!promocional) NOTA_PROMOCION else {
        val n = notaTexto.trim().replace(',', '.').toDoubleOrNull()
        if (n == null || n < Calificacion.NOTA_APROBACION || n > Calificacion.NOTA_MAXIMA) return LecturaReglas(error = ErrorReglas.NOTA_PROMOCION)
        n
    }
    return LecturaReglas(ReglasCursada(asistencia, promocional, nota, tieneFinal))
}
