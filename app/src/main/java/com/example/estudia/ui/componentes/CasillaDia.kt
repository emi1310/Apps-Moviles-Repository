package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.estudia.ui.theme.LocalDegradado
import java.time.LocalDate

// Un casillero del calendario: un bloquecito redondeado con el número de
// día. El día elegido va con el degradado violeta y "hoy" con un violeta
// suave. Abajo dibuja un puntito por cada materia con eventos ese día, con
// el color de la materia. Los casilleros vacíos (fecha == null) no se pueden
// tocar. Los colores salen del tema, así se ven bien en modo claro y oscuro.
@Composable
fun CasillaDia(
    fecha: LocalDate?,
    esHoy: Boolean,
    seleccionado: Boolean,
    coloresEventos: List<Long>,
    modifier: Modifier = Modifier,
    // Versión 10: los feriados llevan el número en rojo (como en los
    // calendarios de papel).
    feriado: Boolean = false,
    onSeleccionar: () -> Unit
) {
    val forma = RoundedCornerShape(14.dp)
    val fondo = when {
        fecha == null -> Modifier
        seleccionado -> Modifier.background(LocalDegradado.current, forma)
        esHoy -> Modifier.background(MaterialTheme.colorScheme.secondaryContainer, forma)
        else -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant, forma)   // velo de vidrio del tema
    }
    val colorTexto = when {
        seleccionado -> Color.White
        feriado -> RojoFeriado
        esHoy -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(forma)
            .then(fondo)
            .semantics { selected = seleccionado }
            .clickable(enabled = fecha != null, role = Role.Button, onClick = onSeleccionar),
        contentAlignment = Alignment.Center
    ) {
        if (fecha != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = fecha.dayOfMonth.toString(),
                    color = colorTexto,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (seleccionado || esHoy || feriado) FontWeight.Bold else FontWeight.Medium
                )
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
                                    // puntitos van en blanco, para que se vean.
                                    .background(if (seleccionado) colorTexto else Color(color), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Rojo de los feriados: se lee sobre el fondo claro y el oscuro.
private val RojoFeriado = Color(0xFFE5484D)
