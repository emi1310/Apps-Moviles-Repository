package com.example.estudia.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Esquema de colores de Material 3 armado con la paleta de Color.kt.
// Las tarjetas (Card) usan surfaceContainerHighest: en modo claro son
// blancas sobre un fondo lavanda muy suave, así se despegan del fondo.
private val EsquemaClaro = lightColorScheme(
    primary = VioletaPrincipal,
    onPrimary = Color.White,
    primaryContainer = VioletaContenedor,
    onPrimaryContainer = VioletaSobreContenedor,
    secondary = VerdeSecundario,
    onSecondary = Color.White,
    // Material usa secondaryContainer para lo "seleccionado" (chips, barra
    // inferior): se usa un violeta claro para que todo combine.
    secondaryContainer = VioletaContenedor,
    onSecondaryContainer = VioletaSobreContenedor,
    tertiary = NaranjaAcento,
    onTertiary = Color.White,
    tertiaryContainer = NaranjaContenedor,
    onTertiaryContainer = NaranjaSobreContenedor,
    background = FondoClaro,
    onBackground = TextoClaro,
    surface = FondoClaro,
    onSurface = TextoClaro,
    surfaceVariant = TarjetaClaraAlta,
    onSurfaceVariant = TextoSecundarioClaro,
    surfaceContainerLowest = TarjetaClara,
    surfaceContainerLow = TarjetaClara,
    surfaceContainer = TarjetaClara,
    surfaceContainerHigh = TarjetaClara,
    surfaceContainerHighest = TarjetaClara,
    outline = BordeClaro,
    outlineVariant = BordeClaro
)

private val EsquemaOscuro = darkColorScheme(
    primary = VioletaPrincipalOscuro,
    onPrimary = Color(0xFF26178A),
    primaryContainer = VioletaContenedorOscuro,
    onPrimaryContainer = VioletaSobreContenedorOscuro,
    secondary = VerdeSecundarioOscuro,
    onSecondary = Color(0xFF003828),
    secondaryContainer = VioletaContenedorOscuro,
    onSecondaryContainer = VioletaSobreContenedorOscuro,
    tertiary = NaranjaAcentoOscuro,
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = NaranjaContenedorOscuro,
    onTertiaryContainer = NaranjaSobreContenedorOscuro,
    background = FondoOscuro,
    onBackground = TextoOscuro,
    surface = FondoOscuro,
    onSurface = TextoOscuro,
    surfaceVariant = TarjetaOscuraAlta,
    onSurfaceVariant = TextoSecundarioOscuro,
    surfaceContainerLowest = TarjetaOscura,
    surfaceContainerLow = TarjetaOscura,
    surfaceContainer = TarjetaOscura,
    surfaceContainerHigh = TarjetaOscura,
    surfaceContainerHighest = TarjetaOscura,
    outline = BordeOscuro,
    outlineVariant = BordeOscuro
)

// Bordes redondeados de toda la app (tarjetas, botones, diálogos...).
private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Degradado de los encabezados. Se elige según el modo, así que se expone
// con un CompositionLocal: cualquier pantalla lo pide con LocalDegradado.current.
val LocalDegradado = staticCompositionLocalOf {
    Brush.linearGradient(listOf(DegradadoInicio, DegradadoFin))
}

// Tema de la app. Ya no usa el "color dinámico" de Android 12+ (que tomaba
// los colores del fondo de pantalla del teléfono): así la app tiene siempre
// su propia identidad, en modo claro y oscuro.
@Composable
fun EstudiaTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val degradado = if (darkTheme) {
        Brush.linearGradient(listOf(DegradadoInicioOscuro, DegradadoFinOscuro))
    } else {
        Brush.linearGradient(listOf(DegradadoInicio, DegradadoFin))
    }
    CompositionLocalProvider(LocalDegradado provides degradado) {
        MaterialTheme(
            colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
            typography = Typography,
            shapes = Formas,
            content = content
        )
    }
}
