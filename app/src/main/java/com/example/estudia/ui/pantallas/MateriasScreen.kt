package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Usuario
import com.example.estudia.ui.componentes.DialogoNuevaMateria
import com.example.estudia.ui.componentes.TarjetaMateria

// Pantalla "Mis Materias": muestra la lista de materias del usuario,
// separadas en pestañas Activas/Archivadas, y permite agregar una nueva.
@Composable
fun MateriasScreen(usuario: Usuario) {
    var tabSeleccionada by remember { mutableStateOf(0) } // 0 = Activas, 1 = Archivadas
    var mostrarDialogo by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Mis Materias", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(selectedTabIndex = tabSeleccionada) {
            Tab(
                selected = tabSeleccionada == 0,
                onClick = { tabSeleccionada = 0 },
                text = { Text("Activas") }
            )
            Tab(
                selected = tabSeleccionada == 1,
                onClick = { tabSeleccionada = 1 },
                text = { Text("Archivadas") }
            )
        }

        val materiasFiltradas: List<Materia> = usuario.materias.filter {
            it.archivada == (tabSeleccionada == 1)
        }

        LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            items(materiasFiltradas) { materia ->
                TarjetaMateria(materia)
            }
        }

        Button(
            onClick = { mostrarDialogo = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Agregar Materia")
        }
    }

    if (mostrarDialogo) {
        DialogoNuevaMateria(
            onConfirmar = { nombreNuevo ->
                usuario.materias.add(
                    Materia(
                        id = usuario.materias.size + 1,
                        nombre = nombreNuevo,
                        color = "#6C5CE7"
                    )
                )
                mostrarDialogo = false
            },
            onCancelar = { mostrarDialogo = false }
        )
    }
}