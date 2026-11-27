package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.Receso
import com.example.estudia.modelo.diasSinClase
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Tema
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.sePuedeRepetir
import com.example.estudia.modelo.normalizarEnlace
import com.example.estudia.notificaciones.AvisosSistema
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    val materiasActivas: List<Materia> = emptyList(),
    // Versión 10: feriados del mes (fecha -> nombre) y el del día elegido.
    val feriadosDelMes: Map<LocalDate, String> = emptyMap(),
    val feriadoDelDia: String? = null,
    val hayFeriados: Boolean = false,
    val actualizandoFeriados: Boolean = false,
    // Versión 14: recesos del usuario (días sin clases).
    val recesos: List<Receso> = emptyList(),
    // El calendario del teléfono se mantiene al día solo.
    val calendarioAutomatico: Boolean = false,
    // Búsqueda desde la lupa del dock: con texto, en lugar del mes se muestran
    // los eventos que coinciden.
    val busqueda: String = "",
    val resultadosBusqueda: List<EventoEncontrado> = emptyList(),
    // Todos los eventos (para buscar en ellos).
    val todosLosEventos: List<EventoConMateria> = emptyList()
) {
    val buscando: Boolean get() = busqueda.isNotBlank()
}

// Un evento encontrado y la fecha con la que se muestra: la suya o, en las
// clases semanales, la próxima (o la última si ya terminaron).
data class EventoEncontrado(val item: EventoConMateria, val fecha: LocalDate)

