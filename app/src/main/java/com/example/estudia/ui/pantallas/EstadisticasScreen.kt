package com.example.estudia.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.School
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.ui.componentes.EtiquetaNota
import com.example.estudia.ui.componentes.GraficoSemanas
import com.example.estudia.ui.componentes.GraficoTortaHoras
import com.example.estudia.ui.componentes.TarjetaSeccion
import com.example.estudia.ui.componentes.textoHoras
import com.example.estudia.ui.theme.LocalDegradado
import com.example.estudia.viewmodel.EstadisticasUiState
import com.example.estudia.viewmodel.EstadisticasViewModel

// Pantalla "Estadísticas de estudio": la racha de días seguidos, las horas
// de las últimas semanas, el promedio de las notas y en qué materias se
// estudió más. Solo dibuja: los cálculos los hace EstadisticasViewModel.
@Composable
fun EstadisticasScreen(viewModel: EstadisticasViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
            }
            Text(text = stringResource(R.string.estadisticas_titulo), style = MaterialTheme.typography.headlineSmall)
        }

        TarjetaRacha(estado)

        // ---------- Horas por semana ----------
        TarjetaSeccion(
            titulo = stringResource(R.string.estadisticas_horas_semana),
            icono = Icons.Filled.BarChart,
            color = MaterialTheme.colorScheme.primary,
            detalle = textoHoras(estado.minutosEstaSemana)
        ) {
            val meta = estado.metaHorasSemanales
            Text(
                text = if (meta != null) stringResource(R.string.estadisticas_meta, meta)
                else stringResource(R.string.estadisticas_sin_meta),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            GraficoSemanas(semanas = estado.semanas, metaHoras = meta)
        }

        // ---------- Promedio de las notas ----------
        TarjetaSeccion(
            titulo = stringResource(R.string.estadisticas_promedio),
            icono = Icons.Filled.School,
            color = MaterialTheme.colorScheme.tertiary
        ) {
            val promedio = estado.promedioGeneral
            if (promedio == null) {
                Text(
                    text = stringResource(R.string.estadisticas_sin_notas),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.estadisticas_promedio_general), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    EtiquetaNota(promedio)
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                estado.promediosPorMateria.forEach { (materia, promedioMateria) ->
                    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(Color(materia.color)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(materia.nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        EtiquetaNota(promedioMateria)
                    }
                }
            }
        }

        // ---------- En qué se estudió más ----------
        if (estado.minutosUltimoMes.isNotEmpty()) {
            TarjetaSeccion(
                titulo = stringResource(R.string.estadisticas_ultimo_mes),
                icono = Icons.Filled.DonutLarge,
                color = MaterialTheme.colorScheme.secondary,
                detalle = textoHoras(estado.minutosUltimoMes.sumOf { it.second })
            ) {
                GraficoTortaHoras(datos = estado.minutosUltimoMes)
            }
        }
    }
}

// Racha de días seguidos estudiando, sobre el degradado de la app.
@Composable
private fun TarjetaRacha(estado: EstadisticasUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(LocalDegradado.current)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🔥 ${estado.rachaActual}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            text = pluralStringResource(R.plurals.estadisticas_racha, estado.rachaActual),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when {
                estado.rachaActual == 0 -> stringResource(R.string.estadisticas_racha_cero)
                !estado.estudioHoy -> stringResource(R.string.estadisticas_racha_seguir)
                else -> stringResource(R.string.estadisticas_racha_hoy)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = pluralStringResource(R.plurals.estadisticas_mejor_racha, estado.mejorRacha, estado.mejorRacha),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.18f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}
