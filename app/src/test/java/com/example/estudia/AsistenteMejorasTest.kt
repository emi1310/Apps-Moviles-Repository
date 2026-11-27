package com.example.estudia

import com.example.estudia.datos.HistorialIA
import com.example.estudia.modelo.AccionPropuesta
import com.example.estudia.modelo.AdjuntoIA
import com.example.estudia.modelo.ConversacionGuardada
import com.example.estudia.modelo.ErrorIA
import com.example.estudia.modelo.EstadoAccion
import com.example.estudia.modelo.EvaluacionSimulacro
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MensajeIA
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.NotaParaBuscar
import com.example.estudia.modelo.Prioridad
import com.example.estudia.modelo.ResultadoIA
import com.example.estudia.modelo.ResultadoSimulacro
import com.example.estudia.modelo.TAMANO_MAXIMO_PDF
import com.example.estudia.modelo.TarjetaSugerida
import com.example.estudia.modelo.Tema
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.accionDesdeLlamada
import com.example.estudia.modelo.contextoParaEnviar
import com.example.estudia.modelo.descripcionParaIA
import com.example.estudia.modelo.formulaLegible
import com.example.estudia.modelo.limpiarEvaluacion
import com.example.estudia.modelo.notasRelevantes
import com.example.estudia.modelo.reacomodarSesiones
import com.example.estudia.modelo.sesionesAtrasadas
import com.example.estudia.modelo.textoPlano
import com.example.estudia.viewmodel.Aviso
import com.example.estudia.viewmodel.AsistenteViewModel
import com.example.estudia.viewmodel.Mensaje
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

// Historial en memoria (en el teléfono es un archivo por usuario).
class HistorialFalso : HistorialIA {
    val charlas = MutableStateFlow<List<ConversacionGuardada>>(emptyList())
    val resultados = MutableStateFlow<List<ResultadoSimulacro>>(emptyList())
    override fun conversaciones(usuarioId: Int): Flow<List<ConversacionGuardada>> = charlas.map { l -> l.sortedByDescending { it.actualizada } }
    override suspend fun guardarConversacion(usuarioId: Int, conversacion: ConversacionGuardada) {
        charlas.value = charlas.value.filter { it.id != conversacion.id } + conversacion
    }
    override suspend fun borrarConversacion(usuarioId: Int, id: Long) {
        charlas.value = charlas.value.filter { it.id != id }
    }
    override fun simulacros(usuarioId: Int): Flow<List<ResultadoSimulacro>> = resultados
    override suspend fun guardarSimulacro(usuarioId: Int, resultado: ResultadoSimulacro) {
        resultados.value = resultados.value + resultado
    }
}

