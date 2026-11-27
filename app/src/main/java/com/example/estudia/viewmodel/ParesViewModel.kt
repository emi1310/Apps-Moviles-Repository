package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.FichaPar
import com.example.estudia.modelo.PENALIZACION_PARES_MILLIS
import com.example.estudia.modelo.armarPares
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class ParesUiState(
    val cargando: Boolean = true,
    val nombreMateria: String = "",
    val colorMateria: Long = 0xFF6C5CE7,
    val fichas: List<FichaPar> = emptyList(),
    val resueltas: Set<Int> = emptySet(),       // ids de las fichas ya unidas
    val elegida: Int? = null,                   // la primera ficha tocada
    val equivocadas: Set<Int> = emptySet(),     // las dos de un intento fallido (se ven en rojo un momento)
    val errores: Int = 0,
    val inicioMillis: Long = 0,
    val finMillis: Long? = null,
    val mejorMillis: Long? = null,              // récord de la materia (antes de esta partida)
    val faltanTarjetas: Boolean = false
) {
    val terminado: Boolean get() = finMillis != null
    val penalizacionMillis: Long get() = errores * PENALIZACION_PARES_MILLIS

    // Tiempo con la penalización de los errores (ahora = hora actual mientras se juega).
    fun tiempoMillis(ahora: Long): Long = ((finMillis ?: ahora) - inicioMillis).coerceAtLeast(0) + penalizacionMillis

    val esRecord: Boolean get() = finMillis != null && (mejorMillis == null || tiempoMillis(finMillis) < mejorMillis)
}

// Juego "Unir pares": se mezclan preguntas y respuestas de hasta 6 tarjetas
// y hay que tocar cada pregunta con su respuesta lo más rápido posible.
// Cada error suma un segundo. Se guarda el mejor tiempo de cada materia.
// Como el quiz, no cambia el repaso espaciado: es una práctica.
class ParesViewModel(
    private val repositorio: RepositorioEstudia,
    private val materiaId: Int,
    private val random: Random = Random.Default,
    private val reloj: () -> Long = { System.currentTimeMillis() }
) : ViewModelConAvisos() {

    private val juego = MutableStateFlow(ParesUiState())
    val uiState: StateFlow<ParesUiState> = juego.asStateFlow()

    private var limpiarError: Job? = null

    init {
        empezar()
    }

    fun empezar() {
        viewModelScope.launch {
            val materia = repositorio.usuarioActual.first()?.materias?.firstOrNull { it.id == materiaId }
            val fichas = materia?.let { armarPares(it.tarjetas, random) } ?: emptyList()
            juego.value = ParesUiState(
                cargando = false,
                nombreMateria = materia?.nombre ?: "",
                colorMateria = materia?.color ?: 0xFF6C5CE7,
                fichas = fichas,
                inicioMillis = reloj(),
                mejorMillis = repositorio.mejorTiempoPares(materiaId).first(),
                faltanTarjetas = fichas.isEmpty()
            )
        }
    }

    fun tocar(id: Int) {
        val estado = juego.value
        if (estado.terminado || id in estado.resueltas) return
        val ficha = estado.fichas.firstOrNull { it.id == id } ?: return
        val primera = estado.elegida?.let { e -> estado.fichas.firstOrNull { it.id == e } }
        when {
            primera == null -> juego.value = estado.copy(elegida = id, equivocadas = emptySet())
            primera.id == id -> juego.value = estado.copy(elegida = null)
            // Dos preguntas o dos respuestas: cambia la elegida, no es error.
            primera.esPregunta == ficha.esPregunta -> juego.value = estado.copy(elegida = id, equivocadas = emptySet())
            primera.par == ficha.par -> {
                val resueltas = estado.resueltas + primera.id + id
                val fin = if (resueltas.size == estado.fichas.size) reloj() else null
                juego.value = estado.copy(resueltas = resueltas, elegida = null, equivocadas = emptySet(), finMillis = fin)
                if (fin != null) guardarRecord()
            }
            else -> {
                juego.value = estado.copy(elegida = null, equivocadas = setOf(primera.id, id), errores = estado.errores + 1)
                limpiarError?.cancel()
                limpiarError = viewModelScope.launch {
                    delay(700)
                    juego.update { it.copy(equivocadas = emptySet()) }
                }
            }
        }
    }

    private fun guardarRecord() {
        val estado = juego.value
        val fin = estado.finMillis ?: return
        if (!estado.esRecord) return
        viewModelScope.launch { repositorio.guardarMejorTiempoPares(materiaId, estado.tiempoMillis(fin)) }
    }
}
