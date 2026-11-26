package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Prioridad
import com.example.estudia.ui.theme.PrioridadAlta
import com.example.estudia.ui.theme.PrioridadBaja
import com.example.estudia.ui.theme.PrioridadMedia

// Prioridad de las tareas: color, nombre, etiqueta y selector.

fun colorPrioridad(prioridad: Prioridad): Color = when (prioridad) {
    Prioridad.ALTA -> PrioridadAlta
    Prioridad.MEDIA -> PrioridadMedia
    Prioridad.BAJA -> PrioridadBaja
}

@Composable
fun nombrePrioridad(prioridad: Prioridad): String = stringResource(
    when (prioridad) {
        Prioridad.ALTA -> R.string.prioridad_alta
        Prioridad.MEDIA -> R.string.prioridad_media
        Prioridad.BAJA -> R.string.prioridad_baja
    }
)

// Banderita con el nombre de la prioridad, en una pastilla de su color.
// La media es la normal: no se muestra, así la lista no se llena de etiquetas.
@Composable
fun EtiquetaPrioridad(prioridad: Prioridad, modifier: Modifier = Modifier) {
    if (prioridad == Prioridad.MEDIA) return
    val color = colorPrioridad(prioridad)
    val descripcion = stringResource(R.string.prioridad_descripcion, nombrePrioridad(prioridad).lowercase())
    Row(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = descripcion }
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Flag, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = nombrePrioridad(prioridad),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

// Tres bloques para elegir la prioridad (en el diálogo de la tarea). El
// elegido se pinta de su color con el texto en blanco.
@Composable
fun SelectorPrioridad(elegida: Prioridad, onElegir: (Prioridad) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Prioridad.BAJA, Prioridad.MEDIA, Prioridad.ALTA).forEach { opcion ->
            val color = colorPrioridad(opcion)
            val activa = opcion == elegida
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (activa) color else color.copy(alpha = 0.12f))
                    .selectable(selected = activa, role = Role.RadioButton) { onElegir(opcion) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Flag,
                    contentDescription = null,
                    tint = if (activa) Color.White else color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                TextoAjustable(
                    nombrePrioridad(opcion), MaterialTheme.typography.labelLarge, Modifier.padding(vertical = 6.dp),
                    color = if (activa) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
