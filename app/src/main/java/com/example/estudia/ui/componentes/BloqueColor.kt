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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Estilo "bloque de color" de Materias, Calendario y Notas: la tarjeta entera
// va del color de la materia con un degradado suave y el texto en blanco.

// Blanco translúcido de los círculos y pastillas sobre un bloque de color.
val BlancoTranslucido = Color.White.copy(alpha = 0.22f)

// El color de la materia y un tono un poco más oscuro.
fun degradadoBloque(color: Color): Brush =
    Brush.linearGradient(listOf(color, Color.Black.copy(alpha = 0.18f).compositeOver(color)))

// Tarjeta de color con esquinas grandes. Todo lo de adentro (textos e
// íconos) toma el blanco por defecto.
@Composable
fun BloqueColor(color: Color, modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(degradadoBloque(color))
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            contenido()
        }
    }
}

// Círculo blanco translúcido para el ícono o la inicial de un bloque.
@Composable
fun CirculoBloque(tamano: Dp = 44.dp, contenido: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier.size(tamano).clip(CircleShape).background(BlancoTranslucido),
        contentAlignment = Alignment.Center,
        content = contenido
    )
}
