package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.SesionEstudio
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.notificaciones.AvisosSistema
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import com.example.estudia.modelo.avanceMetaDiaria
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

// Duraciones que se pueden elegir (en minutos). 25 es el "pomodoro" clásico.
val DURACIONES_TEMPORIZADOR = listOf(15, 25, 45, 60)

data class TemporizadorUiState(
    val cargando: Boolean = true,
    val materias: List<Materia> = emptyList(),      // activas, para elegir
    val materiaElegida: Materia? = null,
    val minutosElegidos: Int = 25,
    val sesion: SesionEstudio? = null,              // null = no hay sesión en curso
    val ahora: Long = 0,                            // hora actual, se actualiza cada segundo
    val minutosHoy: Int = 0,                        // estudiados hoy (todas las materias)
    val silenciar: Boolean = false,                 // "Silenciar el teléfono" mientras corre
    val metaDiaria: Int = 0,                        // minutos por día (0 = sin meta)
    val avisarAlSalir: Boolean = true               // notificación si sale de la app con la sesión corriendo
) {
    val avanceMeta: Float get() = avanceMetaDiaria(minutosHoy, metaDiaria)

    val restanteMillis: Long get() = sesion?.restanteMillis(ahora) ?: (minutosElegidos * 60_000L)
    val avance: Float get() = sesion?.avance(ahora) ?: 0f

    // Lo que se ve en el centro del anillo: "25:00". Se redondea hacia
    // arriba (24:59.4 -> 25:00), así llega a 00:00 justo cuando termina.
    val textoRestante: String
        get() {
            val segundos = (restanteMillis + 999) / 1_000
            return "%02d:%02d".format(segundos / 60, segundos % 60)
        }
}

