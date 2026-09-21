package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Ventana emergente para editar una nota existente: solo título y
// contenido (la materia a la que pertenece no cambia).
@Composable
fun DialogoEditarNota(
    tituloInicial: String,
    contenidoInicial: String,
    onConfirmar: (titulo: String, contenido: String) -> Unit,
    onCancelar: () -> Unit
) {
    var titulo by remember { mutableStateOf(tituloInicial) }
    var contenido by remember { mutableStateOf(contenidoInicial) }
    var error by remember { mutableStateOf<String?>(null) }

    val mensajeError = error

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Editar nota") },
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
                    value = contenido,
                    onValueChange = { contenido = it },
                    label = { Text("Contenido") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
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
                    error = "Escribí un título para la nota."
                } else {
                    onConfirmar(tituloLimpio, contenido)
                }
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Button(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}
