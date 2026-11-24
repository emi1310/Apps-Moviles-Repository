package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.ui.navegacion.Screen
import com.example.estudia.ui.theme.DegradadoFin
import com.example.estudia.ui.theme.DegradadoInicio
import com.example.estudia.ui.theme.FondoDockOscuro
import com.example.estudia.ui.theme.IconoDock

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

private data class ColoresDock(val elegido: Color, val normal: Color)

private data class ItemDock(val destino: Screen, val elegido: ImageVector, val normal: ImageVector, val etiqueta: Int)

private val itemsDock = listOf(
    ItemDock(Screen.Inicio, Icons.Filled.Home, Icons.Outlined.Home, R.string.inicio),
    ItemDock(Screen.Materias, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, R.string.materias),
    ItemDock(Screen.Calendario, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, R.string.calendario),
    ItemDock(Screen.Notas, Icons.Filled.StickyNote2, Icons.Outlined.StickyNote2, R.string.notas)
)

// Dock de navegación flotante: una isla (clara u oscura según el tema) con cuatro secciones y el
// botón "+" en el medio, que cambia según la pantalla. Solo íconos;
// TalkBack lee el nombre de cada sección y si está elegida. El Perfil se
// abre desde el avatar de Inicio.
@Composable
fun DockFlotante(rutaActual: String?, onSeleccionar: (Screen) -> Unit, onCrear: (AccionRapida) -> Unit) {
    // Sigue el tema elegido en la app (no el del teléfono): isla clara con
    // íconos grises en el tema claro, isla oscura en el oscuro.
    val oscuro = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val fondo = if (oscuro) FondoDockOscuro else MaterialTheme.colorScheme.surfaceContainer
    val colores = if (oscuro) ColoresDock(Color.White, IconoDock)
    else ColoresDock(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onSurfaceVariant)
    Box(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(16.dp, RoundedCornerShape(50), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(50))
                .background(fondo)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsDock.take(2).forEach { BotonDock(it, rutaActual, colores, onSeleccionar) }
            BotonCrear(accionDelBoton(rutaActual), onCrear)
            itemsDock.drop(2).forEach { BotonDock(it, rutaActual, colores, onSeleccionar) }
        }
    }
}

@Composable
private fun BotonDock(item: ItemDock, rutaActual: String?, colores: ColoresDock, onSeleccionar: (Screen) -> Unit) {
    val elegido = rutaActual == item.destino.ruta
    val nombre = stringResource(item.etiqueta)
    Column(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .clickable(role = Role.Tab, onClickLabel = nombre) { onSeleccionar(item.destino) }
            .semantics { selected = elegido },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (elegido) item.elegido else item.normal,
            contentDescription = nombre,
            tint = if (elegido) colores.elegido else colores.normal,
            modifier = Modifier.size(24.dp)
        )
        // Puntito debajo de la sección elegida.
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (elegido) colores.elegido else Color.Transparent)
        )
    }
}

@Composable
private fun BotonCrear(accion: AccionRapida, onCrear: (AccionRapida) -> Unit) {
    val descripcion = stringResource(
        when (accion) {
            AccionRapida.MATERIA -> R.string.nueva_materia
            AccionRapida.EVENTO -> R.string.nuevo_evento
            AccionRapida.NOTA -> R.string.nueva_nota
            AccionRapida.ESTUDIAR -> R.string.empezar_a_estudiar
        }
    )
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .size(56.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(DegradadoInicio, DegradadoFin)))
            .clickable(role = Role.Button) { onCrear(accion) },
        contentAlignment = Alignment.Center
    ) {
        // En Inicio y Perfil el botón empieza a estudiar: muestra el reloj.
        Icon(
            imageVector = if (accion == AccionRapida.ESTUDIAR) Icons.Filled.Timer else Icons.Filled.Add,
            contentDescription = descripcion,
            tint = Color.White,
            modifier = Modifier.size(if (accion == AccionRapida.ESTUDIAR) 28.dp else 30.dp)
        )
    }
}

