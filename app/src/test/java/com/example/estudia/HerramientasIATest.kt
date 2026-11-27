package com.example.estudia

import com.example.estudia.modelo.ExamenProximo
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MapaMental
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.RamaIA
import com.example.estudia.modelo.SesionPlanTexto
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.Tema
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.copiasParaBorrar
import com.example.estudia.modelo.eventoDeSesion
import com.example.estudia.modelo.examenesProximos
import com.example.estudia.modelo.limpiarPlan
import com.example.estudia.modelo.mapaDesdeIA
import com.example.estudia.modelo.nombreCopiaAutomatica
import com.example.estudia.modelo.pedidoPlanEstudio
import com.example.estudia.modelo.pedidoSimulacro
import com.example.estudia.widget.armarResumenHoy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import java.time.LocalDate
import java.time.LocalTime

// Tests de las herramientas nuevas: plan de estudio, mapa desde una nota,
// simulacro, widget "Hoy" y copia automática (las partes puras).
class HerramientasIATest {

    // 26/11/2026 es jueves.
    private val hoy = LocalDate.of(2026, 11, 26)
    private val parcial = Evento(
        7, "Parcial 2", TipoEvento.EXAMEN, hoy.plusDays(6), LocalTime.of(9, 0), LocalTime.of(11, 0),
        temas = listOf(Tema(1, "Integrales dobles"), Tema(2, "Cambio de variable", estudiado = true))
    )
    private val clase = Evento(8, "Teórico", TipoEvento.CLASE, LocalDate.of(2026, 3, 5), LocalTime.of(18, 0), LocalTime.of(20, 0), semanal = true)
    private val materia = Materia(
        3, "Análisis II", 0xFF6C5CE7, eventos = listOf(parcial, clase),
        tareas = listOf(Tarea(1, "TP 4", fechaLimite = hoy), Tarea(2, "TP 5", fechaLimite = hoy.plusDays(3)))
    )
    private val usuario = Usuario(id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana", materias = listOf(materia))
    private val examen = ExamenProximo(parcial, 3, "Análisis II", 0xFF6C5CE7)

    // ---------- Plan de estudio ----------

    @Test
    fun losExamenesQueVienenSonLosDeExamenYEntrega() {
        assertEquals(listOf("Parcial 2"), examenesProximos(usuario, hoy).map { it.evento.titulo })
    }

    @Test
    fun elPedidoDelPlanLlevaElTemarioYLasFechas() {
        val pedido = pedidoPlanEstudio(examen, hoy)
        assertTrue(pedido.contains("Integrales dobles"))
        assertTrue(pedido.contains("Cambio de variable (ya lo estudié)"))
        assertTrue(pedido.contains("2026-11-27") && pedido.contains("2026-12-01"))
    }

    @Test
    fun delPlanSeSacanLasSesionesInvalidas() {
        val propuestas = listOf(
            SesionPlanTexto("2026-11-28", "16:00", 90, " Integrales dobles "),
            SesionPlanTexto("2026-11-27", "9:30", 60, "Lectura"),
            SesionPlanTexto("2026-12-02", "10:00", 60, "El mismo día del examen"),
            SesionPlanTexto("2026-11-20", "10:00", 60, "Antes de hoy"),
            SesionPlanTexto("mañana", "10:00", 60, "Fecha rota"),
            SesionPlanTexto("2026-11-29", "25:00", 60, "Hora rota"),
            SesionPlanTexto("2026-11-29", "23:30", 120, "Pasa la medianoche"),
            SesionPlanTexto("2026-11-30", "10:00", 600, "Muy larga"),
            SesionPlanTexto("2026-11-30", "12:00", 60, "  ")
        )
        val plan = limpiarPlan(propuestas, hoy, parcial.fecha)
        assertEquals(listOf("Lectura", "Integrales dobles", "Muy larga"), plan.map { it.tema })
        // Ordenadas, con la hora arreglada y la duración acotada.
        assertEquals(LocalTime.of(9, 30), plan.first().hora)
        assertEquals(180, plan.last().minutos)
        val evento = eventoDeSesion(plan[1], "Estudiar")
        assertEquals("Estudiar: Integrales dobles", evento.titulo)
        assertEquals(TipoEvento.BLOQUE_ESTUDIO, evento.tipo)
        assertEquals(LocalTime.of(17, 30), evento.horaFin)
    }

    // ---------- Mapa mental ----------

    @Test
    fun elMapaDeLaIASeArmaConRamasYDetalles() {
        val mapa = mapaDesdeIA(
            " Integrales ",
            listOf(RamaIA("Dobles", listOf("Fubini", "", "Regiones")), RamaIA(" ", listOf("x")), RamaIA("Triples", emptyList())),
            "Análisis II", hoy
        )!!
        assertEquals("Integrales", mapa.titulo)
        assertEquals("Análisis II", mapa.materia)
        val ramas = mapa.hijos(MapaMental.ID_RAIZ)
        assertEquals(listOf("Dobles", "Triples"), ramas.map { it.texto })
        assertEquals(listOf("Fubini", "Regiones"), mapa.hijos(ramas.first().id).map { it.texto })
        // Cada idea en un lugar distinto.
        assertEquals(mapa.nodos.size, mapa.nodos.map { it.x to it.y }.distinct().size)
    }

    @Test
    fun sinRamasNoHayMapa() {
        assertNull(mapaDesdeIA("Tema", emptyList(), "", hoy))
        assertNull(mapaDesdeIA(" ", listOf(RamaIA("a", emptyList())), "", hoy))
    }

    // ---------- Simulacro ----------

    @Test
    fun elSimulacroPideDeAUnaYReparteElTexto() {
        val pedido = pedidoSimulacro(listOf("Guía 1" to "a".repeat(20_000), "Guía 2" to "Corta"))
        assertTrue(pedido.contains("DE A UNA"))
        assertTrue(pedido.contains("Guía 2") && pedido.contains("Corta"))
        assertTrue(pedido.contains("[…recortada]"))
        assertTrue(pedido.length < 14_000)
    }

    // ---------- Widget "Hoy" ----------

    @Test
    fun elWidgetHoyMuestraLaClaseYLaTareaDeHoy() {
        val resumen = armarResumenHoy(usuario, hoy)
        assertEquals(listOf("18:00  Teórico · Análisis II", "☐  TP 4 · Análisis II"), resumen.lineas)
        assertEquals(0, resumen.mas)
        assertTrue(armarResumenHoy(usuario, hoy.plusDays(1)).lineas.isEmpty())
    }

    // ---------- Copia automática ----------

    @Test
    fun seConservanLasCuatroCopiasMasNuevasDelUsuario() {
        val nombres = (1..6).map { nombreCopiaAutomatica("ana", hoy.minusWeeks(it.toLong())) } +
            nombreCopiaAutomatica("beto", hoy.minusWeeks(9)) + "otra_cosa.zip"
        val borrar = copiasParaBorrar(nombres, "ana")
        assertEquals(listOf(nombreCopiaAutomatica("ana", hoy.minusWeeks(5)), nombreCopiaAutomatica("ana", hoy.minusWeeks(6))), borrar)
    }

    @Test
    fun elNombreDeLaCopiaNoTieneCaracteresRaros() {
        assertEquals("estudia_auto_ana_p_rez_2026-11-26.zip", nombreCopiaAutomatica("ana/pérez", hoy))
    }
}

// La copia automática desde Perfil (con el repositorio falso).
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CopiaAutomaticaPerfilTest {

    @get:org.junit.Rule
    val reglaMain = ReglaDispatcherPrincipal()

    private fun usuario() = Usuario(id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana")

    @Test
    fun activarCopiarYDesactivar() = kotlinx.coroutines.test.runTest {
        val repositorio = RepositorioFalso(usuario())
        val vm = com.example.estudia.viewmodel.PerfilViewModel(repositorio, AvisosFalso())
        val avisos = mutableListOf<com.example.estudia.viewmodel.Aviso>()
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.activarCopiaAutomatica("content://carpeta")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.copiaAutomatica.activa)
        assertEquals(LocalDate.of(2026, 11, 26), vm.uiState.value.copiaAutomatica.ultima)
        assertEquals(com.example.estudia.viewmodel.Mensaje.AVISO_COPIA_AUTO_ACTIVADA, avisos.last().mensaje)

        vm.desactivarCopiaAutomatica()
        advanceUntilIdle()
        assertTrue(!vm.uiState.value.copiaAutomatica.activa)
        // Apagada, "copiar ahora" no hace nada y avisa el error.
        vm.copiarAhora()
        advanceUntilIdle()
        assertEquals(com.example.estudia.viewmodel.Mensaje.AVISO_COPIA_AUTO_ERROR, avisos.last().mensaje)
    }
}
