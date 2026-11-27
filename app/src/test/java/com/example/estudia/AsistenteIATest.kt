package com.example.estudia

import com.example.estudia.datos.AsistenteIA
import com.example.estudia.datos.ClavesIA
import com.example.estudia.modelo.ErrorIA
import com.example.estudia.modelo.LARGO_MAXIMO_NOTA
import com.example.estudia.modelo.MENSAJES_DE_CONTEXTO
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MensajeIA
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.ResultadoIA
import com.example.estudia.modelo.TarjetaSugerida
import com.example.estudia.modelo.SesionPlanTexto
import com.example.estudia.modelo.RamaIA
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.contextoParaEnviar
import com.example.estudia.modelo.contextoDelEstudiante
import com.example.estudia.modelo.instruccionConDatos
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.MesaFinal
import java.time.LocalTime
import com.example.estudia.modelo.limpiarRespuesta
import com.example.estudia.modelo.limpiarTarjetas
import com.example.estudia.modelo.pedidoResumir
import com.example.estudia.viewmodel.Aviso
import com.example.estudia.viewmodel.AsistenteViewModel
import com.example.estudia.viewmodel.Mensaje
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

// IA falsa: responde lo que se le indique, sin internet.
class AsistenteFalso(
    var respuesta: ResultadoIA<String> = ResultadoIA.Ok("Respuesta"),
    var tarjetas: ResultadoIA<List<TarjetaSugerida>> = ResultadoIA.Ok(emptyList())
) : AsistenteIA {
    val recibidas = mutableListOf<List<MensajeIA>>()
    val claves = mutableListOf<String>()
    // Si no es null, solo esa clave es válida.
    var claveValida: String? = null
    val contextos = mutableListOf<String>()
    override suspend fun responder(clave: String, conversacion: List<MensajeIA>, contexto: String): ResultadoIA<String> {
        contextos += contexto
        recibidas += conversacion
        claves += clave
        if (claveValida != null && clave != claveValida) return ResultadoIA.Falla(ErrorIA.CLAVE_INVALIDA)
        return respuesta
    }
    // La charla en vivo: manda la respuesta en dos pedazos (y las acciones que
    // se le indiquen), o el error.
    var acciones: List<com.example.estudia.modelo.AccionPropuesta> = emptyList()
    override fun conversar(clave: String, conversacion: List<MensajeIA>, contexto: String): kotlinx.coroutines.flow.Flow<com.example.estudia.modelo.PiezaRespuesta> =
        kotlinx.coroutines.flow.flow {
            contextos += contexto
            recibidas += conversacion
            claves += clave
            if (claveValida != null && clave != claveValida) {
                emit(com.example.estudia.modelo.PiezaRespuesta.Error(ErrorIA.CLAVE_INVALIDA))
                return@flow
            }
            when (val r = respuesta) {
                is ResultadoIA.Ok -> {
                    val mitad = r.valor.length / 2
                    emit(com.example.estudia.modelo.PiezaRespuesta.Texto(r.valor.take(mitad)))
                    emit(com.example.estudia.modelo.PiezaRespuesta.Texto(r.valor.drop(mitad)))
                    acciones.forEach { emit(com.example.estudia.modelo.PiezaRespuesta.Accion(it)) }
                }
                is ResultadoIA.Falla -> emit(com.example.estudia.modelo.PiezaRespuesta.Error(r.error))
            }
        }
    override suspend fun crearTarjetas(clave: String, pedido: String) = tarjetas
    var plan: ResultadoIA<List<SesionPlanTexto>> = ResultadoIA.Ok(emptyList())
    var mapa: ResultadoIA<Pair<String, List<RamaIA>>> = ResultadoIA.Falla(ErrorIA.OTRO)
    var evaluacion: ResultadoIA<com.example.estudia.modelo.EvaluacionSimulacro> = ResultadoIA.Falla(ErrorIA.OTRO)
    val pedidosEvaluacion = mutableListOf<String>()
    override suspend fun evaluarSimulacro(clave: String, pedido: String): ResultadoIA<com.example.estudia.modelo.EvaluacionSimulacro> {
        pedidosEvaluacion += pedido
        return evaluacion
    }
    override suspend fun planEstudio(clave: String, pedido: String, contexto: String) = plan
    override suspend fun mapaMental(clave: String, pedido: String) = mapa
    var horario: ResultadoIA<List<com.example.estudia.modelo.ClaseHorarioTexto>> = ResultadoIA.Ok(emptyList())
    val pedidosHorario = mutableListOf<String>()
    override suspend fun leerHorario(clave: String, jpeg: ByteArray, pedido: String): ResultadoIA<List<com.example.estudia.modelo.ClaseHorarioTexto>> {
        pedidosHorario += pedido
        return horario
    }
}

