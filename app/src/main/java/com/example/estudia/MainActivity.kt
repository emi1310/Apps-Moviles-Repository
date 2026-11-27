package com.example.estudia

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.ui.navegacion.AccionExterna
import com.example.estudia.ui.navegacion.AppNavegacion
import com.example.estudia.ui.navegacion.accionDelIntent
import com.example.estudia.ui.theme.TemaElegido
import com.example.estudia.viewmodel.AppViewModelProvider
import com.example.estudia.viewmodel.SesionViewModel

// La Activity solo obtiene el ViewModel de la sesión y establece el
// contenido. El tema lo aplica TemaElegido y el aviso al sistema del modo
// oscuro lo hace EstudiaApp.
class MainActivity : ComponentActivity() {

    // by viewModels hace que el ViewModel sobreviva a rotaciones de pantalla.
    private val sesionViewModel: SesionViewModel by viewModels { AppViewModelProvider.Factory }

    // Lo que se pidió desde afuera (un botón de un widget o la notificación
    // del temporizador). Android se lo entrega a la Activity, por eso se
    // recibe acá. Es estado de Compose, así AppNavegacion lo hace apenas cambia.
    private var accionExterna by mutableStateOf<AccionExterna?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) accionExterna = accionDelIntent(intent)
        setContent {
            val tema by sesionViewModel.tema.collectAsStateWithLifecycle()
            TemaElegido(tema) {
                AppNavegacion(
                    sesionViewModel = sesionViewModel,
                    accionExterna = accionExterna,
                    onAccionHecha = { accionExterna = null }
                )
            }
        }
    }

    // La app ya estaba abierta y se tocó un widget o la notificación.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        accionDelIntent(intent)?.let { accionExterna = it }
    }

    companion object {
        const val EXTRA_ABRIR_TEMPORIZADOR = "abrir_temporizador"
    }
}
