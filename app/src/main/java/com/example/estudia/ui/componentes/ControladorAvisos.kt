package com.example.estudia.ui.componentes

import android.content.res.Resources
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.estudia.R
import com.example.estudia.viewmodel.Aviso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// Muestra en el Snackbar los avisos que mandan los ViewModels.
//
// Si el aviso tiene "Deshacer", espera a ver qué hace el usuario y ejecuta
// alDeshacer o alConfirmar. Usa el scope de AppNavegacion (no el del
// ViewModel de la pantalla): así el "Deshacer" funciona aunque el usuario
// ya se haya ido a otra pantalla mientras el Snackbar seguía visible.
class ControladorAvisos(
    private val host: SnackbarHostState,
    private val scope: CoroutineScope,
    private val recursos: Resources
) {
    fun mostrar(aviso: Aviso) {
        val texto = recursos.getString(aviso.mensaje, *aviso.argumentos.toTypedArray())
        mostrarTexto(texto, aviso.alDeshacer, aviso.alConfirmar)
    }

    fun mostrarTexto(
        texto: String,
        alDeshacer: (suspend () -> Unit)? = null,
        alConfirmar: (suspend () -> Unit)? = null
    ) {
        scope.launch {
            val resultado = host.showSnackbar(
                message = texto,
                actionLabel = if (alDeshacer != null) recursos.getString(R.string.deshacer) else null,
                duration = if (alDeshacer != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (resultado == SnackbarResult.ActionPerformed) alDeshacer?.invoke() else alConfirmar?.invoke()
        }
    }
}

// Disponible para cualquier Composable de la app (lo provee AppNavegacion).
val LocalAvisos = staticCompositionLocalOf<ControladorAvisos> {
    error("LocalAvisos no fue provisto")
}

// Escucha los avisos de un ViewModel mientras la pantalla está visible.
@Composable
fun RecolectarAvisos(avisos: Flow<Aviso>) {
    val controlador = LocalAvisos.current
    LaunchedEffect(avisos) {
        avisos.collect { controlador.mostrar(it) }
    }
}