// ViewModel del temporizador de estudio. La sesión en curso vive en el
// Repository (DataStore), no acá: así sigue corriendo aunque el usuario
// salga de la pantalla o cierre la app. Después de cada cambio se actualiza
// la notificación y el aviso de fin con AvisosSistema.
class TemporizadorViewModel(
    private val repositorio: RepositorioEstudia,
    private val avisosSistema: AvisosSistema,
    // Reloj del sistema; en las pruebas se reemplaza por uno controlado.
    private val reloj: () -> Long = System::currentTimeMillis
) : ViewModelConAvisos() {

    private val materiaElegidaId = MutableStateFlow<Int?>(null)
    private val minutosElegidos = MutableStateFlow(25)

    // Emite la hora actual una vez por segundo, para que la cuenta regresiva avance.
    private val tic = flow {
        while (true) {
            emit(reloj())
            delay(1_000)
        }
    }

    private val elecciones = combine(materiaElegidaId, minutosElegidos) { id, minutos -> id to minutos }

    private data class Ajustes(val silenciar: Boolean, val metaDiaria: Int, val avisarAlSalir: Boolean)

    private val ajustes = combine(repositorio.silenciarAlEstudiar, repositorio.metaDiariaMinutos, repositorio.avisarAlSalir) { s, m, a -> Ajustes(s, m, a) }

    val uiState: StateFlow<TemporizadorUiState> =
        combine(
            repositorio.usuarioActual,
            repositorio.sesionEstudio,
            elecciones,
            tic,
            ajustes
        ) { usuario, sesion, (id, minutos), ahora, ajustes ->
            val activas = usuario?.materias?.filter { !it.archivada } ?: emptyList()
            val hoy = LocalDate.now()
            TemporizadorUiState(
                cargando = false,
                materias = activas,
                // Si no eligió ninguna, se propone la primera.
                materiaElegida = activas.firstOrNull { it.id == (sesion?.materiaId ?: id) } ?: activas.firstOrNull(),
                minutosElegidos = minutos,
                sesion = sesion,
                ahora = ahora,
                minutosHoy = minutosDeEstudioDelDia(activas, hoy),
                silenciar = ajustes.silenciar,
                metaDiaria = ajustes.metaDiaria,
                avisarAlSalir = ajustes.avisarAlSalir
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TemporizadorUiState())

    init {
        // Mientras la pantalla está abierta, cuando el tiempo llega a cero se
        // guarda la sesión y se avisa. Si la app está cerrada, lo hace
        // TemporizadorWorker (y acá completarSesionSiTermino devuelve null).
        viewModelScope.launch {
            repositorio.sesionEstudio.collectLatest { sesion ->
                if (sesion == null || sesion.enPausa) return@collectLatest
                delay(sesion.restanteMillis(reloj()))
                val antes = minutosHoyAhora()
                val guardada = repositorio.completarSesionSiTermino() ?: return@collectLatest
                avisosSistema.actualizarTemporizador(null)
                avisar(Aviso(Mensaje.AVISO_SESION_COMPLETA, listOf(guardada.minutos, guardada.nombreMateria)))
                avisarSiCumplioMeta(antes)
            }
        }
    }

    private suspend fun minutosHoyAhora(): Int =
        minutosDeEstudioDelDia(repositorio.usuarioActual.first()?.materias?.filter { !it.archivada }.orEmpty(), LocalDate.now())

    // Si con la sesión que se acaba de guardar llegó a la meta del día, lo festeja.
    private suspend fun avisarSiCumplioMeta(minutosAntes: Int) {
        val meta = repositorio.metaDiariaMinutos.first()
        if (meta > 0 && minutosAntes < meta && minutosHoyAhora() >= meta) {
            avisar(Aviso(Mensaje.AVISO_META_DIARIA_CUMPLIDA, listOf(meta)))
        }
    }

    fun cambiarMetaDiaria(minutos: Int) {
        viewModelScope.launch { repositorio.cambiarMetaDiaria(minutos) }
    }

    fun cambiarAvisarAlSalir(activo: Boolean) {
        viewModelScope.launch { repositorio.cambiarAvisarAlSalir(activo) }
    }

    fun elegirMateria(materia: Materia) {
        materiaElegidaId.value = materia.id
    }

    fun elegirMinutos(minutos: Int) {
        minutosElegidos.value = minutos
    }

    // "Silenciar el teléfono": se guarda la elección y se aplica ya a la
    // sesión en curso. El acceso a No molestar lo revisa la UI antes de
    // llamar con true (se da desde Ajustes).
    fun cambiarSilenciar(activo: Boolean) {
        viewModelScope.launch {
            repositorio.cambiarSilenciarAlEstudiar(activo)
            avisosSistema.cambiarSilencio(activo, uiState.value.sesion)
        }
    }

    fun empezar() {
        val materia = uiState.value.materiaElegida ?: return
        viewModelScope.launch { avisosSistema.actualizarTemporizador(repositorio.empezarSesion(materia, minutosElegidos.value), reloj()) }
    }

    fun pausar() {
        viewModelScope.launch { avisosSistema.actualizarTemporizador(repositorio.pausarSesion(), reloj()) }
    }

    fun reanudar() {
        viewModelScope.launch { avisosSistema.actualizarTemporizador(repositorio.reanudarSesion(), reloj()) }
    }

    // Corta antes de tiempo y guarda lo que se estudió hasta ahora.
    fun terminar() {
        viewModelScope.launch {
            val antes = minutosHoyAhora()
            val minutos = repositorio.terminarSesion(guardar = true)
            avisosSistema.actualizarTemporizador(null)
            avisar(
                if (minutos > 0) Aviso(Mensaje.AVISO_SESION_GUARDADA, listOf(minutos))
                else Aviso(Mensaje.AVISO_SESION_MUY_CORTA)
            )
            if (minutos > 0) avisarSiCumplioMeta(antes)
        }
    }

    fun descartar() {
        viewModelScope.launch {
            repositorio.terminarSesion(guardar = false)
            avisosSistema.actualizarTemporizador(null)
            avisar(Aviso(Mensaje.AVISO_SESION_DESCARTADA))
        }
    }
}

// Minutos estudiados en el día: los bloques de estudio de esa fecha
// (cada sesión del temporizador se guarda como uno).
fun minutosDeEstudioDelDia(materias: List<Materia>, dia: LocalDate): Int =
    materias.flatMap { it.eventos }
        .filter { it.tipo == TipoEvento.BLOQUE_ESTUDIO && it.fecha == dia }
        .sumOf { it.duracionMinutos() }.toInt()
