package com.example.estudia

import com.example.estudia.modelo.Asistencia
import com.example.estudia.modelo.EstadoPlan
import com.example.estudia.modelo.Evaluacion
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MateriaPlan
import com.example.estudia.modelo.MesaFinal
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.NotaNecesaria
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.Tarjeta
import com.example.estudia.modelo.TipoAvisoFinal
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.avanceCarrera
import com.example.estudia.modelo.avisosDeFinales
import com.example.estudia.modelo.calcularNotas
import com.example.estudia.modelo.clasesEntre
import com.example.estudia.modelo.resumenAsistencia
import com.example.estudia.ui.componentes.GeneradorRuido
import com.example.estudia.ui.componentes.TipoSonido
import com.example.estudia.viewmodel.Aviso
import com.example.estudia.viewmodel.FinalesViewModel
import com.example.estudia.viewmodel.Mensaje
import com.example.estudia.viewmodel.NotaNecesariaViewModel
import com.example.estudia.viewmodel.NotasViewModel
import com.example.estudia.viewmodel.QuizViewModel
import com.example.estudia.viewmodel.TipoResultado
import com.example.estudia.viewmodel.armarQuiz
import com.example.estudia.modelo.armarResumenSemanal
import com.example.estudia.viewmodel.buscarEnTodo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

