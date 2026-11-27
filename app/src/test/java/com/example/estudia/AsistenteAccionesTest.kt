package com.example.estudia

import com.example.estudia.modelo.AccionPropuesta
import com.example.estudia.modelo.DECLARACION_FUNCIONES
import com.example.estudia.modelo.EstadoAccion
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.FUNCION_BUSCAR
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.Prioridad
import com.example.estudia.modelo.ResultadoIA
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.accionDesdeLlamada
import com.example.estudia.modelo.contextoDelEstudiante
import com.example.estudia.modelo.descripcionParaIA
import com.example.estudia.viewmodel.AsistenteViewModel
import com.example.estudia.viewmodel.Aviso
import com.example.estudia.viewmodel.Mensaje
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

// Tests de lo que la IA puede hacer en la app: editar, borrar y cambiar
// tareas, eventos, notas y materias (siempre con confirmación).
@OptIn(ExperimentalCoroutinesApi::class)
class AsistenteAccionesTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    private val hoy = LocalDate.of(2026, 11, 27)
    private val guia = Nota(5, "Guía 7: integrales", "Integrales dobles.", hoy)
    private val tp = Tarea(8, "TP 3", hoy.plusDays(2))
    private val parcial = Evento(20, "Parcial 2", TipoEvento.EXAMEN, hoy.plusDays(5), LocalTime.of(9, 0), LocalTime.of(11, 0))
    private val materia = Materia(3, "Análisis II", 0xFF6C5CE7, notas = listOf(guia), tareas = listOf(tp), eventos = listOf(parcial))
    private fun usuario() = Usuario(id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana", materias = listOf(materia))

    private fun TestScope.vm(repositorio: RepositorioFalso, acciones: List<AccionPropuesta>): Pair<AsistenteViewModel, MutableList<Aviso>> {
        val ia = AsistenteFalso(respuesta = ResultadoIA.Ok("Confirmá:")).apply { this.acciones = acciones }
        val vm = AsistenteViewModel(repositorio, ia, ClavesFalsas(), "clave-app", hoy = { hoy })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }
        return vm to avisos
    }

    @Test
    fun lasDeclaracionesSonJsonValidoYTienenLaBusqueda() {
        // (org.json no anda en los tests comunes: se miran los nombres y que las llaves cierren.)
        val nombres = Regex(""""name":"([a-z_]+)"""").findAll(DECLARACION_FUNCIONES).map { it.groupValues[1] }.toList()
        assertEquals(DECLARACION_FUNCIONES.count { it == '{' }, DECLARACION_FUNCIONES.count { it == '}' })
        assertEquals(DECLARACION_FUNCIONES.count { it == '[' }, DECLARACION_FUNCIONES.count { it == ']' })
        assertTrue(FUNCION_BUSCAR in nombres)
        assertTrue(listOf("editar_nota", "borrar_nota", "borrar_materia", "editar_evento", "completar_tarea").all { it in nombres })
    }

    @Test
    fun lasLlamadasNuevasSeRevisan() {
        // Una edición que no cambia nada no sirve.
        assertNull(accionDesdeLlamada("editar_tarea", mapOf("materia" to "A", "tarea" to "TP")))
        assertEquals(
            AccionPropuesta.EditarTarea("A", "TP", quitarFecha = true),
            accionDesdeLlamada("editar_tarea", mapOf("materia" to "A", "tarea" to "TP", "nueva_fecha_limite" to "SIN_FECHA"))
        )
        assertEquals(
            AccionPropuesta.CompletarTarea("A", "TP", hecha = false),
            accionDesdeLlamada("completar_tarea", mapOf("materia" to "A", "tarea" to "TP", "hecha" to "false"))
        )
        assertNull(accionDesdeLlamada("editar_evento", mapOf("materia" to "A", "evento" to "P", "nueva_hora_inicio" to "11:00", "nueva_hora_fin" to "10:00")))
        assertEquals(AccionPropuesta.CrearMateria("Física I"), accionDesdeLlamada("crear_materia", mapOf("nombre" to " Física I ")))
        assertEquals(8.5, (accionDesdeLlamada("agregar_calificacion", mapOf("materia" to "A", "titulo" to "P1", "nota" to "8,5")) as AccionPropuesta.AgregarCalificacion).nota, 0.0)
        assertNull(accionDesdeLlamada("agregar_calificacion", mapOf("materia" to "A", "titulo" to "P1", "nota" to "15")))
        val borrar = accionDesdeLlamada("borrar_nota", mapOf("materia" to "A", "nota" to "Guía"))!!
        assertTrue(borrar.destructiva)
        assertTrue(descripcionParaIA(borrar, EstadoAccion.HECHA).contains("borrar la nota"))
    }

    @Test
    fun laIaVeLosExamenesYTodasLasNotas() {
        val contexto = contextoDelEstudiante(usuario(), hoy)
        assertTrue(contexto.contains("Examen \"Parcial 2\""))
        assertTrue(contexto.contains("Guía 7: integrales"))
    }

    @Test
    fun editarUnaNotaLeSumaTextoAlFinal() = runTest {
        val repositorio = RepositorioFalso(usuario())
        val (vm, _) = vm(repositorio, listOf(AccionPropuesta.EditarNota("analisis ii", "guia 7", agregarTexto = "Teorema de **Fubini**.")))
        vm.enviar("Agregale Fubini a la guía 7")
        advanceUntilIdle()
        vm.confirmarAccion(vm.uiState.value.conversacion.lastIndex)
        advanceUntilIdle()
        assertEquals("Integrales dobles.\n\nTeorema de Fubini.", repositorio.usuario.value!!.materias.single().notas.single().contenido)
    }

    @Test
    fun borrarUnaNotaSePuedeDeshacer() = runTest {
        val repositorio = RepositorioFalso(usuario())
        val (vm, avisos) = vm(repositorio, listOf(AccionPropuesta.BorrarNota("Análisis II", "Guía 7: integrales")))
        vm.enviar("Borrá la guía 7")
        advanceUntilIdle()
        vm.confirmarAccion(vm.uiState.value.conversacion.lastIndex)
        advanceUntilIdle()
        assertTrue(repositorio.usuario.value!!.materias.single().notas.isEmpty())
        val aviso = avisos.last()
        assertEquals(Mensaje.AVISO_ACCION_IA_BORRADA, aviso.mensaje)
        assertNotNull(aviso.alDeshacer)
        aviso.alDeshacer!!.invoke()
        assertEquals("Guía 7: integrales", repositorio.usuario.value!!.materias.single().notas.single().titulo)
    }

    @Test
    fun moverElParcialMantieneLoQueDura() = runTest {
        val repositorio = RepositorioFalso(usuario())
        val (vm, _) = vm(repositorio, listOf(AccionPropuesta.EditarEvento("Análisis II", "parcial 2", nuevaFecha = hoy.plusDays(7), nuevoInicio = LocalTime.of(14, 0))))
        vm.enviar("Pasá el parcial al viernes que viene a las 14")
        advanceUntilIdle()
        vm.confirmarAccion(vm.uiState.value.conversacion.lastIndex)
        advanceUntilIdle()
        val evento = repositorio.usuario.value!!.materias.single().eventos.single()
        assertEquals(hoy.plusDays(7), evento.fecha)
        assertEquals(LocalTime.of(14, 0), evento.horaInicio)
        assertEquals(LocalTime.of(16, 0), evento.horaFin)
    }

    @Test
    fun confirmarTodasHaceVariasDeUna() = runTest {
        val repositorio = RepositorioFalso(usuario())
        val (vm, avisos) = vm(
            repositorio,
            listOf(
                AccionPropuesta.CompletarTarea("Análisis II", "tp 3"),
                AccionPropuesta.CrearTarjeta("Análisis II", "¿Qué dice Fubini?", "Que se puede integrar en cualquier orden"),
                AccionPropuesta.CrearMateria("Física I")
            )
        )
        vm.enviar("Marcá el TP 3, haceme una tarjeta y agregá Física I")
        advanceUntilIdle()
        vm.confirmarTodas()
        advanceUntilIdle()
        val materias = repositorio.usuario.value!!.materias
        assertTrue(materias.first { it.id == 3 }.tareas.single().completada)
        assertEquals(1, materias.first { it.id == 3 }.tarjetas.size)
        assertTrue(materias.any { it.nombre == "Física I" })
        assertTrue(vm.uiState.value.conversacion.filter { it.accion != null }.all { it.estadoAccion == EstadoAccion.HECHA })
        assertEquals(Mensaje.AVISO_ACCIONES_IA_HECHAS, avisos.last().mensaje)
    }

    @Test
    fun siNoEncuentraLaTareaAvisa() = runTest {
        val repositorio = RepositorioFalso(usuario())
        val (vm, avisos) = vm(repositorio, listOf(AccionPropuesta.BorrarTarea("Análisis II", "Informe de laboratorio")))
        vm.enviar("Borrá el informe")
        advanceUntilIdle()
        vm.confirmarAccion(vm.uiState.value.conversacion.lastIndex)
        advanceUntilIdle()
        assertEquals(EstadoAccion.FALLO, vm.uiState.value.conversacion.last().estadoAccion)
        assertEquals(Mensaje.AVISO_ACCION_IA_NO_ENCONTRADO, avisos.last().mensaje)
        assertEquals(1, repositorio.usuario.value!!.materias.single().tareas.size)
    }
}
