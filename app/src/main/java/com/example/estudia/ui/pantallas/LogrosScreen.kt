package com.example.estudia.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.ui.componentes.BloqueColor
import com.example.estudia.ui.componentes.CirculoBloque
import com.example.estudia.ui.componentes.Medalla
import com.example.estudia.ui.componentes.aspecto
import com.example.estudia.ui.componentes.colorAcentoTexto
import com.example.estudia.ui.componentes.progresoAnimado
import com.example.estudia.ui.theme.BloqueNaranja
import com.example.estudia.viewmodel.Logro
import com.example.estudia.viewmodel.LogrosViewModel

// Pantalla "Logros": un resumen arriba y todas las medallas en una grilla
// de bloques. Las conseguidas van en su color; las demás, en gris con su
// avance. Se abre desde Perfil. Los cálculos los hace LogrosViewModel.
@Composable
fun LogrosScreen(viewModel: LogrosViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                }
                Column {
                    Text(text = stringResource(R.string.logros_titulo), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = stringResource(R.string.logros_subtitulo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Resumen: cuántos van, con una barra de avance.
        item(span = { GridItemSpan(maxLineSpan) }) {
            BloqueColor(color = BloqueNaranja, modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CirculoBloque(tamano = 48.dp) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = stringResource(R.string.logros_resumen, estado.conseguidos, estado.logros.size),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    val avance = progresoAnimado(if (estado.logros.isEmpty()) 0f else estado.conseguidos.toFloat() / estado.logros.size)
                    LinearProgressIndicator(
                        progress = { avance },
                        color = BloqueNaranja,
                        trackColor = BloqueNaranja.copy(alpha = 0.16f),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                }
            }
        }

        items(estado.logros, key = { it.tipo.name }) { BloqueLogro(it) }
    }
}

// Un logro: conseguido = la medalla llena de su color y "¡Conseguido!";
// si no, la medalla apagada (gris) y cuánto falta.
@Composable
private fun BloqueLogro(logro: Logro) {
    val aspecto = aspecto(logro.tipo)
    val titulo = stringResource(aspecto.titulo)
    val descripcion = stringResource(aspecto.descripcion)
    if (logro.conseguido) {
        BloqueColor(color = aspecto.color, modifier = Modifier.height(170.dp).semantics(mergeDescendants = true) {}) {
            Column(modifier = Modifier.padding(14.dp)) {
                Medalla(logro.tipo, conseguido = true, tamano = 44.dp)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.logro_conseguido),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorAcentoTexto(aspecto.color)
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .semantics(mergeDescendants = true) {}
                .padding(14.dp)
        ) {
            Medalla(logro.tipo, conseguido = false, tamano = 44.dp)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { logro.progreso },
                    color = aspecto.color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.logro_progreso, logro.actual, logro.objetivo),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
