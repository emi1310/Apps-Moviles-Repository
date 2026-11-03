package com.example.estudia

import com.example.estudia.viewmodel.SesionUiState
import com.example.estudia.viewmodel.SesionViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SesionViewModelTest {

    @get:Rule
    val reglaMain = ReglaDispatcherPrincipal()

    private val repositorio = RepositorioFalso()
    private val avisosSistema = AvisosFalso()
    // by lazy: se crea dentro de cada test, cuando la regla ya reemplazó
    // Dispatchers.Main (el init del ViewModel lanza una corrutina).
    private val viewModel by lazy { SesionViewModel(repositorio, avisosSistema) }

    @Test
    fun loginConCamposVaciosMuestraError() {
        viewModel.iniciarSesion("", "")
        assertEquals(R.string.error_completar_campos, viewModel.authUiState.value.error)
    }

    @Test
    fun loginIncorrectoMuestraError() {
        viewModel.iniciarSesion("demo", "otra")
        assertEquals(R.string.error_credenciales, viewModel.authUiState.value.error)
        assertFalse(viewModel.authUiState.value.sesionIniciada)
    }

    @Test
    fun loginCorrectoIniciaSesion() = runTest {
        // stateIn(WhileSubscribed) solo emite si alguien está observando.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.sesionUiState.collect {} }

        viewModel.iniciarSesion("demo", "demo")

        assertTrue(viewModel.authUiState.value.sesionIniciada)
        assertTrue(viewModel.sesionUiState.value is SesionUiState.Activa)
    }

    @Test
    fun registroValidaLosDatos() {
        viewModel.registrarUsuario("Ana", "ana perez", "1234")
        assertEquals(R.string.error_usuario_espacios, viewModel.authUiState.value.error)

        viewModel.registrarUsuario("Ana", "ana", "12")
        assertEquals(R.string.error_contrasena_corta, viewModel.authUiState.value.error)

        viewModel.registrarUsuario("Demo", "demo", "1234")
        assertEquals(R.string.error_usuario_existe, viewModel.authUiState.value.error)
    }

    @Test
    fun registroCorrectoIniciaSesionYConsumirLimpiaElEstado() {
        viewModel.registrarUsuario("Ana", "ana", "1234")
        assertTrue(viewModel.authUiState.value.sesionIniciada)
        assertNull(viewModel.authUiState.value.error)

        viewModel.consumirSesionIniciada()
        assertFalse(viewModel.authUiState.value.sesionIniciada)
    }
}
