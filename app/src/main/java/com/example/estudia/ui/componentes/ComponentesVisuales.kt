package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.TipoEvento

// Piezas visuales que se repiten en varias pantallas. Tenerlas en un solo
// lugar hace que toda la app se vea consistente (mismos márgenes, mismos
// tamaños, mismos colores).

// Título grande de cada pantalla, con un subtítulo opcional y lugar para
// botones a la derecha (ej: "Ir a hoy" en el calendario).
@Composable
fun EncabezadoPantalla(
    titulo: String,
    subtitulo: String? = null,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, style = MaterialTheme.typography.headlineSmall)
            if (subtitulo != null) {
                Text(
                    text = subtitulo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        acciones()
    }
}

// Ícono dentro de un círculo de color suave (el color del ícono con poca
// opacidad de fondo). Se usa en secciones, listas y estados vacíos.
@Composable
fun IconoCircular(
    icono: ImageVector,
    color: Color,
    tamano: Dp = 40.dp,
    fondo: Color = color.copy(alpha = 0.14f)
) {
    Box(
        modifier = Modifier.size(tamano).clip(CircleShape).background(fondo),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(tamano * 0.55f))
    }
}

// Mensaje para cuando una lista está vacía: ícono grande, título y una
// sugerencia de qué hacer. Queda más amable que un texto suelto.
@Composable
fun EstadoVacio(icono: ImageVector, titulo: String, mensaje: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconoCircular(icono, MaterialTheme.colorScheme.primary, tamano = 72.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Tarjeta de una sección (ej: "Tareas pendientes" en Inicio), con ícono,
// título y un detalle opcional a la derecha.
@Composable
fun TarjetaSeccion(
    titulo: String,
    icono: ImageVector,
    color: Color,
    detalle: String? = null,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoCircular(icono, color, tamano = 32.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (detalle != null) {
                    Text(text = detalle, style = MaterialTheme.typography.labelMedium, color = color)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            contenido()
        }
    }
}

// Etiqueta chica con el nombre de una materia, teñida con su color.
@Composable
fun ChipMateria(nombre: String, color: Long) {
    val c = Color(color)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(c))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = nombre,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Ícono de cada tipo de evento.
fun iconoTipo(tipo: TipoEvento): ImageVector = when (tipo) {
    TipoEvento.CLASE -> Icons.Filled.School
    TipoEvento.EXAMEN -> Icons.Filled.Quiz
    TipoEvento.ENTREGA -> Icons.AutoMirrored.Filled.Assignment
    else -> Icons.Filled.Timer
}
