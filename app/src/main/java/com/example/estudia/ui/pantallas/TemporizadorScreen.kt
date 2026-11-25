package com.example.estudia.ui.pantallas

import android.Manifest
import android.os.Build
import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.estudia.ui.componentes.IconoCircular
import com.example.estudia.ui.theme.BloqueVioleta
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
import com.example.estudia.ui.componentes.DialogoApp
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.PastillaFiltro
import com.example.estudia.ui.componentes.SelectorMateria
import com.example.estudia.ui.componentes.TextoDialogo
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

    // ---------- Acceso a No molestar ----------
    // No es un permiso con diálogo: se da en Ajustes. Antes se explica para
    // qué es; al volver a la app se revisa y, si lo dio, se activa solo.
    val gestorNotificaciones = remember { context.getSystemService(NotificationManager::class.java) }
    var accesoNoMolestar by remember { mutableStateOf(gestorNotificaciones.isNotificationPolicyAccessGranted) }
    var mostrarExplicacionNoMolestar by remember { mutableStateOf(false) }
    var esperandoAcceso by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        accesoNoMolestar = gestorNotificaciones.isNotificationPolicyAccessGranted
        if (esperandoAcceso && accesoNoMolestar) viewModel.cambiarSilenciar(true)
        esperandoAcceso = false
        onPauseOrDispose { }
    }

    if (mostrarExplicacionNoMolestar) {
        DialogoApp(
            titulo = stringResource(R.string.permiso_no_molestar_titulo),
            icono = Icons.Filled.DoNotDisturbOn,
            textoConfirmar = stringResource(R.string.ir_a_ajustes),
            onConfirmar = {
                mostrarExplicacionNoMolestar = false
                esperandoAcceso = true
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            },
            textoCancelar = stringResource(R.string.ahora_no),
            onCancelar = { mostrarExplicacionNoMolestar = false }
        ) {
            TextoDialogo(stringResource(R.string.explicacion_permiso_no_molestar))
        }
    }

    if (mostrarExplicacion) {
        DialogoApp(
            titulo = stringResource(R.string.permiso_temporizador_titulo),
            icono = Icons.Filled.Timer,
            textoConfirmar = stringResource(R.string.continuar),
            onConfirmar = {
                mostrarExplicacion = false
                yaSeExplico = true
                pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            textoCancelar = stringResource(R.string.ahora_no),
            onCancelar = {
                mostrarExplicacion = false
                yaSeExplico = true
                viewModel.empezar()
            },
            onDismissRequest = { mostrarExplicacion = false }
        ) {
            TextoDialogo(stringResource(R.string.explicacion_permiso_temporizador))
        }
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
            // Pastillas con el color de cada materia (se deslizan si son muchas).
            SelectorMateria(estado.materias, estado.materiaElegida, viewModel::elegirMateria)
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.temporizador_duracion), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DURACIONES_TEMPORIZADOR.forEach { minutos ->
                    PastillaFiltro(stringResource(R.string.minutos_corto, minutos), elegida = minutos == estado.minutosElegidos) {
                        viewModel.elegirMinutos(minutos)
                    }
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
        // ---------- Silenciar el teléfono (No molestar) ----------
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            IconoCircular(Icons.Filled.DoNotDisturbOn, Color.White, fondo = BloqueVioleta)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.no_molestar_titulo), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(if (estado.silenciar && !accesoNoMolestar) R.string.no_molestar_sin_acceso else R.string.no_molestar_descripcion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = estado.silenciar && accesoNoMolestar,
                onCheckedChange = { activar ->
                    when {
                        !activar -> viewModel.cambiarSilenciar(false)
                        accesoNoMolestar -> viewModel.cambiarSilenciar(true)
                        else -> mostrarExplicacionNoMolestar = true
                    }
                }
            )
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
