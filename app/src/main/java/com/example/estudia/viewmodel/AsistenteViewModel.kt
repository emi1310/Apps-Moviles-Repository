package com.example.estudia.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.estudia.datos.AsistenteIA
import com.example.estudia.datos.ClavesIA
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.modelo.ErrorIA
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MensajeIA
import com.example.estudia.modelo.ResultadoIA
import com.example.estudia.modelo.TarjetaSugerida
import com.example.estudia.modelo.PEDIDO_PRUEBA
import com.example.estudia.modelo.pedidoExplicar
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
    val tarjetasPropuestas: TarjetasPropuestas? = null
)

// Estado interno de la charla (el resto sale del usuario).
private data class Charla(
    val conversacion: List<MensajeIA> = emptyList(),
    val pensando: Boolean = false,
    val error: ErrorIA? = null,
    val tarjetas: TarjetasPropuestas? = null,
    val probandoClave: Boolean = false,
    val errorClave: ErrorIA? = null
)

// ViewModel del Asistente IA: una charla con Gemini y acciones rápidas
// sobre las notas (resumir, preguntas para autoevaluarse y tarjetas). La
// conversación vive mientras está la pantalla: no se guarda.
class AsistenteViewModel(
    private val repositorio: RepositorioEstudia,
    private val asistente: AsistenteIA,
    private val claves: ClavesIA,
    // La de gemini.properties (puede estar vacía).
    private val claveDeLaApp: String = ""
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
            tarjetasPropuestas = c.tarjetas
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
            when (val r = asistente.responder(claveEnUso(), charla.value.conversacion)) {
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
