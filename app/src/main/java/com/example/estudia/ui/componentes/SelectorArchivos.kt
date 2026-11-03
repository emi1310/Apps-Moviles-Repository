package com.example.estudia.ui.componentes

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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

// Permisos peligrosos para leer las fotos y archivos del teléfono (clase
// de permisos). Cambian según la versión de Android:
// - hasta Android 12: READ_EXTERNAL_STORAGE;
// - Android 13: READ_MEDIA_IMAGES;
// - Android 14+: además READ_MEDIA_VISUAL_USER_SELECTED, que es el que da
//   el sistema cuando el usuario elige "Permitir acceso limitado".
private val PERMISOS_ARCHIVOS: Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

// true si el usuario dio el permiso (completo o, en Android 14+, limitado).
fun permisoArchivosConcedido(context: Context): Boolean = PERMISOS_ARCHIVOS.any {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

// Selector de archivos para adjuntar en Materias y Notas. Devuelve la
// función que lo abre.
//
// Solo se puede adjuntar con el permiso de fotos y archivos:
// - Si ya está dado, se abre el selector.
// - La primera vez se explica para qué es y después se pide.
// - Las veces siguientes se vuelve a pedir directamente.
// - Si el usuario no lo da, NO se abre el selector y se le ofrece ir a
//   Ajustes (si eligió "No volver a preguntar", es la única forma).
@Composable
fun selectorDeArchivos(
    explicacionVista: Boolean,
    onExplicacionVista: () -> Unit,
    onElegido: (Uri?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), onElegido)
    var mostrarExplicacion by rememberSaveable { mutableStateOf(false) }
    var mostrarSinPermiso by rememberSaveable { mutableStateOf(false) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (permisoArchivosConcedido(context)) selector.launch(TIPOS_ARCHIVO) else mostrarSinPermiso = true
    }

    if (mostrarExplicacion) {
        AlertDialog(
            onDismissRequest = { mostrarExplicacion = false },
            title = { Text(stringResource(R.string.permiso_archivos_titulo)) },
            text = { Text(stringResource(R.string.explicacion_permiso_archivos)) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarExplicacion = false
                    onExplicacionVista()
                    pedirPermiso.launch(PERMISOS_ARCHIVOS)
                }) { Text(stringResource(R.string.continuar)) }
            },
            dismissButton = {
                // Sin permiso no se adjunta: se cierra y la próxima vez se vuelve a pedir.
                TextButton(onClick = {
                    mostrarExplicacion = false
                    onExplicacionVista()
                }) { Text(stringResource(R.string.ahora_no)) }
            }
        )
    }

    if (mostrarSinPermiso) {
        AlertDialog(
            onDismissRequest = { mostrarSinPermiso = false },
            title = { Text(stringResource(R.string.sin_permiso_archivos_titulo)) },
            text = { Text(stringResource(R.string.sin_permiso_archivos)) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarSinPermiso = false
                    abrirAjustesDeLaApp(context)
                }) { Text(stringResource(R.string.ir_a_ajustes)) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSinPermiso = false }) { Text(stringResource(R.string.cancelar)) }
            }
        )
    }

    return {
        when {
            permisoArchivosConcedido(context) -> selector.launch(TIPOS_ARCHIVO)
            !explicacionVista -> mostrarExplicacion = true
            else -> pedirPermiso.launch(PERMISOS_ARCHIVOS)
        }
    }
}

// Abre la pantalla de Ajustes de esta app, donde se pueden dar los permisos.
private fun abrirAjustesDeLaApp(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
