package com.example.estudia.ui.componentes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.ui.navegacion.Screen
import com.example.estudia.ui.theme.DegradadoFin
import com.example.estudia.ui.theme.DegradadoInicio
import com.example.estudia.ui.theme.FondoDockOscuro
import com.example.estudia.ui.theme.IconoDock
import com.example.estudia.ui.theme.VioletaPrincipalOscuro
import kotlin.math.cos
import kotlin.math.sin

// Espacio que ocupa el dock abajo de la pantalla. Las listas lo suman a su
// relleno inferior para que el último elemento no quede tapado.
val LocalEspacioDock = compositionLocalOf<Dp> { 0.dp }

// Lo que hace el botón "+" del dock: depende de la pantalla en la que se está.
enum class AccionRapida { MATERIA, EVENTO, NOTA, ESTUDIAR }

// En Materias crea una materia, en Calendario un evento y en Notas una nota;
// en Inicio y en Perfil empieza una sesión de estudio.
fun accionDelBoton(rutaActual: String?): AccionRapida = when (rutaActual) {
    Screen.Materias.ruta -> AccionRapida.MATERIA
    Screen.Calendario.ruta -> AccionRapida.EVENTO
    Screen.Notas.ruta -> AccionRapida.NOTA
    else -> AccionRapida.ESTUDIAR
}

private data class ColoresDock(val elegido: Color, val normal: Color, val punto: Color)

private data class ItemDock(val destino: Screen, val elegido: ImageVector, val normal: ImageVector, val etiqueta: Int)

private val itemsDock = listOf(
    ItemDock(Screen.Inicio, Icons.Filled.Home, Icons.Outlined.Home, R.string.inicio),
    ItemDock(Screen.Materias, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, R.string.materias),
    ItemDock(Screen.Calendario, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, R.string.calendario),
    ItemDock(Screen.Notas, Icons.Filled.StickyNote2, Icons.Outlined.StickyNote2, R.string.notas)
)

// Medidas del dock: la barra y la "joroba" del medio donde se apoya el "+".
private val AltoBarra = 64.dp
private val AltoJoroba = 14.dp
private val TamanoBotonCrear = 56.dp

// Dock de navegación: una barra (clara u oscura según el tema) que se
// levanta en el medio, donde está el botón "+" con un brillo violeta.
// Solo íconos; TalkBack lee el nombre de cada sección y si está elegida.
// El Perfil se abre desde el avatar de Inicio.
@Composable
fun DockFlotante(rutaActual: String?, onSeleccionar: (Screen) -> Unit, onCrear: (AccionRapida) -> Unit) {
    // Sigue el tema elegido en la app (no el del teléfono).
    val oscuro = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val fondo = if (oscuro) FondoDockOscuro else MaterialTheme.colorScheme.surfaceContainerLowest
    val colores = if (oscuro) ColoresDock(Color.White, IconoDock, VioletaPrincipalOscuro)
    else ColoresDock(MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.onSurfaceVariant, DegradadoFin)
    val forma = FormaDockConJoroba(AltoJoroba)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            .height(AltoBarra + AltoJoroba)
    ) {
        // Fondo de la barra con su sombra (separado de los botones para que
        // el brillo del "+" no quede recortado).
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(18.dp, forma, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(forma)
                .background(fondo)
        )
        Row(
            modifier = Modifier.fillMaxSize().padding(top = AltoJoroba, start = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsDock.take(2).forEach { BotonDock(it, rutaActual, colores, onSeleccionar) }
            Box(modifier = Modifier.weight(1.3f), contentAlignment = Alignment.Center) {
                // Sube un poco para quedar dentro de la joroba.
                BotonCrear(accionDelBoton(rutaActual), oscuro, onCrear, Modifier.offset(y = -(AltoJoroba / 2)))
            }
            itemsDock.drop(2).forEach { BotonDock(it, rutaActual, colores, onSeleccionar) }
        }
    }
}

@Composable
private fun RowScope.BotonDock(item: ItemDock, rutaActual: String?, colores: ColoresDock, onSeleccionar: (Screen) -> Unit) {
    val elegido = rutaActual == item.destino.ruta
    val nombre = stringResource(item.etiqueta)
    // Al elegir una sección: el ícono cambia de color, da un saltito
    // (crece y vuelve con un rebote) y el puntito aparece creciendo.
    val color by animateColorAsState(if (elegido) colores.elegido else colores.normal, tween(250), label = "color")
    val escalaIcono by animateFloatAsState(
        if (elegido) 1.12f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "escalaIcono"
    )
    val escalaPunto by animateFloatAsState(
        if (elegido) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "escalaPunto"
    )
    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .clickable(role = Role.Tab, onClickLabel = nombre) { onSeleccionar(item.destino) }
                .semantics { selected = elegido },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Crossfade(targetState = elegido, animationSpec = tween(200), label = "icono") { relleno ->
                Icon(
                    imageVector = if (relleno) item.elegido else item.normal,
                    contentDescription = nombre,
                    tint = color,
                    modifier = Modifier.size(24.dp).graphicsLayer { scaleX = escalaIcono; scaleY = escalaIcono }
                )
            }
            // Puntito violeta debajo de la sección elegida.
            Spacer(modifier = Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .graphicsLayer { scaleX = escalaPunto; scaleY = escalaPunto }
                    .clip(CircleShape)
                    .background(colores.punto)
            )
        }
    }
}

