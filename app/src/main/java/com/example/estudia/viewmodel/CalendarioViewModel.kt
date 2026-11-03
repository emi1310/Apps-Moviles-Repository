package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.sePuedeRepetir
import com.example.estudia.notificaciones.AvisosSistema
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
    // Para cada día del mes con eventos, los colores de sus materias (hasta 3,
    // sin repetir): se dibujan como puntitos debajo del número.
    val coloresPorDia: Map<LocalDate, List<Long>> = emptyMap(),
    val eventosDelDia: List<EventoConMateria> = emptyList(),
    val materiasActivas: List<Materia> = emptyList()
)

// ViewModel del calendario: qué mes se muestra, qué día está elegido y
// los eventos de ese día. Antes todo esto se calculaba en el Composable.
class CalendarioViewModel(
    private val repositorio: RepositorioEstudia,
    private val avisosSistema: AvisosSistema
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
                // Se recorre día por día porque las clases semanales aparecen
                // en varias fechas aunque estén guardadas una sola vez.
                coloresPorDia = (1..mesActual.lengthOfMonth())
                    .map { mesActual.atDay(it) }
                    .associateWith { fecha ->
                        eventos.filter { it.evento.ocurreEl(fecha) }.map { it.colorMateria }.distinct().take(3)
                    }
                    .filterValues { it.isNotEmpty() },
                eventosDelDia = eventos.filter { it.evento.ocurreEl(dia) }.sortedBy { it.evento.horaInicio },
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

    // Crea o edita un evento (las clases pueden repetirse cada semana).
    // Devuelve el error a mostrar en el diálogo, o null si se guardó.
    // Si se edita, el evento puede cambiar de materia.
    fun guardarEvento(
        materiaId: Int,
        existente: Evento?,
        titulo: String,
        tipo: TipoEvento,
        fecha: LocalDate,
        inicio: LocalTime,
        fin: LocalTime,
        semanal: Boolean = false,
        repetirHasta: LocalDate? = null
    ): Mensaje? {
        if (!fin.isAfter(inicio)) return Mensaje.ERROR_HORA_FIN
        // Solo las clases se repiten; "hasta" no puede ser antes de la primera clase.
        val seRepite = semanal && tipo.sePuedeRepetir
        if (seRepite && repetirHasta != null && repetirHasta.isBefore(fecha)) return Mensaje.ERROR_REPETIR_HASTA
        val evento = Evento(
            existente?.id ?: 0, titulo.trim(), tipo, fecha, inicio, fin,
            semanal = seRepite,
            repetirHasta = if (seRepite) repetirHasta else null
        )
        viewModelScope.launch {
            // Cada cambio en un evento actualiza su recordatorio.
            val guardado = if (existente == null) {
                repositorio.agregarEvento(materiaId, evento)
            } else {
                repositorio.actualizarEvento(materiaId, evento)
                evento
            }
            avisosSistema.programarEvento(guardado)
        }
        seleccionar(fecha)
        mes.value = YearMonth.from(fecha)
        return null
    }

    fun eliminarEvento(item: EventoConMateria) {
        viewModelScope.launch {
            repositorio.eliminarEvento(item.materiaId, item.evento)
            avisosSistema.cancelarEvento(item.evento.id)
            avisar(
                Aviso(
                    Mensaje.AVISO_EVENTO_ELIMINADO,
                    alDeshacer = {
                        repositorio.restaurarEvento(item.materiaId, item.evento)
                        avisosSistema.programarEvento(item.evento)
                    }
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
