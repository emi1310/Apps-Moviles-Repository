package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.Tarjeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

// Una pregunta del quiz: la pregunta de una tarjeta, su respuesta y tres
// respuestas de otras tarjetas de la misma materia, mezcladas.
data class PreguntaQuiz(val pregunta: String, val opciones: List<String>, val correcta: Int)

data class QuizUiState(
    val cargando: Boolean = true,
    val nombreMateria: String = "",
    val colorMateria: Long = 0xFF6C5CE7,
    val preguntas: List<PreguntaQuiz> = emptyList(),
    val indice: Int = 0,
    val elegida: Int? = null,         // la opción que tocó en la pregunta actual
    val aciertos: Int = 0,
    val terminado: Boolean = false,
    val inicioMillis: Long = 0,
    val finMillis: Long? = null,
    // true si la materia no tiene suficientes tarjetas (hacen falta 4 con
    // respuestas distintas).
    val faltanTarjetas: Boolean = false
) {
    val actual: PreguntaQuiz? get() = preguntas.getOrNull(indice)
    val puntaje: Int get() = if (preguntas.isEmpty()) 0 else aciertos * 100 / preguntas.size
}

// ViewModel del quiz de opción múltiple con las tarjetas de una materia.
// No guarda nada: es una práctica (el repaso espaciado sigue en Repaso).
class QuizViewModel(
    private val repositorio: RepositorioEstudia,
    private val materiaId: Int,
    private val random: Random = Random.Default,
    private val reloj: () -> Long = { System.currentTimeMillis() }
) : ViewModelConAvisos() {

    private val juego = MutableStateFlow(QuizUiState())

    val uiState: StateFlow<QuizUiState> = juego.asStateFlow()

    init {
        empezar()
    }

    fun empezar() {
        viewModelScope.launch {
            val usuario = repositorio.usuarioActual.first() ?: return@launch
            val materia = usuario.materias.firstOrNull { it.id == materiaId }
            val preguntas = materia?.let { armarQuiz(it.tarjetas, random) } ?: emptyList()
            juego.value = QuizUiState(
                cargando = false,
                nombreMateria = materia?.nombre ?: "",
                colorMateria = materia?.color ?: 0xFF6C5CE7,
                preguntas = preguntas,
                inicioMillis = reloj(),
                faltanTarjetas = preguntas.isEmpty()
            )
        }
    }

    // Una sola respuesta por pregunta.
    fun elegir(opcion: Int) {
        val estado = juego.value
        val actual = estado.actual ?: return
        if (estado.elegida != null) return
        juego.value = estado.copy(elegida = opcion, aciertos = estado.aciertos + if (opcion == actual.correcta) 1 else 0)
    }

    fun siguiente() {
        val estado = juego.value
        if (estado.elegida == null) return
        juego.value = if (estado.indice + 1 >= estado.preguntas.size) {
            estado.copy(terminado = true, finMillis = reloj())
        } else {
            estado.copy(indice = estado.indice + 1, elegida = null)
        }
    }
}

const val PREGUNTAS_POR_QUIZ = 10
const val OPCIONES_POR_PREGUNTA = 4

// Función pura: hasta 10 preguntas al azar. Las opciones incorrectas salen
// de las respuestas de las otras tarjetas (sin repetir la correcta).
// Lista vacía si no hay al menos 4 respuestas distintas.
fun armarQuiz(tarjetas: List<Tarjeta>, random: Random, cantidad: Int = PREGUNTAS_POR_QUIZ): List<PreguntaQuiz> {
    val respuestas = tarjetas.map { it.respuesta.trim() }.distinctBy { it.lowercase() }
    if (respuestas.size < OPCIONES_POR_PREGUNTA) return emptyList()
    return tarjetas.shuffled(random).take(cantidad).map { tarjeta ->
        val correcta = tarjeta.respuesta.trim()
        val incorrectas = respuestas.filter { !it.equals(correcta, ignoreCase = true) }
            .shuffled(random)
            .take(OPCIONES_POR_PREGUNTA - 1)
        val opciones = (incorrectas + correcta).shuffled(random)
        PreguntaQuiz(tarjeta.pregunta, opciones, opciones.indexOf(correcta))
    }
}
