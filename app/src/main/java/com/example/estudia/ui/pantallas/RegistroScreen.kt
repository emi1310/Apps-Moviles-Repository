package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Usuario

// Pantalla de creación de cuenta. Pide solo usuario, contraseña y
// nombre (los demás datos del perfil se completan después, como
// se definió en el modelo UML). El usuario nuevo se guarda desde
// el ViewModel, en onRegistroExitoso.
@Composable
fun RegistroScreen(
    usuariosRegistrados: List<Usuario>,
    onRegistroExitoso: (Usuario) -> Unit,
    onVolverALogin: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var nombreUsuario by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Crear cuenta", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        TextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre completo") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        TextField(
            value = nombreUsuario,
            onValueChange = { nombreUsuario = it },
            label = { Text("Nombre de usuario") },
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
                var yaExiste = false
                for (usuario in usuariosRegistrados) {
                    if (usuario.nombreUsuario == nombreUsuario) {
                        yaExiste = true
                    }
                }
                if (nombre.isBlank() || nombreUsuario.isBlank() || contrasena.isBlank()) {
                    error = "Completá todos los campos"
                } else if (yaExiste) {
                    error = "Ese nombre de usuario ya existe"
                } else {
                    val nuevoUsuario = Usuario(
                        id = usuariosRegistrados.size + 1,
                        nombreUsuario = nombreUsuario,
                        contrasena = contrasena,
                        metodoAuth = MetodoAuth.LOCAL,
                        nombre = nombre
                    )
                    onRegistroExitoso(nuevoUsuario)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear cuenta")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onVolverALogin, modifier = Modifier.fillMaxWidth()) {
            Text("¿Ya tenés cuenta? Iniciá sesión")
        }
    }
}
