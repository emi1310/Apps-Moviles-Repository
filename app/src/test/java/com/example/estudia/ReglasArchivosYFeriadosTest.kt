package com.example.estudia

import com.example.estudia.datos.FuenteFeriados
import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.Asistencia
import com.example.estudia.modelo.CategoriaArchivo
import com.example.estudia.modelo.ErrorReglas
import com.example.estudia.modelo.EstadoPlan
import com.example.estudia.modelo.Evaluacion
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Feriado
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MateriaPlan
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.NotaNecesaria
import com.example.estudia.modelo.ReglasCursada
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.calcularNotas
import com.example.estudia.modelo.clasesEntre
import com.example.estudia.modelo.leerReglas
import com.example.estudia.modelo.normalizarEnlace
import com.example.estudia.modelo.resumenAsistencia
import com.example.estudia.ui.componentes.AccionRapida
import com.example.estudia.ui.componentes.accionDelBoton
import com.example.estudia.ui.componentes.iconoAccion
import com.example.estudia.ui.navegacion.Screen
import com.example.estudia.viewmodel.Aviso
import com.example.estudia.viewmodel.CalendarioViewModel
import com.example.estudia.viewmodel.MateriasViewModel
import com.example.estudia.viewmodel.Mensaje
import com.example.estudia.viewmodel.armarAsistencia
import com.example.estudia.viewmodel.armarFinales
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

