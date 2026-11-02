package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.ui.componentes.TextoError
import com.example.estudia.viewmodel.AuthUiState

// Pantalla de inicio de sesión. Solo muestra el formulario y le envía
// los datos al ViewModel, que es quien verifica las credenciales.
@Composable
fun LoginScreen(
    estado: AuthUiState,
    onIniciarSesion: (nombreUsuario: String, contrasena: String) -> Unit,
    onIrARegistro: () -> Unit
) {
    var nombreUsuario by rememberSaveable { mutableStateOf("") }
    // La contraseña no se guarda con rememberSaveable (no queda en el estado guardado).
    var contrasena by remember { mutableStateOf("") }

    FormularioCuenta(
        icono = Icons.Filled.Lock,
        titulo = stringResource(R.string.iniciar_sesion),
        subtitulo = stringResource(R.string.login_subtitulo),
        campos = {
            OutlinedTextField(
                value = nombreUsuario,
                onValueChange = { nombreUsuario = it },
                label = { Text(stringResource(R.string.usuario)) },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it },
                label = { Text(stringResource(R.string.contrasena)) },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextoError(estado.error?.let { stringResource(it) })
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onIniciarSesion(nombreUsuario, contrasena) },
                enabled = !estado.procesando,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (estado.procesando) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.iniciar_sesion))
            }
        },
        pie = {
            TextButton(onClick = onIrARegistro) {
                Text(stringResource(R.string.ir_a_registro))
            }
        }
    )
}
