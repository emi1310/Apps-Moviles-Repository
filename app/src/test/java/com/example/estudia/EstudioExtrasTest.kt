package com.example.estudia

import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.ResultadoRespuesta
import com.example.estudia.modelo.Tarjeta
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.armarPares
import com.example.estudia.modelo.avanceMetaDiaria
import com.example.estudia.modelo.compararRespuesta
import com.example.estudia.modelo.leerTarjetasDeTexto
import com.example.estudia.modelo.textoTiempoPares
import com.example.estudia.viewmodel.MateriasViewModel
import com.example.estudia.viewmodel.ParesViewModel
import com.example.estudia.viewmodel.RepasoViewModel
import com.example.estudia.viewmodel.minutosDeEstudioDelDia
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceUntilIdle
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
import kotlin.random.Random

// Respuesta escrita, imágenes en tarjetas, importar mazos, "Unir pares" y
// la meta diaria de estudio.
@OptIn(ExperimentalCoroutinesApi::class)
class EstudioExtrasTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    private val hoy = LocalDate.of(2026, 11, 27)

    @Test
    fun laRespuestaEscritaPerdonaTildesMayusculasYErroresChicos() {
        assertEquals(ResultadoRespuesta.CORRECTA, compararRespuesta("la celula", "¡La Célula!"))
        assertEquals(ResultadoRespuesta.CASI, compararRespuesta("mitocondira", "Mitocondria"))
        assertEquals(ResultadoRespuesta.INCORRECTA, compararRespuesta("ribosoma", "Mitocondria"))
        assertEquals(ResultadoRespuesta.INCORRECTA, compararRespuesta("   ", "Mitocondria"))
        // Respuesta larga: casi todas las palabras importantes alcanzan para "casi".
        assertEquals(
            ResultadoRespuesta.CASI,
            compararRespuesta("produce energia celula forma ATP", "Produce la energía de la célula en forma de ATP mediante respiración")
        )
    }

    @Test
    fun seLeenLosMazosDeQuizletAnkiYCsv() {
        // Quizlet: tabulación.
        assertEquals(listOf("Ácido" to "pH < 7", "Base" to "pH > 7"), leerTarjetasDeTexto("Ácido\tpH < 7\nBase\tpH > 7\n"))
        // Anki: líneas de configuración y HTML.
        val anki = "#separator:tab\n#html:true\n<b>Capital de Francia</b>\tParís<br>(desde 987)\n"
        assertEquals(listOf("Capital de Francia" to "París\n(desde 987)"), leerTarjetasDeTexto(anki))
        // CSV con comillas, comas adentro y saltos de línea; se saltean filas incompletas y repetidas.
        val csv = "﻿\"Ley de Ohm\",\"V = I · R, en un circuito\"\r\nSolo pregunta,\n\"Newton\",\"F = m·a\nsegunda ley\"\nley de ohm,repetida\n"
        assertEquals(
            listOf("Ley de Ohm" to "V = I · R, en un circuito", "Newton" to "F = m·a\nsegunda ley"),
            leerTarjetasDeTexto(csv)
        )
        // Punto y coma (Excel en español).
        assertEquals(listOf("Uno" to "1", "Dos" to "2"), leerTarjetasDeTexto("Uno;1\nDos;2"))
        assertTrue(leerTarjetasDeTexto("texto sin separador").isEmpty())
    }

    @Test
    fun importarDesdeMateriasNoDuplicaLasQueYaEstan() = runTest {
        val repositorio = RepositorioFalso(Usuario(1, "ana", MetodoAuth.LOCAL, "Ana", materias = listOf(
            Materia(3, "Química", 0, tarjetas = listOf(Tarjeta(1, "Ácido", "pH < 7")))
        )))
        val vm = MateriasViewModel(repositorio, AvisosFalso())
        vm.importarTarjetas(3, listOf("ácido" to "otra", "Base" to "pH > 7"))
        advanceUntilIdle()
        assertEquals(listOf("Ácido", "Base"), repositorio.usuario.value!!.materias.single().tarjetas.map { it.pregunta })
        // Una tarjeta con imagen puede no tener texto en la pregunta; sin respuesta no.
        assertTrue(vm.guardarTarjeta(3, null, "", "Corazón", imagen = "foto.jpg"))
        assertFalse(vm.guardarTarjeta(3, null, "", "Corazón"))
        advanceUntilIdle()
        assertEquals("foto.jpg", repositorio.usuario.value!!.materias.single().tarjetas.last().imagen)
    }

    @Test
    fun elRepasoEscritoComparaYDespuesSeResponde() = runTest {
        val repositorio = RepositorioFalso(Usuario(1, "ana", MetodoAuth.LOCAL, "Ana", materias = listOf(
            Materia(3, "Bio", 0, tarjetas = listOf(Tarjeta(1, "¿Qué produce energía?", "La mitocondria", proximoRepaso = hoy)))
        )))
        val vm = RepasoViewModel(repositorio, 3) { hoy }
        vm.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        vm.cambiarEscribir(true)
        vm.escribirRespuesta("la mitocondria")
        vm.comprobar()
        advanceUntilIdle()
        assertEquals(ResultadoRespuesta.CORRECTA, vm.uiState.value.resultado)
        assertTrue(vm.uiState.value.mostrandoRespuesta)
        vm.responder(true)
        advanceUntilIdle()
        assertNull(vm.uiState.value.actual)
        assertEquals(1, vm.uiState.value.repasadas)
        assertEquals("", vm.uiState.value.escrita)
        assertEquals(2, repositorio.usuario.value!!.materias.single().tarjetas.single().caja)
    }

    @Test
    fun armarParesNecesitaTresTarjetasDistintas() {
        val tarjetas = listOf(Tarjeta(1, "a", "1"), Tarjeta(2, "b", "2"), Tarjeta(3, "c", "1"))
        // Dos tienen la misma respuesta: quedan 2 y no alcanza.
        assertTrue(armarPares(tarjetas, Random(1)).isEmpty())
        val fichas = armarPares(tarjetas + Tarjeta(4, "d", "4") + Tarjeta(5, "", "5", imagen = "x.jpg"), Random(1))
        assertEquals(8, fichas.size)
        assertEquals((0 until 8).toList(), fichas.map { it.id })
        assertTrue(fichas.groupBy { it.par }.values.all { par -> par.size == 2 && par.count { it.esPregunta } == 1 })
        assertEquals("x.jpg", fichas.first { it.par == 5 && it.esPregunta }.imagen)
        assertEquals("1:05.3", textoTiempoPares(65_340))
    }

    @Test
    fun unirParesCuentaErroresYGuardaElRecord() = runTest {
        var ahora = 10_000L
        val tarjetas = listOf(Tarjeta(1, "a", "1"), Tarjeta(2, "b", "2"), Tarjeta(3, "c", "3"))
        val repositorio = RepositorioFalso(Usuario(1, "ana", MetodoAuth.LOCAL, "Ana", materias = listOf(Materia(3, "Mate", 0, tarjetas = tarjetas))))
        val vm = ParesViewModel(repositorio, 3, Random(7)) { ahora }
        advanceUntilIdle()
        val fichas = vm.uiState.value.fichas
        assertEquals(6, fichas.size)
        // Un error: pregunta de una con respuesta de otra.
        val p1 = fichas.first { it.par == 1 && it.esPregunta }
        val r2 = fichas.first { it.par == 2 && !it.esPregunta }
        vm.tocar(p1.id)
        vm.tocar(r2.id)
        assertEquals(1, vm.uiState.value.errores)
        assertEquals(setOf(p1.id, r2.id), vm.uiState.value.equivocadas)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.equivocadas.isEmpty())
        // Ahora todas bien.
        ahora = 25_000L
        listOf(1, 2, 3).forEach { par ->
            vm.tocar(fichas.first { it.par == par && it.esPregunta }.id)
            vm.tocar(fichas.first { it.par == par && !it.esPregunta }.id)
        }
        advanceUntilIdle()
        val estado = vm.uiState.value
        assertTrue(estado.terminado)
        // 15 s jugando + 1 s por el error.
        assertEquals(16_000L, estado.tiempoMillis(ahora))
        assertTrue(estado.esRecord)
        assertEquals(16_000L, repositorio.mejoresTiempos.value[3])
    }

    @Test
    fun laMetaDiariaSumaLosBloquesDeHoy() {
        val materias = listOf(Materia(1, "Física", 0, eventos = listOf(
            Evento(1, "Estudio", TipoEvento.BLOQUE_ESTUDIO, hoy, LocalTime.of(9, 0), LocalTime.of(9, 45)),
            Evento(2, "Estudio", TipoEvento.BLOQUE_ESTUDIO, hoy, LocalTime.of(15, 0), LocalTime.of(15, 30)),
            Evento(3, "Ayer", TipoEvento.BLOQUE_ESTUDIO, hoy.minusDays(1), LocalTime.of(9, 0), LocalTime.of(11, 0)),
            Evento(4, "Clase", TipoEvento.CLASE, hoy, LocalTime.of(10, 0), LocalTime.of(12, 0))
        )))
        val minutos = minutosDeEstudioDelDia(materias, hoy)
        assertEquals(75, minutos)
        assertEquals(0.625f, avanceMetaDiaria(minutos, 120), 0.001f)
        assertEquals(1f, avanceMetaDiaria(200, 120), 0.001f)
        assertEquals(0f, avanceMetaDiaria(minutos, 0), 0.001f)
        assertNotNull(materias)
    }
}
