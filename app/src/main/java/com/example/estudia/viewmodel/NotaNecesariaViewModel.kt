package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.CalculoNotas
import com.example.estudia.modelo.Calificacion
import com.example.estudia.modelo.Evaluacion
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.calcularNotas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotaNecesariaUiState(
    val cargando: Boolean = true,
    val materias: List<Materia> = emptyList(),
    val materia: Materia? = null,
    val calculo: CalculoNotas? = null,
    // Calificaciones de la materia que todavía no están como evaluación.
    val paraImportar: List<Calificacion> = emptyList()
)

// ViewModel de "¿Qué nota necesito?". materiaInicial = -1: la primera materia activa.
class NotaNecesariaViewModel(
    private val repositorio: RepositorioEstudia,
    materiaInicial: Int
) : ViewModelConAvisos() {

    private val elegida = MutableStateFlow(materiaInicial)

    val uiState: StateFlow<NotaNecesariaUiState> = combine(repositorio.usuarioActual, elegida) { usuario, id ->
        if (usuario == null) return@combine NotaNecesariaUiState()
        val activas = usuario.materias.filter { !it.archivada }
        val materia = activas.firstOrNull { it.id == id } ?: activas.firstOrNull()
        NotaNecesariaUiState(
            cargando = false,
            materias = activas,
            materia = materia,
            calculo = materia?.let { calcularNotas(it.evaluaciones, it.notaPromocion.takeIf { _ -> it.promocional }) },
            paraImportar = materia?.let { calificacionesParaImportar(it) } ?: emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotaNecesariaUiState())

    fun elegirMateria(materia: Materia) {
        elegida.value = materia.id
    }

    // Agrega (evaluacion == null) o edita. La nota puede quedar vacía (todavía
    // no se rindió). Devuelve el error a mostrar, o null si se guardó.
    fun guardar(evaluacion: Evaluacion?, nombre: String, pesoTexto: String, notaTexto: String): Mensaje? {
        val materia = uiState.value.materia ?: return null
        val peso = pesoTexto.trim().removeSuffix("%").trim().toIntOrNull()
        if (nombre.isBlank()) return Mensaje.ERROR_COMPLETAR_CAMPOS
        if (peso == null || peso !in 1..100) return Mensaje.ERROR_PESO_INVALIDO
        val nota = if (notaTexto.isBlank()) null else {
            val n = notaTexto.trim().replace(',', '.').toDoubleOrNull()
            if (n == null || n < Calificacion.NOTA_MINIMA || n > Calificacion.NOTA_MAXIMA) return Mensaje.ERROR_NOTA_INVALIDA
            n
        }
        val otras = materia.evaluaciones.filter { it.id != evaluacion?.id }.sumOf { it.peso }
        if (otras + peso > 100) return Mensaje.ERROR_PESO_SUPERA
        viewModelScope.launch {
            if (evaluacion == null) repositorio.agregarEvaluacion(materia.id, nombre.trim(), peso, nota)
            else repositorio.actualizarEvaluacion(materia.id, evaluacion.copy(nombre = nombre.trim(), peso = peso, nota = nota))
        }
        return null
    }

    fun eliminar(evaluacion: Evaluacion) {
        val materia = uiState.value.materia ?: return
        viewModelScope.launch {
            repositorio.eliminarEvaluacion(materia.id, evaluacion)
            avisar(Aviso(Mensaje.AVISO_EVALUACION_ELIMINADA, alDeshacer = { repositorio.restaurarEvaluacion(materia.id, evaluacion) }))
        }
    }

    // Trae las notas de exámenes ya cargadas en la materia. El peso que
    // queda libre se reparte en partes iguales (después se puede editar).
    fun importarCalificaciones() {
        val estado = uiState.value
        val materia = estado.materia ?: return
        val nuevas = estado.paraImportar
        if (nuevas.isEmpty()) return
        val libre = 100 - materia.evaluaciones.sumOf { it.peso }
        val peso = (libre / nuevas.size).coerceAtLeast(1)
        viewModelScope.launch {
            nuevas.forEach { repositorio.agregarEvaluacion(materia.id, it.titulo, peso, it.nota) }
            avisar(Aviso(Mensaje.AVISO_EVALUACIONES_IMPORTADAS, listOf(nuevas.size)))
        }
    }
}

fun calificacionesParaImportar(materia: Materia): List<Calificacion> {
    val nombres = materia.evaluaciones.map { it.nombre.trim().lowercase() }.toSet()
    return materia.calificaciones.filter { it.titulo.trim().lowercase() !in nombres }
}
