package com.example.estudia.ui.componentes

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.estudia.ui.theme.BloqueAzul
import com.example.estudia.ui.theme.BloqueVerde
import com.example.estudia.ui.theme.DegradadoFin
import com.example.estudia.ui.theme.DegradadoInicio
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

// Efecto de vidrio de la app.
//
// - El fondo de todas las pantallas tiene unas manchas de color muy suaves
//   ("aurora"): sobre ellas las tarjetas semitransparentes del tema se ven
//   como vidrio.
// - El dock es vidrio esmerilado de verdad: desenfoca lo que pasa por detrás
//   (con la librería Haze). El desenfoque anda desde Android 12; en los
//   teléfonos más viejos queda un fondo semitransparente.

// Estado del desenfoque: lo crea AppNavegacion (marca qué se desenfoca) y lo
// usa el dock.
val LocalHaze = staticCompositionLocalOf<HazeState?> { null }

@Composable
fun esTemaOscuro(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

// Fondo con manchas de color difusas: violeta arriba a la izquierda, azul
// arriba a la derecha, verde azulado y violeta más abajo.
@Composable
fun Modifier.fondoAurora(): Modifier {
    val oscuro = esTemaOscuro()
    val base = MaterialTheme.colorScheme.background
    val intensidad = if (oscuro) 1.6f else 1f
    return this.drawBehind {
        drawRect(base)
        fun mancha(color: Color, alfa: Float, x: Float, y: Float, radio: Float) {
            val centro = Offset(size.width * x, size.height * y)
            val r = size.maxDimension * radio
            drawCircle(
                brush = Brush.radialGradient(listOf(color.copy(alpha = alfa * intensidad), Color.Transparent), centro, r),
                radius = r,
                center = centro
            )
        }
        mancha(DegradadoInicio, 0.20f, 0.05f, 0.04f, 0.55f)
        mancha(BloqueAzul, 0.13f, 0.98f, 0.18f, 0.45f)
        mancha(BloqueVerde, 0.08f, 0.10f, 0.62f, 0.40f)
        mancha(DegradadoFin, 0.15f, 0.95f, 0.92f, 0.50f)
    }
}

// Borde fino de las piezas de vidrio: más brillante arriba (donde "pega la
// luz") y casi invisible abajo.
@Composable
fun bordeVidrio(ancho: Dp = 1.dp): BorderStroke {
    val oscuro = esTemaOscuro()
    val arriba = Color.White.copy(alpha = if (oscuro) 0.16f else 0.85f)
    val abajo = Color.White.copy(alpha = if (oscuro) 0.03f else 0.25f)
    return BorderStroke(ancho, Brush.verticalGradient(listOf(arriba, abajo)))
}

// Pieza de vidrio esmerilado: sombra suave, desenfoque de lo que hay detrás,
// un tinte claro u oscuro según el tema y el borde brillante.
@Composable
fun Modifier.vidrio(forma: Shape, sombra: Dp = 16.dp): Modifier {
    val oscuro = esTemaOscuro()
    val haze = LocalHaze.current
    val tinte = if (oscuro) Color(0xFF1E1D27).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.55f)
    // Sin desenfoque (Android 11 o anterior) el tinte va más opaco para que
    // se pueda leer bien lo que hay encima.
    val tinteSinDesenfoque = if (oscuro) Color(0xFF1E1D27).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.92f)
    val estilo = HazeStyle(
        backgroundColor = MaterialTheme.colorScheme.background,
        tint = HazeTint(tinte),
        blurRadius = 22.dp,
        noiseFactor = 0.06f,
        fallbackTint = HazeTint(tinteSinDesenfoque)
    )
    return this
        .shadow(sombra, forma, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = if (oscuro) 0.6f else 0.25f))
        .clip(forma)
        .then(if (haze != null) Modifier.hazeEffect(haze, estilo) else Modifier.background(tinteSinDesenfoque))
        .border(bordeVidrio(), forma)
}

// Desenfoca lo que queda detrás de un diálogo (la pantalla de la app). Se
// llama adentro del Dialog. Anda desde Android 12 y si el teléfono lo
// permite (en modo ahorro de batería, por ejemplo, Android lo apaga); si no,
// queda el oscurecido de siempre.
@Composable
fun DesenfocarDetras(radio: Int = 48) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val ventana = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    SideEffect {
        ventana.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        ventana.attributes = ventana.attributes.apply { blurBehindRadius = radio }
        // Si de verdad se desenfoca, alcanza con un oscurecido suave.
        if (ventana.windowManager.isCrossWindowBlurEnabled) ventana.setDimAmount(0.3f)
    }
}

// Relleno de lo que va adentro de una pieza de vidrio (campos de los
// diálogos, bloques de la materia): un velo apenas visible del color del
// texto, así se sigue viendo el vidrio por detrás.
@Composable
fun rellenoVidrio(): Color = MaterialTheme.colorScheme.surfaceVariant

// ¿El teléfono puede desenfocar lo que hay detrás de una ventana? (Android
// 12 o más nuevo y que no esté apagado, por ejemplo con ahorro de batería).
@Composable
fun desenfoqueDisponible(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    val contexto = LocalContext.current
    return remember(contexto) {
        contexto.getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true
    }
}

// Fondo de las ventanas (diálogos y menús): vidrio translúcido si se puede
// desenfocar lo de atrás; si no, opaco para que se lea bien.
@Composable
fun colorVentanaVidrio(): Color {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    return if (desenfoqueDisponible()) base.copy(alpha = if (esTemaOscuro()) 0.70f else 0.76f) else base
}

// Lo mismo que DesenfocarDetras, pero para los menús (que son un "popup",
// no un diálogo): se cambian los datos de la ventana del popup.
@Composable
fun DesenfocarDetrasDelPopup(radio: Int = 36) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val vista = LocalView.current
    SideEffect {
        val raiz = vista.rootView
        val datos = raiz.layoutParams as? WindowManager.LayoutParams ?: return@SideEffect
        if (datos.blurBehindRadius == radio) return@SideEffect
        datos.flags = datos.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
        datos.blurBehindRadius = radio
        runCatching { vista.context.getSystemService(WindowManager::class.java)?.updateViewLayout(raiz, datos) }
    }
}
