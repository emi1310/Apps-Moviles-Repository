package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Usuario

// Ventana emergente para editar los datos del perfil. Carrera, año y
// email son opcionales (si se dejan vacíos, se guardan como null).
@Composable
fun DialogoEditarPerfil(
    usuario: Usuario,
    onConfirmar: (nombre: String, carrera: String?, anio: Int?, email: String?) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf(usuario.nombre) }
    var carrera by remember { mutableStateOf(usuario.carrera ?: "") }
    var anio by remember { mutableStateOf(usuario.anio?.toString() ?: "") }
    var email by remember { mutableStateOf(usuario.email ?: "") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.editar_perfil)) },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it; error = false },
                    label = { Text(stringResource(R.string.nombre)) },
                    isError = error,
                    supportingText = { if (error) Text(stringResource(R.string.error_nombre)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = carrera,
                    onValueChange = { carrera = it },
                    label = { Text(stringResource(R.string.carrera)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = anio,
                    onValueChange = { nuevo -> if (nuevo.all { it.isDigit() } && nuevo.length <= 2) anio = nuevo },
                    label = { Text(stringResource(R.string.anio)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombre.isBlank()) {
                    error = true
                } else {
                    onConfirmar(
                        nombre.trim(),
                        carrera.trim().ifBlank { null },
                        anio.trim().toIntOrNull(),
                        email.trim().ifBlank { null }
                    )
                }
            }) { Text(stringResource(R.string.guardar)) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) }
        }
    )
}

// Ventana emergente para fijar la meta semanal de horas de estudio.
// Si se deja vacía, se quita la meta.
@Composable
fun DialogoMeta(
    metaActual: Int?,
    onConfirmar: (Int?) -> Unit,
    onCancelar: () -> Unit
) {
    var horas by remember { mutableStateOf(metaActual?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.meta_semanal)) },
        text = {
            Column {
                Text(stringResource(R.string.meta_explicacion))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = horas,
                    onValueChange = { nuevo -> if (nuevo.all { it.isDigit() } && nuevo.length <= 3) horas = nuevo },
                    label = { Text(stringResource(R.string.horas_por_semana)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(horas.toIntOrNull()) }) { Text(stringResource(R.string.guardar)) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) }
        }
    )
}
