package com.example.estudia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.Prioridad
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.Usuario
import com.example.estudia.notificaciones.AvisosSistema
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// Matriz de prioridades (urgente / importante), para decidir qué hacer
// primero con las tareas pendientes:
//  - Urgente: vence en DIAS_URGENTE días o menos (o ya está vencida).
//  - Importante: prioridad alta.
enum class Cuadrante { HACER_YA, PLANIFICAR, SACAR_RAPIDO, DESPUES }

const val DIAS_URGENTE = 2L

fun cuadranteDe(tarea: Tarea, hoy: LocalDate): Cuadrante {
    val fecha = tarea.fechaLimite
    val urgente = fecha != null && !fecha.isAfter(hoy.plusDays(DIAS_URGENTE))
    val importante = tarea.prioridad == Prioridad.ALTA
    return when {
        urgente && importante -> Cuadrante.HACER_YA
        importante -> Cuadrante.PLANIFICAR
        urgente -> Cuadrante.SACAR_RAPIDO
        else -> Cuadrante.DESPUES
    }
}

data class MatrizUiState(
    val cargando: Boolean = true,
    val hoy: LocalDate = LocalDate.now(),
    // Las tareas pendientes de cada cuadrante, las que vencen antes primero.
    val cuadrantes: Map<Cuadrante, List<TareaConMateria>> = Cuadrante.entries.associateWith { emptyList() }
) {
    val total: Int get() = cuadrantes.values.sumOf { it.size }
}

// Función pura: reparte las tareas pendientes de las materias activas.
fun armarMatriz(usuario: Usuario, hoy: LocalDate): MatrizUiState {
    val pendientes = usuario.materias.filter { !it.archivada }.flatMap { materia ->
        materia.tareas.filter { !it.completada }.map { TareaConMateria(it, materia.id, materia.nombre, materia.color) }
    }.sortedWith(compareBy<TareaConMateria> { it.tarea.fechaLimite == null }.thenBy { it.tarea.fechaLimite })
    val porCuadrante = pendientes.groupBy { cuadranteDe(it.tarea, hoy) }
    return MatrizUiState(
        cargando = false,
        hoy = hoy,
        cuadrantes = Cuadrante.entries.associateWith { porCuadrante[it].orEmpty() }
    )
}

class MatrizViewModel(
    private val repositorio: RepositorioEstudia,
    private val avisosSistema: AvisosSistema? = null,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    val uiState: StateFlow<MatrizUiState> = repositorio.usuarioActual
        .map { usuario -> if (usuario == null) MatrizUiState() else armarMatriz(usuario, hoy()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MatrizUiState())

    // Completar desde la matriz (si se repite, aparece la siguiente con su aviso).
    fun completar(item: TareaConMateria) {
        viewModelScope.launch {
            val siguiente = repositorio.actualizarTarea(item.materiaId, item.tarea.copy(completada = true))
            avisosSistema?.cancelarTarea(item.tarea.id)
            siguiente?.let { avisosSistema?.programarTarea(it) }
        }
    }

    // Pasar de "importante" (prioridad alta) a no importante (media), o al revés.
    fun alternarImportante(item: TareaConMateria) {
        val nueva = if (item.tarea.prioridad == Prioridad.ALTA) Prioridad.MEDIA else Prioridad.ALTA
        viewModelScope.launch { repositorio.actualizarTarea(item.materiaId, item.tarea.copy(prioridad = nueva)) }
    }
}
