package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.estudia.R

// Ventana emergente que pide el nombre de una materia. Sirve para crear
// una materia nueva (nombreInicial vacío) o renombrar una existente.
// nombreDisponible lo decide el ViewModel (no se puede repetir el nombre).
@Composable
fun DialogoMateria(
    nombreInicial: String,
    nombreDisponible: (String) -> Boolean,
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    val esNueva = nombreInicial.isEmpty()
    var nombre by remember { mutableStateOf(nombreInicial) }
    var error by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(if (esNueva) R.string.nueva_materia else R.string.editar_materia)) },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    nombre = it
                    error = null
                },
                label = { Text(stringResource(R.string.nombre_materia)) },
                isError = error != null,
                supportingText = { error?.let { Text(stringResource(it)) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    nombre.isBlank() -> R.string.error_nombre_materia
                    !nombreDisponible(nombre) -> R.string.error_materia_repetida
                    else -> null
                }
                if (error == null) onConfirmar(nombre.trim())
            }) { Text(stringResource(if (esNueva) R.string.agregar else R.string.guardar)) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) }
        }
    )
}
