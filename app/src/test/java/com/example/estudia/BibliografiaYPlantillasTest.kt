package com.example.estudia

import com.example.estudia.modelo.Fuente
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.TipoFuente
import com.example.estudia.modelo.TrozoCita
import com.example.estudia.modelo.Usuario
import com.example.estudia.modelo.agregarCita
import com.example.estudia.modelo.aplicarPlantilla
import com.example.estudia.modelo.citaApa
import com.example.estudia.modelo.citaEnTexto
import com.example.estudia.modelo.textoPlano
import com.example.estudia.viewmodel.Aviso
import com.example.estudia.viewmodel.BibliografiaViewModel
import com.example.estudia.viewmodel.Mensaje
import com.example.estudia.viewmodel.TipoResultado
import com.example.estudia.viewmodel.armarBibliografia
import com.example.estudia.viewmodel.buscarEnTodo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Tests de la bibliografía (citas APA, filtro, guardar y deshacer) y de las
// plantillas de notas.
@OptIn(ExperimentalCoroutinesApi::class)
class BibliografiaYPlantillasTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    private fun usuario(fuentes: List<Fuente> = emptyList(), materias: List<Materia> = emptyList()) = Usuario(
        id = 1, nombreUsuario = "ana", metodoAuth = MetodoAuth.LOCAL, nombre = "Ana", materias = materias, fuentes = fuentes
    )

    private val libro = Fuente(
        1, "Algoritmos", TipoFuente.LIBRO, "Cormen, T.; Leiserson, C.; Rivest, R.", 2009,
        "Introduction to Algorithms", origen = "MIT Press"
    )

    // ---------- Citas APA ----------

    @Test
    fun citaDeUnLibroConTresAutores() {
        val cita = citaApa(libro)
        assertEquals("Cormen, T., Leiserson, C. y Rivest, R. (2009). Introduction to Algorithms. MIT Press.", cita.textoPlano())
        // El título del libro va en cursiva.
        assertEquals(listOf("Introduction to Algorithms"), cita.filter { it.cursiva }.map { it.texto })
    }

    @Test
    fun citaDeUnArticuloConRevistaYPaginas() {
        val articulo = Fuente(
            2, "", TipoFuente.ARTICULO, "Pérez, A.", 2021, "Redes neuronales en el aula",
            origen = "Revista de Educación", paginas = "12-30", enlace = "https://doi.org/10.1/abc"
        )
        val cita = citaApa(articulo)
        assertEquals(
            "Pérez, A. (2021). Redes neuronales en el aula. Revista de Educación, 12-30. https://doi.org/10.1/abc",
            cita.textoPlano()
        )
        // En un artículo la cursiva es la revista, no el título.
        assertEquals(listOf(TrozoCita("Revista de Educación", cursiva = true)), cita.filter { it.cursiva })
    }

    @Test
    fun webSinAutorNiAnioEmpiezaPorElTitulo() {
        val web = Fuente(3, "", TipoFuente.WEB, "", null, "Guía de estilo", origen = "Wikipedia", enlace = "https://es.wikipedia.org")
        assertEquals("Guía de estilo. (s.f.). Wikipedia. https://es.wikipedia.org", citaApa(web).textoPlano())
    }

    @Test
    fun apunteDeUnaInstitucion() {
        val apunte = Fuente(4, "Física II", TipoFuente.APUNTE, "Cátedra de Física II", 2024, "Guía de electrostática", origen = "UTN FRBA")
        assertEquals(
            "Cátedra de Física II. (2024). Guía de electrostática [Apunte de cátedra]. UTN FRBA.",
            citaApa(apunte).textoPlano()
        )
    }

    @Test
    fun citaEnElTextoSegunLaCantidadDeAutores() {
        assertEquals("(Cormen et al., 2009)", citaEnTexto(libro))
        assertEquals("(Cormen y Leiserson, 2009)", citaEnTexto(libro.copy(autores = "Cormen, T.\nLeiserson, C.")))
        assertEquals("(Cormen, s.f.)", citaEnTexto(libro.copy(autores = "Cormen, T.", anio = null)))
        assertEquals("(Guía de estilo, s.f.)", citaEnTexto(libro.copy(autores = "", anio = null, titulo = "Guía de estilo.")))
    }

    // ---------- Pantalla ----------

    @Test
    fun elFiltroMuestraSoloLaMateriaElegida() {
        val otra = libro.copy(id = 2, materia = "Física II", titulo = "Física")
        val u = usuario(listOf(libro, otra))
        assertEquals(2, armarBibliografia(u, null).fuentes.size)
        assertEquals(listOf("Física"), armarBibliografia(u, "física ii").fuentes.map { it.titulo })
        assertEquals(listOf("Algoritmos", "Física II"), armarBibliografia(u, null).materiasConFuentes)
        // Una materia que ya no tiene fuentes vuelve a "Todas".
        assertNull(armarBibliografia(u, "Química").filtro)
    }

    @Test
    fun guardarPideTituloYAnioValido() = runTest {
        val vm = BibliografiaViewModel(RepositorioFalso(usuario()))
        assertEquals(Mensaje.ERROR_COMPLETAR_CAMPOS, vm.guardar(libro.copy(id = 0, titulo = "  ")))
        assertEquals(Mensaje.ERROR_ANIO_INVALIDO, vm.guardar(libro.copy(id = 0, anio = 20)))
    }

    @Test
    fun agregarEliminarYDeshacer() = runTest {
        val repositorio = RepositorioFalso(usuario())
        val vm = BibliografiaViewModel(repositorio)
        val avisos = mutableListOf<Aviso>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.avisos.collect { avisos += it } }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()

        assertNull(vm.guardar(libro.copy(id = 0, titulo = "  Introduction to Algorithms ")))
        advanceUntilIdle()
        val guardada = repositorio.usuario.value!!.fuentes.single()
        assertEquals("Introduction to Algorithms", guardada.titulo)

        vm.eliminar(guardada)
        advanceUntilIdle()
        assertTrue(repositorio.usuario.value!!.fuentes.isEmpty())
        assertEquals(Mensaje.AVISO_FUENTE_ELIMINADA, avisos.single().mensaje)
        avisos.single().alDeshacer!!.invoke()
        assertEquals(1, repositorio.usuario.value!!.fuentes.size)
    }

    @Test
    fun laBusquedaGlobalEncuentraLaBibliografia() {
        val resultados = buscarEnTodo(usuario(listOf(libro)), "leiserson")
        assertEquals(TipoResultado.FUENTE, resultados.single().tipo)
        assertEquals("Introduction to Algorithms", resultados.single().titulo)
    }

    // ---------- Plantillas de notas ----------

    @Test
    fun laPlantillaReemplazaSoloSiNoSeEscribioNada() {
        val cornell = "▸ IDEAS CLAVE\n• "
        val esquema = "1. Tema principal"
        assertEquals(cornell, aplicarPlantilla("", null, cornell))
        // Sin tocar la plantilla anterior: se cambia por la nueva.
        assertEquals(esquema, aplicarPlantilla(cornell, cornell, esquema))
        // Con algo escrito: la nueva va al final, sin borrar nada.
        assertEquals("Mis apuntes\n\n$esquema", aplicarPlantilla("Mis apuntes\n", cornell, esquema))
        // "Libre" no borra lo escrito.
        assertEquals("Mis apuntes", aplicarPlantilla("Mis apuntes", cornell, ""))
        assertEquals("", aplicarPlantilla(cornell, cornell, ""))
    }

    @Test
    fun citarAgregaLaCitaAlFinal() {
        assertEquals("(Cormen et al., 2009)", agregarCita("", "(Cormen et al., 2009)"))
        assertEquals("Según el libro (Cormen et al., 2009)", agregarCita("Según el libro", "(Cormen et al., 2009)"))
        assertEquals("Línea\n(Cormen et al., 2009)", agregarCita("Línea\n", "(Cormen et al., 2009)"))
    }
}
