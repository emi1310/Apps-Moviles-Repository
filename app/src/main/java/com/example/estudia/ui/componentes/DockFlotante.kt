package com.example.estudia.ui.componentes

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.ui.navegacion.Screen
import com.example.estudia.ui.theme.DegradadoFin
import com.example.estudia.ui.theme.DegradadoInicio

// Espacio que ocupa el dock abajo de la pantalla. Las listas lo suman a su
// relleno inferior para que el último elemento no quede tapado.
val LocalEspacioDock = compositionLocalOf<Dp> { 0.dp }

// Lo que hace el botón "+" del dock: depende de la pantalla en la que se está.
enum class AccionRapida { MATERIA, EVENTO, NOTA, ESTUDIAR, GEMINI }

// En Materias crea una materia, en Calendario un evento y en Notas una nota;
// en el Asistente abre la app de Gemini; en Inicio y en Perfil empieza una
// sesión de estudio.
fun accionDelBoton(rutaActual: String?): AccionRapida = when (rutaActual) {
    Screen.Materias.ruta -> AccionRapida.MATERIA
    Screen.Calendario.ruta -> AccionRapida.EVENTO
    Screen.Notas.ruta -> AccionRapida.NOTA
    Screen.Asistente.ruta -> AccionRapida.GEMINI
    else -> AccionRapida.ESTUDIAR
}

// Ícono del botón "+" según lo que va a hacer.
fun iconoAccion(accion: AccionRapida): ImageVector = when (accion) {
    AccionRapida.MATERIA -> Icons.Outlined.LibraryAdd
    AccionRapida.EVENTO -> Icons.Outlined.EditCalendar
    AccionRapida.NOTA -> Icons.Outlined.PostAdd
    AccionRapida.ESTUDIAR -> Icons.Outlined.Timer
    AccionRapida.GEMINI -> Icons.Outlined.OpenInNew
}

// Pantallas donde la lupa del dock filtra la lista ahí mismo (en las demás
// abre la búsqueda en toda la app).
fun buscaEnLaPantalla(rutaActual: String?): Boolean =
    rutaActual == Screen.Materias.ruta || rutaActual == Screen.Notas.ruta || rutaActual == Screen.Calendario.ruta

private data class ItemDock(val destino: Screen, val elegido: ImageVector, val normal: ImageVector, val etiqueta: Int)

// Las secciones del dock en orden (para pasar de una a otra deslizando).
val seccionesDock: List<Screen> get() = itemsDock.map { it.destino }

// La sección de al lado: paso = 1 la siguiente, -1 la anterior (null si no hay).
fun seccionVecina(rutaActual: String?, paso: Int): Screen? {
    val i = seccionesDock.indexOfFirst { it.ruta == rutaActual }
    return if (i < 0) null else seccionesDock.getOrNull(i + paso)
}

private val itemsDock = listOf(
    ItemDock(Screen.Inicio, Icons.Filled.Home, Icons.Outlined.Home, R.string.inicio),
    ItemDock(Screen.Materias, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, R.string.materias),
    ItemDock(Screen.Calendario, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, R.string.calendario),
    ItemDock(Screen.Notas, Icons.Filled.StickyNote2, Icons.Outlined.StickyNote2, R.string.notas),
    ItemDock(Screen.Asistente, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, R.string.asistente)
)

private val AltoDock = 60.dp
// El + y la lupa son más chicos que la pastilla: así le queda más lugar a
// las cinco secciones.
private val TamanoCirculo = 48.dp
private val Separacion = 8.dp

// Dock de navegación de vidrio líquido (refracción en los bordes, Android 13
// o más; en los anteriores, vidrio esmerilado):
// - una pastilla con las 5 secciones (la última, el Asistente IA) (la elegida con un círculo suave detrás),
// - un círculo violeta con el "+" (su ícono cambia según la pantalla),
// - un círculo de vidrio con la lupa.
// Al tocar la lupa en Materias, Calendario o Notas, el dock se convierte en
// una barra de búsqueda que filtra esa pantalla (la cruz la cierra). En las demás secciones abre la búsqueda en
// toda la app. Solo íconos; TalkBack lee el nombre de cada botón.
@Composable
fun DockFlotante(
    rutaActual: String?,
    buscando: Boolean,
    textoBusqueda: String,
    onSeleccionar: (Screen) -> Unit,
    onCrear: (AccionRapida) -> Unit,
    onBuscar: () -> Unit,
    onCambiarBusqueda: (String) -> Unit,
    onCerrarBusqueda: () -> Unit
) {
    // Atrás cierra la búsqueda antes de salir de la sección.
    BackHandler(enabled = buscando, onBack = onCerrarBusqueda)

    // Con el teclado abierto, el dock sube y queda apoyado encima.
    val bordes = if (buscando) WindowInsets.navigationBars.union(WindowInsets.ime) else WindowInsets.navigationBars
    AnimatedContent(
        targetState = buscando,
        transitionSpec = {
            (fadeIn(tween(220, delayMillis = 60)) + scaleIn(tween(260), initialScale = 0.92f))
                .togetherWith(fadeOut(tween(90)))
        },
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(bordes)
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 8.dp),
        label = "dock"
    ) { conBusqueda ->
        Row(
            modifier = Modifier.fillMaxWidth().height(AltoDock),
            horizontalArrangement = Arrangement.spacedBy(Separacion),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (conBusqueda) {
                BarraBusqueda(
                    texto = textoBusqueda,
                    sugerencia = stringResource(
                        when (rutaActual) {
                            Screen.Notas.ruta -> R.string.buscar_notas
                            Screen.Calendario.ruta -> R.string.buscar_eventos
                            else -> R.string.buscar_tareas
                        }
                    ),
                    onCambiar = onCambiarBusqueda,
                    onCerrar = onCerrarBusqueda,
                    modifier = Modifier.weight(1f)
                )
            } else {
                PastillaSecciones(rutaActual, onSeleccionar, Modifier.weight(1f))
                BotonCrear(accionDelBoton(rutaActual), onCrear)
                CirculoVidrio(
                    icono = Icons.Outlined.Search,
                    descripcion = stringResource(R.string.busqueda_titulo),
                    tinte = MaterialTheme.colorScheme.onSurface,
                    onClick = onBuscar
                )
            }
        }
    }
}

