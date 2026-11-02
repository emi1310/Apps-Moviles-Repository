package com.example.estudia.ui.navegacion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.estudia.ui.componentes.BarraInferior
import com.example.estudia.ui.componentes.ControladorAvisos
import com.example.estudia.ui.componentes.LocalAvisos
import com.example.estudia.ui.pantallas.AyudaScreen
import com.example.estudia.ui.pantallas.BienvenidaScreen
import com.example.estudia.ui.pantallas.CalendarioScreen
import com.example.estudia.ui.pantallas.InicioScreen
import com.example.estudia.ui.pantallas.LoginScreen
import com.example.estudia.ui.pantallas.MateriasScreen
import com.example.estudia.ui.pantallas.NotasScreen
import com.example.estudia.ui.pantallas.PerfilScreen
import com.example.estudia.ui.pantallas.RegistroScreen
import com.example.estudia.viewmodel.AppViewModelProvider
import com.example.estudia.viewmodel.SesionUiState
import com.example.estudia.viewmodel.SesionViewModel

// Grafo de navegación de la app: rutas, barra inferior y el Snackbar.
// Cada pantalla pide su propio ViewModel con viewModel(factory = ...):
// queda asociado a esa entrada de la navegación y se destruye al salir.
// La sesión la maneja SesionViewModel, que vive mientras vive la Activity.
@Composable
fun AppNavegacion(sesionViewModel: SesionViewModel) {
    val sesionUiState by sesionViewModel.sesionUiState.collectAsStateWithLifecycle()
    val authUiState by sesionViewModel.authUiState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route
    val mostrarBarra = pantallasConBarra.any { it.ruta == rutaActual }

    val snackbarHostState = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val recursos = LocalContext.current.resources
    val avisos = remember { ControladorAvisos(snackbarHostState, alcance, recursos) }

    // Mientras se lee la sesión guardada, se muestra un indicador de carga.
    if (sesionUiState is SesionUiState.Cargando) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Se decide una sola vez (remember): si había sesión guardada se arranca
    // directo en Inicio; si no, en Bienvenida.
    val destinoInicial = remember {
        if (sesionUiState is SesionUiState.Activa) Screen.Inicio.ruta else Screen.Bienvenida.ruta
    }

    // Cuando el login o el registro salen bien, se navega a Inicio una vez.
    LaunchedEffect(authUiState.sesionIniciada) {
        if (authUiState.sesionIniciada) {
            navController.navigate(Screen.Inicio.ruta) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
            sesionViewModel.consumirSesionIniciada()
        }
    }

    CompositionLocalProvider(LocalAvisos provides avisos) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (mostrarBarra) {
                    BarraInferior(
                        rutaActual = rutaActual,
                        onSeleccionar = { destino ->
                            navController.navigate(destino.ruta) {
                                popUpTo(Screen.Inicio.ruta) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { espacioInterno ->
            NavHost(
                navController = navController,
                startDestination = destinoInicial,
                // Las pantallas con barra inferior respetan el espacio del Scaffold;
                // las demás (Bienvenida, Login, Registro, Ayuda) dibujan de borde a
                // borde y manejan ellas mismas las barras del sistema.
                modifier = if (mostrarBarra) Modifier.padding(espacioInterno) else Modifier
            ) {
                composable(Screen.Bienvenida.ruta) {
                    BienvenidaScreen(onComenzar = { navController.navigate(Screen.Login.ruta) })
                }

                composable(Screen.Login.ruta) {
                    LoginScreen(
                        estado = authUiState,
                        onIniciarSesion = sesionViewModel::iniciarSesion,
                        onIrARegistro = {
                            sesionViewModel.limpiarErrorAuth()
                            navController.navigate(Screen.Registro.ruta)
                        }
                    )
                }

                composable(Screen.Registro.ruta) {
                    RegistroScreen(
                        estado = authUiState,
                        onRegistrar = sesionViewModel::registrarUsuario,
                        onVolverALogin = {
                            sesionViewModel.limpiarErrorAuth()
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.Inicio.ruta) {
                    InicioScreen(viewModel(factory = AppViewModelProvider.Factory))
                }

                composable(Screen.Materias.ruta) {
                    MateriasScreen(viewModel(factory = AppViewModelProvider.Factory))
                }

                composable(Screen.Calendario.ruta) {
                    CalendarioScreen(viewModel(factory = AppViewModelProvider.Factory))
                }

                composable(Screen.Notas.ruta) {
                    NotasScreen(viewModel(factory = AppViewModelProvider.Factory))
                }

                composable(Screen.Perfil.ruta) {
                    PerfilScreen(
                        viewModel = viewModel(factory = AppViewModelProvider.Factory),
                        onAbrirAyuda = { navController.navigate(Screen.Ayuda.ruta) },
                        onCerrarSesion = {
                            sesionViewModel.cerrarSesion()
                            navController.navigate(Screen.Login.ruta) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Ayuda.ruta) {
                    AyudaScreen(onVolver = { navController.popBackStack() })
                }
            }
        }
    }
}
