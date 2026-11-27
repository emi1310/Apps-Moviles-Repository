package com.example.estudia

import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Feriado
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Prioridad
import com.example.estudia.modelo.Receso
import com.example.estudia.modelo.Repeticion
import com.example.estudia.modelo.Subtarea
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.diasSinClase
import com.example.estudia.modelo.resumenDeLlegada
import com.example.estudia.modelo.siguienteTareaRepetida
import com.example.estudia.viewmodel.Cuadrante
import com.example.estudia.viewmodel.armarMatriz
import com.example.estudia.viewmodel.cuadranteDe
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.time.LocalDateTime
import java.time.LocalTime

// Tareas que se repiten, clases de semana por medio, recesos, la matriz de
// prioridades y el aviso al llegar a la facultad.
@OptIn(ExperimentalCoroutinesApi::class)
class AgendaExtrasTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    // Viernes 27/11/2026.
    private val hoy = LocalDate.of(2026, 11, 27)

    @Test
    fun alCompletarUnaTareaRepetidaSaleLaSiguiente() {
        val lunes = LocalDate.of(2026, 11, 23)
        val tp = Tarea(1, "TP semanal", lunes, prioridad = Prioridad.ALTA, avisoDiasAntes = 1,
            subtareas = listOf(Subtarea(5, "Leer", completada = true)), repeticion = Repeticion.SEMANAL)
        val siguiente = siguienteTareaRepetida(tp, tp.copy(completada = true), hoy)!!
        // Se hizo tarde (el viernes): pasa al lunes que viene, no al de esta semana.
        assertEquals(LocalDate.of(2026, 11, 30), siguiente.fechaLimite)
        assertFalse(siguiente.completada)
        assertEquals(0, siguiente.id)
        assertEquals(Repeticion.SEMANAL, siguiente.repeticion)
        assertEquals(1, siguiente.avisoDiasAntes)
        assertFalse(siguiente.subtareas.single().completada)
        // Desmarcarla, o una que no se repite, no crea nada.
        assertNull(siguienteTareaRepetida(tp.copy(completada = true), tp, hoy))
        assertNull(siguienteTareaRepetida(tp.copy(repeticion = null), tp.copy(completada = true, repeticion = null), hoy))
        assertEquals(LocalDate.of(2026, 12, 23), Repeticion.MENSUAL.siguiente(LocalDate.of(2026, 11, 23)))
    }

    @Test
    fun completarDesdeMateriasCreaLaSiguienteYSoloUnaVez() = runTest {
        val tarea = Tarea(7, "Guía", hoy, repeticion = Repeticion.DIARIA)
        val repositorio = RepositorioFalso(Usuario(1, "ana", MetodoAuth.LOCAL, "Ana", materias = listOf(Materia(3, "Física", 0, tareas = listOf(tarea)))))
        val vm = com.example.estudia.viewmodel.MateriasViewModel(repositorio, AvisosFalso())
        vm.alternarTarea(3, tarea)
        advanceUntilIdle()
        val tareas = repositorio.usuario.value!!.materias.single().tareas
        assertEquals(2, tareas.size)
        val hecha = tareas.first { it.id == 7 }
        assertTrue(hecha.completada)
        // La repetición pasó a la nueva: desmarcar y volver a marcar la vieja no duplica.
        assertNull(hecha.repeticion)
        val nueva = tareas.first { it.id != 7 }
        assertEquals(Repeticion.DIARIA, nueva.repeticion)
        assertFalse(nueva.completada)
        assertNotNull(nueva.fechaLimite)
    }

    @Test
    fun lasClasesDeSemanaPorMedioSalteanUnaSemana() {
        // Lunes 2/11, cada 2 semanas: 2/11, 16/11, 30/11...
        val clase = Evento(1, "Lab", TipoEvento.CLASE, LocalDate.of(2026, 11, 2), LocalTime.of(10, 0), LocalTime.of(12, 0), semanal = true, cadaSemanas = 2)
        assertTrue(clase.ocurreEl(LocalDate.of(2026, 11, 16)))
        assertFalse(clase.ocurreEl(LocalDate.of(2026, 11, 9)))
        assertFalse(clase.ocurreEl(LocalDate.of(2026, 11, 23)))
        assertTrue(clase.ocurreEl(LocalDate.of(2026, 11, 30)))
        // El viernes 27 la próxima es el lunes 30 (el 23 no toca).
        assertEquals(LocalDateTime.of(2026, 11, 30, 10, 0), clase.proximoInicio(hoy.atTime(8, 0)))
        // Lunes 16 a las 11: la de ese día ya empezó, la próxima es la del 30.
        assertEquals(LocalDateTime.of(2026, 11, 30, 10, 0), clase.proximoInicio(LocalDateTime.of(2026, 11, 16, 11, 0)))
        // Las de todas las semanas siguen igual.
        assertTrue(clase.copy(cadaSemanas = 1).ocurreEl(LocalDate.of(2026, 11, 9)))
    }

    @Test
    fun losRecesosSonDiasSinClase() {
        val dias = diasSinClase(
            listOf(Feriado(LocalDate.of(2026, 7, 9), "Independencia")),
            listOf(Receso(1, "Invierno", LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 17)))
        )
        assertEquals(12, dias.size)
        assertEquals("Invierno", dias[LocalDate.of(2026, 7, 6)])
        assertEquals("Independencia", dias[LocalDate.of(2026, 7, 9)])
        assertFalse(LocalDate.of(2026, 7, 18) in dias)
    }

    @Test
    fun laMatrizReparteSegunUrgenteEImportante() {
        assertEquals(Cuadrante.HACER_YA, cuadranteDe(Tarea(1, "a", hoy.plusDays(2), prioridad = Prioridad.ALTA), hoy))
        assertEquals(Cuadrante.PLANIFICAR, cuadranteDe(Tarea(2, "b", hoy.plusDays(3), prioridad = Prioridad.ALTA), hoy))
        assertEquals(Cuadrante.SACAR_RAPIDO, cuadranteDe(Tarea(3, "c", hoy.minusDays(1)), hoy))
        assertEquals(Cuadrante.DESPUES, cuadranteDe(Tarea(4, "d", null), hoy))

        val usuario = Usuario(
            1, "ana", MetodoAuth.LOCAL, "Ana",
            materias = listOf(
                Materia(3, "Física", 0, tareas = listOf(
                    Tarea(1, "Parcial", hoy.plusDays(1), prioridad = Prioridad.ALTA),
                    Tarea(2, "Hecha", hoy, completada = true, prioridad = Prioridad.ALTA),
                    Tarea(3, "Leer", null)
                )),
                Materia(4, "Vieja", 0, archivada = true, tareas = listOf(Tarea(9, "Archivada", hoy, prioridad = Prioridad.ALTA)))
            )
        )
        val matriz = armarMatriz(usuario, hoy)
        assertEquals(2, matriz.total)
        assertEquals(listOf("Parcial"), matriz.cuadrantes.getValue(Cuadrante.HACER_YA).map { it.tarea.titulo })
        assertEquals(listOf("Leer"), matriz.cuadrantes.getValue(Cuadrante.DESPUES).map { it.tarea.titulo })
    }

    @Test
    fun alLlegarSeAvisanLasClasesYLoQueVence() {
        val clase = Evento(1, "Teoría", TipoEvento.CLASE, LocalDate.of(2026, 11, 6), LocalTime.of(14, 0), LocalTime.of(16, 0), semanal = true)
        val usuario = Usuario(
            1, "ana", MetodoAuth.LOCAL, "Ana",
            materias = listOf(
                Materia(3, "Física", 0, eventos = listOf(clase), tareas = listOf(
                    Tarea(1, "TP", hoy.plusDays(1)),
                    Tarea(2, "Informe", hoy.plusDays(5))
                ))
            )
        )
        val resumen = resumenDeLlegada(usuario, hoy, emptySet())
        assertEquals(listOf("14:00 Física"), resumen.clases)
        assertEquals(listOf("TP (Física)"), resumen.tareas)
        // Si hoy es receso, no hay clases (las tareas sí).
        val enReceso = resumenDeLlegada(usuario, hoy, setOf(hoy))
        assertTrue(enReceso.clases.isEmpty())
        assertFalse(enReceso.vacio)
    }
}
