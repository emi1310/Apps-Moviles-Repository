package com.example.estudia

import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.SesionEstudio
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.viewmodel.Aviso
import com.example.estudia.viewmodel.CalendarioViewModel
import com.example.estudia.viewmodel.TemporizadorViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

// Tests de las clases que se repiten cada semana y del temporizador de estudio.
@OptIn(ExperimentalCoroutinesApi::class)
class TemporizadorYRepeticionTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    private val minuto = 60_000L
    private val lunes = LocalDate.of(2026, 11, 2)
    private val fisica = Materia(id = 1, nombre = "Física", color = 0)
    private val avisosSistema = AvisosFalso()
    private val repositorio = RepositorioFalso(
        Usuario(id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana", materias = listOf(fisica))
    )

    private fun TestScope.observar(flujo: Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flujo.collect {} }
    }

    private fun TestScope.recolectarAvisos(flujo: Flow<Aviso>): List<Aviso> {
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flujo.collect { avisos += it } }
        return avisos
    }

    // ---------- Clases que se repiten ----------

    @Test
    fun claseSemanalOcurreElMismoDiaHastaLaUltimaClase() {
        val clase = Evento(
            1, "Teórico", TipoEvento.CLASE, lunes, LocalTime.of(9, 0), LocalTime.of(11, 0),
            semanal = true, repetirHasta = lunes.plusWeeks(2)
        )
        assertTrue(clase.ocurreEl(lunes))
        assertTrue(clase.ocurreEl(lunes.plusWeeks(2)))
        assertFalse(clase.ocurreEl(lunes.plusDays(1)))      // martes
        assertFalse(clase.ocurreEl(lunes.minusWeeks(1)))    // antes de la primera
        assertFalse(clase.ocurreEl(lunes.plusWeeks(3)))     // después de la última

        val sinFin = clase.copy(repetirHasta = null)
        assertTrue(sinFin.ocurreEl(lunes.plusWeeks(40)))

        val unaVez = clase.copy(semanal = false)
        assertFalse(unaVez.ocurreEl(lunes.plusWeeks(1)))
    }

    @Test
    fun guardarClaseSemanalLaMuestraTodasLasSemanas() = runTest {
        val vm = CalendarioViewModel(repositorio, avisosSistema)
        observar(vm.uiState)
        val error = vm.guardarEvento(
            1, null, "Teórico", TipoEvento.CLASE, lunes, LocalTime.of(9, 0), LocalTime.of(11, 0),
            semanal = true
        )
        assertNull(error)

        vm.seleccionar(lunes.plusWeeks(3))
        assertEquals(listOf("Teórico"), vm.uiState.value.eventosDelDia.map { it.evento.titulo })
        // El mes muestra el puntito en todos los lunes desde la primera clase.
        val lunesDelMes = vm.uiState.value.coloresPorDia.keys.sorted()
        assertEquals(listOf(2, 9, 16, 23, 30), lunesDelMes.map { it.dayOfMonth })
    }

    @Test
    fun ultimaClaseAntesDeLaPrimeraDaError() = runTest {
        val vm = CalendarioViewModel(repositorio, avisosSistema)
        observar(vm.uiState)
        val error = vm.guardarEvento(
            1, null, "Teórico", TipoEvento.CLASE, lunes, LocalTime.of(9, 0), LocalTime.of(11, 0),
            semanal = true, repetirHasta = lunes.minusDays(1)
        )
        assertEquals(R.string.error_repetir_hasta, error)
    }

    @Test
    fun soloLasClasesSePuedenRepetir() = runTest {
        val vm = CalendarioViewModel(repositorio, avisosSistema)
        observar(vm.uiState)
        vm.guardarEvento(
            1, null, "Parcial", TipoEvento.EXAMEN, lunes, LocalTime.of(9, 0), LocalTime.of(11, 0),
            semanal = true
        )
        val parcial = repositorio.usuario.value!!.materias[0].eventos.single()
        assertFalse(parcial.semanal)
    }

    @Test
    fun guardarYEliminarEventoActualizaSuRecordatorio() = runTest {
        val vm = CalendarioViewModel(repositorio, avisosSistema)
        observar(vm.uiState)
        vm.guardarEvento(1, null, "Parcial", TipoEvento.EXAMEN, lunes, LocalTime.of(9, 0), LocalTime.of(11, 0))
        val parcial = vm.uiState.value.eventosDelDia.single()
        // Se programa con el id que le dio el repositorio, no con 0.
        assertEquals(listOf(parcial.evento.id), avisosSistema.eventosProgramados)

        vm.eliminarEvento(parcial)
        assertEquals(listOf(parcial.evento.id), avisosSistema.eventosCancelados)
    }

    // ---------- Temporizador ----------

    @Test
    fun sesionCalculaLoQueFaltaYSePuedePausar() {
        val sesion = SesionEstudio.nueva(fisica, minutos = 25, ahora = 0)
        assertEquals(25 * minuto, sesion.restanteMillis(0))
        assertEquals(10, sesion.minutosEstudiados(10 * minuto))

        // En pausa el tiempo no corre: a los 10 min se pausa y se sigue a los 30.
        val pausada = sesion.pausar(10 * minuto)
        assertTrue(pausada.enPausa)
        assertEquals(15 * minuto, pausada.restanteMillis(30 * minuto))
        val reanudada = pausada.reanudar(30 * minuto)
        assertEquals(15 * minuto, reanudada.restanteMillis(30 * minuto))
        assertTrue(reanudada.terminada(45 * minuto))
        assertFalse(reanudada.terminada(44 * minuto))
    }

    @Test
    fun terminarAntesGuardaLoEstudiado() = runTest {
        val vm = TemporizadorViewModel(repositorio, avisosSistema) { repositorio.reloj }
        observar(vm.uiState)
        val avisos = recolectarAvisos(vm.avisos)

        vm.elegirMinutos(45)
        vm.empezar()
        assertEquals(45, vm.uiState.value.sesion?.minutos)
        // La notificación fija se muestra con la sesión nueva...
        assertEquals(45, avisosSistema.sesionEnNotificacion?.minutos)

        repositorio.reloj = 20 * minuto
        vm.terminar()
        assertNull(vm.uiState.value.sesion)
        // ... y se oculta al terminar.
        assertNull(avisosSistema.sesionEnNotificacion)
        assertEquals(R.string.aviso_sesion_guardada, avisos.single().mensaje)
        assertEquals(listOf(20), avisos.single().argumentos)
        // Se guardó como bloque de estudio de la materia.
        val bloque = repositorio.usuario.value!!.materias[0].eventos.single()
        assertEquals(TipoEvento.BLOQUE_ESTUDIO, bloque.tipo)
        assertEquals(20L, bloque.duracionMinutos())
    }

    @Test
    fun pausarYSeguirActualizanLaNotificacion() = runTest {
        val vm = TemporizadorViewModel(repositorio, avisosSistema) { repositorio.reloj }
        observar(vm.uiState)
        vm.empezar()
        repositorio.reloj = 5 * minuto
        vm.pausar()
        assertTrue(avisosSistema.sesionEnNotificacion!!.enPausa)
        vm.reanudar()
        assertFalse(avisosSistema.sesionEnNotificacion!!.enPausa)
        vm.descartar()
        assertNull(avisosSistema.sesionEnNotificacion)
    }

    @Test
    fun sesionDeMenosDeUnMinutoNoSeGuarda() = runTest {
        val vm = TemporizadorViewModel(repositorio, avisosSistema) { repositorio.reloj }
        observar(vm.uiState)
        val avisos = recolectarAvisos(vm.avisos)

        vm.empezar()
        repositorio.reloj = 30_000
        vm.terminar()
        assertEquals(R.string.aviso_sesion_muy_corta, avisos.single().mensaje)
        assertTrue(repositorio.usuario.value!!.materias[0].eventos.isEmpty())
    }
}
