package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.LocalDate

// Un casillero del calendario: muestra el número de día. Si es "hoy" lo
// resalta con un círculo de fondo, y si es el día seleccionado, con el
// color principal. Abajo dibuja un puntito por cada materia con eventos ese
// día, con el color de la materia. Los casilleros vacíos (fecha == null) no
// se pueden tocar. Los colores salen del tema, así se ven bien en modo claro
// y oscuro.
@Composable
fun CasillaDia(
    fecha: LocalDate?,
    esHoy: Boolean,
    seleccionado: Boolean,
    coloresEventos: List<Long>,
    modifier: Modifier = Modifier,
    onSeleccionar: () -> Unit
) {
    val colorFondo = when {
        seleccionado -> MaterialTheme.colorScheme.primary
        esHoy -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val colorTexto = when {
        seleccionado -> MaterialTheme.colorScheme.onPrimary
        esHoy -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .background(colorFondo, shape = CircleShape)
            .clickable(enabled = fecha != null, onClick = onSeleccionar),
        contentAlignment = Alignment.Center
    ) {
        if (fecha != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = fecha.dayOfMonth.toString(), color = colorTexto)
                if (coloresEventos.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        coloresEventos.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    // Sobre el día elegido (fondo violeta) los
                                    // puntitos van del color del texto, para que se vean.
                                    .background(if (seleccionado) colorTexto else Color(color), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}
