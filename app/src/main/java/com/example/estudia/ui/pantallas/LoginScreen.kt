package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.Usuario

// Pantalla de inicio de sesión. Busca, dentro de la lista de usuarios
// registrados en memoria, uno cuyas credenciales coincidan.
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

        OutlinedTextField(
            value = nombreUsuario,
            onValueChange = { nombreUsuario = it },
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val usuarioEncontrado = usuariosRegistrados.firstOrNull {
                    it.iniciarSesion(nombreUsuario, contrasena)
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

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                // Simulación: no hay conexión real con Google (requeriría
                // una librería externa). Se crea/usa un usuario de prueba
                // marcado como autenticado por Google.
                val usuarioGoogle = usuariosRegistrados.firstOrNull { it.metodoAuth == com.example.estudia.modelo.MetodoAuth.GOOGLE }
                    ?: Usuario(
                        id = usuariosRegistrados.size + 1,
                        nombreUsuario = "usuario_google",
                        contrasena = null,
                        metodoAuth = com.example.estudia.modelo.MetodoAuth.GOOGLE,
                        nombre = "Usuario de Google"
                    )
                onLoginExitoso(usuarioGoogle)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continuar con Google")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onIrARegistro, modifier = Modifier.fillMaxWidth()) {
            Text("¿No tenés cuenta? Creá una")
        }
    }
}