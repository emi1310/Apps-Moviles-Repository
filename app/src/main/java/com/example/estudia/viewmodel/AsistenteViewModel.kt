package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.AsistenteIA
import com.example.estudia.datos.ClavesIA
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.ErrorIA
import com.example.estudia.modelo.ExamenProximo
import com.example.estudia.modelo.MAXIMO_NOTAS_SIMULACRO
import com.example.estudia.modelo.PEDIDO_FOTO_APUNTE
import com.example.estudia.modelo.SesionPlan
import com.example.estudia.modelo.ClaseHorario
import com.example.estudia.modelo.eventoDeClase
import com.example.estudia.modelo.limpiarHorario
import com.example.estudia.modelo.materiaParecida
import com.example.estudia.modelo.normalizarNombre
import com.example.estudia.modelo.pedidoHorario
import com.example.estudia.modelo.eventoDeSesion
import com.example.estudia.modelo.examenesProximos
import com.example.estudia.modelo.limpiarPlan
import com.example.estudia.modelo.mapaDesdeIA
import com.example.estudia.modelo.pedidoMapa
import com.example.estudia.modelo.pedidoPlanEstudio
import com.example.estudia.modelo.pedidoSimulacro
import com.example.estudia.notificaciones.AvisosSistema
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MensajeIA
import com.example.estudia.modelo.ResultadoIA
import com.example.estudia.modelo.TarjetaSugerida
import com.example.estudia.modelo.PEDIDO_PRUEBA
import com.example.estudia.modelo.contextoDelEstudiante
import com.example.estudia.modelo.pedidoExplicar
import java.time.LocalDate
import com.example.estudia.modelo.pedidoPreguntas
import com.example.estudia.modelo.pedidoResumir
import com.example.estudia.modelo.pedidoTarjetas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Tarjetas que propuso la IA a partir de una nota, para revisarlas antes de guardar.
data class TarjetasPropuestas(val nota: NotaConMateria, val tarjetas: List<TarjetaSugerida>)

data class AsistenteUiState(
    // Hay alguna clave para usar (la del usuario o la de la app).
    val configurado: Boolean = true,
    // El usuario conectó la suya.
    val clavePropia: Boolean = false,
    // Probando una clave nueva y, si falló, por qué.
    val probandoClave: Boolean = false,
    val errorClave: ErrorIA? = null,
    val conversacion: List<MensajeIA> = emptyList(),
    val pensando: Boolean = false,
    val error: ErrorIA? = null,
    // Para elegir sobre qué nota trabajar y dónde guardar una respuesta.
    val notas: List<NotaConMateria> = emptyList(),
    val materias: List<Materia> = emptyList(),
    val tarjetasPropuestas: TarjetasPropuestas? = null,
    // Exámenes y entregas que vienen (para el plan de estudio).
    val examenes: List<ExamenProximo> = emptyList(),
    val planPropuesto: PlanPropuesto? = null,
    // Mapa recién creado desde una nota: la pantalla lo abre y lo avisa.
    val mapaCreado: Int? = null,
    // Clases que leyó la IA de la foto del horario, para revisarlas antes de guardar.
    val horarioPropuesto: List<ClaseDelHorario>? = null
)

// Plan que propuso la IA para un examen, para revisarlo antes de guardarlo.
data class PlanPropuesto(val examen: ExamenProximo, val sesiones: List<SesionPlan>)

// Una clase leída del horario y la materia ya cargada a la que va (null = se crea).
data class ClaseDelHorario(val clase: ClaseHorario, val materia: Materia?)

// Estado interno de la charla (el resto sale del usuario).
private data class Charla(
    val conversacion: List<MensajeIA> = emptyList(),
    val pensando: Boolean = false,
    val error: ErrorIA? = null,
    val tarjetas: TarjetasPropuestas? = null,
    val probandoClave: Boolean = false,
    val errorClave: ErrorIA? = null,
    val plan: PlanPropuesto? = null,
    val mapaCreado: Int? = null,
    val horario: List<ClaseHorario>? = null
)

