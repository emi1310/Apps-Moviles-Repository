package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.AsistenteIA
import com.example.estudia.datos.ClavesIA
import com.example.estudia.datos.HistorialIA
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.AccionPropuesta
import com.example.estudia.modelo.AdjuntoIA
import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.ClaseHorario
import com.example.estudia.modelo.ConversacionGuardada
import com.example.estudia.modelo.ErrorIA
import com.example.estudia.modelo.EstadoAccion
import com.example.estudia.modelo.EvaluacionSimulacro
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.ExamenProximo
import com.example.estudia.modelo.MAXIMO_FOTOS_APUNTE
import com.example.estudia.modelo.MAXIMO_NOTAS_SIMULACRO
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MensajeIA
import com.example.estudia.modelo.NotaParaBuscar
import com.example.estudia.modelo.PEDIDO_PRUEBA
import com.example.estudia.modelo.PiezaRespuesta
import com.example.estudia.modelo.ResultadoIA
import com.example.estudia.modelo.ResultadoSimulacro
import com.example.estudia.modelo.SesionAtrasada
import com.example.estudia.modelo.SesionPlan
import com.example.estudia.modelo.TAMANO_MAXIMO_PDF
import com.example.estudia.modelo.TarjetaSugerida
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.buscarParecido
import com.example.estudia.modelo.contextoDeNotas
import com.example.estudia.modelo.contextoDelEstudiante
import com.example.estudia.modelo.contextoSimulacros
import com.example.estudia.modelo.eventoDeClase
import com.example.estudia.modelo.eventoDeSesion
import com.example.estudia.modelo.examenesProximos
import com.example.estudia.modelo.guardarMensajes
import com.example.estudia.modelo.limpiarEvaluacion
import com.example.estudia.modelo.limpiarHorario
import com.example.estudia.modelo.limpiarPlan
import com.example.estudia.modelo.limpiarRespuesta
import com.example.estudia.modelo.mapaDesdeIA
import com.example.estudia.modelo.materiaParecida
import com.example.estudia.modelo.mensajesDeGuardados
import com.example.estudia.modelo.normalizarNombre
import com.example.estudia.modelo.notasRelevantes
import com.example.estudia.modelo.pedidoEvaluarSimulacro
import com.example.estudia.modelo.pedidoExplicar
import com.example.estudia.modelo.pedidoFotoApunte
import com.example.estudia.modelo.pedidoHorario
import com.example.estudia.modelo.pedidoMapa
import com.example.estudia.modelo.pedidoPdf
import com.example.estudia.modelo.pedidoPlanEstudio
import com.example.estudia.modelo.pedidoPreguntas
import com.example.estudia.modelo.pedidoResumir
import com.example.estudia.modelo.pedidoSimulacro
import com.example.estudia.modelo.pedidoTarjetas
import com.example.estudia.modelo.reacomodarSesiones
import com.example.estudia.modelo.sesionesAtrasadas
import com.example.estudia.modelo.textoPlano
import com.example.estudia.modelo.tituloDeConversacion
import com.example.estudia.notificaciones.AvisosSistema
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
import java.time.LocalDate
import java.time.LocalDateTime

// Tarjetas que propuso la IA a partir de una nota, para revisarlas antes de guardar.
data class TarjetasPropuestas(val nota: NotaConMateria, val tarjetas: List<TarjetaSugerida>)

// Plan que propuso la IA para un examen, para revisarlo antes de guardarlo.
data class PlanPropuesto(val examen: ExamenProximo, val sesiones: List<SesionPlan>)

// Una clase leída del horario y la materia ya cargada a la que va (null = se crea).
data class ClaseDelHorario(val clase: ClaseHorario, val materia: Materia?)

// Un PDF adjunto en una materia (o en una de sus notas), para preguntarle a la IA.
data class PdfDisponible(val archivo: Archivo, val nombreMateria: String, val uri: String)

