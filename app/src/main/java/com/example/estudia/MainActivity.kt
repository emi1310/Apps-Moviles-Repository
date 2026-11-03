package com.example.estudia

import android.app.UiModeManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
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

    // Pedido de abrir el temporizador: llega al tocar su notificación
    // (NotificacionTemporizador). Es estado de Compose, así AppNavegacion
    // navega apenas cambia.
    private var abrirTemporizador by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) abrirTemporizador = intent.getBooleanExtra(EXTRA_ABRIR_TEMPORIZADOR, false)
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
            // En Android 12+ se le avisa al sistema el tema elegido: así la
            // pantalla de carga (que se dibuja antes que Compose) sale oscura
            // o clara según la app, y no según el teléfono.
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@LaunchedEffect
                sesionViewModel.temaGuardado.distinctUntilChanged().collect { tema ->
                    getSystemService(UiModeManager::class.java).setApplicationNightMode(
                        when (tema) {
                            PreferenciaTema.CLARO -> UiModeManager.MODE_NIGHT_NO
                            PreferenciaTema.OSCURO -> UiModeManager.MODE_NIGHT_YES
                            PreferenciaTema.SISTEMA -> UiModeManager.MODE_NIGHT_AUTO
                        }
                    )
                }
            }
            EstudiaTheme(darkTheme = oscuro) {
                AppNavegacion(
                    sesionViewModel = sesionViewModel,
                    abrirTemporizador = abrirTemporizador,
                    onTemporizadorAbierto = { abrirTemporizador = false }
                )
            }
        }
    }

    // La app ya estaba abierta y se tocó la notificación del temporizador.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_ABRIR_TEMPORIZADOR, false)) abrirTemporizador = true
    }

    companion object {
        const val EXTRA_ABRIR_TEMPORIZADOR = "abrir_temporizador"
    }
}
