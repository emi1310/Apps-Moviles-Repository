package com.example.estudia.ui.componentes

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

// Mientras está esta pantalla, el teclado no empuja la ventana hacia arriba:
// la pantalla misma se acomoda con WindowInsets.ime. Sin esto, algunos
// teléfonos suben todo (adjustPan) y además se suma el alto del teclado, así
// que la barra queda muy lejos del teclado. Al salir se vuelve a lo de antes.
@Composable
fun TecladoAjustable() {
    val actividad = LocalContext.current.buscarActividad() ?: return
    DisposableEffect(actividad) {
        val ventana = actividad.window
        val antes = ventana.attributes.softInputMode
        ventana.setSoftInputMode(
            (antes and WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE) or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        onDispose { ventana.setSoftInputMode(antes) }
    }
}

private tailrec fun Context.buscarActividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActividad()
    else -> null
}