// Tests de las mejoras del Asistente: respuestas en vivo con formato, acciones
// desde el chat, charlas guardadas, PDFs, simulacro con memoria, búsqueda en
// las notas y reacomodar el plan.
@OptIn(ExperimentalCoroutinesApi::class)
class AsistenteMejorasTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    // 26/11/2026 es jueves.
    private val hoy = LocalDate.of(2026, 11, 26)
    private val guia = Nota(5, "Guía 7: integrales", "Integrales dobles: se integra primero en y, después en x. Teorema de Fubini.", hoy)
    private val otra = Nota(6, "Derivadas", "Regla de la cadena y derivadas parciales.", hoy)
    private val parcial = Evento(
        20, "Parcial 2", TipoEvento.EXAMEN, hoy.plusDays(5), LocalTime.of(9, 0), LocalTime.of(11, 0),
        temas = listOf(Tema(1, "Integrales dobles"), Tema(2, "Series"))
    )
    private val materia = Materia(3, "Análisis II", 0xFF6C5CE7, notas = listOf(guia, otra), eventos = listOf(parcial))
    private fun usuario(m: Materia = materia) = Usuario(id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana", materias = listOf(m))

    private fun TestScope.vm(
        repositorio: RepositorioFalso,
        ia: AsistenteFalso = AsistenteFalso(),
        historial: HistorialIA? = null,
        leerArchivo: suspend (String) -> ByteArray? = { null }
    ): AsistenteViewModel {
        val vm = AsistenteViewModel(repositorio, ia, ClavesFalsas(), "clave-app", hoy = { hoy }, historial = historial, leerArchivo = leerArchivo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    // ---------- Fórmulas y formato ----------

    @Test
    fun lasFormulasSeLeenComoTexto() {
        assertEquals("(a+b)/2", formulaLegible("\\frac{a+b}{2}"))
        assertEquals("1/2", formulaLegible("\\frac{1}{2}"))
        assertEquals("x² + y³", formulaLegible("x^2 + y^3"))
        assertEquals("√x", formulaLegible("\\sqrt{x}"))
        assertEquals("√(x+1)", formulaLegible("\\sqrt{x+1}"))
        assertEquals("e^(iπ) = -1", formulaLegible("e^{i\\pi} = -1"))
        assertEquals("aₙ ≤ α", formulaLegible("a_n \\leq \\alpha"))
        assertEquals("∫ f(x) dx", formulaLegible("\\int f(x)\\, dx"))
    }

    @Test
    fun elTextoPlanoSacaElFormato() {
        assertEquals("Paso 1\n• uno\n• dos x²", textoPlano("### Paso 1\n- **uno**\n* dos \$x^2\$"))
        // Un precio con $ suelto no se toma como fórmula.
        assertEquals("Cuesta \$500 y \$300", textoPlano("Cuesta \$500 y \$300"))
    }

    // ---------- Acciones ----------

    @Test
    fun lasAccionesDeLaIASeRevisan() {
        val tarea = accionDesdeLlamada("crear_tarea", mapOf("materia" to "Análisis II", "titulo" to " TP 4 ", "fecha_limite" to "2026-12-01", "prioridad" to "alta"))
        assertEquals(AccionPropuesta.CrearTarea("Análisis II", "TP 4", LocalDate.of(2026, 12, 1), Prioridad.ALTA), tarea)
        // Fecha rota, hora que termina antes de empezar o función desconocida: nada.
        assertNull(accionDesdeLlamada("crear_tarea", mapOf("materia" to "A", "titulo" to "TP", "fecha_limite" to "el viernes")))
        assertNull(accionDesdeLlamada("crear_evento", mapOf("materia" to "A", "titulo" to "P", "tipo" to "EXAMEN", "fecha" to "2026-12-01", "hora_inicio" to "11:00", "hora_fin" to "9:00")))
        assertNull(accionDesdeLlamada("borrar_todo", mapOf("materia" to "A")))
        val evento = accionDesdeLlamada("crear_evento", mapOf("materia" to "A", "titulo" to "Final", "tipo" to "examen", "fecha" to "2026-12-10", "hora_inicio" to "9", "hora_fin" to "12:00"))
        assertEquals(TipoEvento.EXAMEN, (evento as AccionPropuesta.CrearEvento).tipo)
        assertEquals(LocalTime.of(9, 0), evento.inicio)
        assertTrue(descripcionParaIA(evento, EstadoAccion.HECHA).contains("confirmó"))
    }

    @Test
    fun confirmarUnaAccionCreaLaTarea() = runTest {
        val repositorio = RepositorioFalso(usuario())
        val ia = AsistenteFalso(respuesta = ResultadoIA.Ok("Te propongo esto:")).apply {
            acciones = listOf(AccionPropuesta.CrearTarea("analisis ii", "TP 4", hoy.plusDays(3), Prioridad.MEDIA))
        }
        val vm = vm(repositorio, ia)
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }

        vm.enviar("Anotame el TP 4 para el domingo")
        advanceUntilIdle()
        val respuesta = vm.uiState.value.conversacion.last()
        assertEquals("Te propongo esto:", respuesta.texto)
        assertEquals(EstadoAccion.PENDIENTE, respuesta.estadoAccion)
        // Hasta confirmar no se guarda nada.
        assertTrue(repositorio.usuario.value!!.materias.single().tareas.isEmpty())

        vm.confirmarAccion(vm.uiState.value.conversacion.lastIndex)
        advanceUntilIdle()
        assertEquals("TP 4", repositorio.usuario.value!!.materias.single().tareas.single().titulo)
        assertEquals(EstadoAccion.HECHA, vm.uiState.value.conversacion.last().estadoAccion)
        assertEquals(Mensaje.AVISO_ACCION_IA_HECHA, avisos.last().mensaje)

        // Lo que se le manda después a la IA dice que se confirmó.
        vm.enviar("Gracias")
        advanceUntilIdle()
        assertTrue(com.example.estudia.modelo.textoParaIA(ia.recibidas.last()[1]).contains("confirmó"))
    }

    @Test
    fun marcarUnTemaYUnaMateriaQueNoExiste() = runTest {
        val repositorio = RepositorioFalso(usuario())
        val ia = AsistenteFalso(respuesta = ResultadoIA.Ok("Listo")).apply {
            acciones = listOf(
                AccionPropuesta.MarcarTema("Análisis II", "parcial 2", "integrales"),
                AccionPropuesta.CrearTarea("Química", "Informe", null, Prioridad.MEDIA)
            )
        }
        val vm = vm(repositorio, ia)
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }
        vm.enviar("Ya estudié integrales")
        advanceUntilIdle()
        // Dos acciones: cada una en su burbuja.
        val conversacion = vm.uiState.value.conversacion
        assertEquals(3, conversacion.size)

        vm.confirmarAccion(1)
        advanceUntilIdle()
        assertTrue(repositorio.usuario.value!!.materias.single().eventos.single().temas.first().estudiado)

        vm.confirmarAccion(2)
        advanceUntilIdle()
        assertEquals(EstadoAccion.FALLO, vm.uiState.value.conversacion[2].estadoAccion)
        assertEquals(Mensaje.AVISO_ACCION_IA_SIN_MATERIA, avisos.last().mensaje)
    }

    // ---------- Respuesta en vivo y búsqueda en las notas ----------

    @Test
    fun laRespuestaSeArmaDeAPedazosYUsaLasNotas() = runTest {
        val ia = AsistenteFalso(respuesta = ResultadoIA.Ok("Según tu nota «Guía 7», primero en y."))
        val vm = vm(RepositorioFalso(usuario()), ia)
        vm.enviar("¿Cómo se resuelven las integrales dobles?")
        advanceUntilIdle()
        val respuesta = vm.uiState.value.conversacion.last()
        assertEquals("Según tu nota «Guía 7», primero en y.", respuesta.texto)
        assertFalse(vm.uiState.value.pensando)
        // Se le pasó el contenido de la nota que tiene que ver (y no la otra).
        assertTrue(ia.contextos.last().contains("Teorema de Fubini"))
        assertFalse(ia.contextos.last().contains("Regla de la cadena"))
        assertEquals(listOf("Guía 7: integrales"), respuesta.fuentes)
    }

    @Test
    fun lasNotasQueTienenQueVerVanPrimero() {
        val notas = listOf(
            NotaParaBuscar("Derivadas", "Análisis", "Regla de la cadena."),
            NotaParaBuscar("Integrales", "Análisis", "Integrales dobles e integrales triples."),
            NotaParaBuscar("Repaso", "Análisis", "Un poco de integrales.")
        )
        assertEquals(listOf("Integrales", "Repaso"), notasRelevantes("explicame integrales dobles", notas).map { it.titulo })
        // Palabras que no dicen nada: no se busca.
        assertTrue(notasRelevantes("hola, cómo estás?", notas).isEmpty())
    }

    @Test
    fun elArchivoSigueEnLaCharlaAunqueSeaLarga() {
        val pdf = MensajeIA(true, "Leé este PDF", adjuntos = listOf(AdjuntoIA("application/pdf", byteArrayOf(1), "apunte.pdf")))
        val charla = listOf(pdf) + (1..30).map { MensajeIA(deUsuario = it % 2 == 0, texto = "m$it") }
        val enviados = contextoParaEnviar(charla)
        assertTrue(enviados.first() === pdf)
        assertEquals("m30", enviados.last().texto)
    }

    // ---------- PDF ----------

    @Test
    fun unPdfVaComoAdjuntoYUnoMuyGrandeAvisa() = runTest {
        val ia = AsistenteFalso()
        val vm = vm(RepositorioFalso(usuario()), ia, leerArchivo = { uri -> if (uri.contains("grande")) ByteArray(TAMANO_MAXIMO_PDF + 1) else byteArrayOf(37, 80, 68, 70) })
        vm.preguntarPdf("content://grande.pdf", "grande.pdf", null, "PDF: grande.pdf")
        advanceUntilIdle()
        assertEquals(ErrorIA.ARCHIVO_GRANDE, vm.uiState.value.error)
        assertTrue(vm.uiState.value.conversacion.isEmpty())

        vm.preguntarPdf("content://guia.pdf", "guia.pdf", "Análisis II", "PDF: guia.pdf")
        advanceUntilIdle()
        val enviado = ia.recibidas.single().single()
        assertEquals("application/pdf", enviado.adjuntos.single().tipoMime)
        assertTrue(enviado.texto.contains("guia.pdf"))
    }

    // ---------- Charlas guardadas ----------

    @Test
    fun laCharlaSeGuardaYSePuedeRetomar() = runTest {
        val historial = HistorialFalso()
        val vm = vm(RepositorioFalso(usuario()), AsistenteFalso(respuesta = ResultadoIA.Ok("Respuesta **uno**")), historial)
        vm.resumir(vm.uiState.value.notas.first { it.nota.id == 5 }, "Resumir: Guía 7")
        advanceUntilIdle()
        val guardada = historial.charlas.value.single()
        assertEquals("Resumir: Guía 7", guardada.titulo)
        assertEquals("Análisis II", guardada.materia)
        assertEquals(2, guardada.mensajes.size)
        // La misma charla se va actualizando (no se duplica).
        vm.enviar("Otra pregunta")
        advanceUntilIdle()
        assertEquals(4, historial.charlas.value.single().mensajes.size)

        vm.nuevaConversacion()
        assertTrue(vm.uiState.value.conversacion.isEmpty())
        vm.abrirConversacion(vm.uiState.value.historial.single())
        assertEquals(4, vm.uiState.value.conversacion.size)
        assertEquals(guardada.id, vm.uiState.value.conversacionId)

        vm.borrarConversacion(guardada.id)
        advanceUntilIdle()
        assertTrue(historial.charlas.value.isEmpty())
        assertTrue(vm.uiState.value.conversacion.isEmpty())
    }

    // ---------- Simulacro con memoria ----------

    @Test
    fun terminarElSimulacroGuardaLaNotaYLasTarjetas() = runTest {
        val repositorio = RepositorioFalso(usuario())
        val historial = HistorialFalso()
        val ia = AsistenteFalso(respuesta = ResultadoIA.Ok("Pregunta 1: ¿qué dice Fubini?")).apply {
            evaluacion = ResultadoIA.Ok(EvaluacionSimulacro(6.3, " Bien en lo básico. ", listOf("Fubini", "Fubini", " "), listOf(TarjetaSugerida("¿Fubini?", "Permite cambiar el orden"))))
        }
        val vm = vm(repositorio, ia, historial)
        vm.simulacro(vm.uiState.value.notas.filter { it.nota.id == 5 }, "Simulacro: Guía 7")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.simulacroEnCurso)
        vm.enviar("Que da lo mismo el orden")
        advanceUntilIdle()

        vm.terminarSimulacro()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.simulacroEnCurso)
        val resultado = vm.uiState.value.resultadoSimulacro!!
        assertEquals(6.5, resultado.nota, 0.0)
        assertEquals(listOf("Fubini"), resultado.temasFlojos)
        // Se guardó el resultado y la tarjeta quedó para el repaso.
        assertEquals(listOf("Fubini"), historial.resultados.value.single().temasFlojos)
        assertEquals("¿Fubini?", repositorio.usuario.value!!.materias.single().tarjetas.single().pregunta)
        // La evaluación usa la charla del simulacro (con la respuesta del estudiante).
        assertTrue(ia.pedidosEvaluacion.single().contains("Que da lo mismo el orden"))

        // En las preguntas siguientes la IA sabe qué le costó.
        vm.cerrarResultadoSimulacro()
        vm.enviar("¿Qué repaso?")
        advanceUntilIdle()
        assertTrue(ia.contextos.last().contains("le costó: Fubini"))
    }

    @Test
    fun laEvaluacionSeAcota() {
        val limpia = limpiarEvaluacion(EvaluacionSimulacro(14.0, "x", (1..9).map { "Tema $it" }, emptyList()))
        assertEquals(10.0, limpia.nota, 0.0)
        assertEquals(5, limpia.temasFlojos.size)
    }

    // ---------- Reacomodar el plan ----------

    private val sesion = Evento(30, "Estudiar: integrales", TipoEvento.BLOQUE_ESTUDIO, hoy.minusDays(1), LocalTime.of(18, 0), LocalTime.of(19, 30))
    private val clase = Evento(31, "Teórico", TipoEvento.CLASE, LocalDate.of(2026, 3, 6), LocalTime.of(18, 0), LocalTime.of(20, 0), semanal = true) // viernes

    @Test
    fun lasSesionesQuePasaronSeMuevenAUnHuecoLibre() {
        val u = usuario(materia.copy(eventos = listOf(parcial, sesion, clase)))
        val atrasadas = sesionesAtrasadas(u, hoy, "Estudiar", null)
        assertEquals(listOf(30), atrasadas.map { it.evento.id })
        // Ya revisadas: no se vuelve a preguntar.
        assertTrue(sesionesAtrasadas(u, hoy, "Estudiar", hoy.minusDays(1)).isEmpty())

        val movida = reacomodarSesiones(atrasadas, u, hoy).single().evento
        // Mañana (viernes) a las 18 hay clase: pasa al primer horario libre de ese día.
        assertEquals(hoy.plusDays(1), movida.fecha)
        assertEquals(LocalTime.of(8, 0), movida.horaInicio)
        assertEquals(LocalTime.of(9, 30), movida.horaFin)
    }

    @Test
    fun siElExamenYaPasoNoSePregunta() {
        val pasado = parcial.copy(fecha = hoy)
        val u = usuario(materia.copy(eventos = listOf(pasado, sesion)))
        assertTrue(sesionesAtrasadas(u, hoy, "Estudiar", null).isEmpty())
    }

    @Test
    fun reacomodarDesdeElAsistente() = runTest {
        val repositorio = RepositorioFalso(usuario(materia.copy(eventos = listOf(parcial, sesion))))
        val vm = vm(repositorio)
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }
        advanceUntilIdle()
        val atrasadas = vm.uiState.value.sesionesAtrasadas
        assertEquals(1, atrasadas.size)

        vm.reacomodarPlan(atrasadas)
        advanceUntilIdle()
        val movida = repositorio.usuario.value!!.materias.single().eventos.first { it.id == 30 }
        assertEquals(hoy.plusDays(1), movida.fecha)
        assertEquals(LocalTime.of(18, 0), movida.horaInicio)
        assertTrue(vm.uiState.value.sesionesAtrasadas.isEmpty())
        assertEquals(Mensaje.AVISO_PLAN_REACOMODADO, avisos.last().mensaje)
        assertEquals(listOf(1, 0), avisos.last().argumentos)
    }
}
