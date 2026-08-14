package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

// Ventana emergente (dialog) que pide el nombre de una materia nueva.
// onConfirmar se ejecuta cuando el usuario toca "Agregar", pasándole
// el texto escrito. onCancelar se ejecuta si cierra sin confirmar.
@Composable
fun DialogoNuevaMateria(
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nueva materia") },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre de la materia") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombre.isNotBlank()) {
                    onConfirmar(nombre)
                }
            }) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}