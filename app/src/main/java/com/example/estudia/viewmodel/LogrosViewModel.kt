package com.example.estudia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.Calificacion
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

// Los logros (medallas) que se pueden ganar. Cada uno tiene un objetivo;
// los textos, íconos y colores los pone la UI (ver LogrosScreen).
enum class TipoLogro(val objetivo: Int) {
    PRIMERA_TAREA(1),
    DIEZ_TAREAS(10),
    VEINTICINCO_TAREAS(25),
    RACHA_3(3),
    RACHA_7(7),
    HORAS_10(10),
    HORAS_50(50),
    META_CUMPLIDA(1),
    APROBADO(1),
    PROMEDIO_8(8),
    CINCO_NOTAS(5)
}

// Un logro con cuánto se avanzó (actual) hacia su objetivo.
data class Logro(val tipo: TipoLogro, val actual: Int) {
    val objetivo: Int get() = tipo.objetivo
    val conseguido: Boolean get() = actual >= objetivo
    val progreso: Float get() = (actual.toFloat() / objetivo).coerceIn(0f, 1f)
}

data class LogrosUiState(
    val cargando: Boolean = true,
    val logros: List<Logro> = emptyList()
) {
    val conseguidos: Int get() = logros.count { it.conseguido }
}

// ViewModel de la pantalla de Logros: solo lee al usuario y calcula. Los
// logros no se guardan en la base: salen de los datos que ya hay (tareas,
// bloques de estudio, notas de exámenes), así siempre están al día.
class LogrosViewModel(
    repositorio: RepositorioEstudia,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    val uiState: StateFlow<LogrosUiState> = repositorio.usuarioActual
        .map { usuario -> LogrosUiState(cargando = false, logros = if (usuario == null) emptyList() else calcularLogros(usuario, hoy())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LogrosUiState())
}

// Función pura (sin Android ni corrutinas): fácil de testear. Cuenta todas
// las materias (también las archivadas): lo que ya se logró no se pierde
// por archivar una materia. Los conseguidos van primero.
fun calcularLogros(usuario: Usuario, hoy: LocalDate): List<Logro> {
    val tareasHechas = usuario.materias.sumOf { materia -> materia.tareas.count { it.completada } }
    val mejorRacha = calcularEstadisticas(usuario, hoy).mejorRacha

    // Bloques de estudio que ya pasaron (hasta hoy), agrupados por semana.
    val bloques = usuario.materias.flatMap { it.eventos }
        .filter { it.tipo == TipoEvento.BLOQUE_ESTUDIO && !it.fecha.isAfter(hoy) }
    val horas = (bloques.sumOf { it.duracionMinutos() } / 60).toInt()
    val meta = usuario.metaHorasSemanales?.takeIf { it > 0 }
    val semanasCumplidas = if (meta == null) 0 else bloques
        .groupBy { it.fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
        .count { (_, deLaSemana) -> deLaSemana.sumOf { it.duracionMinutos() } >= meta * 60L }

    val calificaciones = usuario.materias.flatMap { it.calificaciones }
    val aprobados = calificaciones.count { it.nota >= Calificacion.NOTA_APROBACION }
    // El promedio cuenta en enteros (7,6 todavía no llega a 8).
    val promedio = calificaciones.takeIf { it.isNotEmpty() }?.map { it.nota }?.average()?.toInt() ?: 0

    return listOf(
        Logro(TipoLogro.PRIMERA_TAREA, tareasHechas),
        Logro(TipoLogro.DIEZ_TAREAS, tareasHechas),
        Logro(TipoLogro.VEINTICINCO_TAREAS, tareasHechas),
        Logro(TipoLogro.RACHA_3, mejorRacha),
        Logro(TipoLogro.RACHA_7, mejorRacha),
        Logro(TipoLogro.HORAS_10, horas),
        Logro(TipoLogro.HORAS_50, horas),
        Logro(TipoLogro.META_CUMPLIDA, semanasCumplidas),
        Logro(TipoLogro.APROBADO, aprobados),
        Logro(TipoLogro.PROMEDIO_8, promedio),
        Logro(TipoLogro.CINCO_NOTAS, usuario.contarNotas())
    ).map { it.copy(actual = it.actual.coerceAtMost(it.objetivo)) }
        .sortedByDescending { it.conseguido }
}
