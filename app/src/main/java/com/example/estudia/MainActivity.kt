package com.example.estudia

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.modelo.PreferenciaTema
import com.example.estudia.ui.navegacion.AppNavegacion
import com.example.estudia.ui.theme.EstudiaTheme
import com.example.estudia.viewmodel.AppViewModelProvider
import com.example.estudia.viewmodel.SesionViewModel

// La Activity solo obtiene el ViewModel de la sesión, aplica el tema y
// establece el contenido. No tiene lógica de la app.
class MainActivity : ComponentActivity() {

    // by viewModels hace que el ViewModel sobreviva a rotaciones de pantalla.
    private val sesionViewModel: SesionViewModel by viewModels { AppViewModelProvider.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val tema by sesionViewModel.tema.collectAsStateWithLifecycle()
            val oscuro = when (tema) {
                PreferenciaTema.CLARO -> false
                PreferenciaTema.OSCURO -> true
                PreferenciaTema.SISTEMA -> isSystemInDarkTheme()
            }
            // Los íconos de la barra de estado (hora, batería) siguen el tema
            // elegido en la app, no solo el del sistema: si no, en tema claro
            // con el teléfono en modo oscuro quedarían blancos sobre blanco.
            DisposableEffect(oscuro) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro }
                )
                onDispose { }
            }
            EstudiaTheme(darkTheme = oscuro) {
                AppNavegacion(sesionViewModel)
            }
        }
    }
}
