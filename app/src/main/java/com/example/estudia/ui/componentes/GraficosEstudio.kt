package com.example.estudia.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.Materia

// Gráfico de barras con el progreso (% de tareas completadas) de cada
// materia. Se dibuja a mano con Canvas y drawRect: cada materia es una
// fila con el nombre, una barra de fondo gris y una barra de color
// encima cuyo largo depende del progreso.
@Composable
fun GraficoBarrasProgreso(materias: List<Materia>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        materias.forEach { materia ->
            val progreso = materia.calcularProgreso().coerceIn(0, 100)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = materia.nombre,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(90.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                ) {
                    // Fondo de la barra (representa el 100%).
                    drawRect(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        size = size
                    )
                    // Relleno según el progreso real de la materia.
                    drawRect(
                        color = materia.color,
                        size = Size(width = size.width * (progreso / 100f), height = size.height)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "$progreso%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// Gráfico de torta con la distribución de las horas de estudio
// planificadas entre las materias. Se dibuja con Canvas y drawArc:
// cada materia ocupa una porción proporcional a sus horas sobre el total.
@Composable
fun GraficoTortaHoras(datos: List<Pair<Materia, Int>>, modifier: Modifier = Modifier) {
    val totalMinutos = datos.sumOf { it.second }

    if (totalMinutos <= 0) {
        Text(
            text = "Todavía no hay bloques de estudio cargados",
            style = MaterialTheme.typography.bodySmall
        )
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
                        color = materia.color,
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
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawRect(color = materia.color, size = size)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${materia.nombre} (${minutos / 60}h)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
