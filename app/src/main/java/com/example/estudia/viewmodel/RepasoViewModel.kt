package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.Tarjeta
import com.example.estudia.modelo.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// Una tarjeta para repasar, con los datos de su materia.
data class TarjetaEnRepaso(val tarjeta: Tarjeta, val materiaId: Int, val nombreMateria: String, val colorMateria: Long)

data class RepasoUiState(
    val cargando: Boolean = true,
    val nombreMateria: String? = null,          // null = todas las materias
    val pendientes: List<TarjetaEnRepaso> = emptyList(),
    val repasadas: Int = 0,                     // en esta sesión de repaso
    val mostrandoRespuesta: Boolean = false
) {
    val actual: TarjetaEnRepaso? get() = pendientes.firstOrNull()
}

// ViewModel del repaso de tarjetas (repetición espaciada). materiaId = -1
// repasa las de todas las materias activas. Al responder, la tarjeta se
// guarda con su caja y su próximo repaso nuevos: como ya no toca hoy, sale
// sola de la lista (la lista sale de la base, como en el resto de la app).
class RepasoViewModel(
    private val repositorio: RepositorioEstudia,
    private val materiaId: Int,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModelConAvisos() {

    private val repasadas = MutableStateFlow(0)
    private val mostrandoRespuesta = MutableStateFlow(false)

    val uiState: StateFlow<RepasoUiState> =
        combine(repositorio.usuarioActual, repasadas, mostrandoRespuesta) { usuario, cantidad, mostrando ->
            if (usuario == null) RepasoUiState()
            else RepasoUiState(
                cargando = false,
                nombreMateria = usuario.materias.firstOrNull { it.id == materiaId }?.nombre,
                pendientes = tarjetasParaRepasar(usuario, materiaId, hoy()),
                repasadas = cantidad,
                mostrandoRespuesta = mostrando
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RepasoUiState())

    fun verRespuesta() {
        mostrandoRespuesta.value = true
    }

    fun responder(sabia: Boolean) {
        val actual = uiState.value.actual ?: return
        mostrandoRespuesta.value = false
        repasadas.value++
        viewModelScope.launch {
            repositorio.actualizarTarjeta(actual.materiaId, actual.tarjeta.responder(sabia, hoy()))
        }
    }
}

// Función pura: las tarjetas que tocan hoy, de una materia o de todas las
// activas, las más atrasadas primero.
fun tarjetasParaRepasar(usuario: Usuario, materiaId: Int, hoy: LocalDate): List<TarjetaEnRepaso> =
    usuario.materias
        .filter { if (materiaId == TODAS_LAS_MATERIAS) !it.archivada else it.id == materiaId }
        .flatMap { m -> m.tarjetasParaHoy(hoy).map { TarjetaEnRepaso(it, m.id, m.nombre, m.color) } }
        .sortedWith(compareBy<TarjetaEnRepaso> { it.tarjeta.proximoRepaso }.thenBy { it.tarjeta.id })

const val TODAS_LAS_MATERIAS = -1
