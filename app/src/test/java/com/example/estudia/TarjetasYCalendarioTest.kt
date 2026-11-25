package com.example.estudia

import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Tarjeta
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.viewmodel.Aviso
import com.example.estudia.viewmodel.CalendarioViewModel
import com.example.estudia.viewmodel.MateriasViewModel
import com.example.estudia.viewmodel.Mensaje
import com.example.estudia.viewmodel.RepasoViewModel
import com.example.estudia.viewmodel.TODAS_LAS_MATERIAS
import com.example.estudia.viewmodel.tarjetasParaRepasar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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

// Tests de la versión 8: tarjetas de estudio con repetición espaciada y el
// paso de los eventos al calendario del teléfono.
@OptIn(ExperimentalCoroutinesApi::class)
class TarjetasYCalendarioTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    private val hoy = LocalDate.now()

    private fun usuario(vararg materias: Materia) =
        Usuario(id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana", materias = materias.toList())

    private fun TestScope.observar(flujo: Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flujo.collect {} }
    }

    // ---------- Repetición espaciada ----------

    @Test
    fun laTarjetaSubeDeCajaSiLaSabiaYVuelveALaUnoSiNo() {
        val tarjeta = Tarjeta(1, "¿Qué es una onda?", "Una perturbación", caja = 2, proximoRepaso = hoy)
        val sabia = tarjeta.responder(sabia = true, hoy)
        assertEquals(3, sabia.caja)
        assertEquals(hoy.plusDays(4), sabia.proximoRepaso)
        assertFalse(sabia.tocaRepasar(hoy))

        val noSabia = tarjeta.responder(sabia = false, hoy)
        assertEquals(1, noSabia.caja)
        assertEquals(hoy.plusDays(1), noSabia.proximoRepaso)

        // En la última caja se queda ahí, cada 16 días.
        val ultima = tarjeta.copy(caja = 5).responder(sabia = true, hoy)
        assertEquals(5, ultima.caja)
        assertEquals(hoy.plusDays(16), ultima.proximoRepaso)
    }

    @Test
    fun soloSeRepasanLasQueTocanHoyDeLasMateriasActivas() {
        val fisica = Materia(
            1, "Física", 0,
            tarjetas = listOf(
                Tarjeta(1, "A", "a", proximoRepaso = hoy.minusDays(2)),
                Tarjeta(2, "B", "b", proximoRepaso = hoy.plusDays(3)),
                Tarjeta(3, "C", "c", proximoRepaso = hoy)
            )
        )
        val archivada = Materia(2, "Química", 0, archivada = true, tarjetas = listOf(Tarjeta(4, "D", "d", proximoRepaso = hoy)))
        val todas = tarjetasParaRepasar(usuario(fisica, archivada), TODAS_LAS_MATERIAS, hoy)
        // La más atrasada primero; la de dentro de 3 días y la archivada, no.
        assertEquals(listOf(1, 3), todas.map { it.tarjeta.id })
        // Pidiendo una materia puntual, sí aparece aunque esté archivada.
        assertEquals(listOf(4), tarjetasParaRepasar(usuario(fisica, archivada), 2, hoy).map { it.tarjeta.id })
    }

    @Test
    fun alResponderLaTarjetaSaleDelRepasoDeHoy() = runTest {
        val materia = Materia(1, "Física", 0, tarjetas = listOf(Tarjeta(1, "A", "a", proximoRepaso = hoy), Tarjeta(2, "B", "b", proximoRepaso = hoy)))
        val repositorio = RepositorioFalso(usuario(materia))
        val vm = RepasoViewModel(repositorio, 1) { hoy }
        observar(vm.uiState)

        assertEquals(2, vm.uiState.value.pendientes.size)
        vm.verRespuesta()
        assertTrue(vm.uiState.value.mostrandoRespuesta)
        vm.responder(sabia = true)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.pendientes.size)
        assertFalse(vm.uiState.value.mostrandoRespuesta)
        vm.responder(sabia = false)
        advanceUntilIdle()
        assertNull(vm.uiState.value.actual)
        assertEquals(2, vm.uiState.value.repasadas)
    }

    @Test
    fun unaTarjetaNecesitaPreguntaYRespuesta() = runTest {
        val repositorio = RepositorioFalso(usuario(Materia(1, "Física", 0)))
        val vm = MateriasViewModel(repositorio, AvisosFalso())
        observar(vm.uiState)

        assertFalse(vm.guardarTarjeta(1, null, "¿Qué es?", " "))
        assertTrue(vm.guardarTarjeta(1, null, "¿Qué es una onda?", "Una perturbación"))
        advanceUntilIdle()
        val tarjeta = vm.uiState.value.todas.single().tarjetas.single()
        assertEquals(1, tarjeta.caja)
        assertTrue(tarjeta.tocaRepasar(hoy))
    }

    // ---------- Calendario del teléfono ----------

    @Test
    fun pasarAlCalendarioAvisaCuantosEventosPaso() = runTest {
        val eventos = listOf(
            Evento(1, "Teórico", TipoEvento.CLASE, hoy, LocalTime.of(9, 0), LocalTime.of(11, 0), semanal = true),
            Evento(2, "Parcial", TipoEvento.EXAMEN, hoy, LocalTime.of(18, 0), LocalTime.of(20, 0)),
            // Los bloques de estudio no se pasan.
            Evento(3, "Estudio", TipoEvento.BLOQUE_ESTUDIO, hoy, LocalTime.of(14, 0), LocalTime.of(15, 0))
        )
        val repositorio = RepositorioFalso(usuario(Materia(1, "Física", 0, eventos = eventos)))
        val vm = CalendarioViewModel(repositorio, AvisosFalso())
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }

        vm.sincronizarConTelefono("Estudia+")
        advanceUntilIdle()
        assertEquals(Mensaje.AVISO_CALENDARIO_SINCRONIZADO, avisos.single().mensaje)
        assertEquals(listOf(2), avisos.single().argumentos)
    }
}
