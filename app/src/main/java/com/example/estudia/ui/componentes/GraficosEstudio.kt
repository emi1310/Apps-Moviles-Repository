package com.example.estudia.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Materia
import com.example.estudia.viewmodel.SemanaEstudio

// Gráfico de barras con el progreso (% de tareas completadas) de cada
// materia. Se dibuja a mano con Canvas y drawRect: cada materia es una
// fila con el nombre, una barra de fondo y una barra de color encima
// cuyo largo depende del progreso. El fondo usa un color del tema, así
// se ve bien en modo claro y oscuro.
@Composable
fun GraficoBarrasProgreso(materias: List<Materia>, modifier: Modifier = Modifier) {
    val fondo = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier = modifier) {
        materias.forEach { materia ->
            val progreso = materia.calcularProgreso().coerceIn(0, 100)
            // key: cada barra recuerda su propia animación aunque cambie la lista.
            val avance = key(materia.id) { progresoAnimado(progreso / 100f) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = materia.nombre,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(90.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Canvas(modifier = Modifier.weight(1f).height(14.dp)) {
                    val radio = CornerRadius(size.height / 2)
                    drawRoundRect(color = fondo, size = size, cornerRadius = radio)
                    drawRoundRect(
                        color = Color(materia.color),
                        size = Size(width = size.width * avance, height = size.height),
                        cornerRadius = radio
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "$progreso%", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(40.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// Gráfico de torta con la distribución de las horas de estudio
// planificadas entre las materias. Se dibuja con Canvas y drawArc:
// cada materia ocupa una porción proporcional a sus minutos sobre el total.
@Composable
fun GraficoTortaHoras(datos: List<Pair<Materia, Long>>, modifier: Modifier = Modifier) {
    val totalMinutos = datos.sumOf { it.second }
    if (totalMinutos <= 0) {
        Text(text = stringResource(R.string.sin_bloques_estudio), style = MaterialTheme.typography.bodySmall)
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Canvas(modifier = Modifier.size(96.dp)) {
            var anguloInicial = -90f
            val grosor = size.minDimension * 0.28f
            datos.forEach { (materia, minutos) ->
                if (minutos > 0) {
                    val barrido = 360f * (minutos.toFloat() / totalMinutos)
                    drawArc(
                        color = Color(materia.color),
                        startAngle = anguloInicial,
                        sweepAngle = barrido,
                        useCenter = false,
                        topLeft = Offset(grosor / 2, grosor / 2),
                        size = Size(size.width - grosor, size.height - grosor),
                        style = Stroke(width = grosor)
                    )
                    anguloInicial += barrido
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            datos.filter { it.second > 0 }.forEach { (materia, minutos) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(10.dp)) { drawRect(color = Color(materia.color), size = size) }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.materia_horas, materia.nombre, minutos / 60),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// Gráfico de barras con las horas de estudio de cada semana (de lunes a
// domingo). Las semanas que llegaron a la meta se pintan con el color
// secundario (verde); la semana actual se resalta con el texto en negrita.
// Se arma con Box de distinta altura (no hace falta Canvas) y cada barra
// crece animada al abrir la pantalla.
@Composable
fun GraficoSemanas(
    semanas: List<SemanaEstudio>,
    metaHoras: Int?,
    modifier: Modifier = Modifier
) {
    val maximo = maxOf(semanas.maxOfOrNull { it.minutos } ?: 0, (metaHoras ?: 0) * 60L, 60L)
    val altoMaximo = 120.dp
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        semanas.forEachIndexed { indice, semana ->
            val esActual = indice == semanas.lastIndex
            val llegoALaMeta = metaHoras != null && semana.minutos >= metaHoras * 60L
            val color = if (llegoALaMeta) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            val avance = key(semana.lunes) { progresoAnimado(semana.minutos.toFloat() / maximo) }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = textoHoras(semana.minutos),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(altoMaximo * avance + 3.dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (semana.minutos == 0L) color.copy(alpha = 0.2f) else color)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${semana.lunes.dayOfMonth}/${semana.lunes.monthValue}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (esActual) FontWeight.Bold else FontWeight.Normal,
                    color = if (esActual) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 90 -> "1,5h", 120 -> "2h", 0 -> "0".
fun textoHoras(minutos: Long): String {
    if (minutos == 0L) return "0"
    val horas = Math.round(minutos / 6.0) / 10.0      // redondeado a 1 decimal
    val texto = if (horas % 1.0 == 0.0) horas.toInt().toString() else horas.toString().replace('.', ',')
    return texto + "h"
}
