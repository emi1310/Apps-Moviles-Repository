package com.example.estudia.ui.pantallas

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.estudia.datos.crearUsuarioDePrueba

// Este archivo junta todas las vistas previas (@Preview) de las pantallas
// de la app, separadas de la lógica real, para poder ver cada diseño sin
// tener que correr la app entera. Estas funciones NO forman parte de la
// app final — Android Studio las descarta automáticamente al compilar
// para instalar en un celular.

@Preview(showBackground = true, name = "Inicio")
@Composable
fun InicioScreenPreview() {
    InicioScreen(usuario = crearUsuarioDePrueba())
}

@Preview(showBackground = true, name = "Materias")
@Composable
fun MateriasScreenPreview() {
    MateriasScreen(usuario = crearUsuarioDePrueba())
}

@Preview(showBackground = true, name = "Calendario")
@Composable
fun CalendarioScreenPreview() {
    CalendarioScreen(usuario = crearUsuarioDePrueba())
}

@Preview(showBackground = true, name = "Notas")
@Composable
fun NotasScreenPreview() {
    NotasScreen(usuario = crearUsuarioDePrueba())
}

@Preview(showBackground = true, name = "Perfil")
@Composable
fun PerfilScreenPreview() {
    PerfilScreen(usuario = crearUsuarioDePrueba(), onCerrarSesion = {})
}

@Preview(showBackground = true, name = "Bienvenida")
@Composable
fun BienvenidaScreenPreview() {
    BienvenidaScreen(onComenzar = {})
}

@Preview(showBackground = true, name = "Login")
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        usuariosRegistrados = listOf(crearUsuarioDePrueba()),
        onLoginExitoso = {},
        onIrARegistro = {}
    )
}

@Preview(showBackground = true, name = "Registro")
@Composable
fun RegistroScreenPreview() {
    RegistroScreen(
        usuariosRegistrados = mutableListOf(crearUsuarioDePrueba()),
        onRegistroExitoso = {},
        onVolverALogin = {}
    )
}