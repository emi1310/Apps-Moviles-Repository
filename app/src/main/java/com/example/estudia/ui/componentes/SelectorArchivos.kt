package com.example.estudia.ui.componentes

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.estudia.R

// Permiso peligroso para leer las fotos del teléfono (clase de permisos).
// Desde Android 13 se pide por tipo de archivo; antes era uno general.
val PERMISO_ARCHIVOS: String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
    else Manifest.permission.READ_EXTERNAL_STORAGE

// Selector de archivos para adjuntar en Materias y Notas. Devuelve la
// función que lo abre.
//
// La primera vez que el usuario adjunta algo se le explica para qué es el
// permiso de fotos y archivos y se le pide (en contexto, como en la clase).
// Se pide una sola vez: después, se dé o no, se abre directo el selector
// del sistema (Storage Access Framework), que funciona con o sin permiso
// porque solo da acceso al archivo elegido.
@Composable
fun selectorDeArchivos(
    permisoYaPedido: Boolean,
    onPermisoPedido: () -> Unit,
    onElegido: (Uri?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), onElegido)
    // Se conceda o no, después del diálogo del sistema se abre el selector.
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        selector.launch(TIPOS_ARCHIVO)
    }
    var mostrarExplicacion by rememberSaveable { mutableStateOf(false) }

    if (mostrarExplicacion) {
        AlertDialog(
            onDismissRequest = { mostrarExplicacion = false },
            title = { Text(stringResource(R.string.permiso_archivos_titulo)) },
            text = { Text(stringResource(R.string.explicacion_permiso_archivos)) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarExplicacion = false
                    onPermisoPedido()
                    pedirPermiso.launch(PERMISO_ARCHIVOS)
                }) { Text(stringResource(R.string.continuar)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarExplicacion = false
                    onPermisoPedido()
                    selector.launch(TIPOS_ARCHIVO)
                }) { Text(stringResource(R.string.ahora_no)) }
            }
        )
    }

    return {
        val concedido = ContextCompat.checkSelfPermission(context, PERMISO_ARCHIVOS) == PackageManager.PERMISSION_GRANTED
        if (permisoYaPedido || concedido) selector.launch(TIPOS_ARCHIVO) else mostrarExplicacion = true
    }
}
