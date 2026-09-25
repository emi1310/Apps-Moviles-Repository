package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DialogoNuevaTarea(
    onConfirmar: (titulo: String, fechaLimite: String) -> Unit,
    onCancelar: () -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var fechaLimite by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val mensajeError = error

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nueva tarea") },
        text = {
            Column {
                TextField(
                    value = titulo,
                    onValueChange = {
                        titulo = it
                        error = null
                    },
                    label = { Text("Título") },
                    isError = mensajeError != null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = fechaLimite,
                    onValueChange = { fechaLimite = it },
                    label = { Text("Fecha límite (ej: Hoy, Mañana, 25 May)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (mensajeError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(mensajeError, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val tituloLimpio = titulo.trim()
                if (tituloLimpio.isBlank()) {
                    error = "Escribí un título para la tarea."
                } else {
                    val fechaLimpia = fechaLimite.trim().ifBlank { "Sin fecha" }
                    onConfirmar(tituloLimpio, fechaLimpia)
                }
            }) {
                Text("Crear")
            }
        },
        dismissButton = {
            Button(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}
