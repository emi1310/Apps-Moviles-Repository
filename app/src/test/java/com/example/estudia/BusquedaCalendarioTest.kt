package com.example.estudia

import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Tema
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.viewmodel.EventoConMateria
import com.example.estudia.viewmodel.buscarEventos
import com.example.estudia.viewmodel.fechaParaMostrar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

// Búsqueda de eventos desde la lupa del dock en el Calendario.
class BusquedaCalendarioTest {

    private val hoy = LocalDate.of(2026, 11, 25) // miércoles

    private fun evento(
        id: Int,
        titulo: String,
        fecha: LocalDate,
        semanal: Boolean = false,
        repetirHasta: LocalDate? = null,
        lugar: String? = null,
        temas: List<Tema> = emptyList()
    ) = Evento(
        id, titulo, TipoEvento.CLASE, fecha, LocalTime.of(10, 0), LocalTime.of(12, 0),
        semanal = semanal, repetirHasta = repetirHasta, lugar = lugar, temas = temas
    )

    private fun conMateria(e: Evento, materia: String = "Física I") = EventoConMateria(e, 1, materia, 0xFF0984E3)

    @Test
    fun buscaEnTituloMateriaLugarYTemarioSinTildes() {
        val eventos = listOf(
            conMateria(evento(1, "Parcial", hoy.plusDays(3), temas = listOf(Tema(1, "Derivadas")))),
            conMateria(evento(2, "Clase", hoy.plusDays(1), lugar = "Aula 12"), materia = "Matemática II"),
            conMateria(evento(3, "Entrega TP", hoy.plusDays(5)))
        )
        assertEquals(listOf(1), buscarEventos(eventos, "DERIVADA", hoy).map { it.item.evento.id })
        assertEquals(listOf(2), buscarEventos(eventos, "matematica", hoy).map { it.item.evento.id })
        assertEquals(listOf(2), buscarEventos(eventos, "aula", hoy).map { it.item.evento.id })
        assertTrue(buscarEventos(eventos, "   ", hoy).isEmpty())
    }

    @Test
    fun primeroLosProximosDespuesLosPasados() {
        val eventos = listOf(
            conMateria(evento(1, "Parcial viejo", hoy.minusDays(20))),
            conMateria(evento(2, "Parcial lejano", hoy.plusDays(30))),
            conMateria(evento(3, "Parcial reciente", hoy.minusDays(2))),
            conMateria(evento(4, "Parcial hoy", hoy))
        )
        assertEquals(listOf(4, 2, 3, 1), buscarEventos(eventos, "parcial", hoy).map { it.item.evento.id })
    }

    @Test
    fun lasClasesSemanalesMuestranLaProximaOLaUltima() {
        // Empezó un lunes de septiembre y sigue: la próxima es el lunes 30/11.
        val sigue = evento(1, "Clase", LocalDate.of(2026, 9, 7), semanal = true)
        assertEquals(LocalDate.of(2026, 11, 30), fechaParaMostrar(sigue, hoy))
        // Ya terminó: se muestra la última clase.
        val termino = evento(2, "Clase", LocalDate.of(2026, 9, 7), semanal = true, repetirHasta = LocalDate.of(2026, 11, 10))
        assertEquals(LocalDate.of(2026, 11, 9), fechaParaMostrar(termino, hoy))
        // Todavía no empezó: la primera.
        val futura = evento(3, "Clase", LocalDate.of(2027, 3, 1), semanal = true)
        assertEquals(LocalDate.of(2027, 3, 1), fechaParaMostrar(futura, hoy))
    }
}
