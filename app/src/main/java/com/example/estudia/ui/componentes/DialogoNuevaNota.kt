package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.Materia

// Ventana emergente para crear una nota nueva. Como una Nota siempre
// pertenece a una Materia, hay que elegir a cuál va a pertenecer
// (por eso el selector desplegable).
@Composable
fun DialogoNuevaNota(
    materiasDisponibles: List<Materia>,
    onConfirmar: (materiaElegida: Materia, titulo: String, contenido: String) -> Unit,
    onCancelar: () -> Unit
) {
    var materiaElegida by remember { mutableStateOf(materiasDisponibles.firstOrNull()) }
    var menuAbierto by remember { mutableStateOf(false) }
    var titulo by remember { mutableStateOf("") }
    var contenido by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nueva nota") },
        text = {
            Column {
                // Selector de materia
                Box {
                    OutlinedButton(onClick = { menuAbierto = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(materiaElegida?.nombre ?: "Elegí una materia")
                    }
                    DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                        materiasDisponibles.forEach { materia ->
                            DropdownMenuItem(
                                text = { Text(materia.nombre) },
                                onClick = {
                                    materiaElegida = materia
                                    menuAbierto = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = contenido,
                    onValueChange = { contenido = it },
                    label = { Text("Contenido") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val materia = materiaElegida
                if (materia != null && titulo.isNotBlank()) {
                    onConfirmar(materia, titulo, contenido)
                }
            }) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}