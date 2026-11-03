package com.example.estudia

import com.example.estudia.modelo.Calificacion
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.notificaciones.momentoDelAviso
import com.example.estudia.ui.componentes.textoHoras
import com.example.estudia.ui.componentes.textoNota
import com.example.estudia.viewmodel.Mensaje
import com.example.estudia.viewmodel.Aviso
import com.example.estudia.viewmodel.MateriasViewModel
import com.example.estudia.viewmodel.calcularEstadisticas
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

// Tests de las notas de los exámenes, las estadísticas (racha y semanas) y
// el momento en que se avisa cada clase.
@OptIn(ExperimentalCoroutinesApi::class)
class EstadisticasYCalificacionesTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    // Martes 3 de noviembre de 2026.
    private val hoy = LocalDate.of(2026, 11, 3)

    private fun estudio(fecha: LocalDate, horas: Long = 1) =
        Evento(0, "Estudio", TipoEvento.BLOQUE_ESTUDIO, fecha, LocalTime.of(10, 0), LocalTime.of(10, 0).plusHours(horas))

    private fun usuarioCon(vararg materias: Materia, meta: Int? = null) =
        Usuario(id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana", metaHorasSemanales = meta, materias = materias.toList())

    private fun TestScope.observar(flujo: Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flujo.collect {} }
    }

    private fun TestScope.recolectarAvisos(flujo: Flow<Aviso>): List<Aviso> {
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flujo.collect { avisos += it } }
        return avisos
    }

    // ---------- Estadísticas ----------

    @Test
    fun rachaCuentaLosDiasSeguidosHastaHoy() {
        val fisica = Materia(
            1, "Física", 0,
            eventos = listOf(
                estudio(hoy), estudio(hoy.minusDays(1)), estudio(hoy.minusDays(2)),
                // Un tramo viejo más largo: es la mejor racha.
                estudio(hoy.minusDays(10)), estudio(hoy.minusDays(11)), estudio(hoy.minusDays(12)), estudio(hoy.minusDays(13)),
                // Planificado para mañana: todavía no cuenta.
                estudio(hoy.plusDays(1))
            )
        )
        val estado = calcularEstadisticas(usuarioCon(fisica), hoy)
        assertEquals(3, estado.rachaActual)
        assertEquals(4, estado.mejorRacha)
        assertTrue(estado.estudioHoy)
    }

    @Test
    fun siHoyNoEstudioLaRachaDeAyerSigue() {
        val fisica = Materia(1, "Física", 0, eventos = listOf(estudio(hoy.minusDays(1)), estudio(hoy.minusDays(2))))
        val estado = calcularEstadisticas(usuarioCon(fisica), hoy)
        assertEquals(2, estado.rachaActual)

        val sinEstudiarAyer = calcularEstadisticas(usuarioCon(fisica), hoy.plusDays(1))
        assertEquals(0, sinEstudiarAyer.rachaActual)
    }

    @Test
    fun horasPorSemanaDeLasUltimas8() {
        val lunes = LocalDate.of(2026, 11, 2)
        val fisica = Materia(
            1, "Física", 0,
            eventos = listOf(estudio(hoy, horas = 2), estudio(lunes.minusDays(1), horas = 3), estudio(lunes.minusWeeks(9)))
        )
        val estado = calcularEstadisticas(usuarioCon(fisica, meta = 5), hoy)
        assertEquals(8, estado.semanas.size)
        assertEquals(lunes, estado.semanas.last().lunes)
        assertEquals(120L, estado.minutosEstaSemana)
        assertEquals(180L, estado.semanas[6].minutos)          // la semana anterior (el domingo)
        assertEquals(300L, estado.semanas.sumOf { it.minutos })  // la de hace 9 semanas no entra
        assertEquals(5, estado.metaHorasSemanales)
    }

    @Test
    fun promedioGeneralYPorMateria() {
        val fisica = Materia(1, "Física", 0, calificaciones = listOf(Calificacion(1, "P1", 3.0, hoy), Calificacion(2, "Recu", 6.0, hoy)))
        val quimica = Materia(2, "Química", 0, calificaciones = listOf(Calificacion(3, "P1", 9.0, hoy)))
        val historia = Materia(3, "Historia", 0)
        val estado = calcularEstadisticas(usuarioCon(fisica, quimica, historia), hoy)
        assertEquals(6.0, estado.promedioGeneral!!, 0.001)
        assertEquals(listOf("Física" to 4.5, "Química" to 9.0), estado.promediosPorMateria.map { it.first.nombre to it.second })
        assertNull(historia.promedio())
    }

    @Test
    fun textosDeNotasYHoras() {
        assertEquals("9", textoNota(9.0))
        assertEquals("7,5", textoNota(7.5))
        assertEquals("6,33", textoNota(19.0 / 3))
        assertEquals("1,5h", textoHoras(90))
        assertEquals("2h", textoHoras(120))
        assertEquals("0", textoHoras(0))
        assertEquals("2h", textoHoras(121))
    }

    // ---------- Calificaciones en el ViewModel ----------

    @Test
    fun guardarCalificacionValidaLaNota() = runTest {
        val repositorio = RepositorioFalso(usuarioCon(Materia(1, "Física", 0)))
        val avisosSistema = AvisosFalso()
        val vm = MateriasViewModel(repositorio, avisosSistema)
        observar(vm.uiState)

        assertEquals(Mensaje.ERROR_TITULO_CALIFICACION, vm.guardarCalificacion(1, null, " ", "7", hoy))
        assertEquals(Mensaje.ERROR_NOTA_INVALIDA, vm.guardarCalificacion(1, null, "Parcial", "11", hoy))
        assertEquals(Mensaje.ERROR_NOTA_INVALIDA, vm.guardarCalificacion(1, null, "Parcial", "siete", hoy))
        assertNull(vm.guardarCalificacion(1, null, "Parcial", "7,5", hoy))

        val guardada = repositorio.usuario.value!!.materias[0].calificaciones.single()
        assertEquals(7.5, guardada.nota, 0.001)
        assertTrue(guardada.aprobada)
    }

    @Test
    fun eliminarCalificacionSePuedeDeshacer() = runTest {
        val parcial = Calificacion(5, "Parcial", 8.0, hoy)
        val repositorio = RepositorioFalso(usuarioCon(Materia(1, "Física", 0, calificaciones = listOf(parcial))))
        val avisosSistema = AvisosFalso()
        val vm = MateriasViewModel(repositorio, avisosSistema)
        observar(vm.uiState)
        val avisos = recolectarAvisos(vm.avisos)

        vm.eliminarCalificacion(1, parcial)
        assertTrue(repositorio.usuario.value!!.materias[0].calificaciones.isEmpty())
        avisos.single().alDeshacer!!.invoke()
        assertEquals(listOf(parcial), repositorio.usuario.value!!.materias[0].calificaciones)
    }

    // ---------- Aviso antes de cada clase ----------

    @Test
    fun laClaseSemanalSeAvisa15MinutosAntesDeLaProxima() {
        val martes = hoy
        val clase = Evento(1, "Teórico", TipoEvento.CLASE, martes.minusWeeks(2), LocalTime.of(9, 0), LocalTime.of(11, 0), semanal = true)

        // Antes de las 8:45 de hoy: se avisa hoy.
        assertEquals(martes.atTime(8, 45), momentoDelAviso(clase, martes.atTime(7, 0)))
        // Pasadas las 8:45: la clase de hoy ya no llega, se avisa la del martes que viene.
        assertEquals(martes.plusWeeks(1).atTime(8, 45), momentoDelAviso(clase, martes.atTime(8, 50)))
        // Con fecha de fin, después de la última clase no hay más avisos.
        assertNull(momentoDelAviso(clase.copy(repetirHasta = martes), martes.atTime(10, 0)))
    }

    @Test
    fun losExamenesSiguenAvisandoseLaNocheAnterior() {
        val examen = Evento(2, "Parcial", TipoEvento.EXAMEN, hoy.plusDays(2), LocalTime.of(9, 0), LocalTime.of(11, 0))
        assertEquals(hoy.plusDays(1).atTime(20, 0), momentoDelAviso(examen, hoy.atTime(12, 0)))
        assertNull(momentoDelAviso(estudio(hoy.plusDays(1)), hoy.atTime(12, 0)))
    }
}
