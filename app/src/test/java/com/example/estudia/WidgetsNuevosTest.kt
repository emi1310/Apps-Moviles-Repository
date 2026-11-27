package com.example.estudia

import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.Tema
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.widget.TareaEnWidget
import com.example.estudia.widget.armarResumenWidget
import com.example.estudia.widget.proximoExamen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

// Tests de los widgets nuevos: las tareas con sus ids (para completarlas
// desde el widget) y el próximo examen.
class WidgetsNuevosTest {

    private val hoy = LocalDate.of(2026, 11, 27)
    private val nueve = LocalTime.of(9, 0)
    private val once = LocalTime.of(11, 0)

    private fun usuario(vararg materias: Materia) =
        Usuario(id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana", materias = materias.toList())

    @Test
    fun lasTareasDelWidgetLlevanSusIds() {
        val fisica = Materia(4, "Física", 0, tareas = listOf(Tarea(10, "TP 1", hoy), Tarea(11, "TP 2", hoy.plusDays(2))))
        val resumen = armarResumenWidget(usuario(fisica), hoy)
        assertEquals(
            listOf(TareaEnWidget(10, 4, "TP 1 · Física"), TareaEnWidget(11, 4, "TP 2 · Física")),
            resumen.tareas
        )
        assertEquals(resumen.tareas.map { it.texto }, resumen.proximas)
    }

    @Test
    fun elProximoExamenEsElMasCercanoDeHoyEnAdelante() {
        val fisica = Materia(
            1, "Física", 0,
            eventos = listOf(
                Evento(1, "Parcial viejo", TipoEvento.EXAMEN, hoy.minusDays(2), nueve, once),
                Evento(2, "Parcial 2", TipoEvento.EXAMEN, hoy.plusDays(5), nueve, once,
                    temas = listOf(Tema(1, "Cinemática", estudiado = true), Tema(2, "Dinámica"), Tema(3, "Energía", estudiado = true))),
                Evento(3, "Entrega", TipoEvento.ENTREGA, hoy.plusDays(1), nueve, once)
            )
        )
        val quimica = Materia(2, "Química", 0, eventos = listOf(Evento(4, "Final", TipoEvento.EXAMEN, hoy.plusDays(9), nueve, once)))
        val archivada = Materia(3, "Vieja", 0, archivada = true, eventos = listOf(Evento(5, "No va", TipoEvento.EXAMEN, hoy, nueve, once)))

        val examen = proximoExamen(usuario(fisica, quimica, archivada), hoy)!!
        assertEquals("Parcial 2", examen.titulo)
        assertEquals("Física", examen.materia)
        assertEquals(5L, examen.dias)
        assertEquals(2, examen.temasEstudiados)
        assertEquals(3, examen.temasTotal)
    }

    @Test
    fun sinExamenesNoHayProximo() {
        assertNull(proximoExamen(usuario(Materia(1, "Física", 0)), hoy))
    }
}
