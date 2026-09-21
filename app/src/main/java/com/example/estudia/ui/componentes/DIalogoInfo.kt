package com.example.estudia.ui.componentes

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

// Ventana emergente genérica para avisar algo simple, como que una
// función todavía no está implementada. Se reutiliza en varias pantallas.
@Composable
fun DialogoInfo(mensaje: String, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = onCerrar) {
                Text("Entendido")
            }
        }
    )
}