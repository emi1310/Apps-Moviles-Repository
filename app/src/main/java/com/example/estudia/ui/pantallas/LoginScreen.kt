package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.Usuario

@Composable
fun LoginScreen(
    usuariosRegistrados: List<Usuario>,
    onLoginExitoso: (Usuario) -> Unit,
    onIrARegistro: () -> Unit
) {
    var nombreUsuario by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Iniciar sesión", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        TextField(
            value = nombreUsuario,
            onValueChange = { nombreUsuario = it },
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        val mensajeError = error
        if (mensajeError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = mensajeError, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                var usuarioEncontrado: Usuario? = null
                for (usuario in usuariosRegistrados) {
                    if (usuario.iniciarSesion(nombreUsuario, contrasena)) {
                        usuarioEncontrado = usuario
                    }
                }
                if (usuarioEncontrado != null) {
                    onLoginExitoso(usuarioEncontrado)
                } else {
                    error = "Usuario o contraseña incorrectos"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Iniciar sesión")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onIrARegistro, modifier = Modifier.fillMaxWidth()) {
            Text("¿No tenés cuenta? Creá una")
        }
    }
}