data class AsistenteUiState(
    // Hay alguna clave para usar (la del usuario o la de la app).
    val configurado: Boolean = true,
    // El usuario conectó la suya.
    val clavePropia: Boolean = false,
    // Probando una clave nueva y, si falló, por qué.
    val probandoClave: Boolean = false,
    val errorClave: ErrorIA? = null,
    val conversacion: List<MensajeIA> = emptyList(),
    // Esperando o recibiendo la respuesta.
    val pensando: Boolean = false,
    val error: ErrorIA? = null,
    // Para elegir sobre qué nota trabajar y dónde guardar una respuesta.
    val notas: List<NotaConMateria> = emptyList(),
    val materias: List<Materia> = emptyList(),
    val pdfs: List<PdfDisponible> = emptyList(),
    val tarjetasPropuestas: TarjetasPropuestas? = null,
    // Exámenes y entregas que vienen (para el plan de estudio).
    val examenes: List<ExamenProximo> = emptyList(),
    val planPropuesto: PlanPropuesto? = null,
    // Mapa recién creado desde una nota: la pantalla lo abre y lo avisa.
    val mapaCreado: Int? = null,
    // Clases que leyó la IA de la foto del horario, para revisarlas antes de guardar.
    val horarioPropuesto: List<ClaseDelHorario>? = null,
    // Charlas guardadas y simulacros hechos.
    val historial: List<ConversacionGuardada> = emptyList(),
    val simulacros: List<ResultadoSimulacro> = emptyList(),
    val conversacionId: Long? = null,
    // Hay un simulacro en curso (se puede terminar y guardar).
    val simulacroEnCurso: Boolean = false,
    // Cómo le fue en el simulacro que se acaba de terminar.
    val resultadoSimulacro: EvaluacionSimulacro? = null,
    // Sesiones del plan que ya pasaron, para preguntar si se hicieron.
    val sesionesAtrasadas: List<SesionAtrasada> = emptyList()
) {
    // La respuesta ya empezó a llegar (se ve escribiéndose).
    val escribiendo: Boolean get() = pensando && conversacion.lastOrNull()?.let { !it.deUsuario && it.texto.isNotEmpty() } == true
}

// Un simulacro en curso: de qué materia y desde qué mensaje de la charla empezó.
private data class SimulacroEnCurso(val materiaId: Int, val materia: String, val notas: List<String>, val desde: Int)

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
    val horario: List<ClaseHorario>? = null,
    // Con qué id se guarda (null = todavía no se guardó) y de qué materia es.
    val id: Long? = null,
    val materia: String? = null,
    val simulacro: SimulacroEnCurso? = null,
    val resultadoSimulacro: EvaluacionSimulacro? = null
)

// Lo guardado del usuario: charlas, simulacros y hasta cuándo revisó el plan.
private data class Memoria(
    val historial: List<ConversacionGuardada> = emptyList(),
    val simulacros: List<ResultadoSimulacro> = emptyList(),
    val planRevisado: LocalDate? = null
)

