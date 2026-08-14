package com.example.estudia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.estudia.datos.crearUsuarioDePrueba
import com.example.estudia.modelo.Usuario
import com.example.estudia.ui.componentes.BarraInferior
import com.example.estudia.ui.navegacion.EstadoApp
import com.example.estudia.ui.navegacion.PantallaApp
import com.example.estudia.ui.pantallas.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Lista de usuarios "registrados", en memoria. Arranca con el
            // usuario de prueba (Lucas) ya cargado, para poder probar el
            // login sin tener que registrarse primero.
            val usuariosRegistrados = remember { mutableStateListOf(crearUsuarioDePrueba()) }

            var estadoApp by remember { mutableStateOf(EstadoApp.BIENVENIDA) }
            var usuarioActual by remember { mutableStateOf<Usuario?>(null) }
            var pantallaActual by remember { mutableStateOf(PantallaApp.INICIO) }

            MaterialTheme {
                Surface {
                    when (estadoApp) {

                        EstadoApp.BIENVENIDA -> BienvenidaScreen(
                            onComenzar = { estadoApp = EstadoApp.LOGIN }
                        )

                        EstadoApp.LOGIN -> LoginScreen(
                            usuariosRegistrados = usuariosRegistrados,
                            onLoginExitoso = { usuario ->
                                usuarioActual = usuario
                                estadoApp = EstadoApp.PRINCIPAL
                            },
                            onIrARegistro = { estadoApp = EstadoApp.REGISTRO }
                        )

                        EstadoApp.REGISTRO -> RegistroScreen(
                            usuariosRegistrados = usuariosRegistrados,
                            onRegistroExitoso = { usuario ->
                                usuarioActual = usuario
                                estadoApp = EstadoApp.PRINCIPAL
                            },
                            onVolverALogin = { estadoApp = EstadoApp.LOGIN }
                        )

                        EstadoApp.PRINCIPAL -> {
                            val usuario = usuarioActual
                            if (usuario != null) {
                                Scaffold(
                                    bottomBar = {
                                        BarraInferior(
                                            pantallaSeleccionada = pantallaActual,
                                            onSeleccionar = { pantallaActual = it }
                                        )
                                    }
                                ) { espacioInterno ->
                                    Surface(modifier = Modifier.padding(espacioInterno)) {
                                        when (pantallaActual) {
                                            PantallaApp.INICIO -> InicioScreen(usuario)
                                            PantallaApp.MATERIAS -> MateriasScreen(usuario)
                                            PantallaApp.CALENDARIO -> CalendarioScreen(usuario)
                                            PantallaApp.NOTAS -> NotasScreen(usuario)
                                            PantallaApp.PERFIL -> PerfilScreen(
                                                usuario = usuario,
                                                onCerrarSesion = {
                                                    usuarioActual = null
                                                    pantallaActual = PantallaApp.INICIO
                                                    estadoApp = EstadoApp.LOGIN
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}