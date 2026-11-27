package com.example.estudia

import com.example.estudia.modelo.ClaseHorario
import com.example.estudia.modelo.ClaseHorarioTexto
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.EventoDeCalendario
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.ResultadoIA
import com.example.estudia.modelo.Tema
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.calendarioIcs
import com.example.estudia.modelo.eventoDeClase
import com.example.estudia.modelo.limpiarHorario
import com.example.estudia.modelo.materiaParecida
import com.example.estudia.modelo.plegarLineaIcs
import com.example.estudia.viewmodel.AsistenteViewModel
import com.example.estudia.viewmodel.Aviso
import com.example.estudia.viewmodel.CalendarioViewModel
import com.example.estudia.viewmodel.Mensaje
import com.example.estudia.viewmodel.SesionViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// Tests del horario desde una foto, el archivo .ics del calendario y la
// guía de primera vez.
@OptIn(ExperimentalCoroutinesApi::class)
class HorarioYCalendarioTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    // 26/11/2026 es jueves.
    private val hoy = LocalDate.of(2026, 11, 26)
    private val fisica = Materia(1, "Física I", 0xFF6C5CE7)
    private fun usuario(vararg materias: Materia) =
        Usuario(id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana", materias = materias.toList())

    // ---------- Horario desde una foto ----------

    @Test
    fun delHorarioSeSacanLasClasesInvalidas() {
        val leidas = listOf(
            ClaseHorarioTexto("Análisis  II", "Teórico", "Miércoles", "18:00", "21:00", "Aula 5"),
            ClaseHorarioTexto("Física I", "", "lunes", "8.30", "10:30", " "),
            ClaseHorarioTexto("Física I", "Práctico", "LUNES", "08:30", "11:00", ""),   // repetida (mismo día y hora)
            ClaseHorarioTexto("Química", "Teórico", "Feriado", "10:00", "12:00", ""),   // día que no existe
            ClaseHorarioTexto("Química", "Teórico", "Martes", "12:00", "10:00", ""),    // termina antes de empezar
            ClaseHorarioTexto(" ", "Teórico", "Martes", "10:00", "12:00", ""),          // sin materia
            ClaseHorarioTexto("Inglés", "Clase", "Sabado", "25:00", "26:00", "")       // hora rota
        )
        val clases = limpiarHorario(leidas)
        assertEquals(listOf("Física I", "Análisis II"), clases.map { it.materia })
        // El tipo vacío queda como "Clase", la hora con punto se entiende y el lugar vacío es null.
        assertEquals(ClaseHorario("Física I", "Clase", DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 30), null), clases.first())
        assertEquals("Aula 5", clases.last().lugar)
    }

    @Test
    fun laClaseVaALaMateriaQueYaEstaCargada() {
        val materias = listOf(fisica, Materia(2, "Análisis Matemático II", 0))
        assertEquals(1, materiaParecida("FISICA I", materias)?.id)
        assertEquals(1, materiaParecida("Física", materias)?.id)
        // Nombres distintos no se mezclan.
        assertNull(materiaParecida("Análisis II", materias))
        assertNull(materiaParecida("Fí", materias))
    }

    @Test
    fun laClaseEmpiezaElPrimerDiaQueToca() {
        val lunes = ClaseHorario("Física I", "Teórico", DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0), "Aula 2")
        val evento = eventoDeClase(lunes, hoy)
        assertEquals(LocalDate.of(2026, 11, 30), evento.fecha)
        assertTrue(evento.semanal)
        assertEquals(TipoEvento.CLASE, evento.tipo)
        assertEquals("Aula 2", evento.lugar)
        // Si es hoy, empieza hoy.
        assertEquals(hoy, eventoDeClase(lunes.copy(dia = DayOfWeek.THURSDAY), hoy).fecha)
    }

    @Test
    fun guardarElHorarioCreaLasMateriasQueFaltan() = runTest {
        val repositorio = RepositorioFalso(usuario(fisica))
        val ia = AsistenteFalso().apply {
            horario = ResultadoIA.Ok(
                listOf(
                    ClaseHorarioTexto("Física I", "Teórico", "LUNES", "08:00", "10:00", ""),
                    ClaseHorarioTexto("Química General", "Teórico", "MARTES", "14:00", "16:00", "Aula 1"),
                    ClaseHorarioTexto("Química General", "Práctico", "JUEVES", "14:00", "16:00", "")
                )
            )
        }
        val avisosSistema = AvisosFalso()
        val vm = AsistenteViewModel(
            repositorio, ia, ClavesFalsas(), "clave-app", hoy = { hoy }, avisosSistema = avisosSistema, leerFoto = { byteArrayOf(1) }
        )
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.leerHorario("content://horario")
        advanceUntilIdle()
        // A la IA se le pasan las materias cargadas, para que use esos nombres.
        assertTrue(ia.pedidosHorario.single().contains("Física I"))
        val propuesto = vm.uiState.value.horarioPropuesto!!
        assertEquals(listOf(1, null, null), propuesto.map { it.materia?.id })

        vm.guardarHorario(propuesto)
        advanceUntilIdle()
        assertNull(vm.uiState.value.horarioPropuesto)
        val materias = repositorio.usuario.value!!.materias
        // Química se creó una sola vez, con sus dos clases.
        assertEquals(listOf("Física I", "Química General"), materias.map { it.nombre })
        assertEquals(1, materias[0].eventos.size)
        assertEquals(2, materias[1].eventos.size)
        assertTrue(materias.flatMap { it.eventos }.all { it.semanal && it.tipo == TipoEvento.CLASE })
        assertEquals(Mensaje.AVISO_HORARIO_IA_GUARDADO, avisos.last().mensaje)
        assertEquals(listOf(3, 1), avisos.last().argumentos)
    }

    @Test
    fun siLaFotoNoEsUnHorarioQuedaElError() = runTest {
        val ia = AsistenteFalso()   // devuelve la lista vacía
        val vm = AsistenteViewModel(RepositorioFalso(usuario(fisica)), ia, ClavesFalsas(), "clave-app", hoy = { hoy }, leerFoto = { byteArrayOf(1) })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.leerHorario("content://gato")
        advanceUntilIdle()
        assertNull(vm.uiState.value.horarioPropuesto)
        assertEquals(com.example.estudia.modelo.ErrorIA.OTRO, vm.uiState.value.error)
    }

    // ---------- Archivo .ics ----------

    @Test
    fun elIcsTieneLosEventosConSuRepeticion() {
        val clase = Evento(
            7, "Teórico", TipoEvento.CLASE, LocalDate.of(2026, 3, 2), LocalTime.of(18, 0), LocalTime.of(20, 0),
            semanal = true, repetirHasta = LocalDate.of(2026, 7, 3), lugar = "Aula 5, sede centro"
        )
        val parcial = Evento(
            8, "Parcial", TipoEvento.EXAMEN, LocalDate.of(2026, 12, 2), LocalTime.of(9, 0), LocalTime.of(11, 0),
            temas = listOf(Tema(1, "Integrales"), Tema(2, "Series"))
        )
        val ics = calendarioIcs(
            listOf(EventoDeCalendario(clase, "Análisis II"), EventoDeCalendario(parcial, "Análisis II")),
            "Estudia+", ZoneId.of("America/Argentina/Buenos_Aires"), Instant.EPOCH
        )
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"))
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"))
        assertEquals(2, Regex("BEGIN:VEVENT").findAll(ics).count())
        assertTrue(ics.contains("DTSTART;TZID=America/Argentina/Buenos_Aires:20260302T180000\r\n"))
        // Argentina es UTC-3: el último día termina a las 02:59:59 UTC del día siguiente.
        assertTrue(ics.contains("RRULE:FREQ=WEEKLY;UNTIL=20260704T025959Z\r\n"))
        assertTrue(ics.contains("SUMMARY:Teórico · Análisis II\r\n"))
        // La coma se escapa y el temario va en la descripción.
        assertTrue(ics.contains("LOCATION:Aula 5\\, sede centro\r\n"))
        assertTrue(ics.contains("DESCRIPTION:- Integrales\\n- Series\r\n"))
        assertEquals(1, Regex("RRULE").findAll(ics).count())
    }

    @Test
    fun lasLineasLargasSeCortanSinPasarse75Bytes() {
        val linea = "SUMMARY:" + "Matemática ".repeat(20)
        val plegada = plegarLineaIcs(linea)
        val partes = plegada.split("\r\n")
        assertTrue(partes.size > 1)
        assertTrue(partes.all { it.toByteArray(Charsets.UTF_8).size <= 75 })
        assertTrue(partes.drop(1).all { it.startsWith(" ") })
        // Juntando los pedazos (sin el espacio) vuelve la línea original.
        assertEquals(linea, partes.first() + partes.drop(1).joinToString("") { it.drop(1) })
    }

    @Test
    fun exportarYDejarDeSincronizarAvisan() = runTest {
        val hoyReal = LocalDate.now()
        val eventos = listOf(
            Evento(1, "Teórico", TipoEvento.CLASE, hoyReal, LocalTime.of(9, 0), LocalTime.of(11, 0), semanal = true),
            Evento(2, "Parcial", TipoEvento.EXAMEN, hoyReal.plusDays(5), LocalTime.of(18, 0), LocalTime.of(20, 0))
        )
        val repositorio = RepositorioFalso(usuario(Materia(1, "Física", 0, eventos = eventos)))
        val vm = CalendarioViewModel(repositorio, AvisosFalso())
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.exportarIcs("content://archivo.ics", "Estudia+")
        advanceUntilIdle()
        assertEquals(Mensaje.AVISO_CALENDARIO_ICS_GUARDADO, avisos.last().mensaje)
        assertEquals(listOf(2), avisos.last().argumentos)
        assertTrue(repositorio.icsGuardado!!.contains("SUMMARY:Parcial · Física"))

        // Pasar al teléfono deja activada la sincronización automática.
        vm.sincronizarConTelefono("Estudia+")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.calendarioAutomatico)
        vm.dejarDeSincronizar()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.calendarioAutomatico)
        assertEquals(Mensaje.AVISO_CALENDARIO_DESACTIVADO, avisos.last().mensaje)
    }

    // ---------- Guía de primera vez ----------

    @Test
    fun laGuiaSaleUnaVezYSoloSinMaterias() = runTest {
        val repositorio = RepositorioFalso(usuario())
        val vm = SesionViewModel(repositorio, AvisosFalso())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.mostrarGuia.collect {} }
        advanceUntilIdle()
        assertTrue(vm.mostrarGuia.value)
        vm.cerrarGuia()
        advanceUntilIdle()
        assertFalse(vm.mostrarGuia.value)

        // Alguien que ya tiene materias cargadas no la ve.
        val conMaterias = SesionViewModel(RepositorioFalso(usuario(fisica)), AvisosFalso())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { conMaterias.mostrarGuia.collect {} }
        advanceUntilIdle()
        assertFalse(conMaterias.mostrarGuia.value)
    }
}
