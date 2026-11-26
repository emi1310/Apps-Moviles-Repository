package com.example.estudia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.ResumenAsistencia
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.clasesEntre
import com.example.estudia.modelo.resumenAsistencia
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

// Asistencia de una materia, para la lista de arriba.
data class AsistenciaMateria(val materiaId: Int, val nombre: String, val color: Long, val resumen: ResumenAsistencia)

// Una clase que ya pasó (o es hoy), para marcar presente o ausente.
data class ClaseParaMarcar(
    val eventoId: Int,
    val nombreMateria: String,
    val colorMateria: Long,
    val titulo: String,
    val fecha: LocalDate,
    val hora: LocalTime,
    val presente: Boolean?          // null = sin marcar
)

data class AsistenciaUiState(
    val cargando: Boolean = true,
    val hoy: LocalDate = LocalDate.now(),
    val materias: List<AsistenciaMateria> = emptyList(),
    val clases: List<ClaseParaMarcar> = emptyList()
) {
    val sinMarcar: Int get() = clases.count { it.presente == null }
}

// ViewModel de la asistencia: las clases de las últimas dos semanas para
// marcarlas y el porcentaje de cada materia.
class AsistenciaViewModel(
    private val repositorio: RepositorioEstudia,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    // Los feriados no se marcan ni cuentan: ese día no hubo clase.
    val uiState: StateFlow<AsistenciaUiState> = combine(repositorio.usuarioActual, repositorio.feriados) { usuario, feriados ->
        if (usuario == null) AsistenciaUiState() else armarAsistencia(usuario, hoy(), feriados.map { it.fecha }.toSet())
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AsistenciaUiState())

    // Tocar el mismo botón otra vez borra la marca.
    fun marcar(clase: ClaseParaMarcar, presente: Boolean) {
        viewModelScope.launch {
            repositorio.marcarAsistencia(clase.eventoId, clase.fecha, if (clase.presente == presente) null else presente)
        }
    }
}

const val DIAS_PARA_MARCAR = 14L

// Función pura: materias activas con clases y las clases de los últimos
// 14 días (las más recientes primero).
fun armarAsistencia(usuario: Usuario, hoy: LocalDate, feriados: Set<LocalDate> = emptySet()): AsistenciaUiState {
    val activas = usuario.materias.filter { m -> !m.archivada && m.eventos.any { it.tipo == com.example.estudia.modelo.TipoEvento.CLASE } }
    val clases = activas.flatMap { materia ->
        clasesEntre(materia, hoy.minusDays(DIAS_PARA_MARCAR - 1), hoy, feriados).map { (evento, dia) ->
            ClaseParaMarcar(
                eventoId = evento.id,
                nombreMateria = materia.nombre,
                colorMateria = materia.color,
                titulo = evento.titulo,
                fecha = dia,
                hora = evento.horaInicio,
                presente = evento.asistencias.firstOrNull { it.fecha == dia }?.presente
            )
        }
    }.sortedWith(compareByDescending<ClaseParaMarcar> { it.fecha }.thenBy { it.hora })
    return AsistenciaUiState(
        cargando = false,
        hoy = hoy,
        materias = activas.map { AsistenciaMateria(it.id, it.nombre, it.color, resumenAsistencia(it, feriados)) },
        clases = clases
    )
}
