package com.example.estudia.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.ui.componentes.IconoCircular
import com.example.estudia.ui.componentes.PantallaDetalle
import com.example.estudia.ui.componentes.TituloLista
import com.example.estudia.ui.componentes.progresoAnimado
import com.example.estudia.ui.componentes.textoFecha
import com.example.estudia.ui.componentes.textoHoras
import com.example.estudia.ui.theme.BloqueAzul
import com.example.estudia.ui.theme.BloqueNaranja
import com.example.estudia.ui.theme.BloqueVerde
import com.example.estudia.ui.theme.BloqueVioleta
import com.example.estudia.ui.theme.LocalDegradado
import com.example.estudia.ui.theme.NaranjaRacha
import com.example.estudia.viewmodel.ResumenSemanal
import com.example.estudia.viewmodel.ResumenSemanalViewModel

// Pantalla "Tu semana": cuánto se estudió esta semana comparado con la
// anterior, días y sesiones, tareas, racha, materias más estudiadas y lo que
// viene. Se abre desde Estadísticas o la notificación de los domingos.
@Composable
fun ResumenSemanalScreen(viewModel: ResumenSemanalViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val resumen = estado.resumen
    val meses = stringArrayResource(R.array.meses)

    PantallaDetalle(
        titulo = stringResource(R.string.resumen_titulo),
        subtitulo = resumen?.let {
            stringResource(R.string.resumen_rango, it.lunes.dayOfMonth, meses[it.lunes.monthValue - 1].lowercase(), it.hasta.dayOfMonth, meses[it.hasta.monthValue - 1].lowercase())
        },
        onVolver = onVolver
    ) {
        if (resumen == null) return@PantallaDetalle
        item { CabeceraResumen(resumen) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DatoSemana(Icons.Outlined.CalendarMonth, BloqueAzul, stringResource(R.string.resumen_dias), "${resumen.diasEstudiados}/7", Modifier.weight(1f))
                DatoSemana(Icons.Outlined.Timer, BloqueVioleta, stringResource(R.string.resumen_sesiones), resumen.sesiones.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DatoSemana(Icons.Filled.LocalFireDepartment, NaranjaRacha, stringResource(R.string.resumen_racha), resumen.racha.toString(), Modifier.weight(1f))
                DatoSemana(
                    Icons.Filled.CheckCircle, BloqueVerde, stringResource(R.string.resumen_tareas),
                    if (resumen.tareasDeLaSemana == 0) "—" else "${resumen.tareasHechas}/${resumen.tareasDeLaSemana}",
                    Modifier.weight(1f)
                )
            }
        }
        if (resumen.porMateria.isNotEmpty()) {
            item { TituloLista(stringResource(R.string.resumen_por_materia)) }
            val maximo = resumen.porMateria.first().minutos.coerceAtLeast(1)
            items(resumen.porMateria, key = { "m${it.nombre}" }) { m ->
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).semantics(mergeDescendants = true) {}) {
                    Row {
                        Text(m.nombre, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(textoHoras(m.minutos), style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val avance = progresoAnimado(m.minutos / maximo.toFloat())
                    LinearProgressIndicator(
                        progress = { avance },
                        color = Color(m.color),
                        trackColor = Color(m.color).copy(alpha = 0.16f),
                        drawStopIndicator = {},
                        gapSize = 0.dp,
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape)
                    )
                }
            }
        }
        item { TituloLista(stringResource(R.string.resumen_proximos)) }
        if (resumen.proximos.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.resumen_sin_proximos),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        items(resumen.proximos, key = { "p${it.titulo}${it.fecha}" }) { evento ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(14.dp)
            ) {
                IconoCircular(Icons.Outlined.Event, Color(evento.color))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(evento.titulo, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(evento.nombreMateria, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(textoFecha(evento.fecha), style = MaterialTheme.typography.labelLarge, color = BloqueNaranja)
            }
        }
    }
}

// Horas de la semana, comparación con la anterior y la meta.
@Composable
private fun CabeceraResumen(resumen: ResumenSemanal) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(LocalDegradado.current)
            .padding(20.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Text(stringResource(R.string.resumen_estudiaste), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
        Text(textoHoras(resumen.minutos), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White)
        val variacion = resumen.variacion
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (variacion != null) {
                Icon(
                    if (variacion >= 0) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = when {
                    variacion == null && resumen.minutosSemanaAnterior == 0L -> stringResource(R.string.resumen_sin_anterior)
                    variacion != null && variacion >= 0 -> stringResource(R.string.resumen_mas, variacion)
                    else -> stringResource(R.string.resumen_menos, -(variacion ?: 0))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
        resumen.metaHoras?.let { meta ->
            Spacer(modifier = Modifier.height(12.dp))
            val avance = progresoAnimado((resumen.minutos / (meta * 60f)).coerceIn(0f, 1f))
            LinearProgressIndicator(
                progress = { avance },
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
                drawStopIndicator = {},
                gapSize = 0.dp,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                stringResource(if (resumen.metaCumplida) R.string.resumen_meta_cumplida else R.string.resumen_meta, meta),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun DatoSemana(icono: ImageVector, color: Color, etiqueta: String, valor: String, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        IconoCircular(icono, Color.White, fondo = color)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
