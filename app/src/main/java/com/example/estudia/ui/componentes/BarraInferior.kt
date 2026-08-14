package com.example.estudia.ui.componentes

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.estudia.ui.navegacion.PantallaApp

// Barra de navegación inferior con los 5 accesos principales de la app.
// pantallaSeleccionada indica cuál está activa ahora; onSeleccionar se
// llama cuando el usuario toca un ícono distinto.
@Composable
fun BarraInferior(pantallaSeleccionada: PantallaApp, onSeleccionar: (PantallaApp) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = pantallaSeleccionada == PantallaApp.INICIO,
            onClick = { onSeleccionar(PantallaApp.INICIO) },
            icon = { Text("🏠") },
            label = { Text("Inicio") }
        )
        NavigationBarItem(
            selected = pantallaSeleccionada == PantallaApp.MATERIAS,
            onClick = { onSeleccionar(PantallaApp.MATERIAS) },
            icon = { Text("📚") },
            label = { Text("Materias") }
        )
        NavigationBarItem(
            selected = pantallaSeleccionada == PantallaApp.CALENDARIO,
            onClick = { onSeleccionar(PantallaApp.CALENDARIO) },
            icon = { Text("📅") },
            label = { Text("Calendario") }
        )
        NavigationBarItem(
            selected = pantallaSeleccionada == PantallaApp.NOTAS,
            onClick = { onSeleccionar(PantallaApp.NOTAS) },
            icon = { Text("📝") },
            label = { Text("Notas") }
        )
        NavigationBarItem(
            selected = pantallaSeleccionada == PantallaApp.PERFIL,
            onClick = { onSeleccionar(PantallaApp.PERFIL) },
            icon = { Text("👤") },
            label = { Text("Perfil") }
        )
    }
}