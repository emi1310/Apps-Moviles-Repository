package com.example.estudia

import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.viewmodel.CalendarioViewModel
import com.example.estudia.viewmodel.MateriasViewModel
import com.example.estudia.viewmodel.PerfilViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// Tests de la versión 7: aviso de cada tarea, temario de los exámenes y
// copia de seguridad.
@OptIn(ExperimentalCoroutinesApi::class)
class AvisosTemarioYCopiaTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    private val hoy = LocalDate.now()

    private fun usuario(vararg materias: Materia) =
        Usuario(id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana", materias = materias.toList())

    private fun TestScope.observar(flujo: Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flujo.collect {} }
    }

    // ---------- Aviso de cada tarea ----------

    @Test
    fun elAvisoDeUnaTareaLlegaALas9DelDiaElegido() {
        val ahora = LocalDateTime.of(2026, 11, 24, 12, 0)
        val tarea = Tarea(1, "TP", LocalDate.of(2026, 11, 30), avisoDiasAntes = 2)
        assertEquals(LocalDateTime.of(2026, 11, 28, 9, 0), tarea.momentoDelAviso(ahora))
        // Sin aviso, sin fecha, hecha o en el pasado: no hay aviso.
        assertNull(tarea.copy(avisoDiasAntes = null).momentoDelAviso(ahora))
        assertNull(tarea.copy(fechaLimite = null).momentoDelAviso(ahora))
        assertNull(tarea.copy(completada = true).momentoDelAviso(ahora))
        assertNull(tarea.copy(avisoDiasAntes = 7).momentoDelAviso(ahora))
    }

    @Test
    fun guardarYCompletarUnaTareaProgramaYCancelaSuAviso() = runTest {
        val repositorio = RepositorioFalso(usuario(Materia(1, "Física", 0)))
        val avisos = AvisosFalso()
        val vm = MateriasViewModel(repositorio, avisos)
        observar(vm.uiState)

        vm.guardarTarea(1, null, "TP 3", hoy.plusDays(5), avisoDiasAntes = 1)
        advanceUntilIdle()
        val tarea = vm.uiState.value.todas.single().tareas.single()
        assertEquals(1, tarea.avisoDiasAntes)
        assertEquals(tarea.id, avisos.tareasProgramadas.last().id)

        // Sin fecha límite no queda aviso.
        vm.guardarTarea(1, tarea, "TP 3", null, avisoDiasAntes = 1)
        advanceUntilIdle()
        assertNull(vm.uiState.value.todas.single().tareas.single().avisoDiasAntes)

        vm.alternarTarea(1, vm.uiState.value.todas.single().tareas.single())
        advanceUntilIdle()
        assertTrue(tarea.id in avisos.tareasCanceladas)
    }

    // ---------- Temario ----------

    @Test
    fun seCarganYSeMarcanLosTemasDeUnExamen() = runTest {
        val examen = Evento(5, "Parcial", TipoEvento.EXAMEN, hoy, LocalTime.of(9, 0), LocalTime.of(11, 0))
        val repositorio = RepositorioFalso(usuario(Materia(1, "Física", 0, eventos = listOf(examen))))
        val vm = CalendarioViewModel(repositorio, AvisosFalso())
        observar(vm.uiState)

        vm.agregarTema(5, "Unidad 1")
        vm.agregarTema(5, "Unidad 2")
        vm.agregarTema(5, "  ")   // vacío: no se agrega
        advanceUntilIdle()
        var evento = vm.uiState.value.eventosDelDia.single().evento
        assertEquals(listOf("Unidad 1", "Unidad 2"), evento.temas.map { it.titulo })

        vm.alternarTema(5, evento.temas.first())
        advanceUntilIdle()
        evento = vm.uiState.value.eventosDelDia.single().evento
        assertEquals(1, evento.temasEstudiados)

        vm.eliminarTema(5, evento.temas.last())
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.eventosDelDia.single().evento.temas.size)
    }

    // ---------- Copia de seguridad ----------

    @Test
    fun restaurarUnaCopiaDevuelveLasMateriasYReprogramaLosAvisos() = runTest {
        val tarea = Tarea(1, "TP", hoy.plusDays(3), avisoDiasAntes = 1)
        val repositorio = RepositorioFalso(usuario(Materia(1, "Física", 0, tareas = listOf(tarea))))
        val avisos = AvisosFalso()
        val vm = PerfilViewModel(repositorio, avisos)
        observar(vm.uiState)

        vm.exportarCopia("content://copia.zip")
        advanceUntilIdle()
        // Se borra todo y se restaura la copia.
        repositorio.usuario.value = repositorio.usuario.value!!.copy(materias = emptyList())
        vm.importarCopia("content://copia.zip")
        advanceUntilIdle()

        assertEquals(listOf("Física"), vm.uiState.value.usuario!!.materias.map { it.nombre })
        assertEquals(listOf(tarea.id), avisos.tareasProgramadas.map { it.id })
    }
}
