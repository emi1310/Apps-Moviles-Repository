package com.example.estudia.ui.componentes

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.estudia.R
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Cámara para sacar fotos (foto de perfil y fotos de notas o materias).
// Devuelve la función que la abre; cuando el usuario saca la foto se llama
// a onFoto con la uri (como texto) y el ViewModel la copia a la app.
//
// Permiso de cámara (permiso peligroso, se pide en tiempo de ejecución):
// - Si ya está dado, se abre la cámara.
// - Si no, primero se explica para qué es y después se pide.
// - Si el usuario no lo da, NO se abre la cámara. Si además eligió "No
//   volver a preguntar", se le ofrece ir a Ajustes.
//
// La foto la saca la app de cámara del teléfono (ACTION_IMAGE_CAPTURE) y
// la guarda en un archivo temporal de la app (cacheDir/fotos) que se le
// comparte con el FileProvider. Así Estudia+ no necesita leer la galería.
@Composable
fun camaraDeFotos(onFoto: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val avisos = LocalAvisos.current
    val textoSinCamara = stringResource(R.string.sin_app_camara)
    // La uri de la foto en curso se guarda como texto: sobrevive si Android
    // cierra la app mientras la cámara está abierta.
    var fotoEnCurso by rememberSaveable { mutableStateOf<String?>(null) }
    var mostrarExplicacion by rememberSaveable { mutableStateOf(false) }
    var mostrarBloqueado by rememberSaveable { mutableStateOf(false) }

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { sacada ->
        val uri = fotoEnCurso
        if (sacada && uri != null) onFoto(uri)
        fotoEnCurso = null
    }

    val abrirCamara: () -> Unit = {
        val uri = nuevoArchivoDeFoto(context)
        fotoEnCurso = uri.toString()
        try {
            camara.launch(uri)
        } catch (e: ActivityNotFoundException) {
            fotoEnCurso = null
            avisos.mostrarTexto(textoSinCamara)
        }
    }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        when {
            concedido -> abrirCamara()
            // Rechazado con "No volver a preguntar": solo se puede desde Ajustes.
            !debeExplicarCamara(context) -> mostrarBloqueado = true
        }
    }

    if (mostrarExplicacion) {
        DialogoApp(
            titulo = stringResource(R.string.permiso_camara_titulo),
            icono = Icons.Outlined.PhotoCamera,
            textoConfirmar = stringResource(R.string.continuar),
            onConfirmar = {
                mostrarExplicacion = false
                pedirPermiso.launch(Manifest.permission.CAMERA)
            },
            textoCancelar = stringResource(R.string.ahora_no),
            onCancelar = { mostrarExplicacion = false }
        ) {
            TextoDialogo(stringResource(R.string.explicacion_permiso_camara))
        }
    }

    if (mostrarBloqueado) {
        DialogoApp(
            titulo = stringResource(R.string.sin_permiso_camara_titulo),
            icono = Icons.Outlined.NoPhotography,
            textoConfirmar = stringResource(R.string.ir_a_ajustes),
            onConfirmar = {
                mostrarBloqueado = false
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            onCancelar = { mostrarBloqueado = false }
        ) {
            TextoDialogo(stringResource(R.string.sin_permiso_camara))
        }
    }

    return {
        val concedido = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (concedido) abrirCamara() else mostrarExplicacion = true
    }
}

// Archivo nuevo en cacheDir/fotos para la foto que va a sacar la cámara.
// Antes se borran las fotos anteriores: ya se copiaron a la carpeta de la app.
private fun nuevoArchivoDeFoto(context: Context): Uri {
    val carpeta = File(context.cacheDir, "fotos").apply { mkdirs() }
    carpeta.listFiles()?.forEach { it.delete() }
    // El nombre queda a la vista en la lista de archivos: foto_20261124_0935.jpg
    val archivo = File(carpeta, "foto_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
}

// true si Android recomienda explicar el permiso antes de volver a pedirlo.
// Si es false justo después de un rechazo, el usuario eligió "No volver a preguntar".
private fun debeExplicarCamara(context: Context): Boolean {
    var actual: Context? = context
    while (actual is ContextWrapper) {
        if (actual is Activity) return ActivityCompat.shouldShowRequestPermissionRationale(actual, Manifest.permission.CAMERA)
        actual = actual.baseContext
    }
    return false
}