// Claves en memoria (en el teléfono van cifradas).
class ClavesFalsas : ClavesIA {
    val guardadas = MutableStateFlow<Map<Int, String>>(emptyMap())
    override fun clave(usuarioId: Int): Flow<String?> = guardadas.map { it[usuarioId] }
    override suspend fun guardar(usuarioId: Int, clave: String?) {
        guardadas.value = if (clave == null) guardadas.value - usuarioId else guardadas.value + (usuarioId to clave)
    }
}

// Tests del Asistente IA: cómo se arman los pedidos y la pantalla (charla,
// errores, reintentar, tarjetas y guardar como nota) con una IA falsa.
@OptIn(ExperimentalCoroutinesApi::class)
class AsistenteIATest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    private val hoy = LocalDate.of(2026, 11, 26)
    private val nota = Nota(5, "Guía 7", "Integrales dobles: se integra primero en y.", hoy)
    private val materia = Materia(3, "Análisis II", 0xFF6C5CE7, notas = listOf(nota))
    private fun usuario() = Usuario(id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana", materias = listOf(materia))

    // ---------- Pedidos ----------

    @Test
    fun elContextoEmpiezaPorElUsuarioYNoCreceSinFin() {
        val charla = (1..30).map { MensajeIA(deUsuario = it % 2 == 1, texto = "m$it") }
        val enviados = contextoParaEnviar(charla)
        assertTrue(enviados.size <= MENSAJES_DE_CONTEXTO)
        assertTrue(enviados.first().deUsuario)
        assertEquals("m30", enviados.last().texto)
    }

    @Test
    fun lasNotasLargasSeRecortan() {
        val pedido = pedidoResumir("Larga", "a".repeat(LARGO_MAXIMO_NOTA + 500))
        assertTrue(pedido.contains("[…nota recortada]"))
        assertTrue(pedido.length < LARGO_MAXIMO_NOTA + 600)
    }

    @Test
    fun laRespuestaSeOrdenaYElTextoPlanoNoTieneMarkdown() {
        // La respuesta conserva el formato (se dibuja en la pantalla)...
        assertEquals("## Título\n\n* Una idea **clave**", limpiarRespuesta("## Título\r\n\n\n\n* Una idea **clave**  "))
        // ...y al copiarla o guardarla como nota queda en texto plano.
        assertEquals("Título\n• Una idea clave: x²", com.example.estudia.modelo.textoPlano("## Título\n* Una idea **clave**: \$x^2\$"))
    }

    @Test
    fun lasTarjetasVaciasORepetidasSeSacan() {
        val tarjetas = listOf(TarjetaSugerida(" ¿Qué es? ", " Algo "), TarjetaSugerida("¿qué es?", "Otra"), TarjetaSugerida("", "x"))
        assertEquals(listOf(TarjetaSugerida("¿Qué es?", "Algo")), limpiarTarjetas(tarjetas))
    }

    // ---------- Pantalla ----------

    @Test
    fun charlaYResumenDeUnaNota() = runTest {
        val ia = AsistenteFalso()
        val vm = AsistenteViewModel(RepositorioFalso(usuario()), ia, ClavesFalsas(), "clave-app")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        val elegida = vm.uiState.value.notas.single()
        vm.resumir(elegida, "Resumir: Guía 7")
        advanceUntilIdle()
        val charla = vm.uiState.value.conversacion
        assertEquals(listOf("Resumir: Guía 7", "Respuesta"), charla.map { it.mostrado })
        // A la IA le llega la nota entera, no la etiqueta.
        assertTrue(ia.recibidas.single().single().texto.contains("Integrales dobles"))
        assertFalse(vm.uiState.value.pensando)
    }

    @Test
    fun siFallaSePuedeReintentar() = runTest {
        val ia = AsistenteFalso(respuesta = ResultadoIA.Falla(ErrorIA.LIMITE))
        val vm = AsistenteViewModel(RepositorioFalso(usuario()), ia, ClavesFalsas(), "clave-app")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.enviar("¿Qué es un límite?")
        advanceUntilIdle()
        assertEquals(ErrorIA.LIMITE, vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.conversacion.size)

        ia.respuesta = ResultadoIA.Ok("Es a dónde se acerca la función.")
        vm.reintentar()
        advanceUntilIdle()
        assertNull(vm.uiState.value.error)
        assertEquals(2, vm.uiState.value.conversacion.size)
        // No se repite la pregunta al reintentar.
        assertEquals(1, ia.recibidas.last().count { it.deUsuario })
    }

    @Test
    fun lasTarjetasElegidasSeGuardanEnLaMateriaDeLaNota() = runTest {
        val repositorio = RepositorioFalso(usuario())
        val propuestas = listOf(TarjetaSugerida("¿Qué se integra primero?", "y"), TarjetaSugerida("¿Y después?", "x"))
        val vm = AsistenteViewModel(repositorio, AsistenteFalso(tarjetas = ResultadoIA.Ok(propuestas)), ClavesFalsas(), "clave-app")
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.pedirTarjetas(vm.uiState.value.notas.single())
        advanceUntilIdle()
        assertEquals(propuestas, vm.uiState.value.tarjetasPropuestas!!.tarjetas)

        vm.guardarTarjetas(propuestas.take(1))
        advanceUntilIdle()
        assertNull(vm.uiState.value.tarjetasPropuestas)
        assertEquals(listOf("¿Qué se integra primero?"), repositorio.usuario.value!!.materias.single().tarjetas.map { it.pregunta })
        assertEquals(Mensaje.AVISO_TARJETAS_IA_GUARDADAS, avisos.single().mensaje)
    }

    @Test
    fun unaRespuestaSeGuardaComoNota() = runTest {
        val repositorio = RepositorioFalso(usuario())
        val vm = AsistenteViewModel(repositorio, AsistenteFalso(), ClavesFalsas(), "clave-app")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.guardarComoNota(3, " Resumen IA ", "Ideas clave…")
        advanceUntilIdle()
        assertTrue(repositorio.usuario.value!!.materias.single().notas.any { it.titulo == "Resumen IA" && it.contenido == "Ideas clave…" })
    }

    // ---------- Clave de cada usuario ----------

    @Test
    fun cadaUsuarioUsaSuClaveYSiNoTieneLaDeLaApp() = runTest {
        val ia = AsistenteFalso()
        val claves = ClavesFalsas()
        val vm = AsistenteViewModel(RepositorioFalso(usuario()), ia, claves, "clave-app")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.enviar("Hola")
        advanceUntilIdle()
        assertEquals("clave-app", ia.claves.last())
        assertFalse(vm.uiState.value.clavePropia)

        claves.guardar(1, "clave-de-ana")
        vm.enviar("Otra")
        advanceUntilIdle()
        assertEquals("clave-de-ana", ia.claves.last())
        assertTrue(vm.uiState.value.clavePropia)
        // La de otro usuario no se usa.
        claves.guardar(2, "clave-de-otro")
        vm.enviar("Y otra")
        advanceUntilIdle()
        assertEquals("clave-de-ana", ia.claves.last())
    }

    @Test
    fun sinNingunaClaveNoEstaConfigurado() = runTest {
        val vm = AsistenteViewModel(RepositorioFalso(usuario()), AsistenteFalso(), ClavesFalsas(), "")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertFalse(vm.uiState.value.configurado)
    }

    @Test
    fun conectarPruebaLaClaveAntesDeGuardarla() = runTest {
        val ia = AsistenteFalso().apply { claveValida = "buena" }
        val claves = ClavesFalsas()
        val vm = AsistenteViewModel(RepositorioFalso(usuario()), ia, claves, "")
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        var conectada = false
        vm.conectarClave(" mala ") { conectada = true }
        advanceUntilIdle()
        assertFalse(conectada)
        assertEquals(ErrorIA.CLAVE_INVALIDA, vm.uiState.value.errorClave)
        assertTrue(claves.guardadas.value.isEmpty())

        vm.conectarClave(" buena ") { conectada = true }
        advanceUntilIdle()
        assertTrue(conectada)
        assertEquals(mapOf(1 to "buena"), claves.guardadas.value)
        assertTrue(vm.uiState.value.configurado)
        assertNull(vm.uiState.value.errorClave)
        assertEquals(Mensaje.AVISO_IA_CONECTADA, avisos.last().mensaje)
        // La prueba no queda en la charla.
        assertTrue(vm.uiState.value.conversacion.isEmpty())

        vm.quitarClave()
        advanceUntilIdle()
        assertTrue(claves.guardadas.value.isEmpty())
        assertFalse(vm.uiState.value.configurado)
    }

    // ---------- Datos del estudiante ----------

    @Test
    fun elResumenTieneClasesAgendaTareasYFinales() {
        // 26/11/2026 es jueves.
        val clase = Evento(1, "Teórico", TipoEvento.CLASE, LocalDate.of(2026, 3, 5), LocalTime.of(18, 0), LocalTime.of(20, 0), semanal = true, lugar = "Aula 5")
        val parcial = Evento(2, "Parcial 2", TipoEvento.EXAMEN, hoy.plusDays(4), LocalTime.of(9, 0), LocalTime.of(11, 0))
        val viejo = Evento(3, "Parcial 1", TipoEvento.EXAMEN, hoy.minusDays(30), LocalTime.of(9, 0), LocalTime.of(11, 0))
        val m = materia.copy(
            eventos = listOf(clase, parcial, viejo),
            tareas = listOf(Tarea(1, "TP integrales", fechaLimite = hoy.plusDays(2)), Tarea(2, "Hecha", completada = true))
        )
        val u = usuario().copy(materias = listOf(m), finales = listOf(MesaFinal(1, "Física I", hoy.plusDays(10), cierreInscripcion = hoy.plusDays(5))))
        val texto = contextoDelEstudiante(u, hoy)

        assertTrue(texto.contains("Hoy es jueves 26/11/2026"))
        assertTrue(texto.contains("Teórico los jueves de 18:00 a 20:00 (Aula 5)"))
        // La clase aparece hoy y el jueves que viene; el parcial, el lunes.
        assertTrue(texto.contains("jueves 26/11 (hoy): 18:00-20:00 Clase de Análisis II"))
        assertTrue(texto.contains("jueves 3/12"))
        assertTrue(texto.contains("lunes 30/11: 09:00-11:00 Examen de Análisis II: Parcial 2"))
        assertFalse(texto.contains("Parcial 1"))
        assertTrue(texto.contains("TP integrales (Análisis II), vence el sábado 28/11"))
        // Las hechas no van como pendientes, pero sí aparte (para poder desmarcarlas).
        assertFalse(texto.contains("Hecha (Análisis II)"))
        assertTrue(texto.contains("Tareas ya hechas: Hecha"))
        assertTrue(texto.contains("Física I el domingo 6/12 (la inscripción cierra el martes 1/12)"))
        // De las notas va solo el título.
        assertTrue(texto.contains("Guía 7"))
        assertFalse(texto.contains("Integrales dobles"))
    }

    @Test
    fun cadaPreguntaLlevaLosDatosDelEstudiante() = runTest {
        val ia = AsistenteFalso()
        val vm = AsistenteViewModel(RepositorioFalso(usuario()), ia, ClavesFalsas(), "clave-app", hoy = { hoy })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.enviar("¿Qué clases tengo esta semana?")
        advanceUntilIdle()
        assertTrue(ia.contextos.single().contains("Análisis II"))
        assertTrue(instruccionConDatos(ia.contextos.single()).contains("no inventes"))
    }

    // ---------- Herramientas que guardan algo ----------

    @Test
    fun elPlanElegidoSeGuardaComoBloquesConAvisos() = runTest {
        val parcial = Evento(9, "Parcial 2", TipoEvento.EXAMEN, hoy.plusDays(6), LocalTime.of(9, 0), LocalTime.of(11, 0))
        val repositorio = RepositorioFalso(usuario().copy(materias = listOf(materia.copy(eventos = listOf(parcial)))))
        val ia = AsistenteFalso().apply {
            plan = ResultadoIA.Ok(listOf(SesionPlanTexto("2026-11-27", "17:00", 60, "Lectura"), SesionPlanTexto("2026-11-28", "10:00", 90, "Ejercicios")))
        }
        val avisos = AvisosFalso()
        val vm = AsistenteViewModel(repositorio, ia, ClavesFalsas(), "clave-app", hoy = { hoy }, avisosSistema = avisos)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.pedirPlan(vm.uiState.value.examenes.single())
        advanceUntilIdle()
        val plan = vm.uiState.value.planPropuesto!!
        assertEquals(2, plan.sesiones.size)

        vm.guardarPlan(plan.sesiones.take(1), "Estudiar")
        advanceUntilIdle()
        assertNull(vm.uiState.value.planPropuesto)
        val bloques = repositorio.usuario.value!!.materias.single().eventos.filter { it.tipo == TipoEvento.BLOQUE_ESTUDIO }
        assertEquals(listOf("Estudiar: Lectura"), bloques.map { it.titulo })
        assertEquals(bloques.map { it.id }, avisos.eventosProgramados)
    }

    @Test
    fun elMapaDeUnaNotaSeGuardaYSeAbre() = runTest {
        val repositorio = RepositorioFalso(usuario())
        val ia = AsistenteFalso().apply { mapa = ResultadoIA.Ok("Integrales" to listOf(RamaIA("Dobles", listOf("Fubini")))) }
        val vm = AsistenteViewModel(repositorio, ia, ClavesFalsas(), "clave-app", hoy = { hoy })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.crearMapa(vm.uiState.value.notas.single())
        advanceUntilIdle()
        val guardado = repositorio.usuario.value!!.mapas.single()
        assertEquals("Análisis II", guardado.materia)
        assertEquals(3, guardado.nodos.size)
        assertEquals(guardado.id, vm.uiState.value.mapaCreado)
        vm.mapaAbierto()
        assertNull(vm.uiState.value.mapaCreado)
    }

    @Test
    fun lasFotosDeUnApunteQuedanEnLaCharla() = runTest {
        val ia = AsistenteFalso(respuesta = ResultadoIA.Ok("Texto del apunte"))
        val vm = AsistenteViewModel(RepositorioFalso(usuario()), ia, ClavesFalsas(), "clave-app", hoy = { hoy }, leerFoto = { byteArrayOf(1, 2, 3) })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.leerApunte(listOf("content://hoja1", "content://hoja2"), "Foto de un apunte (2)")
        advanceUntilIdle()
        assertEquals(listOf("Foto de un apunte (2)", "Texto del apunte"), vm.uiState.value.conversacion.map { it.mostrado })
        // Las dos hojas van juntas, en un mismo mensaje.
        val enviado = ia.recibidas.single().single()
        assertEquals(2, enviado.adjuntos.size)
        assertTrue(enviado.texto.contains("2 fotos"))

        // Si la foto no se puede leer, queda el error para reintentar.
        val sinFoto = AsistenteViewModel(RepositorioFalso(usuario()), AsistenteFalso(), ClavesFalsas(), "clave-app", hoy = { hoy })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { sinFoto.uiState.collect {} }
        sinFoto.leerApunte(listOf("content://rota"), "Foto")
        advanceUntilIdle()
        assertEquals(ErrorIA.OTRO, sinFoto.uiState.value.error)
    }

    @Test
    fun elSimulacroMandaLasNotasElegidas() = runTest {
        val ia = AsistenteFalso()
        val vm = AsistenteViewModel(RepositorioFalso(usuario()), ia, ClavesFalsas(), "clave-app", hoy = { hoy })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.simulacro(vm.uiState.value.notas, "Simulacro: Guía 7")
        advanceUntilIdle()
        assertEquals("Simulacro: Guía 7", vm.uiState.value.conversacion.first().mostrado)
        assertTrue(ia.recibidas.single().single().texto.contains("Integrales dobles"))
    }
}
