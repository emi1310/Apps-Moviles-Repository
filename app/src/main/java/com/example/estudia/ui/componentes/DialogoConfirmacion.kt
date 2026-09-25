package com.example.estudia.ui.componentes

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun DialogoConfirmacion(
    mensaje: String,
    textoConfirmar: String = "Confirmar",
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        text = { Text(mensaje) },
        confirmButton = {
            Button(onClick = onConfirmar) {
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
