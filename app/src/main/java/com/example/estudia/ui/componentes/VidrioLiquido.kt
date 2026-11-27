package com.example.estudia.ui.componentes

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.sqrt

// Vidrio líquido: casi transparente, con refracción en los bordes (lo de
// atrás se curva cerca del borde, como a través de una lente), un borde con
// volumen (más brillante del lado de la luz y con sombra del otro), un
// reflejo que sigue la inclinación del teléfono y que se "hincha" al tocarlo.
//
// Cómo funciona:
// 1. AppNavegacion guarda la pantalla en dos capas (FondoLiquido): una con
//    todo (fondo y contenido) y otra solo con el fondo "aurora".
// 2. Cada pieza de vidrio dibuja una de esas capas corrida a su posición, a
//    través de un shader (programa de la placa de video) que deforma y
//    sombrea los bordes. Encima van el velo y el reflejo.
//    - El dock usa la capa con todo: se ve el contenido pasar por detrás.
//    - Las tarjetas y bloques usan la del fondo (no pueden usar la otra
//      porque ellas mismas están adentro).
// Los shaders propios andan desde Android 13; en los teléfonos más viejos
// queda el vidrio de siempre.

class FondoLiquido(val capa: GraphicsLayer, val capaAurora: GraphicsLayer) {
    var origen by mutableStateOf(Offset.Zero)
    // La misma posición pero en la pantalla: la usan los diálogos y menús,
    // que son otra ventana.
    var origenPantalla by mutableStateOf(Offset.Zero)
    // Cambia cada vez que se vuelve a guardar la pantalla, para que el dock
    // se redibuje con lo nuevo (por ejemplo al hacer scroll).
    var version by mutableIntStateOf(0)
}

val LocalFondoLiquido = staticCompositionLocalOf<FondoLiquido?> { null }

fun vidrioLiquidoDisponible(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

// Lo crea AppNavegacion (null en Android 12 o anterior).
@Composable
fun rememberFondoLiquido(): FondoLiquido? {
    if (!vidrioLiquidoDisponible()) return null
    val capa = rememberGraphicsLayer()
    val capaAurora = rememberGraphicsLayer()
    return remember(capa, capaAurora) { FondoLiquido(capa, capaAurora) }
}

// Va en la caja que tiene el fondo y las pantallas: dibuja todo como siempre y
// además lo deja guardado en la capa.
fun Modifier.guardarFondoLiquido(fondo: FondoLiquido?): Modifier =
    if (fondo == null) this
    else this
        .onGloballyPositioned {
            fondo.origen = it.positionInRoot()
            fondo.origenPantalla = it.localToScreen(Offset.Zero)
        }
        .drawWithContent {
            fondo.capa.record { this@drawWithContent.drawContent() }
            drawLayer(fondo.capa)
            // Sin "leer" version acá: si no, esta caja se redibujaría sin fin.
            Snapshot.withoutReadObservation { fondo.version++ }
        }

// El shader:
// - Refracción: cada punto de la franja del borde muestra lo que hay un poco
//   más adentro (efecto lente) y los colores se separan apenas.
// - Volumen: el borde se aclara del lado de donde viene la luz y se oscurece
//   del otro, más una línea fina muy brillante justo en el canto.
private const val SHADER_REFRACCION = """
uniform shader contenido;
uniform float2 tamano;
uniform float radio;
uniform float franja;
uniform float fuerza;
uniform float dispersion;
uniform float2 luz;
uniform float brilloBorde;
uniform float sombraBorde;

float borde(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

half4 main(float2 c) {
    float2 b = tamano * 0.5;
    float2 p = c - b;
    float d = borde(p, b, radio);
    if (d > 0.0) return contenido.eval(c);
    // 0 en el medio, 1 justo en el borde.
    float t = clamp(1.0 + d / franja, 0.0, 1.0);
    if (t <= 0.0) return contenido.eval(c);
    float e = 0.5;
    float2 n = normalize(float2(
        borde(p + float2(e, 0.0), b, radio) - borde(p - float2(e, 0.0), b, radio),
        borde(p + float2(0.0, e), b, radio) - borde(p - float2(0.0, e), b, radio)) + 0.0001);
    float k = t * t * t;
    float2 desplazo = -n * fuerza * k;
    half4 medio = contenido.eval(c + desplazo);
    half rojo = contenido.eval(c + desplazo * (1.0 + dispersion)).r;
    half azul = contenido.eval(c + desplazo * (1.0 - dispersion)).b;
    half4 color = half4(rojo, medio.g, azul, medio.a);
    // Volumen del borde.
    float frente = dot(n, luz);
    float canto = smoothstep(0.88, 1.0, t);
    float luzBorde = k * max(frente, 0.0) * brilloBorde + canto * (0.35 + 0.65 * max(frente, 0.0)) * brilloBorde;
    float sombra = k * max(-frente, 0.0) * sombraBorde;
    half s = half(sombra);
    half lb = half(luzBorde);
    color.rgb = color.rgb * (1.0 - s) + half3(lb, lb, lb) * color.a;
    return color;
}
"""

// Borde fino del vidrio líquido (el volumen lo da el shader): brillante en
// las esquinas de arriba a la izquierda y de abajo a la derecha.
@Composable
fun bordeLiquido(ancho: Dp = 1.dp): BorderStroke {
    val oscuro = esTemaOscuro()
    return BorderStroke(
        ancho,
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = if (oscuro) 0.30f else 0.75f),
                Color.White.copy(alpha = if (oscuro) 0.04f else 0.18f),
                Color.White.copy(alpha = if (oscuro) 0.20f else 0.55f)
            )
        )
    )
}