// ViewModel del calendario: qué mes se muestra, qué día está elegido y
// los eventos de ese día. Antes todo esto se calculaba en el Composable.
class CalendarioViewModel(
    private val repositorio: RepositorioEstudia,
    private val avisosSistema: AvisosSistema
) : ViewModelConAvisos() {

    private val mes = MutableStateFlow(YearMonth.now())
    private val seleccionado = MutableStateFlow(LocalDate.now())
    private val actualizandoFeriados = MutableStateFlow(false)
    private val busqueda = MutableStateFlow("")

    val uiState: StateFlow<CalendarioUiState> =
        combine(repositorio.usuarioActual, mes, seleccionado, repositorio.feriados, actualizandoFeriados) { usuario, mesActual, dia, feriados, actualizando ->
            // Los recesos cuentan como feriados: días sin clase, en rojo y con su nombre.
            val porFecha = diasSinClase(feriados, usuario?.recesos.orEmpty())
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
                materiasActivas = materias.filter { !it.archivada },
                feriadosDelMes = porFecha.filterKeys { YearMonth.from(it) == mesActual },
                feriadoDelDia = porFecha[dia],
                hayFeriados = feriados.isNotEmpty(),
                recesos = usuario?.recesos.orEmpty(),
                actualizandoFeriados = actualizando,
                todosLosEventos = eventos
            )
        }.combine(busqueda) { estado, texto -> estado to texto }
        .combine(repositorio.calendarioAutomatico) { (estado, texto), automatico ->
            estado.copy(calendarioAutomatico = automatico, busqueda = texto, resultadosBusqueda = buscarEventos(estado.todosLosEventos, texto, estado.hoy))
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

    fun cambiarBusqueda(texto: String) {
        busqueda.value = texto
    }

    // Muestra el mes de esa fecha con ese día elegido (al tocar un resultado).
    fun irA(fecha: LocalDate) {
        mes.value = YearMonth.from(fecha)
        seleccionado.value = fecha
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
        repetirHasta: LocalDate? = null,
        // Versión 10: virtual (con link) o presencial (con aula o lugar).
        virtual: Boolean = false,
        lugar: String? = null,
        enlace: String? = null,
        // Versión 14: 1 = todas las semanas, 2 = semana por medio.
        cadaSemanas: Int = 1
    ): Mensaje? {
        if (!fin.isAfter(inicio)) return Mensaje.ERROR_HORA_FIN
        // Solo las clases se repiten; "hasta" no puede ser antes de la primera clase.
        val seRepite = semanal && tipo.sePuedeRepetir
        if (seRepite && repetirHasta != null && repetirHasta.isBefore(fecha)) return Mensaje.ERROR_REPETIR_HASTA
        val evento = Evento(
            existente?.id ?: 0, titulo.trim(), tipo, fecha, inicio, fin,
            semanal = seRepite,
            repetirHasta = if (seRepite) repetirHasta else null,
            cadaSemanas = if (seRepite) cadaSemanas.coerceIn(1, 2) else 1,
            virtual = virtual,
            // Se guarda solo lo que corresponde a la modalidad elegida.
            lugar = if (virtual) null else lugar?.trim()?.ifEmpty { null },
            enlace = if (virtual) normalizarEnlace(enlace) else null,
            // Al editar se conservan el temario y la asistencia.
            temas = existente?.temas ?: emptyList(),
            asistencias = existente?.asistencias ?: emptyList()
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

    // ---------- Calendario del teléfono ----------

    // La UI lo llama cuando ya tiene el permiso de calendario.
    fun sincronizarConTelefono(nombreCalendario: String) {
        viewModelScope.launch {
            val cantidad = repositorio.sincronizarCalendario(nombreCalendario)
            avisar(
                if (cantidad != null) Aviso(Mensaje.AVISO_CALENDARIO_SINCRONIZADO, listOf(cantidad))
                else Aviso(Mensaje.AVISO_CALENDARIO_ERROR)
            )
        }
    }

    fun dejarDeSincronizar() {
        viewModelScope.launch {
            repositorio.dejarDeSincronizarCalendario()
            avisar(Aviso(Mensaje.AVISO_CALENDARIO_DESACTIVADO))
        }
    }

    // uri: el archivo .ics que eligió el usuario con el selector del sistema.
    fun exportarIcs(uri: String, nombreCalendario: String) {
        viewModelScope.launch {
            val cantidad = repositorio.exportarCalendarioIcs(uri, nombreCalendario)
            avisar(
                if (cantidad != null) Aviso(Mensaje.AVISO_CALENDARIO_ICS_GUARDADO, listOf(cantidad))
                else Aviso(Mensaje.AVISO_CALENDARIO_ERROR)
            )
        }
    }

    // ---------- Feriados ----------

    // usarCalendario: la UI ya tiene el permiso de leer el calendario (si no,
    // se bajan de internet).
    fun importarFeriados(usarCalendario: Boolean) {
        if (actualizandoFeriados.value) return
        actualizandoFeriados.value = true
        viewModelScope.launch {
            val resultado = repositorio.actualizarFeriados(usarCalendario)
            actualizandoFeriados.value = false
            avisar(
                when {
                    resultado == null -> Aviso(Mensaje.AVISO_FERIADOS_ERROR)
                    resultado.delCalendario -> Aviso(Mensaje.AVISO_FERIADOS_CALENDARIO, listOf(resultado.cantidad))
                    else -> Aviso(Mensaje.AVISO_FERIADOS_INTERNET, listOf(resultado.cantidad))
                }
            )
        }
    }

    // ---------- Temario de un examen ----------

    fun agregarTema(eventoId: Int, titulo: String) {
        if (titulo.isBlank()) return
        viewModelScope.launch { repositorio.agregarTema(eventoId, titulo.trim()) }
    }

    fun alternarTema(eventoId: Int, tema: Tema) {
        viewModelScope.launch { repositorio.alternarTema(eventoId, tema) }
    }

    fun eliminarTema(eventoId: Int, tema: Tema) {
        viewModelScope.launch {
            repositorio.eliminarTema(eventoId, tema)
            avisar(Aviso(Mensaje.AVISO_TEMA_ELIMINADO, alDeshacer = { repositorio.restaurarTema(eventoId, tema) }))
        }
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

    // ---------- Recesos ----------

    // Devuelve el error a mostrar o null si se guardó.
    fun guardarReceso(existente: Receso?, nombre: String, desde: LocalDate, hasta: LocalDate): Mensaje? {
        if (nombre.isBlank()) return Mensaje.ERROR_NOMBRE_RECESO
        if (hasta.isBefore(desde)) return Mensaje.ERROR_FECHAS_RECESO
        viewModelScope.launch {
            val usuario = repositorio.usuarioActual.first() ?: return@launch
            repositorio.guardarReceso(usuario.id, Receso(existente?.id ?: 0, nombre.trim(), desde, hasta))
        }
        return null
    }

    fun eliminarReceso(receso: Receso) {
        viewModelScope.launch {
            val usuario = repositorio.usuarioActual.first() ?: return@launch
            repositorio.eliminarReceso(usuario.id, receso)
            avisar(Aviso(Mensaje.AVISO_RECESO_ELIMINADO, alDeshacer = { repositorio.guardarReceso(usuario.id, receso.copy(id = 0)) }))
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

// Función pura: eventos cuyo título, materia, lugar, enlace o temario
// contienen el texto (sin importar tildes ni mayúsculas). Primero los que
// vienen (del más cercano al más lejano) y después los pasados (del más
// reciente al más viejo).
fun buscarEventos(eventos: List<EventoConMateria>, texto: String, hoy: LocalDate): List<EventoEncontrado> {
    val buscado = normalizar(texto.trim())
    if (buscado.isEmpty()) return emptyList()
    fun coincide(campo: String?) = campo != null && normalizar(campo).contains(buscado)
    val encontrados = eventos
        .filter { (evento, _, materia) ->
            coincide(evento.titulo) || coincide(materia) || coincide(evento.lugar) || coincide(evento.enlace) ||
                evento.temas.any { coincide(it.titulo) }
        }
        .map { EventoEncontrado(it, fechaParaMostrar(it.evento, hoy)) }
    val (proximos, pasados) = encontrados.partition { !it.fecha.isBefore(hoy) }
    return proximos.sortedWith(compareBy<EventoEncontrado> { it.fecha }.thenBy { it.item.evento.horaInicio }) +
        pasados.sortedWith(compareByDescending<EventoEncontrado> { it.fecha }.thenBy { it.item.evento.horaInicio })
}

// Fecha con la que se muestra un evento en la búsqueda. Las clases semanales
// se guardan una sola vez: se usa la próxima clase desde hoy o, si la serie
// ya terminó, la última.
fun fechaParaMostrar(evento: Evento, hoy: LocalDate): LocalDate {
    if (!evento.semanal) return evento.fecha
    var dia = maxOf(evento.fecha, hoy)
    repeat(7) {
        if (evento.ocurreEl(dia)) return dia
        dia = dia.plusDays(1)
    }
    var ultima = evento.repetirHasta ?: return evento.fecha
    repeat(7) {
        if (evento.ocurreEl(ultima)) return ultima
        ultima = ultima.minusDays(1)
    }
    return evento.fecha
}
