package com.example.estudia.ui.componentes

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.estudia.R

// Ventana emergente genérica para confirmar una acción (cerrar sesión,
// eliminar una materia, etc). textoConfirmar cambia el texto del botón
// de confirmar para que quede claro qué se va a hacer.
@Composable
fun DialogoConfirmacion(
    mensaje: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text(textoConfirmar) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) }
        }
    )
}
