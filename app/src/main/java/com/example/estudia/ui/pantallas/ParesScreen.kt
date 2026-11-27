package com.example.estudia.ui.pantallas

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.modelo.FichaPar
import com.example.estudia.modelo.textoTiempoPares
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.ImagenTarjeta
import com.example.estudia.ui.componentes.colorAcentoTexto
import com.example.estudia.viewmodel.ParesUiState
import com.example.estudia.viewmodel.ParesViewModel
import kotlinx.coroutines.delay

private val RojoPares = Color(0xFFE74C3C)

// Pantalla del juego "Unir pares": un tablero con preguntas y respuestas
// mezcladas; se toca una y después su pareja. Arriba corre el cronómetro
// (con décimas) y al final se ve el tiempo y el récord de la materia.
@Composable
fun ParesScreen(viewModel: ParesViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val color = Color(estado.colorMateria)

    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(estado.terminado, estado.inicioMillis) {
        while (!estado.terminado) {
            ahora = System.currentTimeMillis()
            delay(100)
        }
    }

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
                Text(stringResource(R.string.pares_titulo), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Text(estado.nombreMateria, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!estado.cargando && !estado.faltanTarjetas) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                    Icon(Icons.Outlined.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(textoTiempoPares(estado.tiempoMillis(ahora)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        when {
            estado.cargando -> Unit
            estado.faltanTarjetas -> EstadoVacio(Icons.Outlined.Extension, stringResource(R.string.pares_faltan), stringResource(R.string.pares_faltan_sugerencia))
            estado.terminado -> ResultadoPares(estado, onOtraVez = viewModel::empezar, onVolver = onVolver)
            else -> {
                Text(
                    text = if (estado.errores > 0) stringResource(R.string.pares_errores, estado.errores) else stringResource(R.string.pares_instrucciones),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                estado.fichas.chunked(3).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        fila.forEach { ficha ->
                            FichaTablero(
                                ficha = ficha,
                                resuelta = ficha.id in estado.resueltas,
                                elegida = ficha.id == estado.elegida,
                                equivocada = ficha.id in estado.equivocadas,
                                color = color,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                onTocar = { viewModel.tocar(ficha.id) }
                            )
                        }
                        repeat(3 - fila.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FichaTablero(
    ficha: FichaPar,
    resuelta: Boolean,
    elegida: Boolean,
    equivocada: Boolean,
    color: Color,
    modifier: Modifier,
    onTocar: () -> Unit
) {
    val transparencia by animateFloatAsState(if (resuelta) 0f else 1f, label = "ficha")
    val escala by animateFloatAsState(if (resuelta) 0.8f else if (elegida) 1.04f else 1f, label = "escala")
    val borde by animateColorAsState(
        when {
            equivocada -> RojoPares
            elegida -> color
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        label = "borde"
    )
    val fondo = when {
        equivocada -> RojoPares.copy(alpha = 0.14f)
        elegida -> color.copy(alpha = 0.18f)
        ficha.esPregunta -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 104.dp)
            .alpha(transparencia)
            .scale(escala)
            .clip(MaterialTheme.shapes.medium)
            .background(fondo)
            .border(if (elegida || equivocada) 2.dp else 1.dp, borde, MaterialTheme.shapes.medium)
            .clickable(enabled = !resuelta, role = Role.Button, onClick = onTocar)
            .padding(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ficha.imagen?.let { ImagenTarjeta(it, Modifier.fillMaxWidth(), altoMaximo = 64.dp) }
            if (ficha.texto.isNotBlank()) {
                Text(
                    text = ficha.texto,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (ficha.esPregunta) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = if (ficha.imagen != null) 2 else 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ResultadoPares(estado: ParesUiState, onOtraVez: () -> Unit, onVolver: () -> Unit) {
    val fin = estado.finMillis ?: return
    val tiempo = estado.tiempoMillis(fin)
    val acento = colorAcentoTexto(Color(estado.colorMateria))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
    ) {
        Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = acento, modifier = Modifier.size(64.dp))
        Text(
            stringResource(if (estado.esRecord) R.string.pares_record else R.string.pares_listo),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(textoTiempoPares(tiempo), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = acento)
        if (estado.errores > 0) {
            Text(
                stringResource(R.string.pares_penalizacion, estado.errores),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        estado.mejorMillis?.let { mejor ->
            if (!estado.esRecord) {
                Text(
                    stringResource(R.string.pares_mejor, textoTiempoPares(mejor)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onOtraVez, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Icon(Icons.Outlined.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.pares_otra_vez))
        }
        TextButton(onClick = onVolver) { Text(stringResource(R.string.volver)) }
    }
}
