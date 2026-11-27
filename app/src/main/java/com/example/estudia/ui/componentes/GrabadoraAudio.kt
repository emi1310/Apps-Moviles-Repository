package com.example.estudia.ui.componentes

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.estudia.R
import com.example.estudia.modelo.tiempoGrabacion
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Grabar audio (por ejemplo, la clase) para adjuntarlo a una nota.
// Devuelve la función que empieza: pide el permiso del micrófono si hace
// falta y abre la ventana de grabación (tiempo, Pausar, Marcar momento).
// Al guardar llama a onGrabado con la uri del archivo (cacheDir/grabaciones,
// que después se copia a los adjuntos), cuándo empezó y las marcas ("03:10").
@Composable
fun grabadoraDeAudio(onGrabado: (uri: String, cuando: String, marcas: List<String>) -> Unit): () -> Unit {
    val context = LocalContext.current
    val avisos = LocalAvisos.current
    val textoSinPermiso = stringResource(R.string.grabacion_sin_permiso)
    var grabando by remember { mutableStateOf(false) }

    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) grabando = true else avisos.mostrarTexto(textoSinPermiso)
    }

    if (grabando) {
        VentanaGrabacion(
            onGuardar = { uri, cuando, marcas ->
                grabando = false
                onGrabado(uri, cuando, marcas)
            },
            onCerrar = { grabando = false }
        )
    }

    return {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) grabando = true
        else pedirPermiso.launch(Manifest.permission.RECORD_AUDIO)
    }
}

@Composable
private fun VentanaGrabacion(onGuardar: (String, String, List<String>) -> Unit, onCerrar: () -> Unit) {
    val context = LocalContext.current
    val avisos = LocalAvisos.current
    val textoError = stringResource(R.string.grabacion_error)
    val alCerrar by rememberUpdatedState(onCerrar)
    val inicio = remember { LocalDateTime.now() }
    val archivo = remember { nuevoArchivoDeAudio(context, inicio) }
    var grabadora by remember { mutableStateOf<MediaRecorder?>(null) }
    var pausada by remember { mutableStateOf(false) }
    var segundos by remember { mutableLongStateOf(0L) }
    val marcas = remember { mutableStateListOf<String>() }
    var terminada by remember { mutableStateOf(false) }

    // Arranca al abrir la ventana; si se cierra sin guardar, se borra el archivo.
    DisposableEffect(Unit) {
        val nueva = runCatching { empezarGrabacion(context, archivo) }.getOrNull()
        if (nueva == null) {
            avisos.mostrarTexto(textoError)
            alCerrar()
        }
        grabadora = nueva
        onDispose {
            runCatching { grabadora?.stop() }
            grabadora?.release()
            grabadora = null
            if (!terminada) archivo.delete()
        }
    }
    // El reloj: suma un segundo mientras no esté en pausa.
    LaunchedEffect(pausada, grabadora) {
        while (grabadora != null && !pausada) {
            delay(1000)
            segundos++
        }
    }

    val pulso by rememberInfiniteTransition(label = "grabando").animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "punto"
    )

    DialogoApp(
        titulo = stringResource(R.string.grabacion_titulo),
        icono = Icons.Outlined.Mic,
        textoConfirmar = stringResource(R.string.grabacion_guardar),
        onConfirmar = {
            val actual = grabadora
            val ok = actual != null && runCatching { actual.stop() }.isSuccess
            actual?.release()
            grabadora = null
            if (ok && archivo.length() > 0) {
                terminada = true
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
                onGuardar(uri.toString(), inicio.format(DateTimeFormatter.ofPattern("dd/MM HH:mm")), marcas.toList())
            } else {
                avisos.mostrarTexto(textoError)
                onCerrar()
            }
        },
        textoCancelar = stringResource(R.string.grabacion_descartar),
        onCancelar = onCerrar,
        // Tocar afuera no corta la grabación.
        onDismissRequest = {}
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .alpha(if (pausada) 0.3f else pulso)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(tiempoGrabacion(segundos), fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
        TextoDialogo(stringResource(R.string.grabacion_explicacion))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            // Pausar solo existe desde Android 7 (API 24 es el mínimo de la app).
            BotonSuave(
                stringResource(if (pausada) R.string.grabacion_seguir else R.string.grabacion_pausar),
                if (pausada) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                MaterialTheme.colorScheme.primary,
                {
                    val actual = grabadora
                    if (actual != null) {
                        runCatching { if (pausada) actual.resume() else actual.pause() }
                            .onSuccess { pausada = !pausada }
                    }
                },
                Modifier.weight(1f)
            )
            BotonSuave(
                stringResource(R.string.grabacion_marcar),
                Icons.Outlined.BookmarkAdd,
                MaterialTheme.colorScheme.primary,
                { marcas += tiempoGrabacion(segundos) },
                Modifier.weight(1f)
            )
        }
        if (marcas.isNotEmpty()) {
            Text(
                stringResource(R.string.grabacion_marcas, marcas.joinToString(", ")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun nuevoArchivoDeAudio(context: Context, inicio: LocalDateTime): File {
    val carpeta = File(context.cacheDir, "grabaciones").apply { mkdirs() }
    // Se borran las de antes (ya se copiaron a los adjuntos).
    carpeta.listFiles()?.forEach { it.delete() }
    return File(carpeta, "grabacion_${inicio.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.m4a")
}

// AAC mono en un .m4a: se escucha con cualquier reproductor y ocupa poco
// (unos 0,25 MB por minuto: entran unos 80 minutos en el máximo de 20 MB
// de los adjuntos).
private fun empezarGrabacion(context: Context, archivo: File): MediaRecorder {
    val grabadora = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
    try {
        grabadora.setAudioSource(MediaRecorder.AudioSource.MIC)
        grabadora.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        grabadora.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        grabadora.setAudioChannels(1)
        grabadora.setAudioEncodingBitRate(32_000)
        grabadora.setAudioSamplingRate(22_050)
        grabadora.setOutputFile(archivo.absolutePath)
        grabadora.prepare()
        grabadora.start()
    } catch (e: Exception) {
        grabadora.release()
        throw e
    }
    return grabadora
}