// Tests de la versión 10: ícono del "+", reglas de cada materia (asistencia,
// promoción y final), archivos por categoría, clases virtuales y feriados.
@OptIn(ExperimentalCoroutinesApi::class)
class ReglasArchivosYFeriadosTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    private val hoy = LocalDate.of(2026, 11, 25)

    private fun usuario(materias: List<Materia> = emptyList(), plan: List<MateriaPlan> = emptyList()) = Usuario(
        id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana",
        materias = materias, planEstudio = plan
    )

    // ---------- Botón "+" del dock ----------

    @Test
    fun cadaPantallaTieneSuIconoEnElBotonCrear() {
        val acciones = listOf(Screen.Materias.ruta, Screen.Calendario.ruta, Screen.Notas.ruta, Screen.Asistente.ruta, Screen.Inicio.ruta).map { accionDelBoton(it) }
        assertEquals(listOf(AccionRapida.MATERIA, AccionRapida.EVENTO, AccionRapida.NOTA, AccionRapida.GEMINI, AccionRapida.ESTUDIAR), acciones)
        // Los cinco íconos son distintos.
        assertEquals(5, AccionRapida.entries.map { iconoAccion(it) }.toSet().size)
    }

    // ---------- Reglas de la cursada ----------

    @Test
    fun seLeenYSeValidanLasReglasDeLaMateria() {
        assertEquals(ReglasCursada(80, true, 8.0, false), leerReglas("80 %", true, "8", false).reglas)
        assertEquals(ReglasCursada(0, false, 7.0, true), leerReglas("0", false, "cualquier cosa", true).reglas)
        assertEquals(ErrorReglas.ASISTENCIA, leerReglas("120", true, "7", true).error)
        assertEquals(ErrorReglas.ASISTENCIA, leerReglas("", true, "7", true).error)
        assertEquals(ErrorReglas.NOTA_PROMOCION, leerReglas("75", true, "3", true).error)
        assertEquals(6.5, leerReglas("75", true, "6,5", true).reglas!!.notaPromocion, 0.001)
    }

    private fun claseConAsistencias(presentes: Int, ausentes: Int, hasta: LocalDate? = null): Evento {
        val inicio = hoy.minusWeeks((presentes + ausentes).toLong())
        val asistencias = (0 until presentes + ausentes).map { i -> Asistencia(i, inicio.plusWeeks(i.toLong()), i < presentes) }
        return Evento(1, "Clase", TipoEvento.CLASE, inicio, LocalTime.of(10, 0), LocalTime.of(12, 0), semanal = true, repetirHasta = hasta, asistencias = asistencias)
    }

    @Test
    fun laAsistenciaUsaElMinimoDeCadaMateria() {
        val clase = claseConAsistencias(presentes = 6, ausentes = 2)
        // Con 75 %: 6 * 100 / 75 - 8 = 0 faltas más.
        assertEquals(0, resumenAsistencia(Materia(1, "Física", 0, eventos = listOf(clase))).faltasDisponibles)
        // Con 50 %: 6 * 100 / 50 - 8 = 4 faltas más.
        val resumen = resumenAsistencia(Materia(1, "Física", 0, eventos = listOf(clase), asistenciaMinima = 50))
        assertEquals(50, resumen.minimo)
        assertEquals(4, resumen.faltasDisponibles)
        assertFalse(resumen.enRiesgo)
        // Si no pide asistencia, nunca está en riesgo.
        val libre = resumenAsistencia(Materia(1, "Física", 0, eventos = listOf(claseConAsistencias(0, 5)), asistenciaMinima = 0))
        assertFalse(libre.enRiesgo)
        assertFalse(libre.debajoDelMinimo)
    }

    @Test
    fun losFeriadosNoSonClaseNiCuentanEnLaAsistencia() {
        val clase = Evento(1, "Clase", TipoEvento.CLASE, hoy.minusWeeks(3), LocalTime.of(10, 0), LocalTime.of(12, 0), semanal = true, repetirHasta = hoy.plusWeeks(6),
            asistencias = listOf(Asistencia(1, hoy.minusWeeks(1), false)))
        val materia = Materia(1, "Física", 0, eventos = listOf(clase))
        val feriados = setOf(hoy.minusWeeks(1))
        assertEquals(2, clasesEntre(materia, hoy.minusDays(13), hoy).size)
        assertEquals(1, clasesEntre(materia, hoy.minusDays(13), hoy, feriados).size)
        // El ausente marcado ese día ya no cuenta y el total baja en una clase.
        val sin = resumenAsistencia(materia)
        val con = resumenAsistencia(materia, feriados)
        assertEquals(1, sin.ausentes)
        assertEquals(0, con.ausentes)
        assertEquals(sin.clasesTotales!! - 1, con.clasesTotales)
        // Y en la pantalla tampoco aparece para marcar.
        val estado = armarAsistencia(usuario(listOf(materia)), hoy, feriados)
        assertTrue(estado.clases.none { it.fecha == hoy.minusWeeks(1) })
    }

    @Test
    fun siNoEsPromocionalSoloCalculaParaAprobar() {
        val evaluaciones = listOf(Evaluacion(1, "P1", 50, 8.0), Evaluacion(2, "P2", 50, null))
        assertNull(calcularNotas(evaluaciones, null).paraPromocionar)
        // Promoción con 6: con 8 en la mitad ya necesita solo 4 en lo que falta.
        assertEquals(NotaNecesaria.Necesita(4.0), calcularNotas(evaluaciones, 6.0).paraPromocionar)
    }

    @Test
    fun lasMateriasSinFinalNoSeSugierenEnFinales() {
        val plan = listOf(MateriaPlan(1, "Física", 1, EstadoPlan.REGULAR), MateriaPlan(2, "Química", 1, EstadoPlan.REGULAR))
        val materias = listOf(Materia(1, "Física", 0, tieneFinal = false), Materia(2, "Química", 0))
        assertEquals(listOf("Química"), armarFinales(usuario(materias, plan), hoy).sugerencias)
    }

    @Test
    fun alCrearYEditarUnaMateriaSeGuardanSusReglas() = runTest {
        val repositorio = RepositorioFalso(usuario())
        val vm = MateriasViewModel(repositorio, AvisosFalso())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        vm.agregarMateria("Física", ReglasCursada(80, false, 7.0, true))
        advanceUntilIdle()
        var fisica = repositorio.usuarioActual.first()!!.materias.single()
        assertEquals(80, fisica.asistenciaMinima)
        assertFalse(fisica.promocional)

        vm.editarMateria(fisica, "Física I", ReglasCursada(60, true, 8.0, false))
        advanceUntilIdle()
        fisica = repositorio.usuarioActual.first()!!.materias.single()
        assertEquals("Física I", fisica.nombre)
        assertEquals(ReglasCursada(60, true, 8.0, false), ReglasCursada(fisica.asistenciaMinima, fisica.promocional, fisica.notaPromocion, fisica.tieneFinal))
    }

    // ---------- Archivos por categoría ----------

    @Test
    fun losArchivosSeAdjuntanYSeMuevenDeCategoria() = runTest {
        val archivo = Archivo(7, "programa.pdf", "application/pdf", "p.pdf", 10, hoy)
        val repositorio = RepositorioFalso(usuario(listOf(Materia(1, "Física", 0, archivos = listOf(archivo)))))
        val vm = MateriasViewModel(repositorio, AvisosFalso())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.adjuntarArchivo(1, "content://apunte", CategoriaArchivo.TEORIA)
        advanceUntilIdle()
        assertEquals(CategoriaArchivo.TEORIA, repositorio.ultimaCategoriaAdjuntada)

        vm.cambiarCategoriaArchivo(1, archivo, CategoriaArchivo.INFORMACION)
        advanceUntilIdle()
        assertEquals(CategoriaArchivo.INFORMACION, repositorio.usuarioActual.first()!!.materias.single().archivos.single().categoria)
    }

    @Test
    fun unaCategoriaDesconocidaQuedaEnOtros() {
        assertEquals(CategoriaArchivo.TPS, CategoriaArchivo.desdeTexto("TPS"))
        assertEquals(CategoriaArchivo.OTROS, CategoriaArchivo.desdeTexto(null))
        assertEquals(CategoriaArchivo.OTROS, CategoriaArchivo.desdeTexto("APUNTES"))
    }

    // ---------- Clases virtuales ----------

    @Test
    fun elLinkSeCompletaConHttps() {
        assertEquals("https://meet.google.com/abc", normalizarEnlace(" meet.google.com/abc "))
        assertEquals("http://zoom.us/j/1", normalizarEnlace("http://zoom.us/j/1"))
        assertNull(normalizarEnlace("   "))
        assertNull(normalizarEnlace(null))
    }

    @Test
    fun unaClaseVirtualGuardaElLinkYNoElAula() = runTest {
        val repositorio = RepositorioFalso(usuario(listOf(Materia(1, "Física", 0))))
        val vm = CalendarioViewModel(repositorio, AvisosFalso())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        val error = vm.guardarEvento(
            1, null, "Teórico", TipoEvento.CLASE, hoy, LocalTime.of(9, 0), LocalTime.of(11, 0),
            virtual = true, lugar = "Aula 5", enlace = "meet.google.com/xyz"
        )
        assertNull(error)
        vm.guardarEvento(1, null, "Práctico", TipoEvento.CLASE, hoy, LocalTime.of(14, 0), LocalTime.of(16, 0), virtual = false, lugar = " Aula 12 ", enlace = "algo")
        advanceUntilIdle()
        val eventos = repositorio.usuarioActual.first()!!.materias.single().eventos
        val virtual = eventos.first { it.titulo == "Teórico" }
        assertTrue(virtual.virtual)
        assertEquals("https://meet.google.com/xyz", virtual.enlace)
        assertNull(virtual.lugar)
        val presencial = eventos.first { it.titulo == "Práctico" }
        assertFalse(presencial.virtual)
        assertEquals("Aula 12", presencial.lugar)
        assertNull(presencial.enlace)
    }

    // ---------- Feriados ----------

    @Test
    fun seReconoceElCalendarioDeFeriadosDeGoogle() {
        assertTrue(FuenteFeriados.esCalendarioDeFeriados("es.ar#holiday@group.v.calendar.google.com"))
        assertTrue(FuenteFeriados.esCalendarioDeFeriados("Feriados en Argentina"))
        assertFalse(FuenteFeriados.esCalendarioDeFeriados("Estudia+"))
        assertFalse(FuenteFeriados.esCalendarioDeFeriados("ana@gmail.com"))
    }

    @Test
    fun dosFeriadosElMismoDiaSeJuntan() {
        val dia = LocalDate.of(2026, 12, 8)
        val juntos = FuenteFeriados.juntarPorDia(listOf(Feriado(dia, "Inmaculada Concepción"), Feriado(dia, "Puente"), Feriado(dia.minusDays(1), "Otro")))
        assertEquals(listOf(dia.minusDays(1), dia), juntos.map { it.fecha })
        assertEquals("Inmaculada Concepción · Puente", juntos.last().nombre)
    }

    @Test
    fun alTraerLosFeriadosSeMarcanEnElCalendario() = runTest {
        val repositorio = RepositorioFalso(usuario(listOf(Materia(1, "Física", 0))))
        val vm = CalendarioViewModel(repositorio, AvisosFalso())
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        // Sin conexión: avisa el error y no marca nada.
        repositorio.feriadosParaTraer = null
        vm.importarFeriados(usarCalendario = false)
        advanceUntilIdle()
        assertEquals(Mensaje.AVISO_FERIADOS_ERROR, avisos.last().mensaje)
        assertFalse(vm.uiState.value.hayFeriados)

        // Del calendario de Google: el día elegido (hoy) queda como feriado.
        val elegido = vm.uiState.value.seleccionado
        repositorio.feriadosParaTraer = listOf(Feriado(elegido, "Día de prueba"))
        repositorio.feriadosEnElCalendario = true
        vm.importarFeriados(usarCalendario = true)
        advanceUntilIdle()
        assertEquals(Mensaje.AVISO_FERIADOS_CALENDARIO, avisos.last().mensaje)
        assertEquals(listOf<Any>(1), avisos.last().argumentos)
        assertEquals("Día de prueba", vm.uiState.value.feriadoDelDia)
        assertTrue(elegido in vm.uiState.value.feriadosDelMes)

        // Sin permiso: vienen de internet.
        vm.importarFeriados(usarCalendario = false)
        advanceUntilIdle()
        assertEquals(Mensaje.AVISO_FERIADOS_INTERNET, avisos.last().mensaje)
        assertNotEquals(Mensaje.AVISO_FERIADOS_CALENDARIO, avisos.last().mensaje)
    }
}
