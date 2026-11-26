package com.example.estudia.ui.pantallas

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.ui.componentes.BloqueColor
import com.example.estudia.ui.componentes.CirculoBloque
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.RecolectarAvisos
import com.example.estudia.ui.componentes.colorAcentoTexto
import com.example.estudia.ui.componentes.fondoSuave
import com.example.estudia.ui.theme.LocalDegradado
import com.example.estudia.viewmodel.RepasoViewModel

// Pantalla de repaso de tarjetas: muestra la pregunta; al tocar "Ver
// respuesta" aparece la respuesta y se elige "La sabía" o "No la sabía".
// Cuando no quedan tarjetas para hoy, muestra cuántas se repasaron.
@Composable
fun RepasoScreen(viewModel: RepasoViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RecolectarAvisos(viewModel.avisos)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.volver))
            }
            Column {
                Text(text = stringResource(R.string.repaso_titulo), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = estado.nombreMateria ?: stringResource(R.string.repaso_todas),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (estado.cargando) return@Column

        val actual = estado.actual
        if (actual == null) {
            // Terminado (o no había nada para hoy).
            if (estado.repasadas > 0) {
                EstadoVacio(
                    Icons.Outlined.EmojiEvents,
                    stringResource(R.string.repaso_listo),
                    pluralStringResource(R.plurals.repaso_listo_detalle, estado.repasadas, estado.repasadas)
                )
            } else {
                EstadoVacio(Icons.Outlined.Style, stringResource(R.string.repaso_listo), stringResource(R.string.repaso_nada_hoy))
            }
            Button(onClick = onVolver, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.volver))
            }
            return@Column
        }

        val color = Color(actual.colorMateria)
        Text(
            text = pluralStringResource(R.plurals.repaso_quedan, estado.pendientes.size, estado.pendientes.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // La tarjeta: materia y caja arriba, la pregunta y (si se pidió) la respuesta.
        BloqueColor(color = color) {
            Column(modifier = Modifier.padding(20.dp).heightIn(min = 260.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CirculoBloque(tamano = 40.dp) {
                        Icon(Icons.Outlined.Style, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = actual.nombreMateria,
                        style = MaterialTheme.typography.labelLarge,
                        color = colorAcentoTexto(color),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.tarjeta_caja, actual.tarjeta.caja),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorAcentoTexto(color),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(fondoSuave(color))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = actual.tarjeta.pregunta,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedContent(targetState = estado.mostrandoRespuesta, label = "respuesta") { mostrando ->
                    if (mostrando) {
                        Column {
                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = actual.tarjeta.respuesta,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        if (!estado.mostrandoRespuesta) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(LocalDegradado.current),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = viewModel::verRespuesta, modifier = Modifier.fillMaxSize()) {
                    Text(stringResource(R.string.repaso_ver_respuesta), color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { viewModel.responder(false) },
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.repaso_no_la_sabia))
                }
                Button(
                    onClick = { viewModel.responder(true) },
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.repaso_la_sabia))
                }
            }
        }
        Text(
            text = stringResource(R.string.repaso_explicacion),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
