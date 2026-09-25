package com.example.estudia.ui.navegacion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.estudia.datos.SesionDataStore
import com.example.estudia.ui.componentes.BarraInferior
import com.example.estudia.ui.pantallas.BienvenidaScreen
import com.example.estudia.ui.pantallas.CalendarioScreen
import com.example.estudia.ui.pantallas.InicioScreen
import com.example.estudia.ui.pantallas.LoginScreen
import com.example.estudia.ui.pantallas.MateriasScreen
import com.example.estudia.ui.pantallas.NotasScreen
import com.example.estudia.ui.pantallas.PerfilScreen
import com.example.estudia.ui.pantallas.RegistroScreen
import com.example.estudia.viewmodel.AppViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AppNavegacion(viewModel: AppViewModel, sesionDataStore: SesionDataStore) {
    val navController = rememberNavController()
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route
    val mostrarBarra = pantallasConBarra.any { it.ruta == rutaActual }

    val snackbarHostState = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    val mostrarMensaje: (String) -> Unit = { mensaje ->
        alcance.launch {
            snackbarHostState.showSnackbar(mensaje)
        }
    }

    var sesionLista by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val idGuardado = sesionDataStore.idUsuarioActivo.first()
        if (idGuardado != null) {
            val usuarioGuardado = viewModel.usuariosRegistrados.firstOrNull { it.id == idGuardado }
            if (usuarioGuardado != null) {
                viewModel.guardarSesion(usuarioGuardado)
            }
        }
        sesionLista = true
    }

    if (!sesionLista) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val destinoInicial = if (viewModel.usuarioActual != null) Screen.Inicio.ruta else Screen.Bienvenida.ruta

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (mostrarBarra) {
                BarraInferior(
                    rutaActual = rutaActual,
                    onSeleccionar = { destino ->
                        navController.navigate(destino.ruta) {
                            popUpTo(Screen.Inicio.ruta)
                        }
                    }
                )
            }
        }
    ) { espacioInterno ->
        NavHost(
            navController = navController,
            startDestination = destinoInicial,
            modifier = Modifier.padding(espacioInterno)
        ) {
            composable(Screen.Bienvenida.ruta) {
                BienvenidaScreen(
                    onComenzar = {
                        navController.navigate(Screen.Login.ruta)
                    }
                )
            }

            composable(Screen.Login.ruta) {
                LoginScreen(
                    usuariosRegistrados = viewModel.usuariosRegistrados,
                    onLoginExitoso = { usuario ->
                        viewModel.guardarSesion(usuario)
                        alcance.launch { sesionDataStore.guardarSesion(usuario.id) }
                        navController.navigate(Screen.Inicio.ruta) {
                            popUpTo(Screen.Bienvenida.ruta) { inclusive = true }
                        }
                    },
                    onIrARegistro = {
                        navController.navigate(Screen.Registro.ruta)
                    }
                )
            }

            composable(Screen.Registro.ruta) {
                RegistroScreen(
                    usuariosRegistrados = viewModel.usuariosRegistrados,
                    onRegistroExitoso = { usuario ->
                        viewModel.registrarUsuario(usuario)
                        alcance.launch { sesionDataStore.guardarSesion(usuario.id) }
                        navController.navigate(Screen.Inicio.ruta) {
                            popUpTo(Screen.Bienvenida.ruta) { inclusive = true }
                        }
                    },
                    onVolverALogin = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Inicio.ruta) {
                val usuario = viewModel.usuarioActual
                if (usuario != null) {
                    InicioScreen(usuario)
                }
            }

            composable(Screen.Materias.ruta) {
                val usuario = viewModel.usuarioActual
                if (usuario != null) {
                    MateriasScreen(
                        usuario = usuario,
                        onAgregarMateria = { nombre ->
                            viewModel.agregarMateria(usuario, nombre)
                        },
                        onAgregarTarea = { materia, titulo, fechaLimite ->
                            viewModel.agregarTarea(materia, titulo, fechaLimite)
                        },
                        onAlternarTarea = { materia, tarea ->
                            viewModel.alternarTareaCompletada(materia, tarea)
                        },
                        onEditarMateria = { materia, nuevoNombre ->
                            viewModel.editarMateria(usuario, materia, nuevoNombre)
                        },
                        onArchivarMateria = { materia ->
                            viewModel.alternarArchivadaMateria(usuario, materia)
                        },
                        onEliminarMateria = { materia ->
                            viewModel.eliminarMateria(usuario, materia)
                        }
                    )
                }
            }

            composable(Screen.Calendario.ruta) {
                val usuario = viewModel.usuarioActual
                if (usuario != null) {
                    CalendarioScreen(
                        usuario = usuario,
                        onAgregarEvento = { materia, titulo, tipo, fecha, horaInicio, horaFin ->
                            viewModel.agregarEvento(materia, titulo, tipo, fecha, horaInicio, horaFin)
                        }
                    )
                }
            }

            composable(Screen.Notas.ruta) {
                val usuario = viewModel.usuarioActual
                if (usuario != null) {
                    NotasScreen(
                        usuario = usuario,
                        onAgregarNota = { materia, titulo, contenido ->
                            viewModel.agregarNota(materia, titulo, contenido)
                        },
                        onAlternarFavorita = { materia, nota ->
                            viewModel.alternarFavorita(materia, nota)
                        },
                        onEditarNota = { materia, nota, titulo, contenido ->
                            viewModel.editarNota(materia, nota, titulo, contenido)
                        },
                        onEliminarNota = { materia, nota ->
                            viewModel.eliminarNota(materia, nota)
                        }
                    )
                }
            }

            composable(Screen.Perfil.ruta) {
                val usuario = viewModel.usuarioActual
                if (usuario != null) {
                    PerfilScreen(
                        usuario = usuario,
                        onMostrarMensaje = mostrarMensaje,
                        onEditarPerfil = { nombre, carrera, anio, email ->
                            viewModel.editarPerfil(usuario, nombre, carrera, anio, email)
                        },
                        onCerrarSesion = {
                            viewModel.cerrarSesion()
                            alcance.launch { sesionDataStore.borrarSesion() }
                            navController.navigate(Screen.Login.ruta) {
                                popUpTo(Screen.Inicio.ruta) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
