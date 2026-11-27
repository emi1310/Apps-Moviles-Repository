package com.example.estudia.ui.componentes

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.OPCIONES_META_DIARIA
import com.example.estudia.modelo.leerTarjetasDeTexto
import com.example.estudia.util.leerImagenTarjeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Imagen de una tarjeta de estudio (se lee del disco en segundo plano).
@Composable
fun ImagenTarjeta(nombre: String, modifier: Modifier = Modifier, altoMaximo: Dp = 220.dp) {
    val contexto = LocalContext.current
    val imagen by produceState<Bitmap?>(null, nombre) { value = leerImagenTarjeta(contexto, nombre) }
    imagen?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = stringResource(R.string.tarjeta_imagen),
            contentScale = ContentScale.Fit,
            modifier = modifier.heightIn(max = altoMaximo).clip(MaterialTheme.shapes.medium)
        )
    }
}

// Ventana para importar tarjetas: se pega el texto (copiado de Quizlet, de
// una planilla o de Anki) o se elige un archivo .csv / .txt. Abajo se ve
// cuántas tarjetas salen antes de confirmar.
@Composable
fun DialogoImportarTarjetas(
    onImportar: (List<Pair<String, String>>) -> Unit,
    onCancelar: () -> Unit
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var texto by remember { mutableStateOf("") }
    var errorArchivo by remember { mutableStateOf(false) }
    val tarjetas = remember(texto) { leerTarjetasDeTexto(texto) }
    val elegirArchivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        alcance.launch {
            val leido = withContext(Dispatchers.IO) {
                runCatching {
                    contexto.contentResolver.openInputStream(uri)?.use { entrada ->
                        val bytes = entrada.readBytes()
                        if (bytes.size > 2_000_000) null else String(bytes, Charsets.UTF_8)
                    }
                }.getOrNull()
            }
            errorArchivo = leido == null
            if (leido != null) texto = leido
        }
    }

    DialogoApp(
        titulo = stringResource(R.string.importar_tarjetas),
        icono = Icons.Outlined.UploadFile,
        textoConfirmar = stringResource(R.string.importar),
        onConfirmar = { if (tarjetas.isNotEmpty()) onImportar(tarjetas) },
        onCancelar = onCancelar
    ) {
        TextoDialogo(stringResource(R.string.importar_tarjetas_explicacion))
        OutlinedButton(
            onClick = { elegirArchivo.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.FileOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.importar_elegir_archivo))
        }
        CampoTexto(
            value = texto,
            onValueChange = { texto = it; errorArchivo = false },
            label = { Text(stringResource(R.string.importar_pegar)) },
            placeholder = { Text(stringResource(R.string.importar_ejemplo)) },
            minLines = 4,
            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
        )
        when {
            errorArchivo -> TextoError(stringResource(R.string.importar_error_archivo))
            texto.isNotBlank() && tarjetas.isEmpty() -> TextoError(stringResource(R.string.importar_nada))
            tarjetas.isNotEmpty() -> {
                Text(
                    pluralStringResource(R.plurals.importar_se_crean, tarjetas.size, tarjetas.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                tarjetas.take(3).forEach { (pregunta, respuesta) ->
                    Text(
                        "• $pregunta → $respuesta",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// Elegir la meta de minutos por día (o sacarla).
@Composable
fun DialogoMetaDiaria(meta: Int, onElegir: (Int) -> Unit, onCancelar: () -> Unit) {
    var elegida by remember { mutableStateOf(meta) }
    DialogoApp(
        titulo = stringResource(R.string.meta_diaria_titulo),
        icono = Icons.Outlined.Flag,
        textoConfirmar = stringResource(R.string.guardar),
        onConfirmar = { onElegir(elegida) },
        onCancelar = onCancelar
    ) {
        TextoDialogo(stringResource(R.string.meta_diaria_explicacion))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PastillaFiltro(stringResource(R.string.meta_diaria_sin), elegida = elegida == 0) { elegida = 0 }
            OPCIONES_META_DIARIA.forEach { minutos ->
                PastillaFiltro(textoMinutos(minutos), elegida = elegida == minutos) { elegida = minutos }
            }
        }
    }
}

// "45 min", "1 h", "1 h 30 min".
@Composable
fun textoMinutos(minutos: Int): String = when {
    minutos < 60 -> stringResource(R.string.minutos_corto, minutos)
    minutos % 60 == 0 -> stringResource(R.string.horas_corto, minutos / 60)
    else -> stringResource(R.string.horas_minutos_corto, minutos / 60, minutos % 60)
}

// Anillo chico con el avance de la meta del día y los minutos en el centro.
@Composable
fun AnilloMeta(avance: Float, centro: String, color: Color, tamano: Dp = 64.dp) {
    val animado by animateFloatAsState(avance, label = "meta")
    Box(modifier = Modifier.size(tamano), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grosor = 7.dp.toPx()
            val trazo = Stroke(width = grosor, cap = StrokeCap.Round)
            val tam = androidx.compose.ui.geometry.Size(size.width - grosor, size.height - grosor)
            val esquina = androidx.compose.ui.geometry.Offset(grosor / 2, grosor / 2)
            drawArc(color.copy(alpha = 0.18f), 0f, 360f, false, esquina, tam, style = trazo)
            drawArc(color, -90f, 360f * animado, false, esquina, tam, style = trazo)
        }
        Text(centro, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

// Fila con el anillo de la meta y el texto (para el temporizador).
@Composable
fun FilaMetaDiaria(minutosHoy: Int, meta: Int, avance: Float, color: Color, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        AnilloMeta(avance, if (meta > 0) "${(avance * 100).toInt()}%" else "—", color)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.meta_diaria_titulo), style = MaterialTheme.typography.titleSmall)
            Text(
                text = when {
                    meta <= 0 -> stringResource(R.string.meta_diaria_sin_fijar, textoMinutos(minutosHoy))
                    minutosHoy >= meta -> stringResource(R.string.meta_diaria_cumplida, textoMinutos(minutosHoy))
                    else -> stringResource(R.string.meta_diaria_progreso, textoMinutos(minutosHoy), textoMinutos(meta))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
