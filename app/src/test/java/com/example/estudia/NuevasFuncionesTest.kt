package com.example.estudia

import com.example.estudia.modelo.Calificacion
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.Prioridad
import com.example.estudia.modelo.Subtarea
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.viewmodel.MateriasViewModel
import com.example.estudia.viewmodel.OrdenTareas
import com.example.estudia.viewmodel.PerfilViewModel
import com.example.estudia.viewmodel.TemporizadorViewModel
import com.example.estudia.viewmodel.TipoLogro
import com.example.estudia.viewmodel.armarHorario
import com.example.estudia.viewmodel.calcularLogros
import com.example.estudia.viewmodel.calcularResumen
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

// Tests de las funciones de la versión 6: prioridad y subtareas, horario
// semanal, logros, foto de perfil y "Silenciar el teléfono".
@OptIn(ExperimentalCoroutinesApi::class)
class NuevasFuncionesTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    // Martes 24 de noviembre de 2026; el lunes de esa semana es el 23.
    private val hoy = LocalDate.of(2026, 11, 24)
    private val lunes = LocalDate.of(2026, 11, 23)

    private fun usuario(vararg materias: Materia) =
        Usuario(id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana", materias = materias.toList())

    private fun TestScope.observar(flujo: Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flujo.collect {} }
    }

    // ---------- Prioridad ----------

    @Test
    fun inicioOrdenaPorFechaOPorPrioridad() {
        val materia = Materia(
            1, "Física", 0,
            tareas = listOf(
                Tarea(1, "Vence antes", hoy, prioridad = Prioridad.BAJA),
                Tarea(2, "Importante", hoy.plusDays(5), prioridad = Prioridad.ALTA),
                Tarea(3, "Sin fecha", null, prioridad = Prioridad.MEDIA)
            )
        )
        val porFecha = calcularResumen(usuario(materia), hoy, OrdenTareas.FECHA).tareasPendientes.map { it.tarea.id }
        assertEquals(listOf(1, 2, 3), porFecha)
        val porPrioridad = calcularResumen(usuario(materia), hoy, OrdenTareas.PRIORIDAD).tareasPendientes.map { it.tarea.id }
        assertEquals(listOf(2, 3, 1), porPrioridad)
    }

    // ---------- Subtareas ----------

    @Test
    fun seCreaYSeEditaUnaTareaConSusPasos() = runTest {
        val repositorio = RepositorioFalso(usuario(Materia(1, "Física", 0)))
        val vm = MateriasViewModel(repositorio, AvisosFalso())
        observar(vm.uiState)

        // Los pasos vacíos se descartan.
        vm.guardarTarea(1, null, "TP 2", null, Prioridad.ALTA, listOf(Subtarea(0, "Leer"), Subtarea(0, " "), Subtarea(0, "Resolver")))
        advanceUntilIdle()
        val tarea = vm.uiState.value.todas.single().tareas.single()
        assertEquals(Prioridad.ALTA, tarea.prioridad)
        assertEquals(listOf("Leer", "Resolver"), tarea.subtareas.map { it.titulo })

        // Marcar un paso no marca la tarea.
        vm.alternarSubtarea(tarea, tarea.subtareas.first())
        advanceUntilIdle()
        val marcada = vm.uiState.value.todas.single().tareas.single()
        assertEquals(1, marcada.subtareasHechas)
        assertFalse(marcada.completada)

        // Al editar se puede quitar un paso; el que quedó sigue hecho.
        vm.guardarTarea(1, marcada, "TP 2", null, Prioridad.MEDIA, listOf(marcada.subtareas.first()))
        advanceUntilIdle()
        val editada = vm.uiState.value.todas.single().tareas.single()
        assertEquals(Prioridad.MEDIA, editada.prioridad)
        assertEquals(1, editada.subtareas.size)
        assertTrue(editada.subtareas.single().completada)
    }

    // ---------- Horario semanal ----------

    @Test
    fun elHorarioJuntaLasClasesDeCadaDia() {
        val clases = listOf(
            // Teórico de los lunes, desde hace un mes y sin fin.
            Evento(1, "Teórico", TipoEvento.CLASE, lunes.minusWeeks(4), LocalTime.of(18, 0), LocalTime.of(20, 0), semanal = true),
            // Práctica de los martes que ya terminó: no aparece.
            Evento(2, "Práctica", TipoEvento.CLASE, hoy.minusWeeks(4), LocalTime.of(9, 0), LocalTime.of(11, 0), semanal = true, repetirHasta = hoy.minusWeeks(1)),
            // Clase de consulta suelta de este miércoles.
            Evento(3, "Consulta", TipoEvento.CLASE, lunes.plusDays(2), LocalTime.of(14, 0), LocalTime.of(15, 0)),
            // Un examen no es una clase.
            Evento(4, "Parcial", TipoEvento.EXAMEN, hoy, LocalTime.of(10, 0), LocalTime.of(12, 0))
        )
        val horario = armarHorario(usuario(Materia(1, "Física", 0, eventos = clases)), hoy)

        // De lunes a viernes, sin sábado ni domingo (no tienen clases).
        assertEquals(DayOfWeek.entries.take(5), horario.dias.map { it.dia })
        assertEquals(listOf("Teórico"), horario.dias[0].clases.map { it.titulo })
        assertTrue(horario.dias[1].clases.isEmpty())
        assertEquals(listOf("Consulta"), horario.dias[2].clases.map { it.titulo })
        assertEquals(2, horario.totalClases)
        assertEquals(180L, horario.minutosPorSemana)
    }

    // ---------- Logros ----------

    @Test
    fun losLogrosSalenDeLosDatos() {
        val materia = Materia(
            1, "Física", 0,
            tareas = listOf(Tarea(1, "Hecha", null, completada = true), Tarea(2, "Pendiente", null)),
            // Tres días seguidos estudiando, 2 h cada uno.
            eventos = (0L..2L).map { Evento(it.toInt() + 10, "Estudio", TipoEvento.BLOQUE_ESTUDIO, hoy.minusDays(it), LocalTime.of(10, 0), LocalTime.of(12, 0)) },
            calificaciones = listOf(Calificacion(1, "Parcial", 8.5, hoy)),
            notas = listOf(Nota(1, "Ondas", "", hoy))
        )
        // El domingo 22 es de la semana anterior: esta semana (lunes y martes) suma 4 h.
        val logros = calcularLogros(usuario(materia).copy(metaHorasSemanales = 4), hoy).associateBy { it.tipo }

        assertTrue(logros.getValue(TipoLogro.PRIMERA_TAREA).conseguido)
        assertEquals(1, logros.getValue(TipoLogro.DIEZ_TAREAS).actual)
        assertTrue(logros.getValue(TipoLogro.RACHA_3).conseguido)
        assertFalse(logros.getValue(TipoLogro.RACHA_7).conseguido)
        assertEquals(6, logros.getValue(TipoLogro.HORAS_10).actual)
        // 4 h en la semana con una meta de 4 h: cumplida.
        assertTrue(logros.getValue(TipoLogro.META_CUMPLIDA).conseguido)
        assertTrue(logros.getValue(TipoLogro.APROBADO).conseguido)
        assertTrue(logros.getValue(TipoLogro.PROMEDIO_8).conseguido)
        assertEquals(1, logros.getValue(TipoLogro.CINCO_NOTAS).actual)
        // Los conseguidos van primero.
        val lista = calcularLogros(usuario(materia), hoy)
        assertTrue(lista.first().conseguido)
        assertFalse(lista.last().conseguido)
    }

    // ---------- Foto de perfil ----------

    @Test
    fun seCambiaYSeQuitaLaFotoDePerfil() = runTest {
        val repositorio = RepositorioFalso(usuario())
        val vm = PerfilViewModel(repositorio, AvisosFalso())
        observar(vm.uiState)

        vm.cambiarFotoPerfil("content://foto.jpg")
        advanceUntilIdle()
        assertEquals("content://foto.jpg", vm.uiState.value.fotoUri)

        vm.cambiarFotoPerfil(null)
        advanceUntilIdle()
        assertNull(vm.uiState.value.fotoUri)
    }

    // ---------- Silenciar el teléfono ----------

    @Test
    fun silenciarSeGuardaYSeAvisaAlSistema() = runTest {
        val repositorio = RepositorioFalso(usuario(Materia(1, "Física", 0)))
        val avisos = AvisosFalso()
        val vm = TemporizadorViewModel(repositorio, avisos, reloj = { 0L })
        observar(vm.uiState)

        // Sin advanceUntilIdle: el "tic" del temporizador nunca termina. Con
        // el dispatcher de prueba los cambios se aplican al instante.
        vm.cambiarSilenciar(true)
        assertTrue(vm.uiState.value.silenciar)
        assertEquals(true, avisos.silencioElegido)

        vm.cambiarSilenciar(false)
        assertFalse(vm.uiState.value.silenciar)
        assertEquals(false, avisos.silencioElegido)
    }
}