// Pieza del dock: refracta todo lo que pasa por detrás. Sin Android 13, vidrio().
@Composable
fun Modifier.vidrioLiquido(forma: Shape, sombra: Dp = 16.dp): Modifier {
    val fondo = LocalFondoLiquido.current
    if (fondo == null || !vidrioLiquidoDisponible()) return vidrio(forma, sombra)
    val oscuro = esTemaOscuro()
    val velo = if (oscuro) Color(0xFF14131C).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.28f)
    val pieza = piezaLiquida(fondo, fondo.capa, forma, velo, fuerza = 14.dp, desenfoque = 3.dp, leerVersion = true)
        ?: return vidrio(forma, sombra)
    return this
        .shadow(sombra, forma, ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = if (oscuro) 0.5f else 0.2f))
        .then(pieza)
        .border(bordeLiquido(), forma)
}

// Tarjetas y bloques de las pantallas: el color de la tarjeta (translúcido)
// sobre el fondo refractado, con volumen, reflejo y borde. Sin Android 13
// es lo de siempre: el color y, si borde = true, el borde de vidrio.
@Composable
fun Modifier.superficieVidrio(
    forma: Shape,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    borde: Boolean = true
): Modifier {
    val fondo = LocalFondoLiquido.current
    val pieza = if (fondo != null && vidrioLiquidoDisponible()) {
        piezaLiquida(fondo, fondo.capaAurora, forma, color, fuerza = 12.dp, desenfoque = 0.dp, leerVersion = false)
    } else null
    if (pieza == null) {
        val base = this.clip(forma).background(color)
        return if (borde) base.border(bordeVidrio(), forma) else base
    }
    return this.then(pieza).border(bordeLiquido(), forma)
}

// Diálogos y menús: son otra ventana, así que dibujan la pantalla de la app
// guardada (la capa con todo) según dónde quedan en la pantalla, refractada
// en los bordes y bastante desenfocada para que se lea lo de adentro.
// Sin vidrio líquido queda el vidrio translúcido de siempre. Y si el teléfono
// no llegara a dibujar la capa, detrás del velo se ve el desenfoque de la
// ventana, que también queda bien.
@Composable
fun Modifier.vidrioVentana(forma: Shape): Modifier {
    val fondo = LocalFondoLiquido.current
    val oscuro = esTemaOscuro()
    val velo = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (oscuro) 0.62f else 0.58f)
    val pieza = if (fondo != null && vidrioLiquidoDisponible()) {
        piezaLiquida(fondo, fondo.capa, forma, velo, fuerza = 16.dp, desenfoque = 14.dp, leerVersion = false, enPantalla = true, hinchar = false)
    } else null
    if (pieza == null) return this.clip(forma).background(colorVentanaVidrio()).border(bordeVidrio(), forma)
    return this.then(pieza).border(bordeLiquido(), forma)
}