// ViewModel del Asistente IA: una charla con Gemini y acciones rápidas
// sobre las notas (resumir, preguntas para autoevaluarse y tarjetas). La
// conversación vive mientras está la pantalla: no se guarda.
class AsistenteViewModel(
    private val repositorio: RepositorioEstudia,
    private val asistente: AsistenteIA,
    private val claves: ClavesIA,
    // La de gemini.properties (puede estar vacía).
    private val claveDeLaApp: String = "",
    // Para la agenda que se le manda a la IA (en los tests, una fecha fija).
    private val hoy: () -> LocalDate = { LocalDate.now() },
    // Para programar los avisos de los bloques del plan de estudio.
    private val avisosSistema: AvisosSistema? = null,
    // Lee la foto de un apunte (content://) como JPEG achicado. Lo arma
    // AppViewModelProvider con util/Imagenes (acá no se usa Context).
    private val leerFoto: suspend (String) -> ByteArray? = { null }
) : ViewModelConAvisos() {

    private val charla = MutableStateFlow(Charla())

    // La clave del usuario que está usando la app (null = no conectó la suya).
    @OptIn(ExperimentalCoroutinesApi::class)
    private val clavePropia = repositorio.usuarioActual.flatMapLatest { u -> if (u == null) flowOf(null) else claves.clave(u.id) }

    // La que se usa: la del usuario o, si no tiene, la de la app.
    private suspend fun claveEnUso(): String = clavePropia.first() ?: claveDeLaApp

    val uiState: StateFlow<AsistenteUiState> = combine(repositorio.usuarioActual, clavePropia, charla) { u, propia, c ->
        val materias = u?.materias.orEmpty().filter { !it.archivada }
        AsistenteUiState(
            configurado = !propia.isNullOrBlank() || claveDeLaApp.isNotBlank(),
            clavePropia = !propia.isNullOrBlank(),
            probandoClave = c.probandoClave,
            errorClave = c.errorClave,
            conversacion = c.conversacion,
            pensando = c.pensando,
            error = c.error,
            notas = materias.flatMap { m -> m.notas.map { NotaConMateria(it, m.id, m.nombre, m.color) } }
                .sortedByDescending { it.nota.fechaCreacion },
            materias = materias,
            tarjetasPropuestas = c.tarjetas,
            examenes = u?.let { examenesProximos(it, hoy()) }.orEmpty(),
            planPropuesto = c.plan,
            mapaCreado = c.mapaCreado,
            horarioPropuesto = c.horario?.map { ClaseDelHorario(it, materiaParecida(it.materia, materias)) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AsistenteUiState(configurado = claveDeLaApp.isNotBlank()))

    // Una pregunta escrita en el chat.
    fun enviar(texto: String) {
        if (texto.isBlank()) return
        preguntar(MensajeIA(true, texto.trim()))
    }

    fun explicar(tema: String) {
        if (tema.isBlank()) return
        preguntar(MensajeIA(true, pedidoExplicar(tema), mostrado = tema.trim()))
    }

    // etiqueta: lo que se ve en el chat, ej "Resumir: Guía 7".
    fun resumir(nota: NotaConMateria, etiqueta: String) =
        preguntar(MensajeIA(true, pedidoResumir(nota.nota.titulo, nota.nota.contenido), mostrado = etiqueta))

    fun preguntas(nota: NotaConMateria, etiqueta: String) =
        preguntar(MensajeIA(true, pedidoPreguntas(nota.nota.titulo, nota.nota.contenido), mostrado = etiqueta))

    // Si falló, vuelve a pedir la respuesta al último mensaje.
    fun reintentar() {
        val conversacion = charla.value.conversacion
        if (conversacion.lastOrNull()?.deUsuario != true || charla.value.pensando) return
        pedirRespuesta()
    }

    fun nuevaConversacion() {
        if (charla.value.pensando) return
        charla.value = Charla()
    }

    private fun preguntar(mensaje: MensajeIA) {
        if (charla.value.pensando) return
        charla.update { it.copy(conversacion = it.conversacion + mensaje, error = null) }
        pedirRespuesta()
    }

    private fun pedirRespuesta() {
        charla.update { it.copy(pensando = true, error = null) }
        viewModelScope.launch {
            // Con cada pregunta van los datos cargados del estudiante, para
            // que pueda responder sobre sus clases, tareas, finales, etc.
            val contexto = repositorio.usuarioActual.first()?.let { contextoDelEstudiante(it, hoy()) }.orEmpty()
            when (val r = asistente.responder(claveEnUso(), charla.value.conversacion, contexto)) {
                is ResultadoIA.Ok -> charla.update { it.copy(conversacion = it.conversacion + MensajeIA(false, r.valor), pensando = false) }
                is ResultadoIA.Falla -> charla.update { it.copy(pensando = false, error = r.error) }
            }
        }
    }

    // ---------- Tarjetas desde una nota ----------

    fun pedirTarjetas(nota: NotaConMateria) {
        if (charla.value.pensando) return
        charla.update { it.copy(pensando = true, error = null) }
        viewModelScope.launch {
            when (val r = asistente.crearTarjetas(claveEnUso(), pedidoTarjetas(nota.nota.titulo, nota.nota.contenido))) {
                is ResultadoIA.Ok ->
                    if (r.valor.isEmpty()) charla.update { it.copy(pensando = false, error = ErrorIA.OTRO) }
                    else charla.update { it.copy(pensando = false, tarjetas = TarjetasPropuestas(nota, r.valor)) }
                is ResultadoIA.Falla -> charla.update { it.copy(pensando = false, error = r.error) }
            }
        }
    }

    // Guarda las elegidas en la materia de la nota (entran al repaso).
    fun guardarTarjetas(elegidas: List<TarjetaSugerida>) {
        val propuestas = charla.value.tarjetas ?: return
        charla.update { it.copy(tarjetas = null) }
        if (elegidas.isEmpty()) return
        viewModelScope.launch {
            elegidas.forEach { repositorio.agregarTarjeta(propuestas.nota.materiaId, it.pregunta, it.respuesta) }
            avisar(Aviso(Mensaje.AVISO_TARJETAS_IA_GUARDADAS, argumentos = listOf(elegidas.size, propuestas.nota.nombreMateria)))
        }
    }

    fun descartarTarjetas() = charla.update { it.copy(tarjetas = null) }

    // ---------- Plan de estudio para un examen ----------

    // La IA reparte el temario en sesiones (sin pisar la agenda); se revisan
    // en una ventana antes de guardarlas como bloques de estudio.
    fun pedirPlan(examen: ExamenProximo) {
        if (charla.value.pensando) return
        charla.update { it.copy(pensando = true, error = null) }
        viewModelScope.launch {
            val dia = hoy()
            val contexto = repositorio.usuarioActual.first()?.let { contextoDelEstudiante(it, dia) }.orEmpty()
            when (val r = asistente.planEstudio(claveEnUso(), pedidoPlanEstudio(examen, dia), contexto)) {
                is ResultadoIA.Ok -> {
                    val sesiones = limpiarPlan(r.valor, dia, examen.evento.fecha)
                    if (sesiones.isEmpty()) charla.update { it.copy(pensando = false, error = ErrorIA.OTRO) }
                    else charla.update { it.copy(pensando = false, plan = PlanPropuesto(examen, sesiones)) }
                }
                is ResultadoIA.Falla -> charla.update { it.copy(pensando = false, error = r.error) }
            }
        }
    }

    // Guarda las sesiones elegidas como bloques de estudio en la materia del
    // examen (con sus avisos, si están activados). prefijo: "Estudiar".
    fun guardarPlan(elegidas: List<SesionPlan>, prefijo: String) {
        val plan = charla.value.plan ?: return
        charla.update { it.copy(plan = null) }
        if (elegidas.isEmpty()) return
        viewModelScope.launch {
            val guardados = elegidas.map { repositorio.agregarEvento(plan.examen.materiaId, eventoDeSesion(it, prefijo)) }
            avisosSistema?.programarEventos(guardados)
            avisar(Aviso(Mensaje.AVISO_PLAN_IA_GUARDADO, argumentos = listOf(guardados.size, plan.examen.evento.titulo)))
        }
    }

    fun descartarPlan() = charla.update { it.copy(plan = null) }

    // ---------- Mapa mental desde una nota ----------

    fun crearMapa(nota: NotaConMateria) {
        if (charla.value.pensando) return
        charla.update { it.copy(pensando = true, error = null) }
        viewModelScope.launch {
            val usuario = repositorio.usuarioActual.first()
            val r = asistente.mapaMental(claveEnUso(), pedidoMapa(nota.nota.titulo, nota.nota.contenido))
            val mapa = (r as? ResultadoIA.Ok)?.valor?.let { (central, ramas) -> mapaDesdeIA(central, ramas, nota.nombreMateria, hoy()) }
            when {
                r is ResultadoIA.Falla -> charla.update { it.copy(pensando = false, error = r.error) }
                mapa == null || usuario == null -> charla.update { it.copy(pensando = false, error = ErrorIA.OTRO) }
                else -> {
                    val guardado = repositorio.agregarMapa(usuario.id, mapa)
                    charla.update { it.copy(pensando = false, mapaCreado = guardado.id) }
                    avisar(Aviso(Mensaje.AVISO_MAPA_IA_CREADO))
                }
            }
        }
    }

    // La pantalla ya abrió el mapa nuevo.
    fun mapaAbierto() = charla.update { it.copy(mapaCreado = null) }

    // ---------- Foto de un apunte ----------

    // La IA pasa la foto a texto; la respuesta queda en la charla, con
    // "Guardar como nota" para guardarla en una materia.
    fun leerApunte(uri: String, etiqueta: String) {
        if (charla.value.pensando) return
        charla.update { it.copy(conversacion = it.conversacion + MensajeIA(true, PEDIDO_FOTO_APUNTE, mostrado = etiqueta), pensando = true, error = null) }
        viewModelScope.launch {
            val foto = leerFoto(uri)
            if (foto == null) {
                charla.update { it.copy(pensando = false, error = ErrorIA.OTRO) }
                return@launch
            }
            when (val r = asistente.leerImagen(claveEnUso(), foto, PEDIDO_FOTO_APUNTE)) {
                is ResultadoIA.Ok -> charla.update { it.copy(conversacion = it.conversacion + MensajeIA(false, r.valor), pensando = false) }
                is ResultadoIA.Falla -> charla.update { it.copy(pensando = false, error = r.error) }
            }
        }
    }

    // ---------- Horario de cursada desde una foto ----------

    // La IA lee las clases de la foto; se revisan en una ventana antes de guardarlas.
    fun leerHorario(uri: String) {
        if (charla.value.pensando) return
        charla.update { it.copy(pensando = true, error = null) }
        viewModelScope.launch {
            val foto = leerFoto(uri)
            val materias = repositorio.usuarioActual.first()?.materias.orEmpty().filter { !it.archivada }.map { it.nombre }
            val r = if (foto == null) ResultadoIA.Falla(ErrorIA.OTRO) else asistente.leerHorario(claveEnUso(), foto, pedidoHorario(materias))
            when (r) {
                is ResultadoIA.Ok -> {
                    val clases = limpiarHorario(r.valor)
                    if (clases.isEmpty()) charla.update { it.copy(pensando = false, error = ErrorIA.OTRO) }
                    else charla.update { it.copy(pensando = false, horario = clases) }
                }
                is ResultadoIA.Falla -> charla.update { it.copy(pensando = false, error = r.error) }
            }
        }
    }

    // Guarda las elegidas como clases semanales. Las materias que no están
    // cargadas se crean (una sola vez aunque tengan varias clases).
    fun guardarHorario(elegidas: List<ClaseDelHorario>) {
        charla.update { it.copy(horario = null) }
        if (elegidas.isEmpty()) return
        viewModelScope.launch {
            val usuario = repositorio.usuarioActual.first() ?: return@launch
            val nuevas = elegidas.filter { it.materia == null }.map { it.clase.materia }.distinctBy { normalizarNombre(it) }
            nuevas.forEachIndexed { i, nombre ->
                val colores = MateriasViewModel.COLORES
                repositorio.agregarMateria(usuario.id, nombre, colores[(usuario.materias.size + i) % colores.size])
            }
            val materias = repositorio.usuarioActual.first()?.materias.orEmpty().filter { !it.archivada }
            val dia = hoy()
            val guardados = elegidas.mapNotNull { item ->
                val materia = item.materia ?: materias.firstOrNull { normalizarNombre(it.nombre) == normalizarNombre(item.clase.materia) }
                materia?.let { repositorio.agregarEvento(it.id, eventoDeClase(item.clase, dia)) }
            }
            avisosSistema?.programarEventos(guardados)
            avisar(Aviso(Mensaje.AVISO_HORARIO_IA_GUARDADO, argumentos = listOf(guardados.size, nuevas.size)))
        }
    }

    fun descartarHorario() = charla.update { it.copy(horario = null) }

    // ---------- Simulacro de examen ----------

    // La IA toma preguntas de a una sobre las notas elegidas y corrige en la charla.
    fun simulacro(notas: List<NotaConMateria>, etiqueta: String) {
        if (notas.isEmpty()) return
        val material = notas.take(MAXIMO_NOTAS_SIMULACRO).map { it.nota.titulo to it.nota.contenido }
        preguntar(MensajeIA(true, pedidoSimulacro(material), mostrado = etiqueta))
    }

    // ---------- Clave propia ----------

    // Prueba la clave con un pedido mínimo y, si anda, la guarda para este
    // usuario. Si falla, queda el error para mostrarlo en la ventana.
    fun conectarClave(clave: String, alConectar: () -> Unit) {
        val limpia = clave.trim()
        if (limpia.isEmpty() || charla.value.probandoClave) return
        charla.update { it.copy(probandoClave = true, errorClave = null) }
        viewModelScope.launch {
            val usuario = repositorio.usuarioActual.first() ?: return@launch
            when (val r = asistente.responder(limpia, listOf(MensajeIA(true, PEDIDO_PRUEBA)))) {
                is ResultadoIA.Ok -> {
                    claves.guardar(usuario.id, limpia)
                    charla.update { it.copy(probandoClave = false, error = null) }
                    avisar(Aviso(Mensaje.AVISO_IA_CONECTADA))
                    alConectar()
                }
                // Si es el límite, la clave es válida igual: se guarda.
                is ResultadoIA.Falla -> if (r.error == ErrorIA.LIMITE) {
                    claves.guardar(usuario.id, limpia)
                    charla.update { it.copy(probandoClave = false) }
                    avisar(Aviso(Mensaje.AVISO_IA_CONECTADA))
                    alConectar()
                } else {
                    charla.update { it.copy(probandoClave = false, errorClave = r.error) }
                }
            }
        }
    }

    fun limpiarErrorClave() = charla.update { it.copy(errorClave = null) }

    fun quitarClave() {
        viewModelScope.launch {
            val usuario = repositorio.usuarioActual.first() ?: return@launch
            claves.guardar(usuario.id, null)
            avisar(Aviso(Mensaje.AVISO_IA_DESCONECTADA))
        }
    }

    // Guarda una respuesta de la IA como nota nueva de una materia.
    fun guardarComoNota(materiaId: Int, titulo: String, contenido: String) {
        if (titulo.isBlank()) return
        viewModelScope.launch {
            repositorio.agregarNota(materiaId, titulo.trim(), contenido.trim())
            avisar(Aviso(Mensaje.AVISO_NOTA_IA_GUARDADA))
        }
    }
}
