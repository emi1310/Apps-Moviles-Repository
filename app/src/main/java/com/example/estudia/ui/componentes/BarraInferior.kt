package com.example.estudia.ui.componentes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.estudia.R
import com.example.estudia.ui.navegacion.Screen
import com.example.estudia.ui.navegacion.pantallasConBarra

// Ícono relleno (sección elegida), ícono con contorno (las demás) y etiqueta.
private data class ItemBarra(val elegido: ImageVector, val normal: ImageVector, val etiqueta: Int)

// Barra de navegación inferior con los 5 accesos principales de la app.
// rutaActual sale de currentBackStackEntryAsState(); onSeleccionar
// pide navegar a la ruta de esa Screen. Los íconos son decorativos
// (contentDescription = null): TalkBack lee la etiqueta de texto.
@Composable
fun BarraInferior(rutaActual: String?, onSeleccionar: (Screen) -> Unit) {
    NavigationBar {
        pantallasConBarra.forEach { destino ->
            val item = when (destino) {
                Screen.Inicio -> ItemBarra(Icons.Filled.Home, Icons.Outlined.Home, R.string.inicio)
                Screen.Materias -> ItemBarra(Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, R.string.materias)
                Screen.Calendario -> ItemBarra(Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, R.string.calendario)
                Screen.Notas -> ItemBarra(Icons.Filled.StickyNote2, Icons.Outlined.StickyNote2, R.string.notas)
                else -> ItemBarra(Icons.Filled.Person, Icons.Outlined.Person, R.string.perfil)
            }
            val seleccionado = rutaActual == destino.ruta
            NavigationBarItem(
                selected = seleccionado,
                onClick = { onSeleccionar(destino) },
                icon = { Icon(if (seleccionado) item.elegido else item.normal, contentDescription = null) },
                label = { Text(stringResource(item.etiqueta)) },
                // El indicador de la sección elegida usa el violeta de la app.
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
