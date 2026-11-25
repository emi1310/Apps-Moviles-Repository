package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Estilo de los bloques de Materias, Calendario, Notas, Horario, Logros y
// Perfil: una tarjeta neutra (el fondo de las tarjetas de la app, con el
// texto normal) y el color de la materia solo en los detalles: el círculo
// del ícono o la inicial, las barras de progreso, los números y las pastillas.

// Blanco translúcido de los círculos sobre los bloques con el degradado
// violeta (la tarjeta del usuario en Perfil y el resumen del Horario).
val BlancoTranslucido = Color.White.copy(alpha = 0.22f)

// El color del bloque en el que se está dibujando (null = no hay bloque).
// Lo usan los detalles de adentro (CirculoBloque, fondoSuave...).
val LocalColorBloque = compositionLocalOf<Color?> { null }

// Tarjeta neutra con esquinas grandes; "color" queda disponible para los
// detalles a través de LocalColorBloque.
@Composable
fun BloqueColor(color: Color, modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            LocalColorBloque provides color
        ) {
            contenido()
        }
    }
}

// Círculo del ícono o la inicial de un bloque: dentro de un bloque, un
// tinte suave del color con el ícono en ese color; sobre el degradado
// violeta, blanco translúcido.
@Composable
fun CirculoBloque(tamano: Dp = 44.dp, contenido: @Composable BoxScope.() -> Unit) {
    val color = LocalColorBloque.current
    Box(
        modifier = Modifier
            .size(tamano)
            .clip(CircleShape)
            .background(if (color != null) color.copy(alpha = 0.16f) else BlancoTranslucido),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides (if (color != null) colorAcentoTexto(color) else Color.White)) {
            contenido()
        }
    }
}

// Fondo de las pastillas dentro de un bloque (la hora de una clase, la
// fecha de una nota): un tinte suave del color.
fun fondoSuave(color: Color): Color = color.copy(alpha = 0.14f)

// El color de la materia ajustado para usarlo en textos e íconos sobre la
// tarjeta: oscurecido en el tema claro y aclarado en el oscuro, así se lee
// bien con cualquier color (por ejemplo el naranja sobre fondo blanco).
@Composable
fun colorAcentoTexto(color: Color): Color {
    val oscuro = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return (if (oscuro) Color.White.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.22f)).compositeOver(color)
}
