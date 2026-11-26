package com.example.estudia.modelo

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Una mesa de final: la materia, cuándo es la mesa, hasta cuándo hay que
// inscribirse y cuándo vence la regularidad (después de esa fecha hay que
// volver a cursar). nota null = todavía no se rindió.
data class MesaFinal(
    val id: Int,
    val materia: String,
    val fechaMesa: LocalDate,
    val cierreInscripcion: LocalDate? = null,
    val vencimientoRegularidad: LocalDate? = null,
    val inscripto: Boolean = false,
    val nota: Double? = null
) {
    val rendido: Boolean get() = nota != null
    val aprobado: Boolean get() = nota?.let { Calificacion.aprueba(it) } ?: false

    fun diasParaMesa(hoy: LocalDate): Long = ChronoUnit.DAYS.between(hoy, fechaMesa)

    // Inscripción todavía abierta y sin anotarse.
    fun inscripcionPendiente(hoy: LocalDate): Boolean =
        !inscripto && !rendido && cierreInscripcion != null && !cierreInscripcion.isBefore(hoy)

    // Se pasó el cierre y no se anotó: ya no puede rendir en esta mesa.
    fun inscripcionPerdida(hoy: LocalDate): Boolean =
        !inscripto && !rendido && cierreInscripcion != null && cierreInscripcion.isBefore(hoy)

    // La regularidad ya venció, o vence antes de la fecha de la mesa.
    fun regularidadVencida(hoy: LocalDate): Boolean =
        !rendido && vencimientoRegularidad != null &&
            (vencimientoRegularidad.isBefore(hoy) || vencimientoRegularidad.isBefore(fechaMesa))

    fun regularidadVencePronto(hoy: LocalDate): Boolean =
        !rendido && vencimientoRegularidad != null && !vencimientoRegularidad.isBefore(hoy) &&
            ChronoUnit.DAYS.between(hoy, vencimientoRegularidad) <= DIAS_AVISO_REGULARIDAD

    companion object {
        // Días antes del cierre de inscripción en que empieza a avisar.
        const val DIAS_AVISO_INSCRIPCION = 2L
        const val DIAS_AVISO_REGULARIDAD = 30L
    }
}

// Qué avisar hoy de los finales (lo usa el resumen diario de las 8:00).
data class AvisoFinal(val final: MesaFinal, val tipo: TipoAvisoFinal)

enum class TipoAvisoFinal { CIERRA_INSCRIPCION, MESA_MANANA, MESA_HOY }

fun avisosDeFinales(finales: List<MesaFinal>, hoy: LocalDate): List<AvisoFinal> = finales.filter { !it.rendido }.mapNotNull { f ->
    val cierre = f.cierreInscripcion
    when {
        f.fechaMesa == hoy && f.inscripto -> AvisoFinal(f, TipoAvisoFinal.MESA_HOY)
        f.fechaMesa == hoy.plusDays(1) && f.inscripto -> AvisoFinal(f, TipoAvisoFinal.MESA_MANANA)
        f.inscripcionPendiente(hoy) && cierre != null &&
            ChronoUnit.DAYS.between(hoy, cierre) <= MesaFinal.DIAS_AVISO_INSCRIPCION -> AvisoFinal(f, TipoAvisoFinal.CIERRA_INSCRIPCION)
        else -> null
    }
}
