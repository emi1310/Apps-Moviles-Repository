package com.example.estudia

import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.viewmodel.Mensaje
import com.example.estudia.viewmodel.Aviso
import com.example.estudia.viewmodel.CalendarioViewModel
import com.example.estudia.viewmodel.FiltroNotas
import com.example.estudia.viewmodel.MateriasViewModel
import com.example.estudia.viewmodel.NotasViewModel
import com.example.estudia.viewmodel.PerfilViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

// Tests de los ViewModels de cada pantalla, usando el RepositorioFalso.
@OptIn(ExperimentalCoroutinesApi::class)
class PantallasViewModelTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    private val hoy = LocalDate.now()
    private val archivo = Archivo(7, "apunte.pdf", "application/pdf", "x_apunte.pdf", 100, hoy)
    private val usuarioInicial = Usuario(
        id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana", metaHorasSemanales = 4,
        materias = listOf(
            Materia(
                id = 1, nombre = "Física", color = 0,
                tareas = listOf(Tarea(10, "Leer capítulo 3", hoy)),
                notas = listOf(Nota(20, "Ondas", "frecuencia y amplitud", hoy, esFavorita = true), Nota(21, "Óptica", "lentes", hoy.minusDays(5))),
                eventos = listOf(Evento(30, "Estudio", TipoEvento.BLOQUE_ESTUDIO, hoy, LocalTime.of(10, 0), LocalTime.of(12, 0))),
                archivos = listOf(archivo)
            ),
            Materia(id = 2, nombre = "Química", color = 0, archivada = true)
        )
    )
    private val repositorio = RepositorioFalso(usuarioInicial)
    private val avisosSistema = AvisosFalso()

    // stateIn(WhileSubscribed) solo calcula el estado si alguien lo observa.
    private fun TestScope.observar(flujo: kotlinx.coroutines.flow.Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flujo.collect {} }
    }

    private fun TestScope.recolectarAvisos(flujo: kotlinx.coroutines.flow.Flow<Aviso>): List<Aviso> {
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flujo.collect { avisos += it } }
        return avisos
    }

    // ---------- Materias ----------

    @Test
    fun elPermisoDeArchivosSePideUnaSolaVez() = runTest {
        val vm = NotasViewModel(repositorio)
        advanceUntilIdle()
        assertFalse(vm.permisoArchivosPedido.value)
        vm.marcarPermisoArchivosPedido()
        advanceUntilIdle()
        assertTrue(vm.permisoArchivosPedido.value)
        // Otra pantalla (Materias) ya ve que se pidió.
        val materias = MateriasViewModel(repositorio, avisosSistema)
        advanceUntilIdle()
        assertTrue(materias.permisoArchivosPedido.value)
    }

    @Test
    fun materiasFiltraActivasYArchivadas() = runTest {
        val vm = MateriasViewModel(repositorio, avisosSistema)
        observar(vm.uiState)
        assertEquals(listOf("Física"), vm.uiState.value.materias.map { it.nombre })
        vm.mostrarArchivadas(true)
        assertEquals(listOf("Química"), vm.uiState.value.materias.map { it.nombre })
    }

    @Test
    fun noSePuedeRepetirElNombreDeUnaMateria() = runTest {
        val vm = MateriasViewModel(repositorio, avisosSistema)
        observar(vm.uiState)
        assertFalse(vm.nombreDisponible("física "))
        assertTrue(vm.nombreDisponible("Física", exceptoId = 1))   // renombrar a su propio nombre
        assertTrue(vm.nombreDisponible("Historia"))
    }

    @Test
    fun buscadorDeTareas() = runTest {
        val vm = MateriasViewModel(repositorio, avisosSistema)
        observar(vm.uiState)
        vm.cambiarBusqueda("capítulo")
        assertEquals(listOf("Leer capítulo 3"), vm.uiState.value.resultadosBusqueda.map { it.tarea.titulo })
        vm.cambiarBusqueda("nada")
        assertTrue(vm.uiState.value.resultadosBusqueda.isEmpty())
    }

    @Test
    fun eliminarTareaSePuedeDeshacer() = runTest {
        val vm = MateriasViewModel(repositorio, avisosSistema)
        observar(vm.uiState)
        val avisos = recolectarAvisos(vm.avisos)
        val tarea = usuarioInicial.materias[0].tareas[0]

        vm.eliminarTarea(1, tarea)
        assertTrue(vm.uiState.value.todas[0].tareas.isEmpty())
        assertEquals(Mensaje.AVISO_TAREA_ELIMINADA, avisos.single().mensaje)

        avisos.single().alDeshacer!!.invoke()
        assertEquals(listOf(tarea), vm.uiState.value.todas[0].tareas)
    }

    @Test
    fun eliminarMateriaSePuedeDeshacer() = runTest {
        val vm = MateriasViewModel(repositorio, avisosSistema)
        observar(vm.uiState)
        val avisos = recolectarAvisos(vm.avisos)
        val materia = vm.uiState.value.todas.first { it.nombre == "Física" }

        vm.eliminarMateria(materia)
        assertTrue(vm.uiState.value.materias.isEmpty())
        assertEquals(Mensaje.AVISO_MATERIA_ELIMINADA, avisos.single().mensaje)

        avisos.single().alDeshacer!!.invoke()
        assertEquals(listOf("Física"), vm.uiState.value.materias.map { it.nombre })
        assertTrue(repositorio.archivosBorradosDelDisco.isEmpty())
    }

    @Test
    fun elArchivoSeBorraDelDiscoSoloSiNoSeDeshace() = runTest {
        val vm = MateriasViewModel(repositorio, avisosSistema)
        observar(vm.uiState)
        val avisos = recolectarAvisos(vm.avisos)

        vm.eliminarArchivo(1, archivo)
        assertTrue(repositorio.archivosBorradosDelDisco.isEmpty())
        avisos.single().alConfirmar!!.invoke()
        assertEquals(listOf(archivo), repositorio.archivosBorradosDelDisco)
    }

    @Test
    fun agregarTareaConFecha() = runTest {
        val vm = MateriasViewModel(repositorio, avisosSistema)
        observar(vm.uiState)
        vm.guardarTarea(1, null, "  Resumen  ", hoy.plusDays(2))
        val nueva = vm.uiState.value.todas[0].tareas.last()
        assertEquals("Resumen", nueva.titulo)
        assertEquals(hoy.plusDays(2), nueva.fechaLimite)
    }

    // ---------- Calendario ----------

    @Test
    fun eventoConHoraDeFinAnteriorDaError() = runTest {
        val vm = CalendarioViewModel(repositorio, avisosSistema)
        observar(vm.uiState)
        val error = vm.guardarEvento(1, null, "Parcial", TipoEvento.EXAMEN, hoy, LocalTime.of(10, 0), LocalTime.of(9, 0))
        assertEquals(Mensaje.ERROR_HORA_FIN, error)

        val ok = vm.guardarEvento(1, null, "Parcial", TipoEvento.EXAMEN, hoy, LocalTime.of(9, 0), LocalTime.of(11, 0))
        assertNull(ok)
        // Los eventos del día se ordenan por hora: el parcial (9:00) va antes que el estudio (10:00).
        assertEquals(listOf("Parcial", "Estudio"), vm.uiState.value.eventosDelDia.map { it.evento.titulo })
    }

    // ---------- Notas ----------

    @Test
    fun notasFiltrosYBuscador() = runTest {
        val vm = NotasViewModel(repositorio)
        observar(vm.uiState)
        assertEquals(2, vm.uiState.value.notas.size)

        vm.cambiarFiltro(FiltroNotas.FAVORITAS)
        assertEquals(listOf("Ondas"), vm.uiState.value.notas.map { it.nota.titulo })

        vm.cambiarFiltro(FiltroNotas.TODAS)
        vm.cambiarBusqueda("lentes")
        assertEquals(listOf("Óptica"), vm.uiState.value.notas.map { it.nota.titulo })
    }

    // ---------- Perfil ----------

    @Test
    fun progresoDeLaMetaSemanal() = runTest {
        val vm = PerfilViewModel(repositorio, avisosSistema)
        observar(vm.uiState)
        // 2 h planificadas de una meta de 4 h.
        assertEquals(2, vm.uiState.value.horasEstaSemana)
        assertEquals(0.5f, vm.uiState.value.progresoMeta!!, 0.001f)

        vm.cambiarMetaSemanal(null)
        assertNull(vm.uiState.value.progresoMeta)
        vm.cambiarMetaSemanal(1)
        assertNotNull(vm.uiState.value.progresoMeta)
        assertEquals(1f, vm.uiState.value.progresoMeta!!, 0.001f)
    }
}