// ViewModel del Asistente IA: una charla con Gemini (en vivo, con formato y
// acciones que se confirman), acciones rápidas sobre las notas y los PDFs,
// herramientas que guardan en la app, el historial de charlas y simulacros, y
// reacomodar el plan de estudio.
class AsistenteViewModel(
    private val repositorio: RepositorioEstudia,
    private val asistente: AsistenteIA,
    private val claves: ClavesIA,
    // La de gemini.properties (puede estar vacía).
    private val claveDeLaApp: String = "",
    // Para la agenda que se le manda a la IA (en los tests, una fecha fija).
    private val hoy: () -> LocalDate = { LocalDate.now() },
    // Para programar los avisos de lo que se guarda en el calendario.
    private val avisosSistema: AvisosSistema? = null,
    // Lee una foto (content://) como JPEG achicado. Lo arma
    // AppViewModelProvider con util/Imagenes (acá no se usa Context).
    private val leerFoto: suspend (String) -> ByteArray? = { null },
    // Lee un archivo (content://) entero; si pasa del tope, devuelve un byte de más.
    private val leerArchivo: suspend (String) -> ByteArray? = { null },
    // Dónde se guardan las charlas y los simulacros (null = no se guardan).
    private val historial: HistorialIA? = null,
    // Cómo empiezan los bloques del plan de estudio ("Estudiar: ...").
    private val prefijoPlan: String = "Estudiar"
) : ViewModelConAvisos() {

    private val charla = MutableStateFlow(Charla())
    private var respuesta: Job? = null

    // La clave del usuario que está usando la app (null = no conectó la suya).
    @OptIn(ExperimentalCoroutinesApi::class)
    private val clavePropia = repositorio.usuarioActual.flatMapLatest { u -> if (u == null) flowOf(null) else claves.clave(u.id) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val memoria = repositorio.usuarioActual.flatMapLatest { u ->
        if (u == null) flowOf(Memoria())
        else combine(
            historial?.conversaciones(u.id) ?: flowOf(emptyList()),
            historial?.simulacros(u.id) ?: flowOf(emptyList()),
            repositorio.planRevisadoHasta(u.id)
        ) { c, s, p -> Memoria(c, s, p) }
    }

    // La que se usa: la del usuario o, si no tiene, la de la app.
    private suspend fun claveEnUso(): String = clavePropia.first() ?: claveDeLaApp

    val uiState: StateFlow<AsistenteUiState> = combine(repositorio.usuarioActual, clavePropia, charla, memoria) { u, propia, c, m ->
        val materias = u?.materias.orEmpty().filter { !it.archivada }
        AsistenteUiState(
            configurado = !propia.isNullOrBlank() || claveDeLaApp.isNotBlank(),
            clavePropia = !propia.isNullOrBlank(),
            probandoClave = c.probandoClave,
            errorClave = c.errorClave,
            conversacion = c.conversacion,
            pensando = c.pensando,
            error = c.error,
            notas = materias.flatMap { mat -> mat.notas.map { NotaConMateria(it, mat.id, mat.nombre, mat.color) } }
                .sortedByDescending { it.nota.fechaCreacion },
            materias = materias,
            pdfs = materias.flatMap { mat -> mat.todosLosArchivos().filter { it.esPdf }.map { PdfDisponible(it, mat.nombre, repositorio.uriParaAbrir(it)) } },
            tarjetasPropuestas = c.tarjetas,
            examenes = u?.let { examenesProximos(it, hoy()) }.orEmpty(),
            planPropuesto = c.plan,
            mapaCreado = c.mapaCreado,
            horarioPropuesto = c.horario?.map { ClaseDelHorario(it, materiaParecida(it.materia, materias)) },
            historial = m.historial,
            simulacros = m.simulacros.sortedByDescending { it.fecha },
            conversacionId = c.id,
            simulacroEnCurso = c.simulacro != null,
            resultadoSimulacro = c.resultadoSimulacro,
            sesionesAtrasadas = u?.let { sesionesAtrasadas(it, hoy(), prefijoPlan, m.planRevisado) }.orEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AsistenteUiState(configurado = claveDeLaApp.isNotBlank()))

    // ---------- La charla ----------

    // Una pregunta escrita en el chat (se buscan las notas que tienen que ver).
    fun enviar(texto: String) {
        if (texto.isBlank()) return
        preguntar(MensajeIA(true, texto.trim()), buscarEnNotas = true)
    }

    fun explicar(tema: String) {
        if (tema.isBlank()) return
        preguntar(MensajeIA(true, pedidoExplicar(tema), mostrado = tema.trim()), buscarEnNotas = true)
    }

    // etiqueta: lo que se ve en el chat, ej "Resumir: Guía 7".
    fun resumir(nota: NotaConMateria, etiqueta: String) =
        preguntar(MensajeIA(true, pedidoResumir(nota.nota.titulo, nota.nota.contenido), mostrado = etiqueta), materia = nota.nombreMateria)

    fun preguntas(nota: NotaConMateria, etiqueta: String) =
        preguntar(MensajeIA(true, pedidoPreguntas(nota.nota.titulo, nota.nota.contenido), mostrado = etiqueta), materia = nota.nombreMateria)

    // Si falló, vuelve a pedir la respuesta al último mensaje (si quedó una
    // respuesta cortada a la mitad, se descarta).
    fun reintentar() {
        val c = charla.value
        if (c.pensando) return
        if (c.conversacion.lastOrNull()?.deUsuario == false && c.error != null) {
            charla.update { it.copy(conversacion = it.conversacion.dropLast(1)) }
        }
        if (charla.value.conversacion.lastOrNull()?.deUsuario != true) return
        pedirRespuesta(buscarEnNotas = true)
    }

    // Corta la respuesta que está llegando (queda lo que llegó hasta ahí).
    fun detener() {
        respuesta?.cancel()
    }

    fun nuevaConversacion() {
        if (charla.value.pensando) return
        charla.value = Charla()
    }

    private fun preguntar(mensaje: MensajeIA, buscarEnNotas: Boolean = false, materia: String? = null) {
        if (charla.value.pensando) return
        charla.update { it.copy(conversacion = it.conversacion + mensaje, error = null, materia = it.materia ?: materia) }
        pedirRespuesta(buscarEnNotas)
    }

    // La respuesta llega en vivo: se agrega una burbuja vacía y se va llenando.
    // Con cada pregunta van los datos cargados del estudiante, lo que le costó
    // en los simulacros y, si es una pregunta escrita, las notas que tienen que ver.
    private fun pedirRespuesta(buscarEnNotas: Boolean) {
        charla.update { it.copy(pensando = true, error = null) }
        respuesta = viewModelScope.launch {
            try {
                val usuario = repositorio.usuarioActual.first()
                val pregunta = charla.value.conversacion.last()
                val relevantes = if (buscarEnNotas && usuario != null) notasRelevantes(pregunta.texto, notasParaBuscar(usuario)) else emptyList()
                val simulacros = usuario?.let { historial?.simulacros(it.id)?.first() }.orEmpty()
                val contexto = listOf(
                    usuario?.let { contextoDelEstudiante(it, hoy()) }.orEmpty(),
                    contextoSimulacros(simulacros),
                    contextoDeNotas(relevantes)
                ).filter { it.isNotBlank() }.joinToString("\n\n")
                val enviada = charla.value.conversacion
                charla.update { it.copy(conversacion = it.conversacion + MensajeIA(false, "", fuentes = relevantes.map { n -> n.titulo })) }
                asistente.conversar(claveEnUso(), enviada, contexto).collect { pieza ->
                    when (pieza) {
                        is PiezaRespuesta.Texto -> cambiarUltima { it.copy(texto = it.texto + pieza.texto, buscando = null) }
                        is PiezaRespuesta.Buscando -> cambiarUltima { it.copy(buscando = pieza.consulta) }
                        is PiezaRespuesta.FuentesWeb -> cambiarUltima { it.copy(fuentesWeb = (it.fuentesWeb + pieza.fuentes).distinctBy { f -> f.url }) }
                        is PiezaRespuesta.Accion -> charla.update { c ->
                            val ultima = c.conversacion.last()
                            // Si la burbuja ya tiene una acción, la siguiente va aparte.
                            if (ultima.accion == null) c.copy(conversacion = c.conversacion.dropLast(1) + ultima.copy(accion = pieza.accion))
                            else c.copy(conversacion = c.conversacion + MensajeIA(false, "", accion = pieza.accion))
                        }
                        is PiezaRespuesta.Error -> charla.update { it.copy(error = pieza.error) }
                    }
                }
            } finally {
                // Se ordena el texto; si no llegó nada, se saca la burbuja vacía.
                charla.update { c ->
                    val ultima = c.conversacion.lastOrNull()
                    val conversacion = when {
                        ultima == null || ultima.deUsuario -> c.conversacion
                        ultima.texto.isBlank() && ultima.accion == null -> c.conversacion.dropLast(1)
                        else -> limpiarRespuesta(ultima.texto).let { limpio -> c.conversacion.dropLast(1) + ultima.copy(texto = limpio, mostrado = limpio, buscando = null) }
                    }
                    c.copy(conversacion = conversacion, pensando = false)
                }
                viewModelScope.launch { guardarCharla() }
            }
        }
    }

    private fun cambiarUltima(cambio: (MensajeIA) -> MensajeIA) = charla.update { c ->
        c.copy(conversacion = c.conversacion.dropLast(1) + cambio(c.conversacion.last()))
    }

    private fun notasParaBuscar(usuario: Usuario): List<NotaParaBuscar> =
        usuario.materias.filter { !it.archivada }.flatMap { m -> m.notas.map { NotaParaBuscar(it.titulo, m.nombre, it.contenido) } }

    // ---------- Charlas guardadas ----------

    // Se guarda después de cada respuesta (si hay pregunta y respuesta).
    private suspend fun guardarCharla() {
        val historial = historial ?: return
        val c = charla.value
        if (c.conversacion.size < 2) return
        val usuario = repositorio.usuarioActual.first() ?: return
        val id = c.id ?: System.currentTimeMillis().also { nuevo -> charla.update { it.copy(id = it.id ?: nuevo) } }
        historial.guardarConversacion(
            usuario.id,
            ConversacionGuardada(id, tituloDeConversacion(c.conversacion), c.materia, LocalDateTime.now(), guardarMensajes(c.conversacion))
        )
    }

    fun abrirConversacion(conversacion: ConversacionGuardada) {
        if (charla.value.pensando) return
        charla.value = Charla(conversacion = mensajesDeGuardados(conversacion.mensajes), id = conversacion.id, materia = conversacion.materia)
    }

    fun borrarConversacion(id: Long) {
        viewModelScope.launch {
            val usuario = repositorio.usuarioActual.first() ?: return@launch
            historial?.borrarConversacion(usuario.id, id)
            if (charla.value.id == id) charla.value = Charla()
        }
    }

    // ---------- Acciones que propone la IA ----------

    // indice: la posición del mensaje con la acción en la charla.
    fun confirmarAccion(indice: Int) {
        viewModelScope.launch {
            val aviso = hacerAccion(indice) ?: return@launch
            avisar(aviso)
            guardarCharla()
        }
    }

    // "Confirmar todas": hace, en orden, todas las acciones que esperan.
    fun confirmarTodas() {
        val pendientes = charla.value.conversacion.indices.filter {
            val m = charla.value.conversacion[it]
            m.accion != null && m.estadoAccion == EstadoAccion.PENDIENTE
        }
        if (pendientes.isEmpty()) return
        viewModelScope.launch {
            val avisos = pendientes.mapNotNull { hacerAccion(it) }
            val hechas = pendientes.count { charla.value.conversacion[it].estadoAccion == EstadoAccion.HECHA }
            avisar(
                if (hechas == pendientes.size) Aviso(Mensaje.AVISO_ACCIONES_IA_HECHAS, listOf(hechas))
                else avisos.firstOrNull { it.mensaje != Mensaje.AVISO_ACCION_IA_HECHA && it.mensaje != Mensaje.AVISO_ACCION_IA_BORRADA }
                    ?: Aviso(Mensaje.AVISO_ACCIONES_IA_HECHAS, listOf(hechas))
            )
            guardarCharla()
        }
    }

    // Hace la acción de ese mensaje, marca cómo quedó y devuelve el aviso que
    // corresponde (null si no había nada que hacer).
    private suspend fun hacerAccion(indice: Int): Aviso? {
        val mensaje = charla.value.conversacion.getOrNull(indice) ?: return null
        val accion = mensaje.accion ?: return null
        if (mensaje.estadoAccion != EstadoAccion.PENDIENTE) return null
        val usuario = repositorio.usuarioActual.first() ?: return null
        val hecho = try {
            ejecutar(accion, usuario)
        } catch (e: Exception) {
            Hecho(ResultadoAccion.FALLO)
        }
        cambiarEstado(indice, if (hecho.resultado == ResultadoAccion.HECHA) EstadoAccion.HECHA else EstadoAccion.FALLO)
        return when (hecho.resultado) {
            ResultadoAccion.SIN_MATERIA -> Aviso(Mensaje.AVISO_ACCION_IA_SIN_MATERIA, listOf(accion.materia))
            ResultadoAccion.NO_ENCONTRADO -> Aviso(Mensaje.AVISO_ACCION_IA_NO_ENCONTRADO)
            ResultadoAccion.FALLO -> Aviso(Mensaje.AVISO_ACCION_IA_ERROR)
            ResultadoAccion.HECHA ->
                if (hecho.alDeshacer != null) Aviso(Mensaje.AVISO_ACCION_IA_BORRADA, alDeshacer = hecho.alDeshacer, alConfirmar = hecho.alConfirmar)
                else Aviso(Mensaje.AVISO_ACCION_IA_HECHA)
        }
    }

    fun descartarAccion(indice: Int) {
        if (charla.value.conversacion.getOrNull(indice)?.estadoAccion != EstadoAccion.PENDIENTE) return
        cambiarEstado(indice, EstadoAccion.DESCARTADA)
        viewModelScope.launch { guardarCharla() }
    }

    private fun cambiarEstado(indice: Int, estado: EstadoAccion) = charla.update { c ->
        c.copy(conversacion = c.conversacion.mapIndexed { i, m -> if (i == indice) m.copy(estadoAccion = estado) else m })
    }

    private enum class ResultadoAccion { HECHA, SIN_MATERIA, NO_ENCONTRADO, FALLO }

    // Cómo salió una acción. Las que borran devuelven cómo deshacerlo (y qué
    // hacer si no se deshace, como borrar los archivos del disco).
    private class Hecho(
        val resultado: ResultadoAccion,
        val alDeshacer: (suspend () -> Unit)? = null,
        val alConfirmar: (suspend () -> Unit)? = null
    )

    private val hecha = Hecho(ResultadoAccion.HECHA)
    private val noEncontrada = Hecho(ResultadoAccion.NO_ENCONTRADO)

    // Hace lo que pidió la IA (ya confirmado por el estudiante). Las cosas se
    // buscan por el nombre que usó la IA: la materia, y adentro la tarea, el
    // evento o la nota más parecidos.
    private suspend fun ejecutar(accion: AccionPropuesta, usuario: Usuario): Hecho {
        if (accion is AccionPropuesta.CrearMateria) {
            val colores = MateriasViewModel.COLORES
            repositorio.agregarMateria(usuario.id, accion.materia, colores[usuario.materias.size % colores.size])
            return hecha
        }
        // Para cambiar o borrar una materia vale también una archivada.
        val dondeBuscar = if (accion is AccionPropuesta.EditarMateria || accion is AccionPropuesta.BorrarMateria) usuario.materias
        else usuario.materias.filter { !it.archivada }
        val materia = materiaParecida(accion.materia, dondeBuscar) ?: return Hecho(ResultadoAccion.SIN_MATERIA)
        fun tarea(nombre: String) = buscarParecido(nombre, materia.tareas.map { it.titulo })?.let { materia.tareas[it] }
        fun evento(nombre: String) = buscarParecido(nombre, materia.eventos.map { it.titulo })?.let { materia.eventos[it] }
        fun nota(nombre: String) = buscarParecido(nombre, materia.notas.map { it.titulo })?.let { materia.notas[it] }

        return when (accion) {
            is AccionPropuesta.CrearMateria -> hecha
            is AccionPropuesta.CrearTarea -> {
                val nueva = repositorio.agregarTarea(materia.id, accion.titulo, accion.fechaLimite, accion.prioridad)
                avisosSistema?.programarTarea(nueva)
                hecha
            }
            is AccionPropuesta.EditarTarea -> {
                val t = tarea(accion.tarea) ?: return noEncontrada
                val cambiada = t.copy(
                    titulo = accion.nuevoTitulo ?: t.titulo,
                    fechaLimite = if (accion.quitarFecha) null else accion.nuevaFecha ?: t.fechaLimite,
                    prioridad = accion.nuevaPrioridad ?: t.prioridad
                )
                repositorio.actualizarTarea(materia.id, cambiada)
                if (!cambiada.completada) avisosSistema?.programarTarea(cambiada)
                hecha
            }
            is AccionPropuesta.CompletarTarea -> {
                val t = tarea(accion.tarea) ?: return noEncontrada
                val cambiada = t.copy(completada = accion.hecha)
                repositorio.actualizarTarea(materia.id, cambiada)
                if (cambiada.completada) avisosSistema?.cancelarTarea(t.id) else avisosSistema?.programarTarea(cambiada)
                hecha
            }
            is AccionPropuesta.BorrarTarea -> {
                val t = tarea(accion.tarea) ?: return noEncontrada
                repositorio.eliminarTarea(materia.id, t)
                avisosSistema?.cancelarTarea(t.id)
                Hecho(ResultadoAccion.HECHA, alDeshacer = {
                    repositorio.restaurarTarea(materia.id, t)
                    if (!t.completada) avisosSistema?.programarTarea(t)
                })
            }
            is AccionPropuesta.CrearEvento -> {
                val nuevo = repositorio.agregarEvento(materia.id, Evento(0, accion.titulo, accion.tipo, accion.fecha, accion.inicio, accion.fin))
                avisosSistema?.programarEvento(nuevo)
                hecha
            }
            is AccionPropuesta.EditarEvento -> {
                val e = evento(accion.evento) ?: return noEncontrada
                val inicio = accion.nuevoInicio ?: e.horaInicio
                // Si solo cambia la hora de inicio, se mantiene lo que duraba.
                val fin = accion.nuevoFin ?: if (accion.nuevoInicio != null) inicio.plusMinutes(e.duracionMinutos()) else e.horaFin
                if (!fin.isAfter(inicio)) return Hecho(ResultadoAccion.FALLO)
                val cambiado = e.copy(
                    titulo = accion.nuevoTitulo ?: e.titulo,
                    fecha = accion.nuevaFecha ?: e.fecha,
                    horaInicio = inicio,
                    horaFin = fin,
                    lugar = accion.nuevoLugar ?: e.lugar
                )
                repositorio.actualizarEvento(materia.id, cambiado)
                avisosSistema?.programarEvento(cambiado)
                hecha
            }
            is AccionPropuesta.BorrarEvento -> {
                val e = evento(accion.evento) ?: return noEncontrada
                repositorio.eliminarEvento(materia.id, e)
                avisosSistema?.cancelarEvento(e.id)
                Hecho(ResultadoAccion.HECHA, alDeshacer = {
                    repositorio.restaurarEvento(materia.id, e)
                    avisosSistema?.programarEvento(e)
                })
            }
            is AccionPropuesta.MarcarTema -> {
                val examenes = materia.eventos.filter { it.temas.isNotEmpty() }
                val examen = buscarParecido(accion.examen, examenes.map { it.titulo })?.let { examenes[it] }
                val tema = examen?.let { e -> buscarParecido(accion.tema, e.temas.map { it.titulo })?.let { e.temas[it] } }
                if (examen == null || tema == null) noEncontrada
                else {
                    if (!tema.estudiado) repositorio.alternarTema(examen.id, tema)
                    hecha
                }
            }
            is AccionPropuesta.AgregarTema -> {
                // Primero entre los exámenes; si no, cualquier evento con ese nombre.
                val examenes = materia.eventos.filter { it.tipo == TipoEvento.EXAMEN }
                val examen = buscarParecido(accion.examen, examenes.map { it.titulo })?.let { examenes[it] } ?: evento(accion.examen)
                    ?: return noEncontrada
                repositorio.agregarTema(examen.id, accion.tema)
                hecha
            }
            is AccionPropuesta.CrearNota -> {
                repositorio.agregarNota(materia.id, accion.titulo, textoPlano(accion.contenido))
                hecha
            }
            is AccionPropuesta.EditarNota -> {
                val n = nota(accion.nota) ?: return noEncontrada
                val base = accion.nuevoContenido?.let { textoPlano(it) } ?: n.contenido
                val contenido = accion.agregarTexto?.let { extra -> listOf(base.trimEnd(), textoPlano(extra)).filter { it.isNotBlank() }.joinToString("\n\n") } ?: base
                repositorio.actualizarNota(materia.id, n.copy(titulo = accion.nuevoTitulo ?: n.titulo, contenido = contenido))
                hecha
            }
            is AccionPropuesta.BorrarNota -> {
                val n = nota(accion.nota) ?: return noEncontrada
                repositorio.eliminarNota(materia.id, n)
                Hecho(
                    ResultadoAccion.HECHA,
                    alDeshacer = { repositorio.restaurarNota(materia.id, n) },
                    alConfirmar = { repositorio.borrarArchivosDelDisco(n.archivos) }
                )
            }
            is AccionPropuesta.EditarMateria -> {
                repositorio.actualizarMateria(usuario.id, materia.copy(nombre = accion.nuevoNombre ?: materia.nombre, archivada = accion.archivar ?: materia.archivada))
                hecha
            }
            is AccionPropuesta.BorrarMateria -> {
                repositorio.eliminarMateria(usuario.id, materia)
                materia.eventos.forEach { avisosSistema?.cancelarEvento(it.id) }
                Hecho(
                    ResultadoAccion.HECHA,
                    alDeshacer = {
                        repositorio.restaurarMateria(usuario.id, materia)
                        avisosSistema?.programarEventos(materia.eventos)
                    },
                    alConfirmar = { repositorio.borrarArchivosDelDisco(materia.todosLosArchivos()) }
                )
            }
            is AccionPropuesta.CrearTarjeta -> {
                repositorio.agregarTarjeta(materia.id, accion.pregunta, accion.respuesta)
                hecha
            }
            is AccionPropuesta.AgregarCalificacion -> {
                repositorio.agregarCalificacion(materia.id, accion.titulo, accion.nota, accion.fecha ?: hoy())
                hecha
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
    fun guardarPlan(elegidas: List<SesionPlan>, prefijo: String = prefijoPlan) {
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

    // ---------- Reacomodar el plan ----------

    // noHechas: las sesiones que el estudiante dijo que no hizo. Se mueven a
    // un hueco libre antes del examen; las demás quedan como están.
    fun reacomodarPlan(noHechas: List<SesionAtrasada>) {
        viewModelScope.launch {
            val usuario = repositorio.usuarioActual.first() ?: return@launch
            val movidas = reacomodarSesiones(noHechas, usuario, hoy())
            movidas.forEach { repositorio.actualizarEvento(it.materiaId, it.evento) }
            avisosSistema?.programarEventos(movidas.map { it.evento })
            repositorio.marcarPlanRevisado(usuario.id, hoy().minusDays(1))
            avisar(
                if (noHechas.isEmpty()) Aviso(Mensaje.AVISO_PLAN_AL_DIA)
                else Aviso(Mensaje.AVISO_PLAN_REACOMODADO, listOf(movidas.size, noHechas.size - movidas.size))
            )
        }
    }

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

    // ---------- Fotos de un apunte ----------

    // Una o varias fotos (hasta 10 hojas): la IA las pasa a un solo texto en la
    // charla; se puede seguir preguntando y guardar la respuesta como nota.
    fun leerApunte(uris: List<String>, etiqueta: String) {
        if (charla.value.pensando || uris.isEmpty()) return
        charla.update { it.copy(pensando = true, error = null) }
        viewModelScope.launch {
            val fotos = uris.take(MAXIMO_FOTOS_APUNTE).mapNotNull { leerFoto(it) }
            charla.update { it.copy(pensando = false) }
            if (fotos.isEmpty()) {
                charla.update { it.copy(error = ErrorIA.OTRO) }
                return@launch
            }
            preguntar(
                MensajeIA(
                    true, pedidoFotoApunte(fotos.size), mostrado = etiqueta,
                    adjuntos = fotos.mapIndexed { i, foto -> AdjuntoIA("image/jpeg", foto, "foto ${i + 1}") }
                )
            )
        }
    }

    // ---------- Preguntar sobre un PDF ----------

    // El PDF va con la charla: después de la primera respuesta se puede seguir
    // preguntando sobre él. materia: la del PDF adjunto (null = del teléfono).
    fun preguntarPdf(uri: String, nombre: String, materia: String?, etiqueta: String) {
        if (charla.value.pensando) return
        charla.update { it.copy(pensando = true, error = null) }
        viewModelScope.launch {
            val bytes = leerArchivo(uri)
            charla.update { it.copy(pensando = false) }
            when {
                bytes == null || bytes.isEmpty() -> charla.update { it.copy(error = ErrorIA.OTRO) }
                bytes.size > TAMANO_MAXIMO_PDF -> charla.update { it.copy(error = ErrorIA.ARCHIVO_GRANDE) }
                else -> preguntar(
                    MensajeIA(true, pedidoPdf(nombre), mostrado = etiqueta, adjuntos = listOf(AdjuntoIA("application/pdf", bytes, nombre))),
                    materia = materia
                )
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
        if (notas.isEmpty() || charla.value.pensando) return
        val elegidas = notas.take(MAXIMO_NOTAS_SIMULACRO)
        val primera = elegidas.first()
        charla.update {
            it.copy(simulacro = SimulacroEnCurso(primera.materiaId, primera.nombreMateria, elegidas.map { n -> n.nota.titulo }, it.conversacion.size))
        }
        preguntar(MensajeIA(true, pedidoSimulacro(elegidas.map { it.nota.titulo to it.nota.contenido }), mostrado = etiqueta), materia = primera.nombreMateria)
    }

    // Termina el simulacro: la IA lo evalúa, se guarda la nota y los temas que
    // costaron, y lo que se respondió mal pasa a tarjetas de repaso.
    fun terminarSimulacro() {
        val simulacro = charla.value.simulacro ?: return
        if (charla.value.pensando) return
        charla.update { it.copy(pensando = true, error = null) }
        viewModelScope.launch {
            val mensajes = charla.value.conversacion.drop(simulacro.desde)
            when (val r = asistente.evaluarSimulacro(claveEnUso(), pedidoEvaluarSimulacro(mensajes))) {
                is ResultadoIA.Ok -> {
                    val evaluacion = limpiarEvaluacion(r.valor)
                    repositorio.usuarioActual.first()?.let { u ->
                        historial?.guardarSimulacro(u.id, ResultadoSimulacro(hoy(), simulacro.materia, simulacro.notas, evaluacion.nota, evaluacion.temasFlojos))
                    }
                    evaluacion.tarjetas.forEach { repositorio.agregarTarjeta(simulacro.materiaId, it.pregunta, it.respuesta) }
                    charla.update { it.copy(pensando = false, simulacro = null, resultadoSimulacro = evaluacion) }
                }
                is ResultadoIA.Falla -> charla.update { it.copy(pensando = false, error = r.error) }
            }
        }
    }

    fun cerrarResultadoSimulacro() = charla.update { it.copy(resultadoSimulacro = null) }

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

    // Guarda una respuesta de la IA como nota nueva de una materia (en texto
    // plano: sin asteriscos y con las fórmulas legibles).
    fun guardarComoNota(materiaId: Int, titulo: String, contenido: String) {
        if (titulo.isBlank()) return
        viewModelScope.launch {
            repositorio.agregarNota(materiaId, titulo.trim(), textoPlano(contenido))
            avisar(Aviso(Mensaje.AVISO_NOTA_IA_GUARDADA))
        }
    }
}
