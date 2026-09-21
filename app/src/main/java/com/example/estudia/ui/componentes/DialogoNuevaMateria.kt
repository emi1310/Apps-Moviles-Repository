package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

// Ventana emergente (dialog) que pide el nombre de una materia. Sirve
// tanto para crear una materia nueva como para editar el nombre de una
// existente: nombreInicial precarga el campo, y titulo/textoConfirmar
// cambian los textos según el caso.
@Composable
fun DialogoNuevaMateria(
    nombreYaExiste: (String) -> Boolean,
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit,
    nombreInicial: String = "",
    titulo: String = "Nueva materia",
    textoConfirmar: String = "Agregar"
) {
    var nombre by remember { mutableStateOf(nombreInicial) }
    var error by remember { mutableStateOf<String?>(null) }

    val mensajeError = error

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = {
            TextField(
                value = nombre,
                onValueChange = {
                    nombre = it
                    error = null
                },
                label = { Text("Nombre de la materia") },
                isError = mensajeError != null,
                supportingText = {
                    if (mensajeError != null) {
                        Text(mensajeError)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                val nombreLimpio = nombre.trim()
                if (nombreLimpio.isBlank()) {
                    error = "Ingresá un nombre para la materia."
                } else if (nombreYaExiste(nombreLimpio)) {
                    error = "Ya existe una materia con ese nombre."
                } else {
                    onConfirmar(nombreLimpio)
                }
            }) {
                Text(textoConfirmar)
            }
        },
        dismissButton = {
            Button(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}
