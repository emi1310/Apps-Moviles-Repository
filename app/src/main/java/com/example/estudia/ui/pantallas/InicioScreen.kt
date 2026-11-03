package com.example.estudia.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.modelo.SesionEstudio
import com.example.estudia.ui.componentes.Deslizable
import com.example.estudia.ui.componentes.GraficoBarrasProgreso
import com.example.estudia.ui.componentes.GraficoTortaHoras
import com.example.estudia.ui.componentes.TarjetaEventoResumen
import com.example.estudia.ui.componentes.TarjetaSeccion
import com.example.estudia.ui.componentes.TarjetaTareaPendiente
import com.example.estudia.ui.theme.AmarilloFavorita
import com.example.estudia.ui.theme.LocalDegradado
import com.example.estudia.viewmodel.InicioUiState
import com.example.estudia.viewmodel.InicioViewModel
import kotlinx.coroutines.delay

// Pantalla "Inicio": resumen de todas las materias — tareas pendientes,
// próximos exámenes y entregas, una nota destacada y las horas de estudio.
// Solo dibuja el UiState: los cálculos los hace InicioViewModel.
@Composable
fun InicioScreen(viewModel: InicioViewModel, onAbrirTemporizador: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val sesionEstudio by viewModel.sesionEstudio.collectAsStateWithLifecycle()

    if (estado.cargando) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EncabezadoInicio(estado)
        TarjetaTemporizador(sesionEstudio, onAbrirTemporizador)

        // ---------- Tareas pendientes ----------
        TarjetaSeccion(
            titulo = stringResource(R.string.tareas_pendientes_titulo),
            icono = Icons.Filled.TaskAlt,
            color = MaterialTheme.colorScheme.primary,
            detalle = estado.tareasPendientes.size.toString()
        ) {
            if (estado.tareasPendientes.isEmpty()) {
                TextoSecundario(stringResource(R.string.sin_tareas_pendientes_feliz))
            }
            estado.tareasPendientes.take(5).forEach { item ->
                // Deslizar hacia la derecha la completa (y sale de la lista).
                key(item.tarea.id) {
                    Deslizable(
                        onCompletar = { viewModel.alternarTarea(item) },
                        forma = RoundedCornerShape(12.dp),
                        fondoContenido = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        TarjetaTareaPendiente(item, estado.hoy, onAlternar = { viewModel.alternarTarea(item) })
                    }
                }
            }
        }

        // ---------- Próximos exámenes y entregas ----------
        TarjetaSeccion(
            titulo = stringResource(R.string.proximos_eventos),
            icono = Icons.Filled.Event,
            color = MaterialTheme.colorScheme.tertiary
        ) {
            if (estado.proximosEventos.isEmpty()) {
                TextoSecundario(stringResource(R.string.sin_eventos_proximos))
            }
            estado.proximosEventos.take(3).forEach { TarjetaEventoResumen(it, estado.hoy) }
        }

        // ---------- Nota destacada ----------
        val nota = estado.notaDestacada
        if (nota != null) {
            TarjetaSeccion(
                titulo = stringResource(R.string.nota_destacada),
                icono = Icons.Filled.Star,
                color = AmarilloFavorita
            ) {
                Text(text = nota.titulo, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = nota.contenido,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
        }

        if (estado.materiasActivas.isNotEmpty()) {
            TarjetaSeccion(
                titulo = stringResource(R.string.progreso_por_materia),
                icono = Icons.Filled.BarChart,
                color = MaterialTheme.colorScheme.secondary
            ) {
                GraficoBarrasProgreso(materias = estado.materiasActivas)
            }
            TarjetaSeccion(
                titulo = stringResource(R.string.horas_por_materia),
                icono = Icons.Filled.DonutLarge,
                color = MaterialTheme.colorScheme.primary,
                detalle = stringResource(R.string.horas_corto, estado.horasEstudio)
            ) {
                GraficoTortaHoras(datos = estado.minutosPorMateria)
            }
        }
    }
}

// Encabezado con degradado: saludo, fecha de hoy y tres números clave.
@Composable
private fun EncabezadoInicio(estado: InicioUiState) {
    val dias = stringArrayResource(R.array.dias_semana_largo)
    val meses = stringArrayResource(R.array.meses)
    val hoy = estado.hoy
    val fecha = stringResource(
        R.string.fecha_larga,
        dias[hoy.dayOfWeek.value - 1],
        hoy.dayOfMonth,
        meses[hoy.monthValue - 1].lowercase()
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(LocalDegradado.current)
            .padding(20.dp)
    ) {
        Text(text = fecha, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
        Text(
            text = stringResource(R.string.saludo, estado.nombre),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Text(
            text = stringResource(R.string.saludo_subtitulo),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DatoEncabezado(estado.tareasPendientes.size.toString(), stringResource(R.string.pendientes), Modifier.weight(1f))
            DatoEncabezado(estado.proximosEventos.size.toString(), stringResource(R.string.proximos), Modifier.weight(1f))
            DatoEncabezado(stringResource(R.string.horas_corto, estado.horasEstudio), stringResource(R.string.de_estudio), Modifier.weight(1f))
        }
    }
}

@Composable
private fun DatoEncabezado(valor: String, etiqueta: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = valor, style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text(text = etiqueta, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun TextoSecundario(texto: String) {
    Text(text = texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// Acceso al temporizador de estudio. Si hay una sesión en curso, muestra
// la materia y cuánto falta (se actualiza cada segundo).
@Composable
private fun TarjetaTemporizador(sesion: SesionEstudio?, onAbrir: () -> Unit) {
    val ahora by produceState(System.currentTimeMillis(), sesion) {
        while (sesion != null && !sesion.enPausa) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    Card(
        onClick = onAbrir,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (sesion == null) {
                    Text(stringResource(R.string.temporizador_titulo), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.temporizador_invitacion), style = MaterialTheme.typography.bodyMedium)
                } else {
                    val minutos = (sesion.restanteMillis(ahora) + 59_999) / 60_000
                    Text(
                        text = stringResource(R.string.temporizador_estudiando, sesion.nombreMateria),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (sesion.enPausa) stringResource(R.string.temporizador_en_pausa)
                        else stringResource(R.string.temporizador_faltan, minutos),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Icon(
                if (sesion == null) Icons.Filled.PlayArrow else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null
            )
        }
    }
}
