package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.Calificacion
import com.example.estudia.modelo.EstadoPlan
import com.example.estudia.modelo.MesaFinal
import com.example.estudia.modelo.Usuario
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class FinalesUiState(
    val cargando: Boolean = true,
    val usuarioId: Int = 0,
    val hoy: LocalDate = LocalDate.now(),
    val proximos: List<MesaFinal> = emptyList(),    // sin rendir, la mesa más cercana primero
    val rendidos: List<MesaFinal> = emptyList(),    // el más reciente primero
    // Materias para elegir rápido: las de la app (las que tienen final) y las
    // regulares del plan, sin repetir y sin las que ya tienen mesa anotada.
    val sugerencias: List<String> = emptyList(),
    // Color de cada sugerencia que es una materia de la app (nombre en minúscula).
    val colores: Map<String, Long> = emptyMap()
)

// ViewModel de las mesas de finales. Los avisos (cierre de inscripción y
// mesa de mañana) los da el resumen diario de las 8:00: ver RecordatorioWorker.
class FinalesViewModel(
    private val repositorio: RepositorioEstudia,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModelConAvisos() {

    val uiState: StateFlow<FinalesUiState> = repositorio.usuarioActual
        .map { usuario -> if (usuario == null) FinalesUiState() else armarFinales(usuario, hoy()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinalesUiState())

    fun guardar(
        final: MesaFinal?,
        materia: String,
        fechaMesa: LocalDate?,
        cierreInscripcion: LocalDate?,
        vencimientoRegularidad: LocalDate?,
        inscripto: Boolean
    ): Mensaje? {
        if (materia.isBlank() || fechaMesa == null) return Mensaje.ERROR_COMPLETAR_CAMPOS
        if (cierreInscripcion != null && cierreInscripcion.isAfter(fechaMesa)) return Mensaje.ERROR_CIERRE_INSCRIPCION
        val usuarioId = uiState.value.usuarioId
        viewModelScope.launch {
            val datos = MesaFinal(
                id = final?.id ?: 0,
                materia = materia.trim(),
                fechaMesa = fechaMesa,
                cierreInscripcion = cierreInscripcion,
                vencimientoRegularidad = vencimientoRegularidad,
                inscripto = inscripto,
                nota = final?.nota
            )
            if (final == null) repositorio.agregarFinal(usuarioId, datos) else repositorio.actualizarFinal(usuarioId, datos)
        }
        return null
    }

    fun alternarInscripto(final: MesaFinal) {
        viewModelScope.launch { repositorio.actualizarFinal(uiState.value.usuarioId, final.copy(inscripto = !final.inscripto)) }
    }

    // Carga la nota del final. Si aprobó y la materia está en "Mi carrera",
    // queda aprobada ahí también, con esa nota.
    fun cargarNota(final: MesaFinal, notaTexto: String): Mensaje? {
        val nota = notaTexto.trim().replace(',', '.').toDoubleOrNull()
        if (nota == null || nota < Calificacion.NOTA_MINIMA || nota > Calificacion.NOTA_MAXIMA) return Mensaje.ERROR_NOTA_INVALIDA
        val usuarioId = uiState.value.usuarioId
        viewModelScope.launch {
            repositorio.actualizarFinal(usuarioId, final.copy(nota = nota, inscripto = true))
            if (!Calificacion.aprueba(nota)) return@launch
            val usuario = repositorio.usuarioActual.first() ?: return@launch
            val enElPlan = usuario.planEstudio.firstOrNull { it.nombre.trim().equals(final.materia.trim(), ignoreCase = true) }
            if (enElPlan != null && enElPlan.estado != EstadoPlan.APROBADA) {
                repositorio.actualizarMateriaPlan(usuarioId, enElPlan.copy(estado = EstadoPlan.APROBADA, notaFinal = nota))
                avisar(Aviso(Mensaje.AVISO_FINAL_APROBADO_PLAN, listOf(enElPlan.nombre)))
            }
        }
        return null
    }

    fun eliminar(final: MesaFinal) {
        val usuarioId = uiState.value.usuarioId
        viewModelScope.launch {
            repositorio.eliminarFinal(usuarioId, final)
            avisar(Aviso(Mensaje.AVISO_FINAL_ELIMINADO, alDeshacer = { repositorio.restaurarFinal(usuarioId, final) }))
        }
    }
}

fun armarFinales(usuario: Usuario, hoy: LocalDate): FinalesUiState {
    val (rendidos, proximos) = usuario.finales.partition { it.rendido }
    val yaAnotadas = proximos.map { it.materia.trim().lowercase() }.toSet()
    // Las materias de la app marcadas "sin final" no se sugieren.
    val sinFinal = usuario.materias.filter { !it.tieneFinal }.map { it.nombre.trim().lowercase() }.toSet()
    return FinalesUiState(
        cargando = false,
        usuarioId = usuario.id,
        hoy = hoy,
        proximos = proximos.sortedBy { it.fechaMesa },
        rendidos = rendidos.sortedByDescending { it.fechaMesa },
        sugerencias = (
            usuario.materias.filter { !it.archivada && it.tieneFinal }.map { it.nombre.trim() } +
                usuario.planEstudio.filter { it.estado == EstadoPlan.REGULAR }.map { it.nombre.trim() }
            )
            .filter { it.isNotEmpty() && it.lowercase().let { n -> n !in yaAnotadas && n !in sinFinal } }
            .distinctBy { it.lowercase() },
        colores = usuario.materias.associate { it.nombre.trim().lowercase() to it.color }
    )
}
