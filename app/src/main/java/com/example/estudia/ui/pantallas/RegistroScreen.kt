package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.estudia.viewmodel.SesionViewModel

// Pantalla de creación de cuenta. Pide solo nombre, usuario y contraseña
// (los demás datos del perfil se completan después). Las validaciones y
// el guardado (con la contraseña convertida en hash) los hace el ViewModel.
@Composable
fun RegistroScreen(
    estado: AuthUiState,
    onRegistrar: (nombre: String, nombreUsuario: String, contrasena: String) -> Unit,
    onVolverALogin: () -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var nombreUsuario by rememberSaveable { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }

    Column(
        // imePadding + scroll: cuando aparece el teclado, el formulario se corre
        // hacia arriba en vez de quedar tapado.
        modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(R.string.crear_cuenta), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text(stringResource(R.string.nombre_completo)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = nombreUsuario,
            onValueChange = { nombreUsuario = it },
            label = { Text(stringResource(R.string.nombre_usuario)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text(stringResource(R.string.contrasena)) },
            supportingText = { Text(stringResource(R.string.contrasena_minimo, SesionViewModel.LARGO_MINIMO_CONTRASENA)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        TextoError(estado.error?.let { stringResource(it) })
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onRegistrar(nombre, nombreUsuario, contrasena) },
            enabled = !estado.procesando,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (estado.procesando) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(stringResource(R.string.crear_cuenta))
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onVolverALogin, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ir_a_login))
        }
    }
}
