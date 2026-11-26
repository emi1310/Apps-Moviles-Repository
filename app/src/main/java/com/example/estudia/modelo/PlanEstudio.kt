package com.example.estudia.modelo

// "Mi carrera": el plan de estudios completo, materia por materia, con su
// año y sus correlativas (las materias que hay que tener para cursarla).
//
// Regla de correlatividades que usa la app (la más común en institutos y
// facultades de Argentina):
//  - Para CURSAR una materia, sus correlativas tienen que estar regulares
//    (cursada aprobada) o aprobadas.
//  - Para RENDIR EL FINAL, sus correlativas tienen que estar aprobadas.
enum class EstadoPlan {
    PENDIENTE,   // todavía no se cursó
    CURSANDO,
    REGULAR,     // cursada aprobada, falta el final
    APROBADA
}

data class MateriaPlan(
    val id: Int,
    val nombre: String,
    val anio: Int,                        // año de la carrera (1, 2, 3...)
    val estado: EstadoPlan = EstadoPlan.PENDIENTE,
    val correlativas: List<Int> = emptyList(),   // ids de otras MateriaPlan
    val notaFinal: Double? = null
) {
    // Las correlativas que todavía faltan para cursarla (según el plan).
    fun faltanParaCursar(plan: List<MateriaPlan>): List<MateriaPlan> =
        correlativasEn(plan).filter { it.estado != EstadoPlan.REGULAR && it.estado != EstadoPlan.APROBADA }

    fun faltanParaRendir(plan: List<MateriaPlan>): List<MateriaPlan> =
        correlativasEn(plan).filter { it.estado != EstadoPlan.APROBADA }

    // Se puede anotar: no la cursó todavía y tiene todas las correlativas.
    fun sePuedeCursar(plan: List<MateriaPlan>): Boolean =
        estado == EstadoPlan.PENDIENTE && faltanParaCursar(plan).isEmpty()

    fun sePuedeRendir(plan: List<MateriaPlan>): Boolean =
        estado == EstadoPlan.REGULAR && faltanParaRendir(plan).isEmpty()

    // Las correlativas borradas del plan se ignoran.
    private fun correlativasEn(plan: List<MateriaPlan>): List<MateriaPlan> =
        plan.filter { it.id in correlativas && it.id != id }
}

// Resumen del plan para la cabecera de "Mi carrera".
data class AvanceCarrera(
    val total: Int,
    val aprobadas: Int,
    val regulares: Int,
    val cursando: Int,
    val promedio: Double?           // de las notas de final cargadas
) {
    val porcentaje: Int get() = if (total == 0) 0 else aprobadas * 100 / total
}

fun avanceCarrera(plan: List<MateriaPlan>): AvanceCarrera {
    val notas = plan.filter { it.estado == EstadoPlan.APROBADA }.mapNotNull { it.notaFinal }
    return AvanceCarrera(
        total = plan.size,
        aprobadas = plan.count { it.estado == EstadoPlan.APROBADA },
        regulares = plan.count { it.estado == EstadoPlan.REGULAR },
        cursando = plan.count { it.estado == EstadoPlan.CURSANDO },
        promedio = notas.takeIf { it.isNotEmpty() }?.average()
    )
}
