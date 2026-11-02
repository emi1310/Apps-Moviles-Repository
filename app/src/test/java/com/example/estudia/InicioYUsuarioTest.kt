package com.example.estudia

import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.viewmodel.calcularResumen
import com.example.estudia.viewmodel.semanasDelMes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

// Tests de la lógica que antes estaba dentro de los Composables y ahora
// vive en el ViewModel o en el modelo (funciones puras, sin Android).
class InicioYUsuarioTest {

    private val hoy = LocalDate.of(2026, 11, 4)   // miércoles

    private fun evento(tipo: TipoEvento, fecha: LocalDate, desde: Int, hasta: Int) =
        Evento(0, "E", tipo, fecha, LocalTime.of(desde, 0), LocalTime.of(hasta, 0))

    private val usuario = Usuario(
        id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana Pérez",
        materias = listOf(
            Materia(
                id = 1, nombre = "Física", color = 0,
                tareas = listOf(
                    Tarea(1, "Sin fecha", null),
                    Tarea(2, "Vencida", hoy.minusDays(2)),
                    Tarea(3, "Hecha", hoy, completada = true),
                    Tarea(4, "Para mañana", hoy.plusDays(1))
                ),
                eventos = listOf(
                    evento(TipoEvento.EXAMEN, hoy.plusDays(3), 9, 11),
                    evento(TipoEvento.ENTREGA, hoy.minusDays(1), 9, 10),        // ya pasó
                    evento(TipoEvento.CLASE, hoy.plusDays(1), 9, 10),           // no es examen/entrega
                    evento(TipoEvento.BLOQUE_ESTUDIO, hoy, 18, 20),             // 2 h esta semana
                    evento(TipoEvento.BLOQUE_ESTUDIO, hoy.plusDays(7), 10, 13)  // 3 h la semana que viene
                ),
                notas = listOf(Nota(1, "Vieja", "", hoy.minusDays(9), esFavorita = true), Nota(2, "Nueva", "", hoy, esFavorita = true))
            ),
            Materia(id = 2, nombre = "Archivada", color = 0, archivada = true, tareas = listOf(Tarea(9, "No cuenta", hoy)))
        )
    )

    @Test
    fun lasPendientesSeOrdenanPorFechaYExcluyenArchivadasYCompletadas() {
        val resumen = calcularResumen(usuario, hoy)
        assertEquals(listOf("Vencida", "Para mañana", "Sin fecha"), resumen.tareasPendientes.map { it.tarea.titulo })
        assertEquals("Ana", resumen.nombre)
    }

    @Test
    fun proximosEventosSoloIncluyeExamenesYEntregasFuturas() {
        val resumen = calcularResumen(usuario, hoy)
        assertEquals(1, resumen.proximosEventos.size)
        assertEquals(TipoEvento.EXAMEN, resumen.proximosEventos.first().evento.tipo)
    }

    @Test
    fun laNotaDestacadaEsLaFavoritaMasReciente() {
        assertEquals("Nueva", calcularResumen(usuario, hoy).notaDestacada?.titulo)
    }

    @Test
    fun horasDeEstudioTotalesYDeLaSemana() {
        assertEquals(5, usuario.calcularHorasEstudio())
        assertEquals(2, usuario.horasEstudioDeLaSemana(hoy))
    }

    @Test
    fun tareaVencida() {
        assertTrue(Tarea(1, "x", hoy.minusDays(1)).estaVencida(hoy))
        assertFalse(Tarea(1, "x", hoy).estaVencida(hoy))
        assertFalse(Tarea(1, "x", hoy.minusDays(1), completada = true).estaVencida(hoy))
        assertFalse(Tarea(1, "x", null).estaVencida(hoy))
    }

    @Test
    fun grillaDelMesEmpiezaEnDomingo() {
        // Noviembre de 2026 empieza un domingo: no hay casilleros vacíos antes.
        val noviembre = semanasDelMes(YearMonth.of(2026, 11))
        assertEquals(LocalDate.of(2026, 11, 1), noviembre.first().first())
        // Octubre de 2026 empieza un jueves: 4 casilleros vacíos (D, L, M, M).
        val octubre = semanasDelMes(YearMonth.of(2026, 10))
        assertNull(octubre.first()[3])
        assertEquals(LocalDate.of(2026, 10, 1), octubre.first()[4])
        assertTrue(octubre.all { it.size == 7 })
    }
}