@Composable
private fun piezaLiquida(
    fondo: FondoLiquido,
    capaFuente: GraphicsLayer,
    forma: Shape,
    velo: Color,
    fuerza: Dp,
    desenfoque: Dp,
    leerVersion: Boolean,
    enPantalla: Boolean = false,
    hinchar: Boolean = true
): Modifier? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return piezaLiquida33(fondo, capaFuente, forma, velo, fuerza, desenfoque, leerVersion, enPantalla, hinchar)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun piezaLiquida33(
    fondo: FondoLiquido,
    capaFuente: GraphicsLayer,
    forma: Shape,
    velo: Color,
    fuerza: Dp,
    desenfoque: Dp,
    leerVersion: Boolean,
    enPantalla: Boolean,
    hinchar: Boolean
): Modifier? {
    // Si el teléfono no pudiera compilar el shader, queda el vidrio de siempre.
    val shader = remember { runCatching { RuntimeShader(SHADER_REFRACCION) }.getOrNull() } ?: return null
    val oscuro = esTemaOscuro()
    val luz = LocalLuz.current
    val capa = rememberGraphicsLayer()
    var posicion by remember { mutableStateOf(Offset.Zero) }
    // Al tocarlo, el vidrio se "hincha": crece apenas, refracta y brilla más,
    // y vuelve con un rebote. (No se queda con el toque: el botón o la
    // tarjeta de adentro lo siguen recibiendo.)
    var tocado by remember { mutableStateOf(false) }
    val presion by animateFloatAsState(
        if (tocado) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "presion"
    )
    val brillo = Color.White.copy(alpha = if (oscuro) 0.16f else 0.40f)
    val brilloArriba = Color.White.copy(alpha = if (oscuro) 0.05f else 0.16f)
    val alTocar = if (!hinchar) Modifier else Modifier
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                tocado = true
                // Si se arrastra (scroll), el toque se cancela y vuelve.
                waitForUpOrCancellation(PointerEventPass.Initial)
                tocado = false
            }
        }
    return alTocar
        .graphicsLayer {
            val escala = 1f + 0.025f * presion
            scaleX = escala
            scaleY = escala
        }
        .onGloballyPositioned { posicion = if (enPantalla) it.localToScreen(Offset.Zero) else it.positionInRoot() }
        .clip(forma)
        .drawBehind {
            // El dock se redibuja cada vez que cambia lo de atrás.
            if (leerVersion) fondo.version
            val origen = if (enPantalla) fondo.origenPantalla else fondo.origen
            val luzX = luz?.x ?: 0.82f
            val luzY = luz?.y ?: 0f
            // Dirección de la luz (desde el centro de la pieza hacia el reflejo).
            val dx = luzX - 0.5f
            val dy = luzY - 0.5f - 0.4f
            val largo = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
            shader.setFloatUniform("tamano", size.width, size.height)
            shader.setFloatUniform("radio", radioDeEsquina(forma))
            shader.setFloatUniform("franja", min(size.minDimension / 2, (18 + 6 * presion).dp.toPx()))
            shader.setFloatUniform("fuerza", fuerza.toPx() * (1f + 0.7f * presion))
            shader.setFloatUniform("dispersion", 0.12f)
            shader.setFloatUniform("luz", dx / largo, dy / largo)
            shader.setFloatUniform("brilloBorde", (if (oscuro) 0.16f else 0.30f) * (1f + 0.6f * presion))
            shader.setFloatUniform("sombraBorde", if (oscuro) 0.30f else 0.14f)
            val refraccion = RenderEffect.createRuntimeShaderEffect(shader, "contenido")
            val d = desenfoque.toPx()
            capa.renderEffect = (if (d > 0f) RenderEffect.createChainEffect(refraccion, RenderEffect.createBlurEffect(d, d, Shader.TileMode.CLAMP)) else refraccion)
                .asComposeRenderEffect()
            capa.record {
                translate(origen.x - posicion.x, origen.y - posicion.y) { drawLayer(capaFuente) }
            }
            drawLayer(capa)
            drawRect(velo)
            // Reflejo de luz (sigue la inclinación del teléfono) y un brillo suave arriba.
            drawRect(
                Brush.radialGradient(
                    listOf(brillo.copy(alpha = brillo.alpha * (1f + 0.5f * presion)), Color.Transparent),
                    center = Offset(size.width * luzX, size.height * luzY),
                    radius = min(size.width, size.height * 2.2f) * 0.6f
                )
            )
            drawRect(Brush.verticalGradient(listOf(brilloArriba, Color.Transparent), endY = min(size.height * 0.5f, 60.dp.toPx())))
        }
}

// Radio de las esquinas de la forma con este tamaño (para el shader).
private fun DrawScope.radioDeEsquina(forma: Shape): Float =
    when (val contorno = forma.createOutline(size, layoutDirection, this)) {
        is Outline.Rounded -> min(contorno.roundRect.topLeftCornerRadius.x, size.minDimension / 2)
        else -> 0f
    }
