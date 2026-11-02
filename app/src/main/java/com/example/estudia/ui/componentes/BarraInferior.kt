package com.example.estudia.ui.componentes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.estudia.R
import com.example.estudia.ui.navegacion.Screen
import com.example.estudia.ui.navegacion.pantallasConBarra

// Barra de navegación inferior con los 5 accesos principales de la app.
// rutaActual sale de currentBackStackEntryAsState(); onSeleccionar
// pide navegar a la ruta de esa Screen. Los íconos son decorativos
// (contentDescription = null): TalkBack lee la etiqueta de texto.
@Composable
fun BarraInferior(rutaActual: String?, onSeleccionar: (Screen) -> Unit) {
    NavigationBar {
        pantallasConBarra.forEach { destino ->
            val (icono, etiqueta) = when (destino) {
                Screen.Inicio -> Icons.Filled.Home to R.string.inicio
                Screen.Materias -> Icons.AutoMirrored.Filled.MenuBook to R.string.materias
                Screen.Calendario -> Icons.Filled.CalendarMonth to R.string.calendario
                Screen.Notas -> Icons.AutoMirrored.Filled.Notes to R.string.notas
                else -> Icons.Filled.Person to R.string.perfil
            }
            NavigationBarItem(
                selected = rutaActual == destino.ruta,
                onClick = { onSeleccionar(destino) },
                icon = { Icon(icono, contentDescription = null) },
                label = { Text(stringResource(etiqueta)) }
            )
        }
    }
}
