package com.example.estudia.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.example.estudia.R
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.TipoEvento
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

data class CalendarioUiState(
    val mes: YearMonth = YearMonth.now(),
    val seleccionado: LocalDate = LocalDate.now(),
    val hoy: LocalDate = LocalDate.now(),
    // Grilla del mes: semanas de 7 casilleros (domingo a sábado); null = vacío.
    val semanas: List<List<LocalDate?>> = emptyList(),
    val diasConEventos: Set<LocalDate> = emptySet(),
    val eventosDelDia: List<EventoConMateria> = emptyList(),
    val materiasActivas: List<Materia> = emptyList()
)

// ViewModel del calendario: qué mes se muestra, qué día está elegido y
// los eventos de ese día. Antes todo esto se calculaba en el Composable.
class CalendarioViewModel(
    private val repositorio: RepositorioEstudia
) : ViewModelConAvisos() {

    private val mes = MutableStateFlow(YearMonth.now())
    private val seleccionado = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<CalendarioUiState> =
        combine(repositorio.usuarioActual, mes, seleccionado) { usuario, mesActual, dia ->
            val materias = usuario?.materias ?: emptyList()
            val eventos = materias.flatMap { materia ->
                materia.eventos.map { EventoConMateria(it, materia.id, materia.nombre, materia.color) }
            }
            CalendarioUiState(
                mes = mesActual,
                seleccionado = dia,
                semanas = semanasDelMes(mesActual),
                diasConEventos = eventos.map { it.evento.fecha }.filter { YearMonth.from(it) == mesActual }.toSet(),
                eventosDelDia = eventos.filter { it.evento.fecha == dia }.sortedBy { it.evento.horaInicio },
                materiasActivas = materias.filter { !it.archivada }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarioUiState(semanas = semanasDelMes(YearMonth.now())))

    fun mesAnterior() = cambiarMes(mes.value.minusMonths(1))

    fun mesSiguiente() = cambiarMes(mes.value.plusMonths(1))

    fun seleccionar(fecha: LocalDate) {
        seleccionado.value = fecha
    }

    fun irAHoy() {
        mes.value = YearMonth.now()
        seleccionado.value = LocalDate.now()
    }

    private fun cambiarMes(nuevo: YearMonth) {
        mes.value = nuevo
        seleccionado.value = nuevo.atDay(1)
    }

    // Crea o edita un evento. Devuelve el error a mostrar en el diálogo, o
    // null si se guardó. Si se edita, el evento puede cambiar de materia.
    @StringRes
    fun guardarEvento(
        materiaId: Int,
        existente: Evento?,
        titulo: String,
        tipo: TipoEvento,
        fecha: LocalDate,
        inicio: LocalTime,
        fin: LocalTime
    ): Int? {
        if (!fin.isAfter(inicio)) return R.string.error_hora_fin
        val evento = Evento(existente?.id ?: 0, titulo.trim(), tipo, fecha, inicio, fin)
        viewModelScope.launch {
            if (existente == null) repositorio.agregarEvento(materiaId, evento)
            else repositorio.actualizarEvento(materiaId, evento)
        }
        seleccionar(fecha)
        mes.value = YearMonth.from(fecha)
        return null
    }

    fun eliminarEvento(item: EventoConMateria) {
        viewModelScope.launch {
            repositorio.eliminarEvento(item.materiaId, item.evento)
            avisar(
                Aviso(
                    R.string.aviso_evento_eliminado,
                    alDeshacer = { repositorio.restaurarEvento(item.materiaId, item.evento) }
                )
            )
        }
    }
}

// Arma la grilla del mes: arranca en domingo, con casilleros vacíos (null)
// antes del día 1 y después del último día.
fun semanasDelMes(mes: YearMonth): List<List<LocalDate?>> {
    val vaciosAntes = mes.atDay(1).dayOfWeek.value % 7   // domingo = 0, lunes = 1 ...
    val dias: List<LocalDate?> = List(vaciosAntes) { null } + (1..mes.lengthOfMonth()).map { mes.atDay(it) }
    return dias.chunked(7).map { semana -> semana + List(7 - semana.size) { null } }
}