// Pastilla de vidrio con las cinco secciones. La sección elegida se marca con
// una gota de vidrio que, al cambiar de sección, se estira hacia la nueva y
// se vuelve a juntar con un rebote: el borde de adelante llega rápido y el de
// atrás lo sigue más lento.
@Composable
private fun PastillaSecciones(rutaActual: String?, onSeleccionar: (Screen) -> Unit, modifier: Modifier = Modifier) {
    val elegido = itemsDock.indexOfFirst { it.destino.ruta == rutaActual }
    val oscuro = esTemaOscuro()
    val inicial = elegido.coerceAtLeast(0).toFloat()
    val adelante = remember { Animatable(inicial) }
    val atras = remember { Animatable(inicial) }
    val visible by animateFloatAsState(if (elegido >= 0) 1f else 0f, tween(200), label = "gota")
    LaunchedEffect(elegido) {
        if (elegido < 0) return@LaunchedEffect
        val destino = elegido.toFloat()
        launch { adelante.animateTo(destino, spring(dampingRatio = 0.68f, stiffness = 520f)) }
        launch { atras.animateTo(destino, spring(dampingRatio = 0.72f, stiffness = 150f)) }
    }
    val relleno = if (oscuro) Color.White.copy(alpha = 0.13f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val brillo = Color.White.copy(alpha = if (oscuro) 0.20f else 0.65f)
    val borde = Color.White.copy(alpha = if (oscuro) 0.32f else 0.95f)
    Row(
        modifier = modifier
            .fillMaxHeight()
            .vidrioLiquido(RoundedCornerShape(50))
            .drawBehind {
                if (visible <= 0f) return@drawBehind
                val margen = 6.dp.toPx()
                val anchoItem = (size.width - 2 * margen) / itemsDock.size
                val d = min(anchoItem, 46.dp.toPx())
                fun centro(i: Float) = margen + anchoItem * (i + 0.5f)
                val a = adelante.value
                val b = atras.value
                val izquierda = centro(min(a, b)) - d / 2
                val derecha = centro(max(a, b)) + d / 2
                // Mientras se estira se afina un poco, como una gota.
                val alto = d * (1f - 0.14f * min(abs(a - b), 1f))
                val arriba = size.height / 2 - alto / 2
                val esquina = CornerRadius(alto / 2)
                drawRoundRect(relleno, Offset(izquierda, arriba), Size(derecha - izquierda, alto), esquina, alpha = visible)
                drawRoundRect(
                    Brush.verticalGradient(listOf(brillo, Color.Transparent), startY = arriba, endY = arriba + alto * 0.6f),
                    Offset(izquierda, arriba), Size(derecha - izquierda, alto), esquina, alpha = visible
                )
                drawRoundRect(
                    Brush.verticalGradient(listOf(borde, borde.copy(alpha = borde.alpha * 0.15f)), startY = arriba, endY = arriba + alto),
                    Offset(izquierda, arriba), Size(derecha - izquierda, alto), esquina,
                    style = Stroke(1.dp.toPx()), alpha = visible
                )
            }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsDock.forEach { BotonDock(it, rutaActual, onSeleccionar) }
    }
}

@Composable
private fun RowScope.BotonDock(item: ItemDock, rutaActual: String?, onSeleccionar: (Screen) -> Unit) {
    val elegido = rutaActual == item.destino.ruta
    val nombre = stringResource(item.etiqueta)
    // Al elegir una sección: el ícono se pinta de violeta y da un saltito
    // (crece y vuelve con un rebote); la gota de la pastilla llega detrás.
    val color by animateColorAsState(
        if (elegido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(250),
        label = "color"
    )
    val escalaIcono by animateFloatAsState(
        if (elegido) 1.1f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "escalaIcono"
    )
    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Box(
            // Con cinco secciones en un teléfono angosto no siempre entran 46 dp:
            // el círculo usa el ancho que haya (hasta 46 dp) y queda redondo.
            modifier = Modifier
                .widthIn(max = 46.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .clickable(role = Role.Tab, onClickLabel = nombre) { onSeleccionar(item.destino) }
                .semantics { selected = elegido },
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = elegido, animationSpec = tween(200), label = "icono") { relleno ->
                Icon(
                    imageVector = if (relleno) item.elegido else item.normal,
                    contentDescription = nombre,
                    tint = color,
                    modifier = Modifier.size(24.dp).graphicsLayer { scaleX = escalaIcono; scaleY = escalaIcono }
                )
            }
        }
    }
}

// Círculo de vidrio con un ícono (la lupa, o la sección mientras se busca).
@Composable
private fun CirculoVidrio(icono: ImageVector, descripcion: String, tinte: Color, onClick: () -> Unit) {
    val interaccion = remember { MutableInteractionSource() }
    val apretado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        if (apretado) 0.9f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "escalaCirculo"
    )
    Box(
        modifier = Modifier
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .size(TamanoCirculo)
            .vidrioLiquido(CircleShape)
            .clickable(interactionSource = interaccion, indication = LocalIndication.current, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = descripcion, tint = tinte, modifier = Modifier.size(24.dp))
    }
}

