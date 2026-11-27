package com.example.estudia

import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.EventoDeCalendario
import com.example.estudia.modelo.FilaCalendario
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.aplicarCambioDeCalendario
import com.example.estudia.modelo.duracionEnMinutos
import com.example.estudia.modelo.estadoSilencioClase
import com.example.estudia.modelo.filaDeEvento
import com.example.estudia.modelo.planSincronizacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

// Silenciar en clase y la sincronización en dos sentidos con el calendario.
class ComodidadTest {

    // Viernes 27/11/2026.
    private val hoy = LocalDate.of(2026, 11, 27)
    private val zona = ZoneId.of("America/Argentina/Buenos_Aires")

    private fun usuarioConClases(vararg eventos: Evento) =
        Usuario(1, "ana", MetodoAuth.LOCAL, "Ana", materias = listOf(Materia(1, "Física", 0, eventos = eventos.toList())))

    @Test
    fun elSilencioEmpiezaYTerminaConLasClases() {
        val teoria = Evento(1, "Teoría", TipoEvento.CLASE, LocalDate.of(2026, 11, 6), LocalTime.of(14, 0), LocalTime.of(16, 0), semanal = true)
        // Práctica pegada a la teoría: un solo silencio de 14 a 18.
        val practica = Evento(2, "Práctica", TipoEvento.CLASE, hoy, LocalTime.of(16, 0), LocalTime.of(18, 0))
        val entrega = Evento(3, "TP", TipoEvento.ENTREGA, hoy, LocalTime.of(9, 0), LocalTime.of(10, 0))
        val usuario = usuarioConClases(teoria, practica, entrega)

        // A la mañana: la entrega no silencia; el próximo cambio es a las 14.
        val manana = estadoSilencioClase(usuario, hoy.atTime(9, 30), emptySet())
        assertFalse(manana.enClase)
        assertEquals(hoy.atTime(14, 0), manana.proximoCambio)
        // En clase: se silencia hasta que termina la práctica.
        val enClase = estadoSilencioClase(usuario, hoy.atTime(15, 0), emptySet())
        assertTrue(enClase.enClase)
        assertEquals(hoy.atTime(18, 0), enClase.proximoCambio)
        // Al terminar, el próximo es la teoría del viernes que viene.
        val despues = estadoSilencioClase(usuario, hoy.atTime(18, 0), emptySet())
        assertFalse(despues.enClase)
        assertEquals(hoy.plusDays(7).atTime(14, 0), despues.proximoCambio)
        // Si hoy es feriado o receso, no hay silencio.
        assertFalse(estadoSilencioClase(usuario, hoy.atTime(15, 0), setOf(hoy)).enClase)
    }

    @Test
    fun loQueCambiaEnLaAppVaAlCalendarioYLoDelCalendarioVuelve() {
        val a = FilaCalendario("Parcial · Física", 1_000, 2_000, null, null)
        val b = FilaCalendario("TP · Física", 3_000, 4_000, null, null)
        val c = FilaCalendario("Clase · Física", 5_000, null, "PT120M", "FREQ=WEEKLY")
        val foto = mapOf(1 to a, 2 to b, 3 to c, 4 to a)
        val plan = planSincronizacion(
            deseados = mapOf(
                1 to a,                                  // igual en los tres
                2 to b.copy(titulo = "TP 2 · Física"),   // cambió en Estudia+
                3 to c,                                  // la movieron en el calendario
                5 to b                                   // nuevo en Estudia+
            ),
            actuales = mapOf(1 to a, 2 to b, 3 to c.copy(inicioMillis = 6_000), 4 to a),
            foto = foto
        )
        assertEquals(listOf(5), plan.insertar)
        assertEquals(listOf(2), plan.actualizar)
        assertEquals(listOf(4), plan.borrar)                     // ya no está en Estudia+
        assertEquals(listOf(3), plan.desdeCalendario.map { it.eventoId })
        assertEquals(6_000L, plan.desdeCalendario.single().fila.inicioMillis)
        // Si lo borraron en el calendario, se vuelve a crear.
        assertEquals(listOf(1), planSincronizacion(mapOf(1 to a), emptyMap(), foto).insertar)
    }

    @Test
    fun unCambioDelCalendarioSePasaAlEvento() {
        val evento = Evento(7, "Parcial", TipoEvento.EXAMEN, hoy, LocalTime.of(10, 0), LocalTime.of(12, 0))
        val fila = filaDeEvento(EventoDeCalendario(evento, "Física"), zona)
        assertEquals("Parcial · Física", fila.titulo)
        // Lo pasaron al lunes de 9 a 11 y le cambiaron el nombre.
        val lunes = LocalDateTime.of(2026, 11, 30, 9, 0).atZone(zona).toInstant().toEpochMilli()
        val movido = fila.copy(titulo = "Primer parcial · Física", inicioMillis = lunes, finMillis = lunes + 2 * 3_600_000)
        val cambiado = aplicarCambioDeCalendario(evento, "Física", movido, zona)!!
        assertEquals("Primer parcial", cambiado.titulo)
        assertEquals(LocalDate.of(2026, 11, 30), cambiado.fecha)
        assertEquals(LocalTime.of(9, 0), cambiado.horaInicio)
        assertEquals(LocalTime.of(11, 0), cambiado.horaFin)
        // Si termina otro día, no se toma.
        assertNull(aplicarCambioDeCalendario(evento, "Física", movido.copy(finMillis = lunes + 20 * 3_600_000), zona))
        // Las clases de semana por medio llevan INTERVAL=2.
        val lab = Evento(8, "Lab", TipoEvento.CLASE, hoy, LocalTime.of(8, 0), LocalTime.of(9, 30), semanal = true, cadaSemanas = 2)
        val filaLab = filaDeEvento(EventoDeCalendario(lab, "Física"), zona)
        assertEquals("FREQ=WEEKLY;INTERVAL=2", filaLab.regla)
        assertEquals("PT90M", filaLab.duracion)
        assertEquals(90L, duracionEnMinutos("PT90M"))
        assertEquals(90L, duracionEnMinutos("P5400S"))
        assertEquals(60L, duracionEnMinutos("P0DT1H"))
    }
}
