package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.AvanceCarrera
import com.example.estudia.modelo.Calificacion
import com.example.estudia.modelo.EstadoPlan
import com.example.estudia.modelo.MateriaPlan
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.avanceCarrera
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Las materias de un año de la carrera.
data class AnioCarrera(val anio: Int, val materias: List<MateriaPlan>)

data class CarreraUiState(
    val cargando: Boolean = true,
    val usuarioId: Int = 0,
    val carrera: String? = null,
    val plan: List<MateriaPlan> = emptyList(),
    val anios: List<AnioCarrera> = emptyList(),
    val avance: AvanceCarrera = AvanceCarrera(0, 0, 0, 0, null),
    // Nombres (en minúscula) de las materias activas de la app, para
    // marcar las del plan que ya están cargadas.
    val enLaApp: Set<String> = emptySet(),
    // Materias activas de la app que todavía no están en el plan.
    val paraImportar: List<String> = emptyList(),
    val anioActual: Int = 1
) {
    val vacio: Boolean get() = !cargando && plan.isEmpty()
}

// ViewModel de "Mi carrera": el plan de estudios con las correlativas.
class CarreraViewModel(private val repositorio: RepositorioEstudia) : ViewModelConAvisos() {

    val uiState: StateFlow<CarreraUiState> = repositorio.usuarioActual
        .map { usuario -> if (usuario == null) CarreraUiState() else armarCarrera(usuario) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CarreraUiState())

    // Agrega (materia == null) o edita una materia del plan. Devuelve el
    // error a mostrar, o null si se guardó.
    fun guardar(materia: MateriaPlan?, nombre: String, anioTexto: String, correlativas: List<Int>): Mensaje? {
        val estado = uiState.value
        val anio = anioTexto.trim().toIntOrNull()
        if (nombre.isBlank()) return Mensaje.ERROR_COMPLETAR_CAMPOS
        if (anio == null || anio !in 1..ANIO_MAXIMO) return Mensaje.ERROR_ANIO_INVALIDO
        viewModelScope.launch {
            if (materia == null) {
                repositorio.agregarMateriaPlan(estado.usuarioId, MateriaPlan(0, nombre.trim(), anio, correlativas = correlativas))
            } else {
                repositorio.actualizarMateriaPlan(
                    estado.usuarioId,
                    materia.copy(nombre = nombre.trim(), anio = anio, correlativas = correlativas.filter { it != materia.id })
                )
            }
        }
        return null
    }

    // Cambia el estado. La nota del final solo se guarda si queda aprobada.
    fun cambiarEstado(materia: MateriaPlan, estado: EstadoPlan, notaTexto: String = ""): Mensaje? {
        var nota: Double? = null
        if (estado == EstadoPlan.APROBADA && notaTexto.isNotBlank()) {
            nota = notaTexto.trim().replace(',', '.').toDoubleOrNull()
            if (nota == null || nota < Calificacion.NOTA_APROBACION || nota > Calificacion.NOTA_MAXIMA) return Mensaje.ERROR_NOTA_FINAL
        }
        viewModelScope.launch {
            repositorio.actualizarMateriaPlan(uiState.value.usuarioId, materia.copy(estado = estado, notaFinal = nota))
        }
        return null
    }

    fun eliminar(materia: MateriaPlan) {
        val usuarioId = uiState.value.usuarioId
        viewModelScope.launch {
            repositorio.eliminarMateriaPlan(usuarioId, materia)
            avisar(Aviso(Mensaje.AVISO_MATERIA_PLAN_ELIMINADA, alDeshacer = { repositorio.restaurarMateriaPlan(usuarioId, materia) }))
        }
    }

    // Pasa las materias activas de la app al plan, como "cursando", en el
    // año que figura en el perfil.
    fun importarMateriasDeLaApp() {
        val estado = uiState.value
        viewModelScope.launch {
            estado.paraImportar.forEach { nombre ->
                repositorio.agregarMateriaPlan(estado.usuarioId, MateriaPlan(0, nombre, estado.anioActual, EstadoPlan.CURSANDO))
            }
            avisar(Aviso(Mensaje.AVISO_PLAN_IMPORTADO, listOf(estado.paraImportar.size)))
        }
    }

    companion object {
        const val ANIO_MAXIMO = 8
    }
}

// Función pura: agrupa el plan por año y calcula el avance.
fun armarCarrera(usuario: Usuario): CarreraUiState {
    val activas = usuario.materias.filter { !it.archivada }
    val enElPlan = usuario.planEstudio.map { it.nombre.trim().lowercase() }.toSet()
    return CarreraUiState(
        cargando = false,
        usuarioId = usuario.id,
        carrera = usuario.carrera,
        plan = usuario.planEstudio,
        anios = usuario.planEstudio.groupBy { it.anio }.toSortedMap().map { (anio, materias) -> AnioCarrera(anio, materias) },
        avance = avanceCarrera(usuario.planEstudio),
        enLaApp = activas.map { it.nombre.trim().lowercase() }.toSet(),
        paraImportar = activas.map { it.nombre }.filter { it.trim().lowercase() !in enElPlan },
        anioActual = usuario.anio?.coerceIn(1, CarreraViewModel.ANIO_MAXIMO) ?: 1
    )
}