// El "+": en el tema claro es un círculo violeta lleno con una ronda de
// puntitos alrededor; en el oscuro, un aro violeta con el ícono violeta.
// En los dos tiene un brillo violeta difuso detrás.
@Composable
private fun BotonCrear(accion: AccionRapida, oscuro: Boolean, onCrear: (AccionRapida) -> Unit, modifier: Modifier = Modifier) {
    val descripcion = stringResource(
        when (accion) {
            AccionRapida.MATERIA -> R.string.nueva_materia
            AccionRapida.EVENTO -> R.string.nuevo_evento
            AccionRapida.NOTA -> R.string.nueva_nota
            AccionRapida.ESTUDIAR -> R.string.empezar_a_estudiar
        }
    )
    val degradado = Brush.linearGradient(listOf(DegradadoInicio, DegradadoFin))
    // Al apretarlo se hunde un poco y vuelve con un rebote.
    val interaccion = remember { MutableInteractionSource() }
    val apretado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        if (apretado) 0.88f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "escalaCrear"
    )
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .size(TamanoBotonCrear)
            .drawBehind {
                val centro = Offset(size.width / 2, size.height / 2)
                val radio = size.minDimension / 2
                // Brillo violeta.
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(DegradadoFin.copy(alpha = if (oscuro) 0.45f else 0.35f), Color.Transparent),
                        center = centro.copy(y = centro.y + 4.dp.toPx()),
                        radius = radio * 1.6f
                    ),
                    radius = radio * 1.6f,
                    center = centro.copy(y = centro.y + 4.dp.toPx())
                )
                // Ronda de puntitos (solo en el tema claro).
                if (!oscuro) {
                    val distancia = radio + 7.dp.toPx()
                    repeat(28) { i ->
                        val angulo = 2 * Math.PI * i / 28
                        drawCircle(
                            color = DegradadoFin.copy(alpha = 0.45f),
                            radius = 1.dp.toPx(),
                            center = Offset(
                                centro.x + distancia * cos(angulo).toFloat(),
                                centro.y + distancia * sin(angulo).toFloat()
                            )
                        )
                    }
                }
            }
            .clip(CircleShape)
            .then(
                if (oscuro) Modifier.background(FondoDockOscuro).border(2.5.dp, degradado, CircleShape)
                else Modifier.background(degradado)
            )
            .clickable(interactionSource = interaccion, indication = LocalIndication.current, role = Role.Button) { onCrear(accion) },
        contentAlignment = Alignment.Center
    ) {
        // En Inicio y Perfil el botón empieza a estudiar: muestra el reloj.
        // Al cambiar entre "+" y reloj, el ícono gira y aparece creciendo.
        val estudiar = accion == AccionRapida.ESTUDIAR
        AnimatedContent(
            targetState = estudiar,
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.5f))
                    .togetherWith(fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.5f))
            },
            label = "iconoCrear"
        ) { reloj ->
            val giro = remember { Animatable(-90f) }
            LaunchedEffect(Unit) { giro.animateTo(0f, tween(280, easing = FastOutSlowInEasing)) }
            Icon(
                imageVector = if (reloj) Icons.Filled.Timer else Icons.Filled.Add,
                contentDescription = descripcion,
                tint = if (oscuro) VioletaPrincipalOscuro else Color.White,
                modifier = Modifier.size(if (reloj) 26.dp else 28.dp).graphicsLayer { rotationZ = giro.value }
            )
        }
    }
}

// Forma de la barra: un rectángulo con las puntas bien redondeadas y una
// curva suave hacia arriba en el medio (la "joroba").
private class FormaDockConJoroba(private val altoJoroba: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val joroba = with(density) { altoJoroba.toPx() }
        val arriba = joroba
        val radio = minOf((size.height - arriba) / 2, with(density) { 28.dp.toPx() })
        val centro = size.width / 2
        // Mitad del ancho de la joroba (incluye las curvas de subida y bajada).
        val mitad = minOf(size.width * 0.2f, with(density) { 90.dp.toPx() })
        val camino = Path().apply {
            moveTo(0f, arriba + radio)
            arcTo(Rect(0f, arriba, 2 * radio, arriba + 2 * radio), 180f, 90f, false)
            lineTo(centro - mitad, arriba)
            cubicTo(centro - mitad * 0.45f, arriba, centro - mitad * 0.55f, 0f, centro, 0f)
            cubicTo(centro + mitad * 0.55f, 0f, centro + mitad * 0.45f, arriba, centro + mitad, arriba)
            lineTo(size.width - radio, arriba)
            arcTo(Rect(size.width - 2 * radio, arriba, size.width, arriba + 2 * radio), 270f, 90f, false)
            lineTo(size.width, size.height - radio)
            arcTo(Rect(size.width - 2 * radio, size.height - 2 * radio, size.width, size.height), 0f, 90f, false)
            lineTo(radio, size.height)
            arcTo(Rect(0f, size.height - 2 * radio, 2 * radio, size.height), 90f, 90f, false)
            close()
        }
        return Outline.Generic(camino)
    }
}
