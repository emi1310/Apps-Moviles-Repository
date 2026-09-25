package com.example.estudia.ui.componentes

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.estudia.ui.navegacion.Screen
import com.example.estudia.ui.navegacion.pantallasConBarra

@Composable
fun BarraInferior(rutaActual: String?, onSeleccionar: (Screen) -> Unit) {
    val etiquetas = mapOf(
        Screen.Inicio.ruta to Pair("🏠", "Inicio"),
        Screen.Materias.ruta to Pair("📚", "Materias"),
        Screen.Calendario.ruta to Pair("📅", "Calendario"),
        Screen.Notas.ruta to Pair("📝", "Notas"),
        Screen.Perfil.ruta to Pair("👤", "Perfil")
    )

    NavigationBar {
        pantallasConBarra.forEach { destino ->
            val (icono, etiqueta) = etiquetas.getValue(destino.ruta)
            NavigationBarItem(
                selected = rutaActual == destino.ruta,
                onClick = { onSeleccionar(destino) },
                icon = {
                    Text(icono, modifier = Modifier.clearAndSetSemantics {})
                },
                label = { Text(etiqueta) },
                modifier = Modifier.semantics { contentDescription = etiqueta }
            )
        }
    }
}
