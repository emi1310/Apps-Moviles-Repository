package com.example.estudia

import com.example.estudia.modelo.MapaMental
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.NodoMapa
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.agregarIdea
import com.example.estudia.modelo.cambiarColor
import com.example.estudia.modelo.cambiarTexto
import com.example.estudia.modelo.codificarNodos
import com.example.estudia.modelo.colorDe
import com.example.estudia.modelo.decodificarNodos
import com.example.estudia.modelo.eliminarIdea
import com.example.estudia.modelo.moverRama
import com.example.estudia.viewmodel.Aviso
import com.example.estudia.viewmodel.MapaViewModel
import com.example.estudia.viewmodel.MapasViewModel
import com.example.estudia.viewmodel.Mensaje
import com.example.estudia.viewmodel.TipoResultado
import com.example.estudia.viewmodel.buscarEnTodo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import kotlin.math.hypot

// Tests de los mapas mentales: ubicar, mover y borrar ideas, colores de las
// ramas, cómo se guardan como texto y el editor con "Deshacer".
@OptIn(ExperimentalCoroutinesApi::class)
class MapasMentalesTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    private val hoy = LocalDate.of(2026, 11, 26)

    private fun usuario(mapas: List<MapaMental> = emptyList()) =
        Usuario(id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana", mapas = mapas)

    // Central "Newton" con dos ramas y una subrama en la primera.
    private fun ejemplo(): MapaMental {
        var mapa = MapaMental.nuevo(7, "Física I", "Newton", hoy)
        mapa = mapa.agregarIdea(1, "Inercia").first          // id 2
        mapa = mapa.agregarIdea(1, "F = m·a").first          // id 3
        mapa = mapa.agregarIdea(2, "Ejemplo: colectivo").first // id 4
        return mapa
    }

    @Test
    fun lasIdeasNuevasSeUbicanSinPisarse() {
        val mapa = ejemplo()
        assertEquals(listOf(1, 2, 3, 4), mapa.nodos.map { it.id })
        val (inercia, fuerza) = mapa.nodos[1] to mapa.nodos[2]
        // Las ramas quedan a la distancia de rama de la central y en lugares distintos.
        assertEquals(MapaMental.DISTANCIA_RAMA, hypot(inercia.x, inercia.y), 0.5f)
        assertTrue(hypot(inercia.x - fuerza.x, inercia.y - fuerza.y) > 100f)
        // La subrama sale hacia afuera: queda más lejos de la central que su madre.
        val ejemplo = mapa.nodos[3]
        assertTrue(hypot(ejemplo.x, ejemplo.y) > hypot(inercia.x, inercia.y))
    }

    @Test
    fun moverUnaIdeaLlevaTodaSuRama() {
        val mapa = ejemplo().moverRama(2, 10f, -5f)
        val antes = ejemplo()
        assertEquals(antes.nodos[1].x + 10f, mapa.nodos[1].x, 0.01f)
        assertEquals(antes.nodos[3].y - 5f, mapa.nodos[3].y, 0.01f)
        // La otra rama y la central no se mueven.
        assertEquals(antes.nodos[2], mapa.nodos[2])
        assertEquals(antes.raiz, mapa.raiz)
    }

    @Test
    fun borrarUnaIdeaBorraLoQueSaleDeEllaPeroNoLaCentral() {
        val mapa = ejemplo()
        assertEquals(listOf(1, 3), mapa.eliminarIdea(2).nodos.map { it.id })
        assertEquals(mapa, mapa.eliminarIdea(1))
    }

    @Test
    fun cadaRamaTieneSuColorYLasSubramasLoHeredan() {
        val mapa = ejemplo()
        assertNull(mapa.colorDe(1))
        assertEquals(0, mapa.colorDe(2))
        assertEquals(1, mapa.colorDe(3))
        assertEquals(0, mapa.colorDe(4))
        // Si se le cambia el color a una rama, la subrama lo sigue.
        val pintado = mapa.cambiarColor(2, 4)
        assertEquals(4, pintado.colorDe(4))
    }

    @Test
    fun cambiarLaIdeaCentralCambiaElTitulo() {
        val mapa = ejemplo().cambiarTexto(1, "  Leyes de Newton ")
        assertEquals("Leyes de Newton", mapa.titulo)
        assertEquals("Leyes de Newton", mapa.raiz.texto)
        // Vacío no se acepta.
        assertEquals(mapa, mapa.cambiarTexto(2, "   "))
    }

    @Test
    fun lasIdeasSeGuardanYSeLeenComoTexto() {
        val nodos = listOf(
            NodoMapa(1, null, "Central", 0f, 0f),
            NodoMapa(2, 1, "Con | barra, \\ y\nsalto", 12.5f, -40f, color = 3)
        )
        assertEquals(nodos, decodificarNodos(codificarNodos(nodos)))
        // Una línea rota se ignora en vez de romper todo el mapa.
        assertEquals(nodos.take(1), decodificarNodos(codificarNodos(nodos.take(1)) + "\nbasura"))
    }

    @Test
    fun laBusquedaGlobalEncuentraIdeasDeUnMapa() {
        val resultado = buscarEnTodo(usuario(listOf(ejemplo())), "colectivo").single()
        assertEquals(TipoResultado.MAPA, resultado.tipo)
        assertEquals("Newton", resultado.titulo)
        assertEquals("Ejemplo: colectivo", resultado.detalle)
    }

    @Test
    fun crearUnMapaPideTemaYAvisaSuId() = runTest {
        val repositorio = RepositorioFalso(usuario())
        val vm = MapasViewModel(repositorio) { hoy }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(Mensaje.ERROR_COMPLETAR_CAMPOS, vm.crear(" ", "", {}))
        var abierto = 0
        assertNull(vm.crear(" Célula ", "Biología", { abierto = it }))
        advanceUntilIdle()
        val mapa = repositorio.usuario.value!!.mapas.single()
        assertEquals(mapa.id, abierto)
        assertEquals("Célula", mapa.raiz.texto)
        assertEquals("Biología", mapa.materia)
    }

    @Test
    fun elEditorGuardaCadaCambioYDeshaceElBorrado() = runTest {
        val repositorio = RepositorioFalso(usuario(listOf(ejemplo())))
        val vm = MapaViewModel(repositorio, 7) { hoy }
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }
        advanceUntilIdle()

        vm.agregarIdea(3, "Acción y reacción")
        advanceUntilIdle()
        val guardado = repositorio.usuario.value!!.mapas.single()
        assertEquals(5, guardado.nodos.size)
        // La idea nueva queda elegida.
        assertEquals(5, vm.uiState.value.seleccionada)

        // Arrastrar no guarda hasta soltar.
        vm.mover(5, 30f, 0f)
        assertEquals(guardado, repositorio.usuario.value!!.mapas.single())
        vm.terminarArrastre()
        advanceUntilIdle()
        assertNotEquals(guardado, repositorio.usuario.value!!.mapas.single())

        vm.eliminar(2)
        advanceUntilIdle()
        assertEquals(3, repositorio.usuario.value!!.mapas.single().nodos.size)
        assertEquals(Mensaje.AVISO_IDEA_ELIMINADA, avisos.single().mensaje)
        avisos.single().alDeshacer!!.invoke()
        advanceUntilIdle()
        assertEquals(5, repositorio.usuario.value!!.mapas.single().nodos.size)
    }
}
