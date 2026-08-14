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
// se definió en el modelo UML).
@Composable
fun RegistroScreen(
    usuariosRegistrados: MutableList<Usuario>,
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

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre completo") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = nombreUsuario,
            onValueChange = { nombreUsuario = it },
            label = { Text("Nombre de usuario") },
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
                val yaExiste = usuariosRegistrados.any { it.nombreUsuario == nombreUsuario }
                when {
                    nombre.isBlank() || nombreUsuario.isBlank() || contrasena.isBlank() ->
                        error = "Completá todos los campos"
                    yaExiste ->
                        error = "Ese nombre de usuario ya existe"
                    else -> {
                        val nuevoUsuario = Usuario(
                            id = usuariosRegistrados.size + 1,
                            nombreUsuario = nombreUsuario,
                            contrasena = contrasena,
                            metodoAuth = MetodoAuth.LOCAL,
                            nombre = nombre
                        )
                        usuariosRegistrados.add(nuevoUsuario)
                        onRegistroExitoso(nuevoUsuario)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear cuenta")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onVolverALogin, modifier = Modifier.fillMaxWidth()) {
            Text("¿Ya tenés cuenta? Iniciá sesión")
        }
    }
}