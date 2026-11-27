package com.example.estudia

import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.ParteNota
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.agregarCasilla
import com.example.estudia.modelo.alternarCasilla
import com.example.estudia.modelo.cambioParaHistorial
import com.example.estudia.modelo.etiquetasDe
import com.example.estudia.modelo.partesDeNota
import com.example.estudia.modelo.progresoCasillas
import com.example.estudia.modelo.textoMarcasGrabacion
import com.example.estudia.modelo.tiempoGrabacion
import com.example.estudia.modelo.vistaPreviaNota
import com.example.estudia.viewmodel.NotasViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

// Casillas, etiquetas, notas fijadas, color, historial y marcas de grabación.
@OptIn(ExperimentalCoroutinesApi::class)
class NotasExtrasTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    private val hoy = LocalDate.of(2026, 11, 27)

    @Test
    fun lasCasillasSeLeenYSeMarcan() {
        val texto = "Para el parcial:\n- [ ] Guía 3\n- [x] Guía 2\nRepasar #parcial"
        val partes = partesDeNota(texto)
        assertEquals(
            listOf(
                ParteNota.Texto("Para el parcial:"),
                ParteNota.Casilla(1, "Guía 3", false),
                ParteNota.Casilla(2, "Guía 2", true),
                ParteNota.Texto("Repasar #parcial")
            ),
            partes
        )
        assertEquals(1 to 2, progresoCasillas(texto))
        val marcada = alternarCasilla(texto, 1)
        assertTrue(marcada.contains("- [x] Guía 3"))
        assertEquals(2 to 2, progresoCasillas(marcada))
        // Una línea que no es casilla no cambia nada.
        assertEquals(texto, alternarCasilla(texto, 0))
        assertEquals("Hola\n- [ ] ", agregarCasilla("Hola\n\n"))
        assertEquals("☐ Guía 3", vistaPreviaNota("- [ ] Guía 3"))
    }

    @Test
    fun lasEtiquetasSeSacanDelTexto() {
        assertEquals(
            listOf("parcial", "física-ii", "tp_2"),
            etiquetasDe("# Título\nEsto es #Parcial y #física-ii, #parcial otra vez. Ver #tp_2 y #2026 y mail#no")
        )
    }

    @Test
    fun marcarCasillasNoLlenaElHistorial() {
        assertFalse(cambioParaHistorial("A", "- [ ] uno", "A", "- [x] uno"))
        assertTrue(cambioParaHistorial("A", "- [ ] uno", "A", "- [ ] dos"))
        assertTrue(cambioParaHistorial("A", "x", "B", "x"))
    }

    @Test
    fun lasMarcasDeLaGrabacionVanAlTexto() {
        assertEquals("01:05", tiempoGrabacion(65))
        assertEquals("1:00:01", tiempoGrabacion(3601))
        assertEquals("Hola\n\n🎙 Grabación 27/11 14:05 · marcas: 00:45, 03:10", textoMarcasGrabacion("Hola", "27/11 14:05", listOf("00:45", "03:10")))
        assertEquals("Hola", textoMarcasGrabacion("Hola", "27/11 14:05", emptyList()))
    }

    private fun repositorio() = RepositorioFalso(
        Usuario(
            id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana",
            materias = listOf(
                Materia(
                    3, "Física", 0xFF0984E3,
                    notas = listOf(
                        Nota(1, "Ondas", "Ver #parcial", hoy),
                        Nota(2, "Óptica", "- [ ] lentes\n#tp", hoy),
                        Nota(3, "Calor", "#parcial y #tp", hoy)
                    )
                )
            )
        )
    )

    @Test
    fun lasFijadasVanArribaYSeFiltraPorEtiqueta() = runTest {
        val repositorio = repositorio()
        val vm = NotasViewModel(repositorio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(listOf("parcial", "tp"), vm.uiState.value.etiquetas)

        vm.alternarFijada(vm.uiState.value.notas.first { it.nota.titulo == "Calor" })
        advanceUntilIdle()
        assertEquals("Calor", vm.uiState.value.notas.first().nota.titulo)

        vm.elegirEtiqueta("tp")
        advanceUntilIdle()
        assertEquals(listOf("Calor", "Óptica"), vm.uiState.value.notas.map { it.nota.titulo })
        // Tocarla de nuevo la suelta.
        vm.elegirEtiqueta("tp")
        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.notas.size)

        vm.cambiarColor(vm.uiState.value.notas.first(), 0xFFE84393)
        advanceUntilIdle()
        assertEquals(0xFFE84393, vm.uiState.value.notas.first().nota.color)
    }

    @Test
    fun elHistorialGuardaLoAnteriorYSePuedeVolver() = runTest {
        val repositorio = repositorio()
        val vm = NotasViewModel(repositorio)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        val optica = { vm.uiState.value.notas.first { it.nota.id == 2 } }

        // Marcar una casilla no deja versión.
        vm.alternarCasilla(optica(), 0)
        advanceUntilIdle()
        assertEquals("- [x] lentes\n#tp", optica().nota.contenido)
        assertTrue(vm.versiones(2).first().isEmpty())

        vm.editarNota(optica(), "Óptica geométrica", "Lentes y espejos")
        advanceUntilIdle()
        val versiones = vm.versiones(2).first()
        assertEquals(1, versiones.size)
        assertEquals("Óptica", versiones.single().titulo)

        vm.restaurarVersion(optica(), versiones.single())
        advanceUntilIdle()
        assertEquals("Óptica", optica().nota.titulo)
        assertEquals("- [x] lentes\n#tp", optica().nota.contenido)
        // Lo que había antes de volver también quedó guardado.
        assertEquals("Óptica geométrica", vm.versiones(2).first().first().titulo)
    }
}
