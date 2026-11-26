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
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.contextoParaEnviar
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
private class AsistenteFalso(
    var respuesta: ResultadoIA<String> = ResultadoIA.Ok("Respuesta"),
    var tarjetas: ResultadoIA<List<TarjetaSugerida>> = ResultadoIA.Ok(emptyList())
) : AsistenteIA {
    val recibidas = mutableListOf<List<MensajeIA>>()
    val claves = mutableListOf<String>()
    // Si no es null, solo esa clave es válida.
    var claveValida: String? = null
    override suspend fun responder(clave: String, conversacion: List<MensajeIA>): ResultadoIA<String> {
        recibidas += conversacion
        claves += clave
        if (claveValida != null && clave != claveValida) return ResultadoIA.Falla(ErrorIA.CLAVE_INVALIDA)
        return respuesta
    }
    override suspend fun crearTarjetas(clave: String, pedido: String) = tarjetas
}

// Claves en memoria (en el teléfono van cifradas).
private class ClavesFalsas : ClavesIA {
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
    fun seLimpiaElMarkdownDeLaRespuesta() {
        assertEquals("Título\n• Una idea clave", limpiarRespuesta("## Título\n* Una idea **clave**"))
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
}
