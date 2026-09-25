package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.Usuario

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
    var error by remember { mutableStateOf<String?>(null) }

    val mensajeError = error

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Editar perfil") },
        text = {
            Column {
                TextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        error = null
                    },
                    label = { Text("Nombre") },
                    isError = mensajeError != null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = carrera,
                    onValueChange = { carrera = it },
                    label = { Text("Carrera") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = anio,
                    onValueChange = { nuevoValor ->
                        if (nuevoValor.all { it.isDigit() }) anio = nuevoValor
                    },
                    label = { Text("Año") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
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
                val nombreLimpio = nombre.trim()
                if (nombreLimpio.isBlank()) {
                    error = "Ingresá tu nombre."
                } else {
                    onConfirmar(
                        nombreLimpio,
                        carrera.trim().ifBlank { null },
                        anio.trim().toIntOrNull(),
                        email.trim().ifBlank { null }
                    )
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
