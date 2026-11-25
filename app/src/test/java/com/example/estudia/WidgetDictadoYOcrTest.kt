package com.example.estudia

import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.Prioridad
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.ui.componentes.agregarDictado
import com.example.estudia.viewmodel.Aviso
import com.example.estudia.viewmodel.Mensaje
import com.example.estudia.viewmodel.NotasViewModel
import com.example.estudia.widget.armarResumenWidget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

// Tests de la tanda 3: el widget, el dictado por voz y el OCR de fotos.
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetDictadoYOcrTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    private val hoy = LocalDate.of(2026, 11, 24)

    private fun usuario(vararg materias: Materia) =
        Usuario(id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana", materias = materias.toList())

    // ---------- Widget ----------

    @Test
    fun elWidgetMuestraLasTresTareasMasUrgentes() {
        val fisica = Materia(
            1, "Física", 0,
            tareas = listOf(
                Tarea(1, "Sin fecha", prioridad = Prioridad.ALTA),
                Tarea(2, "Para el viernes", hoy.plusDays(3)),
                Tarea(3, "Hecha", hoy, completada = true),
                Tarea(4, "Vencida", hoy.minusDays(1)),
                Tarea(5, "Mañana baja", hoy.plusDays(1), prioridad = Prioridad.BAJA),
                Tarea(6, "Mañana alta", hoy.plusDays(1), prioridad = Prioridad.ALTA)
            ),
            eventos = listOf(
                Evento(1, "Estudio", TipoEvento.BLOQUE_ESTUDIO, hoy, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                Evento(2, "Estudio", TipoEvento.BLOQUE_ESTUDIO, hoy.minusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0))
            )
        )
        val archivada = Materia(2, "Química", 0, archivada = true, tareas = listOf(Tarea(7, "No va", hoy)))

        val resumen = armarResumenWidget(usuario(fisica, archivada), hoy)
        assertEquals(2, resumen.racha)
        assertEquals(5, resumen.pendientes)
        assertEquals(
            listOf("Vencida · Física", "Mañana alta · Física", "Mañana baja · Física"),
            resumen.proximas
        )
    }

    @Test
    fun sinTareasElWidgetQuedaAlDia() {
        val resumen = armarResumenWidget(usuario(Materia(1, "Física", 0)), hoy)
        assertEquals(0, resumen.pendientes)
        assertEquals(emptyList<String>(), resumen.proximas)
    }

    // ---------- Dictado ----------

    @Test
    fun loDictadoSeSumaAlFinalDelTexto() {
        assertEquals("Hola", agregarDictado("", "Hola"))
        assertEquals("Hola", agregarDictado("   ", "Hola"))
        assertEquals("Primera parte segunda parte", agregarDictado("Primera parte  ", "segunda parte"))
    }

    // ---------- OCR ----------

    private val foto = Archivo(1, "pizarron.jpg", "image/jpeg", "a.jpg", 100, hoy, notaId = 1)

    private fun repositorioConNota(contenido: String) = RepositorioFalso(
        usuario(Materia(1, "Física", 0, notas = listOf(Nota(1, "Clase", contenido, hoy, archivos = listOf(foto)))))
    )

    private suspend fun RepositorioFalso.contenidoDeLaNota() =
        usuarioActual.first()!!.materias.single().notas.single().contenido

    @Test
    fun elTextoDeLaFotoSeSumaALaNotaYSePuedeDeshacer() = runTest {
        val repositorio = repositorioConNota("Apuntes")
        repositorio.textoDeLaFoto = "Ley de Ohm: V = I · R"
        val vm = NotasViewModel(repositorio)
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        val item = vm.uiState.value.notas.single()
        vm.pasarATexto(item, foto)
        advanceUntilIdle()
        assertEquals("Apuntes\n\nLey de Ohm: V = I · R", repositorio.contenidoDeLaNota())
        assertEquals(Mensaje.AVISO_OCR_AGREGADO, avisos.single().mensaje)

        avisos.single().alDeshacer!!.invoke()
        assertEquals("Apuntes", repositorio.contenidoDeLaNota())
    }

    @Test
    fun siLaFotoNoTieneTextoONoSeLeeSoloAvisa() = runTest {
        val repositorio = repositorioConNota("Apuntes")
        val vm = NotasViewModel(repositorio)
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        val item = vm.uiState.value.notas.single()

        repositorio.textoDeLaFoto = ""
        vm.pasarATexto(item, foto)
        advanceUntilIdle()
        repositorio.textoDeLaFoto = null
        vm.pasarATexto(item, foto)
        advanceUntilIdle()

        assertEquals(listOf(Mensaje.AVISO_OCR_SIN_TEXTO, Mensaje.AVISO_OCR_ERROR), avisos.map { it.mensaje })
        assertEquals("Apuntes", repositorio.contenidoDeLaNota())
    }
}