// Tests de la tanda 4: Mi carrera, asistencia, ¿qué nota necesito?, finales,
// quiz, resumen semanal, búsqueda global, sonidos y exportar a PDF.
@OptIn(ExperimentalCoroutinesApi::class)
class CursadaYHerramientasTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    // Miércoles.
    private val hoy = LocalDate.of(2026, 11, 25)

    private fun usuario(
        materias: List<Materia> = emptyList(),
        plan: List<MateriaPlan> = emptyList(),
        finales: List<MesaFinal> = emptyList()
    ) = Usuario(
        id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana",
        metaHorasSemanales = 5, materias = materias, planEstudio = plan, finales = finales
    )

    // ---------- Mi carrera ----------

    private val plan = listOf(
        MateriaPlan(1, "Matemática I", 1, EstadoPlan.APROBADA, notaFinal = 8.0),
        MateriaPlan(2, "Programación I", 1, EstadoPlan.REGULAR),
        MateriaPlan(3, "Matemática II", 2, EstadoPlan.PENDIENTE, correlativas = listOf(1)),
        MateriaPlan(4, "Programación II", 2, EstadoPlan.PENDIENTE, correlativas = listOf(2)),
        MateriaPlan(5, "Base de datos", 3, EstadoPlan.PENDIENTE, correlativas = listOf(3, 4)),
        MateriaPlan(6, "Programación I bis", 1, EstadoPlan.REGULAR, correlativas = listOf(2))
    )

    @Test
    fun lasCorrelativasDicenQueSePuedeCursarYRendir() {
        val (_, prog1, mate2, prog2, bd, prog1bis) = plan
        assertTrue(mate2.sePuedeCursar(plan))         // correlativa aprobada
        assertTrue(prog2.sePuedeCursar(plan))         // con una regular alcanza para cursar
        assertFalse(bd.sePuedeCursar(plan))
        assertEquals(listOf("Matemática II", "Programación II"), bd.faltanParaCursar(plan).map { it.nombre })
        assertTrue(prog1.sePuedeRendir(plan))
        // Para rendir, la correlativa tiene que estar aprobada (no solo regular).
        assertFalse(prog1bis.sePuedeRendir(plan))
        assertEquals(listOf("Programación I"), prog1bis.faltanParaRendir(plan).map { it.nombre })
    }

    @Test
    fun lasCorrelativasBorradasDelPlanSeIgnoran() {
        val sola = MateriaPlan(7, "Inglés II", 2, correlativas = listOf(99))
        assertTrue(sola.sePuedeCursar(plan + sola))
    }

    @Test
    fun elAvanceCuentaLasAprobadasYElPromedio() {
        val avance = avanceCarrera(plan + MateriaPlan(8, "Inglés", 1, EstadoPlan.APROBADA, notaFinal = 10.0))
        assertEquals(7, avance.total)
        assertEquals(2, avance.aprobadas)
        assertEquals(28, avance.porcentaje)
        assertEquals(9.0, avance.promedio!!, 0.001)
    }

    private operator fun <T> List<T>.component6(): T = this[5]

    // ---------- Asistencia ----------

    private fun claseSemanal(asistencias: List<Asistencia>, hasta: LocalDate?) = Evento(
        1, "Clase", TipoEvento.CLASE, hoy.minusWeeks(3), LocalTime.of(10, 0), LocalTime.of(12, 0),
        semanal = true, repetirHasta = hasta, asistencias = asistencias
    )

    @Test
    fun conElTotalDelCuatrimestreCalculaLasFaltasQueQuedan() {
        val asistencias = listOf(
            Asistencia(1, hoy.minusWeeks(3), true),
            Asistencia(2, hoy.minusWeeks(2), false),
            Asistencia(3, hoy.minusWeeks(1), true)
        )
        // 16 clases en total: el 25 % son 4 faltas; ya faltó 1.
        val materia = Materia(1, "Física", 0, eventos = listOf(claseSemanal(asistencias, hoy.minusWeeks(3).plusWeeks(15))))
        val resumen = resumenAsistencia(materia)
        assertEquals(16, resumen.clasesTotales)
        assertEquals(66, resumen.porcentaje)
        assertEquals(3, resumen.faltasDisponibles)
        assertFalse(resumen.enRiesgo)
    }

    @Test
    fun sinFechaDeFinCalculaSobreLasClasesMarcadas() {
        val asistencias = listOf(
            Asistencia(1, hoy.minusWeeks(3), true),
            Asistencia(2, hoy.minusWeeks(2), false),
            Asistencia(3, hoy.minusWeeks(1), false)
        )
        val resumen = resumenAsistencia(Materia(1, "Física", 0, eventos = listOf(claseSemanal(asistencias, null))))
        assertNull(resumen.clasesTotales)
        // 1 presente de 3: aunque vaya a todas, ya está debajo del 75 %.
        assertTrue(resumen.debajoDelMinimo)
    }

    @Test
    fun lasClasesSemanalesAparecenUnaVezPorSemana() {
        val materia = Materia(1, "Física", 0, eventos = listOf(claseSemanal(emptyList(), null)))
        assertEquals(2, clasesEntre(materia, hoy.minusDays(13), hoy).size)
    }

    // ---------- ¿Qué nota necesito? ----------

    @Test
    fun calculaLaNotaQueFaltaParaAprobarYPromocionar() {
        val calculo = calcularNotas(
            listOf(Evaluacion(1, "TP", 20, 9.0), Evaluacion(2, "Parcial 1", 40, 6.0), Evaluacion(3, "Parcial 2", 40, null))
        )
        // Lleva 1,8 + 2,4 = 4,2: ya aprobó; para 7 le faltan 2,8 sobre el 40 % -> 7.
        assertEquals(4.2, calculo.acumulado, 0.001)
        assertEquals(NotaNecesaria.Asegurada, calculo.paraAprobar)
        assertEquals(NotaNecesaria.Necesita(7.0), calculo.paraPromocionar)
        assertEquals(7.0, calculo.promedioActual!!, 0.001)
    }

    @Test
    fun avisaSiYaNoAlcanzaOSiYaRindioTodo() {
        val bajo = calcularNotas(listOf(Evaluacion(1, "P1", 70, 2.0), Evaluacion(2, "P2", 30, null)))
        assertEquals(NotaNecesaria.Necesita(8.7), bajo.paraAprobar)   // (4 - 1,4) / 0,3 = 8,67 -> 8,7
        assertEquals(NotaNecesaria.NoAlcanza, bajo.paraPromocionar)
        val terminado = calcularNotas(listOf(Evaluacion(1, "P1", 50, 8.0), Evaluacion(2, "P2", 50, 5.0)))
        assertEquals(NotaNecesaria.Terminada(true), terminado.paraAprobar)
        assertEquals(NotaNecesaria.Terminada(false), terminado.paraPromocionar)
    }

    @Test
    fun noDejaQueLosPesosPasenDe100() = runTest {
        val materia = Materia(1, "Física", 0, evaluaciones = listOf(Evaluacion(1, "P1", 70, null)))
        val repositorio = RepositorioFalso(usuario(listOf(materia)))
        val vm = NotaNecesariaViewModel(repositorio, 1)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(Mensaje.ERROR_PESO_SUPERA, vm.guardar(null, "P2", "40", ""))
        assertEquals(Mensaje.ERROR_NOTA_INVALIDA, vm.guardar(null, "P2", "30", "11"))
        assertNull(vm.guardar(null, "P2", "30", "7,5"))
        advanceUntilIdle()
        assertEquals(100, vm.uiState.value.calculo!!.pesoTotal)
    }

    // ---------- Finales ----------

    @Test
    fun avisaDelCierreDeInscripcionYDeLaMesa() {
        val finales = listOf(
            MesaFinal(1, "Física", hoy.plusDays(10), cierreInscripcion = hoy.plusDays(2)),
            MesaFinal(2, "Química", hoy.plusDays(1), inscripto = true),
            MesaFinal(3, "Inglés", hoy.plusDays(1)),                                  // no se anotó
            MesaFinal(4, "Historia", hoy.plusDays(10), cierreInscripcion = hoy.plusDays(5)),
            MesaFinal(5, "Arte", hoy, inscripto = true, nota = 9.0)                   // ya rendido
        )
        val avisos = avisosDeFinales(finales, hoy)
        assertEquals(listOf(1 to TipoAvisoFinal.CIERRA_INSCRIPCION, 2 to TipoAvisoFinal.MESA_MANANA), avisos.map { it.final.id to it.tipo })
    }

    @Test
    fun aprobarElFinalApruebaLaMateriaEnElPlan() = runTest {
        val final = MesaFinal(1, "Programación I", hoy, inscripto = true)
        val repositorio = RepositorioFalso(usuario(plan = plan, finales = listOf(final)))
        val vm = FinalesViewModel(repositorio) { hoy }
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(listOf("Programación I bis"), vm.uiState.value.sugerencias)

        assertEquals(Mensaje.ERROR_NOTA_INVALIDA, vm.cargarNota(final, "diez"))
        assertNull(vm.cargarNota(final, "7"))
        advanceUntilIdle()

        val u = repositorio.usuarioActual.first()!!
        assertEquals(7.0, u.finales.single().nota!!, 0.001)
        val prog1 = u.planEstudio.first { it.id == 2 }
        assertEquals(EstadoPlan.APROBADA, prog1.estado)
        assertEquals(7.0, prog1.notaFinal!!, 0.001)
        assertEquals(Mensaje.AVISO_FINAL_APROBADO_PLAN, avisos.single().mensaje)
        assertEquals(1, vm.uiState.value.rendidos.size)
    }

    @Test
    fun laInscripcionTieneQueCerrarAntesDeLaMesa() = runTest {
        val vm = FinalesViewModel(RepositorioFalso(usuario())) { hoy }
        assertEquals(Mensaje.ERROR_COMPLETAR_CAMPOS, vm.guardar(null, " ", hoy, null, null, false))
        assertEquals(Mensaje.ERROR_CIERRE_INSCRIPCION, vm.guardar(null, "Física", hoy, hoy.plusDays(1), null, false))
    }

    // ---------- Quiz ----------

    private val tarjetas = listOf(
        Tarjeta(1, "2 + 2", "4"), Tarjeta(2, "3 + 3", "6"), Tarjeta(3, "5 + 5", "10"),
        Tarjeta(4, "1 + 1", "2"), Tarjeta(5, "4 + 4", "8")
    )

    @Test
    fun cadaPreguntaTieneCuatroOpcionesDistintasConLaCorrecta() {
        val quiz = armarQuiz(tarjetas, Random(7))
        assertEquals(5, quiz.size)
        quiz.forEach { p ->
            assertEquals(4, p.opciones.size)
            assertEquals(4, p.opciones.toSet().size)
            val tarjeta = tarjetas.first { it.pregunta == p.pregunta }
            assertEquals(tarjeta.respuesta, p.opciones[p.correcta])
        }
        // Con menos de 4 respuestas distintas no se puede armar.
        assertTrue(armarQuiz(tarjetas.take(3) + Tarjeta(9, "x", "4"), Random(1)).isEmpty())
    }

    @Test
    fun elQuizCuentaLosAciertosYTermina() = runTest {
        val repositorio = RepositorioFalso(usuario(listOf(Materia(1, "Mate", 0, tarjetas = tarjetas))))
        var reloj = 0L
        val vm = QuizViewModel(repositorio, 1, Random(3)) { reloj }
        advanceUntilIdle()
        val total = vm.uiState.value.preguntas.size
        repeat(total) { i ->
            val actual = vm.uiState.value.actual!!
            // Acierta todas menos la primera.
            vm.elegir(if (i == 0) (actual.correcta + 1) % 4 else actual.correcta)
            vm.elegir(actual.correcta)             // no se puede cambiar la respuesta
            reloj += 5_000
            vm.siguiente()
        }
        val fin = vm.uiState.value
        assertTrue(fin.terminado)
        assertEquals(total - 1, fin.aciertos)
        assertEquals(80, fin.puntaje)
        assertEquals(25_000L, fin.finMillis)
    }

    // ---------- Resumen semanal ----------

    @Test
    fun elResumenComparaConLaSemanaAnterior() {
        fun bloque(id: Int, dia: LocalDate, horas: Long) =
            Evento(id, "Estudio", TipoEvento.BLOQUE_ESTUDIO, dia, LocalTime.of(9, 0), LocalTime.of(9, 0).plusHours(horas))
        val lunes = LocalDate.of(2026, 11, 23)
        val fisica = Materia(
            1, "Física", 0,
            tareas = listOf(Tarea(1, "TP", lunes.plusDays(4), completada = true), Tarea(2, "Informe", lunes.plusDays(6)), Tarea(3, "Vieja", lunes.minusDays(3))),
            eventos = listOf(
                bloque(1, lunes, 2), bloque(2, hoy, 1), bloque(3, hoy, 1),          // esta semana: 4 h en 2 días
                bloque(4, lunes.minusDays(2), 2),                                     // la anterior: 2 h
                Evento(5, "Parcial", TipoEvento.EXAMEN, hoy.plusDays(2), LocalTime.of(9, 0), LocalTime.of(11, 0))
            )
        )
        val r = armarResumenSemanal(usuario(listOf(fisica)), hoy)
        assertEquals(lunes, r.lunes)
        assertEquals(240L, r.minutos)
        assertEquals(100, r.variacion)
        assertEquals(2, r.diasEstudiados)
        assertEquals(3, r.sesiones)
        assertEquals(1 to 2, r.tareasHechas to r.tareasDeLaSemana)
        assertEquals(listOf("Parcial"), r.proximos.map { it.titulo })
        assertFalse(r.metaCumplida)  // meta de 5 h
    }

    // ---------- Búsqueda global ----------

    @Test
    fun laBusquedaIgnoraTildesYBuscaEnTodo() {
        val materia = Materia(
            1, "Matemática II", 0,
            tareas = listOf(Tarea(1, "Ejercicios de matematica")),
            notas = listOf(Nota(1, "Resumen", "Teorema de Pitágoras y matemática básica", hoy)),
            tarjetas = listOf(Tarjeta(1, "¿Qué es una derivada?", "La pendiente"))
        )
        val u = usuario(listOf(materia), plan = listOf(MateriaPlan(1, "Matemática I", 1)), finales = listOf(MesaFinal(1, "Matemática I", hoy)))
        val tipos = buscarEnTodo(u, "MATEMATICA").map { it.tipo }
        assertEquals(
            listOf(TipoResultado.MATERIA, TipoResultado.TAREA, TipoResultado.NOTA, TipoResultado.CARRERA, TipoResultado.FINAL),
            tipos
        )
        assertEquals(TipoResultado.TARJETA, buscarEnTodo(u, "pendiente").single().tipo)
        assertTrue(buscarEnTodo(u, "m").isEmpty())  // menos de 2 letras
    }

    // ---------- Sonidos ----------

    @Test
    fun elRuidoGeneradoQuedaEntreMenosUnoYUno() {
        TipoSonido.entries.forEach { tipo ->
            val generador = GeneradorRuido(tipo, Random(5))
            val muestras = List(5_000) { generador.siguiente() }
            assertTrue(muestras.all { it in -1f..1f })
            if (tipo != TipoSonido.NINGUNO) assertTrue(muestras.any { it != 0f })
        }
    }

    // ---------- Exportar a PDF ----------

    @Test
    fun exportarDejaElPdfParaCompartirOAvisaSiFalla() = runTest {
        val repositorio = RepositorioFalso(usuario(listOf(Materia(1, "Física", 0, notas = listOf(Nota(1, "Clase", "Apuntes", hoy))))))
        val vm = NotasViewModel(repositorio)
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        val item = vm.uiState.value.notas.single()

        vm.exportarPdf(item)
        advanceUntilIdle()
        assertEquals("content://pdf/nota.pdf", vm.pdfParaCompartir.value)
        vm.pdfCompartido()
        assertNull(vm.pdfParaCompartir.value)

        repositorio.pdfExportado = null
        vm.exportarPdf(item)
        advanceUntilIdle()
        assertNull(vm.pdfParaCompartir.value)
        assertEquals(Mensaje.AVISO_PDF_ERROR, avisos.single().mensaje)
    }
}
