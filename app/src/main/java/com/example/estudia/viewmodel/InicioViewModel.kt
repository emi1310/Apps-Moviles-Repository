package com.example.estudia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.estudia.notificaciones.AvisosSistema
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// Una tarea o un evento junto con los datos de su materia (Tarea y Evento
// por sí solos no saben de qué materia son).
data class TareaConMateria(val tarea: Tarea, val materiaId: Int, val nombreMateria: String, val colorMateria: Long)

data class EventoConMateria(val evento: Evento, val materiaId: Int, val nombreMateria: String, val colorMateria: Long)

// Cómo se ordenan las pendientes de Inicio.
enum class OrdenTareas { FECHA, PRIORIDAD }

// Todo lo que muestra la pantalla de Inicio, ya calculado.
data class InicioUiState(
    val orden: OrdenTareas = OrdenTareas.FECHA,
    // Uri de la foto de perfil para el avatar (null = se muestra la inicial).
    val fotoUri: String? = null,
    val cargando: Boolean = true,
    val nombre: String = "",
    val hoy: LocalDate = LocalDate.now(),
    val tareasPendientes: List<TareaConMateria> = emptyList(),
    val proximosEventos: List<EventoConMateria> = emptyList(),
    val notaDestacada: Nota? = null,
    val materiasActivas: List<Materia> = emptyList(),
    val minutosPorMateria: List<Pair<Materia, Long>> = emptyList(),
    val horasEstudio: Int = 0,
    // Tarjetas de estudio: cuántas hay en total y cuántas tocan hoy.
    val totalTarjetas: Int = 0,
    val tarjetasParaHoy: Int = 0,
    // Para el fueguito de la racha del encabezado.
    val rachaDias: Int = 0,
    val estudioHoy: Boolean = false          // si hoy ya hay un bloque de estudio
) {
    // Para la tarjeta de tareas de Inicio.
    val totalTareas: Int get() = materiasActivas.sumOf { it.tareas.size }
    val tareasCompletadas: Int get() = materiasActivas.sumOf { materia -> materia.tareas.count { it.completada } }
    val tareasVencidas: Int get() = tareasPendientes.count { it.tarea.estaVencida(hoy) }
    val tareasParaHoy: Int get() = tareasPendientes.count { it.tarea.fechaLimite == hoy }
    val progresoTareas: Float get() = if (totalTareas == 0) 0f else tareasCompletadas.toFloat() / totalTareas
}

// ViewModel de Inicio: arma el resumen a partir del usuario. Antes estos
// cálculos estaban dentro del Composable (lógica en la View); ahora la
// pantalla solo dibuja el UiState.
class InicioViewModel(
    private val repositorio: RepositorioEstudia,
    private val hoy: () -> LocalDate = { LocalDate.now() },
    // Para los avisos de las tareas (null en los tests que no los usan).
    private val avisosSistema: AvisosSistema? = null
) : ViewModel() {

    private val orden = MutableStateFlow(OrdenTareas.FECHA)

    val uiState: StateFlow<InicioUiState> = combine(repositorio.usuarioActual, orden) { usuario, orden ->
        if (usuario == null) InicioUiState(orden = orden)
        else calcularResumen(usuario, hoy(), orden).copy(fotoUri = usuario.fotoPerfil?.let { repositorio.uriDeFoto(it) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InicioUiState())

    fun cambiarOrden(nuevo: OrdenTareas) {
        orden.value = nuevo
    }

    fun alternarTarea(item: TareaConMateria) {
        viewModelScope.launch {
            val cambiada = item.tarea.copy(completada = !item.tarea.completada)
            val siguiente = repositorio.actualizarTarea(item.materiaId, cambiada)
            if (cambiada.completada) avisosSistema?.cancelarTarea(cambiada.id) else avisosSistema?.programarTarea(cambiada)
            // Si se repite, la siguiente ya tiene su aviso.
            siguiente?.let { avisosSistema?.programarTarea(it) }
        }
    }
}

// Función pura (sin Android ni corrutinas): fácil de testear.
fun calcularResumen(usuario: Usuario, hoy: LocalDate, orden: OrdenTareas = OrdenTareas.FECHA): InicioUiState {
    val activas = usuario.materias.filter { !it.archivada }

    // Pendientes de todas las materias activas. Por fecha: primero las
    // vencidas y las más próximas (las que no tienen fecha, al final), y a
    // igual fecha la más importante. Por prioridad: alta primero y, dentro
    // de cada prioridad, por fecha.
    val porFecha = compareBy<TareaConMateria> { it.tarea.fechaLimite == null }.thenBy { it.tarea.fechaLimite }
    val porPrioridad = compareByDescending<TareaConMateria> { it.tarea.prioridad.peso }
    val pendientes = activas.flatMap { materia ->
        materia.tareas.filter { !it.completada }.map { TareaConMateria(it, materia.id, materia.nombre, materia.color) }
    }.sortedWith(if (orden == OrdenTareas.FECHA) porFecha.then(porPrioridad) else porPrioridad.then(porFecha))

    // Exámenes y entregas desde hoy en adelante, en orden de fecha.
    val proximos = activas.flatMap { materia ->
        materia.eventos
            .filter { it.llevaRecordatorio && !it.fecha.isBefore(hoy) }
            .map { EventoConMateria(it, materia.id, materia.nombre, materia.color) }
    }.sortedBy { it.evento.inicio }

    val minutos = activas.map { materia ->
        materia to materia.eventos.filter { it.tipo == TipoEvento.BLOQUE_ESTUDIO }
            .sumOf { it.duracionMinutos() }
    }

    return InicioUiState(
        orden = orden,
        cargando = false,
        nombre = usuario.nombre.substringBefore(" "),
        hoy = hoy,
        tareasPendientes = pendientes,
        proximosEventos = proximos,
        notaDestacada = activas.flatMap { it.notas }.filter { it.esFavorita }.maxByOrNull { it.fechaCreacion },
        materiasActivas = activas,
        minutosPorMateria = minutos,
        horasEstudio = usuario.calcularHorasEstudio(),
        totalTarjetas = activas.sumOf { it.tarjetas.size },
        tarjetasParaHoy = activas.sumOf { it.tarjetasParaHoy(hoy).size },
        rachaDias = usuario.rachaDeEstudio(hoy),
        estudioHoy = usuario.materias.any { materia ->
            materia.eventos.any { it.tipo == TipoEvento.BLOQUE_ESTUDIO && it.fecha == hoy }
        }
    )
}