// Barra de búsqueda de vidrio: se abre con el teclado listo para escribir.
@Composable
private fun BarraBusqueda(
    texto: String,
    sugerencia: String,
    onCambiar: (String) -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val foco = remember { FocusRequester() }
    val teclado = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { foco.requestFocus() }
    Row(
        modifier = modifier
            .fillMaxHeight()
            .vidrioLiquido(RoundedCornerShape(50))
            .padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = texto,
            onValueChange = onCambiar,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { teclado?.hide() }),
            modifier = Modifier.weight(1f).focusRequester(foco),
            decorationBox = { campo ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (texto.isEmpty()) {
                        Text(sugerencia, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    campo()
                }
            }
        )
        // La cruz borra lo escrito y cierra la búsqueda.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onCerrar),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Clear,
                contentDescription = stringResource(R.string.dock_cerrar_busqueda),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// El "+": círculo violeta con degradado, un brillo de vidrio arriba y un
// resplandor violeta difuso detrás.
@Composable
private fun BotonCrear(accion: AccionRapida, onCrear: (AccionRapida) -> Unit) {
    val descripcion = stringResource(
        when (accion) {
            AccionRapida.MATERIA -> R.string.nueva_materia
            AccionRapida.EVENTO -> R.string.nuevo_evento
            AccionRapida.NOTA -> R.string.nueva_nota
            AccionRapida.ESTUDIAR -> R.string.empezar_a_estudiar
            AccionRapida.GEMINI -> R.string.abrir_gemini
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
        modifier = Modifier
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .size(TamanoCirculo)
            .drawBehind {
                // Resplandor violeta.
                val centro = Offset(size.width / 2, size.height / 2 + 6.dp.toPx())
                val radio = size.minDimension / 2 * 1.5f
                drawCircle(
                    brush = Brush.radialGradient(listOf(DegradadoFin.copy(alpha = 0.45f), Color.Transparent), centro, radio),
                    radius = radio,
                    center = centro
                )
            }
            .clip(CircleShape)
            .background(degradado)
            .drawBehind {
                // Brillo de vidrio en la mitad de arriba.
                drawCircle(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.6f
                    ),
                    radius = size.minDimension / 2
                )
            }
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.05f))), CircleShape)
            .clickable(interactionSource = interaccion, indication = LocalIndication.current, role = Role.Button) { onCrear(accion) },
        contentAlignment = Alignment.Center
    ) {
        // Cada pantalla tiene su ícono: libro con "+" en Materias, calendario
        // en Calendario, hoja con "+" en Notas, salir a Gemini en el Asistente y el reloj (empezar a estudiar)
        // en Inicio y Perfil. Al cambiar, el ícono gira y aparece creciendo.
        AnimatedContent(
            targetState = accion,
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.5f))
                    .togetherWith(fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.5f))
            },
            label = "iconoCrear"
        ) { actual ->
            val giro = remember { Animatable(-90f) }
            LaunchedEffect(Unit) { giro.animateTo(0f, tween(280, easing = FastOutSlowInEasing)) }
            if (actual == AccionRapida.GEMINI) {
                // En el Asistente: el ícono de Gemini, sobre un círculo blanco.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .graphicsLayer { rotationZ = giro.value }
                        .clip(CircleShape)
                        .background(Color.White)
                        .semantics { contentDescription = descripcion }
                ) { IconoGemini(30.dp) }
            } else {
                Icon(
                    imageVector = iconoAccion(actual),
                    contentDescription = descripcion,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = giro.value }
                )
            }
        }
    }
}
