package com.example.estudia.ui.theme

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.example.estudia.modelo.PreferenciaTema

// Aplica el tema que eligió el usuario en el Perfil (claro, oscuro o el
// del sistema). Vive en la capa de UI para que la Activity no tenga que
// decidir nada sobre cómo se ve la app.
@Composable
fun TemaElegido(preferencia: PreferenciaTema, content: @Composable () -> Unit) {
    val oscuro = when (preferencia) {
        PreferenciaTema.CLARO -> false
        PreferenciaTema.OSCURO -> true
        PreferenciaTema.SISTEMA -> isSystemInDarkTheme()
    }
    // Los íconos de la barra de estado (hora, batería) siguen el tema
    // elegido en la app, no solo el del sistema: si no, en tema claro
    // con el teléfono en modo oscuro quedarían blancos sobre blanco.
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity, oscuro) {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro },
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro }
        )
        onDispose { }
    }
    EstudiaTheme(darkTheme = oscuro, content = content)
}
