package com.example.estudia.ui.pantallas

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.notificaciones.Notificaciones
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.RecolectarAvisos
import com.example.estudia.ui.componentes.TarjetaSeccion
import com.example.estudia.viewmodel.DURACIONES_TEMPORIZADOR
import com.example.estudia.viewmodel.TemporizadorUiState
import com.example.estudia.viewmodel.TemporizadorViewModel

// Pantalla "Temporizador de estudio" (método Pomodoro): se elige materia y
// duración, y un anillo muestra el tiempo que falta. Al terminar, la sesión
// se guarda como bloque de estudio. La lógica está en TemporizadorViewModel.
@Composable
fun TemporizadorScreen(viewModel: TemporizadorViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RecolectarAvisos(viewModel.avisos)
    val context = LocalContext.current
    val sesion = estado.sesion
    val color = estado.materiaElegida?.let { Color(it.color) } ?: MaterialTheme.colorScheme.primary

    // Mientras corre el tiempo, la pantalla no se apaga sola.
    val vista = LocalView.current
    DisposableEffect(sesion != null && !sesion.enPausa) {
        vista.keepScreenOn = sesion != null && !sesion.enPausa
        onDispose { vista.keepScreenOn = false }
    }

    // El aviso de fin es una notificación: si falta el permiso (Android 13+),
    // al empezar primero se explica para qué es y después se pide (como en
    // el Perfil). Se empiece con o sin permiso, el temporizador anda.
    // Se explica una sola vez por visita a la pantalla, para no insistir.
    var mostrarExplicacion by remember { mutableStateOf(false) }
    var yaSeExplico by rememberSaveable { mutableStateOf(false) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.empezar()
    }
    val alEmpezar = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notificaciones.permitidas(context) && !yaSeExplico) {
            mostrarExplicacion = true
        } else {
            viewModel.empezar()
        }
    }

    if (mostrarExplicacion) {
        AlertDialog(
            onDismissRequest = { mostrarExplicacion = false },
            title = { Text(stringResource(R.string.permiso_temporizador_titulo)) },
            text = { Text(stringResource(R.string.explicacion_permiso_temporizador)) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarExplicacion = false
                    yaSeExplico = true
                    pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(stringResource(R.string.continuar)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarExplicacion = false
                    yaSeExplico = true
                    viewModel.empezar()
                }) { Text(stringResource(R.string.ahora_no)) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
            }
            Text(text = stringResource(R.string.temporizador_titulo), style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            text = stringResource(R.string.temporizador_subtitulo),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (!estado.cargando && estado.materias.isEmpty() && sesion == null) {
            EstadoVacio(Icons.Filled.Timer, stringResource(R.string.temporizador_sin_materias), stringResource(R.string.temporizador_sin_materias_sugerencia))
            return@Column
        }

        AnilloTemporizador(estado, color, Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(28.dp))

        if (sesion == null) {
            // ---------- Elegir materia y duración ----------
            Text(stringResource(R.string.temporizador_que_estudias), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            // Si hay muchas materias, la fila se desliza hacia los costados.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                estado.materias.forEach { materia ->
                    FilterChip(
                        selected = materia.id == estado.materiaElegida?.id,
                        onClick = { viewModel.elegirMateria(materia) },
                        label = { Text(materia.nombre) },
                        leadingIcon = {
                            Box(Modifier.size(10.dp).padding(1.dp)) {
                                Canvas(Modifier.fillMaxSize()) { drawCircle(Color(materia.color)) }
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.temporizador_duracion), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DURACIONES_TEMPORIZADOR.forEach { minutos ->
                    FilterChip(
                        selected = minutos == estado.minutosElegidos,
                        onClick = { viewModel.elegirMinutos(minutos) },
                        label = { Text(stringResource(R.string.minutos_corto, minutos)) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = alEmpezar,
                enabled = estado.materiaElegida != null,
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.temporizador_empezar), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            // ---------- Sesión en curso ----------
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { if (sesion.enPausa) viewModel.reanudar() else viewModel.pausar() },
                    modifier = Modifier.weight(1f).height(54.dp)
                ) {
                    Icon(if (sesion.enPausa) Icons.Filled.PlayArrow else Icons.Filled.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(if (sesion.enPausa) R.string.temporizador_reanudar else R.string.temporizador_pausar))
                }
                Button(onClick = viewModel::terminar, modifier = Modifier.weight(1f).height(54.dp)) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.temporizador_terminar))
                }
            }
            TextButton(onClick = viewModel::descartar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.temporizador_descartar))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TarjetaSeccion(
            titulo = stringResource(R.string.temporizador_hoy),
            icono = Icons.Filled.Timer,
            color = MaterialTheme.colorScheme.secondary,
            detalle = stringResource(R.string.minutos_corto, estado.minutosHoy)
        ) {
            Text(
                text = stringResource(R.string.temporizador_consejo),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Anillo con el tiempo que falta en el centro. Se va llenando con el color
// de la materia a medida que avanza la sesión.
@Composable
private fun AnilloTemporizador(estado: TemporizadorUiState, color: Color, modifier: Modifier = Modifier) {
    // Entre un segundo y el siguiente el anillo avanza de forma continua.
    val avance by animateFloatAsState(estado.avance, tween(1_000, easing = LinearEasing), label = "anillo")
    val texto = estado.textoRestante
    val sesion = estado.sesion
    val pie = when {
        sesion == null -> stringResource(R.string.temporizador_listo)
        sesion.enPausa -> stringResource(R.string.temporizador_en_pausa)
        else -> sesion.nombreMateria
    }

    Box(modifier = modifier.size(250.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grosor = 16.dp.toPx()
            val trazo = Stroke(width = grosor, cap = StrokeCap.Round)
            val margen = grosor / 2
            val tamano = androidx.compose.ui.geometry.Size(size.width - grosor, size.height - grosor)
            val esquina = androidx.compose.ui.geometry.Offset(margen, margen)
            drawArc(color.copy(alpha = 0.15f), 0f, 360f, false, esquina, tamano, style = trazo)
            // Empieza arriba (-90°) y gira en el sentido del reloj.
            drawArc(color, -90f, 360f * avance, false, esquina, tamano, style = trazo)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = texto, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Text(
                text = pie,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 180.dp)
            )
        }
    }
}
