package com.example.estudia.ui.pantallas

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.ui.componentes.BotonSuave
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.colorAcentoTexto
import com.example.estudia.ui.componentes.progresoAnimado
import com.example.estudia.ui.theme.BloqueVerde
import com.example.estudia.ui.theme.LocalDegradado
import com.example.estudia.viewmodel.PreguntaQuiz
import com.example.estudia.viewmodel.QuizUiState
import com.example.estudia.viewmodel.QuizViewModel
import kotlinx.coroutines.delay

private val RojoQuiz = Color(0xFFE74C3C)

// Pantalla del quiz: preguntas de opción múltiple con las tarjetas de una
// materia, con cronómetro y puntaje al final. Se abre desde el menú de la materia.
@Composable
fun QuizScreen(viewModel: QuizViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val color = Color(estado.colorMateria)

    // Cronómetro: se recalcula cada segundo mientras el quiz no terminó.
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(estado.terminado, estado.inicioMillis) {
        while (!estado.terminado) {
            ahora = System.currentTimeMillis()
            delay(1000)
        }
    }
    val segundos = (((estado.finMillis ?: ahora) - estado.inicioMillis) / 1000).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.volver))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.quiz_titulo), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Text(estado.nombreMateria, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!estado.cargando && !estado.faltanTarjetas) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(textoCronometro(segundos), style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        when {
            estado.cargando -> Unit
            estado.faltanTarjetas -> EstadoVacio(Icons.Outlined.Quiz, stringResource(R.string.quiz_faltan_tarjetas), stringResource(R.string.quiz_faltan_tarjetas_sugerencia))
            estado.terminado -> ResultadoQuiz(estado, segundos, onOtraVez = viewModel::empezar, onVolver = onVolver)
            else -> {
                val avance = progresoAnimado((estado.indice + if (estado.elegida != null) 1 else 0) / estado.preguntas.size.toFloat(), 400)
                Text(
                    stringResource(R.string.quiz_pregunta_de, estado.indice + 1, estado.preguntas.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { avance },
                    color = color,
                    trackColor = color.copy(alpha = 0.18f),
                    drawStopIndicator = {},
                    gapSize = 0.dp,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                )
                // Cada pregunta entra desde la derecha.
                AnimatedContent(
                    targetState = estado.indice,
                    transitionSpec = { (slideInHorizontally { it / 4 } + fadeIn()) togetherWith fadeOut() },
                    label = "pregunta"
                ) { indice ->
                    estado.preguntas.getOrNull(indice)?.let { pregunta ->
                        PreguntaActual(pregunta, if (indice == estado.indice) estado.elegida else null, color, viewModel::elegir)
                    }
                }
                if (estado.elegida != null) {
                    val ultima = estado.indice + 1 == estado.preguntas.size
                    BotonSuave(
                        stringResource(if (ultima) R.string.quiz_ver_resultado else R.string.quiz_siguiente),
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        color,
                        viewModel::siguiente,
                        Modifier.fillMaxWidth(),
                        relleno = true
                    )
                }
            }
        }
    }
}

private fun textoCronometro(segundos: Long): String = "%d:%02d".format(segundos / 60, segundos % 60)

@Composable
private fun PreguntaActual(pregunta: PreguntaQuiz, elegida: Int?, color: Color, onElegir: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        pregunta.imagen?.let { com.example.estudia.ui.componentes.ImagenTarjeta(it, Modifier.fillMaxWidth()) }
        if (pregunta.pregunta.isNotBlank()) Text(
            text = pregunta.pregunta,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(color.copy(alpha = 0.12f))
                .padding(20.dp)
        )
        pregunta.opciones.forEachIndexed { i, opcion ->
            // Después de elegir: la correcta en verde y la elegida (si estaba mal) en rojo.
            val estadoOpcion = when {
                elegida == null -> null
                i == pregunta.correcta -> true
                i == elegida -> false
                else -> null
            }
            val borde = when (estadoOpcion) {
                true -> BloqueVerde
                false -> RojoQuiz
                null -> MaterialTheme.colorScheme.outlineVariant
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        when (estadoOpcion) {
                            true -> BloqueVerde.copy(alpha = 0.14f)
                            false -> RojoQuiz.copy(alpha = 0.14f)
                            null -> MaterialTheme.colorScheme.surfaceContainer
                        }
                    )
                    .border(1.5.dp, borde, RoundedCornerShape(18.dp))
                    .clickable(enabled = elegida == null, role = Role.Button) { onElegir(i) }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = ('A' + i).toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorAcentoTexto(color),
                    modifier = Modifier.width(24.dp)
                )
                Text(opcion, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                when (estadoOpcion) {
                    true -> Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.quiz_correcta), tint = BloqueVerde)
                    false -> Icon(Icons.Outlined.Cancel, contentDescription = stringResource(R.string.quiz_incorrecta), tint = RojoQuiz)
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun ResultadoQuiz(estado: QuizUiState, segundos: Long, onOtraVez: () -> Unit, onVolver: () -> Unit) {
    val mensaje = when {
        estado.puntaje >= 90 -> R.string.quiz_mensaje_excelente
        estado.puntaje >= 60 -> R.string.quiz_mensaje_bien
        else -> R.string.quiz_mensaje_repasar
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(LocalDegradado.current)
            .padding(24.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("${estado.puntaje}%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            stringResource(R.string.quiz_aciertos, estado.aciertos, estado.preguntas.size),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Text(
            stringResource(R.string.quiz_tiempo, textoCronometro(segundos)),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            stringResource(mensaje),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BotonSuave(stringResource(R.string.quiz_otra_vez), Icons.Outlined.Refresh, MaterialTheme.colorScheme.primary, onOtraVez, Modifier.weight(1f), relleno = true)
        BotonSuave(stringResource(R.string.volver), Icons.AutoMirrored.Outlined.ArrowBack, MaterialTheme.colorScheme.primary, onVolver, Modifier.weight(1f))
    }
}
