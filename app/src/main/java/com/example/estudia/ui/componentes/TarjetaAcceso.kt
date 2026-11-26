package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Ícono blanco en un cuadrado redondeado con degradado de su color (más
// claro arriba), un brillo suave y un borde fino: una pieza de vidrio de color.
@Composable
fun IconoDegradado(icono: ImageVector, color: Color, tamano: Dp = 40.dp) {
    val forma = RoundedCornerShape(tamano * 0.32f)
    Box(
        modifier = Modifier
            .size(tamano)
            .clip(forma)
            .background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.28f), color)))
            .drawWithContent {
                drawContent()
                // Brillo en la mitad de arriba.
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), endY = size.height * 0.55f))
            }
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent)), forma),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(tamano * 0.52f))
    }
}

// Acceso a otra pantalla desde una de las principales (ej: "Mi carrera" en
// Materias o "Asistencia" en el Calendario).
// - Apilada (para ir de a dos en una fila): arriba el ícono y la flechita,
//   abajo el nombre y una línea que explica qué hay. La fila usa
//   height(IntrinsicSize.Min) y cada tarjeta fillMaxHeight(): así las dos
//   quedan siempre del mismo alto.
// - Sin apilar (a lo ancho, ej: "Tu semana"): todo en una fila.
@Composable
fun TarjetaAcceso(
    titulo: String,
    detalle: String,
    icono: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    apilada: Boolean = true,
    alTocar: () -> Unit
) {
    val forma = RoundedCornerShape(22.dp)
    val fondo = modifier
        .clip(forma)
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .border(bordeVidrio(), forma)
        .clickable(role = Role.Button, onClick = alTocar)
    if (apilada) {
        Column(modifier = fondo.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.Top) {
                IconoDegradado(icono, color)
                Spacer(modifier = Modifier.weight(1f))
                Flechita()
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                // El nombre en un renglón (si no entra, se achica un poco).
                TextoAjustable(titulo, MaterialTheme.typography.titleSmall, maxLines = 1, minimo = 11.sp)
                TextoAjustable(detalle, MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = fondo.heightIn(min = 64.dp).padding(12.dp)
        ) {
            IconoDegradado(icono, color)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                TextoAjustable(titulo, MaterialTheme.typography.titleSmall, maxLines = 2)
                TextoAjustable(detalle, MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Flechita()
        }
    }
}

// Flechita en un circulito translúcido.
@Composable
private fun Flechita() {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}
