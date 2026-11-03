package com.example.estudia.modelo

// Sesión del temporizador de estudio (método Pomodoro) que está en curso.
//
// No se guarda un contador que baja cada segundo: se guarda CUÁNDO termina
// (finMillis, hora del reloj del sistema). Así el tiempo sigue corriendo
// aunque el usuario cierre la app, y al volver se calcula lo que falta.
// Si está en pausa, finMillis es null y restantePausaMillis guarda lo que
// faltaba al pausar.
data class SesionEstudio(
    val materiaId: Int,
    val nombreMateria: String,
    val minutos: Int,                    // duración elegida (25, 45, 60...)
    val finMillis: Long?,
    val restantePausaMillis: Long = 0
) {
    val enPausa: Boolean get() = finMillis == null

    val duracionMillis: Long get() = minutos * 60_000L

    fun restanteMillis(ahora: Long): Long =
        finMillis?.let { (it - ahora).coerceAtLeast(0) } ?: restantePausaMillis

    // Minutos que faltan, redondeados hacia arriba (quedan 30 s -> "1 min").
    fun minutosRestantes(ahora: Long): Long = (restanteMillis(ahora) + 59_999) / 60_000

    fun terminada(ahora: Long): Boolean = !enPausa && restanteMillis(ahora) == 0L

    // Minutos completos que ya se estudiaron (lo que se guarda si se corta antes).
    fun minutosEstudiados(ahora: Long): Int =
        ((duracionMillis - restanteMillis(ahora)) / 60_000L).toInt()

    // De 0 (recién empieza) a 1 (terminó), para el anillo de progreso.
    fun avance(ahora: Long): Float = 1f - restanteMillis(ahora).toFloat() / duracionMillis

    fun pausar(ahora: Long): SesionEstudio =
        if (enPausa) this else copy(finMillis = null, restantePausaMillis = restanteMillis(ahora))

    fun reanudar(ahora: Long): SesionEstudio =
        if (enPausa) copy(finMillis = ahora + restantePausaMillis, restantePausaMillis = 0) else this

    companion object {
        fun nueva(materia: Materia, minutos: Int, ahora: Long) = SesionEstudio(
            materiaId = materia.id,
            nombreMateria = materia.nombre,
            minutos = minutos,
            finMillis = ahora + minutos * 60_000L
        )
    }
}
