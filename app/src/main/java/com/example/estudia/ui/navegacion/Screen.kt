package com.example.estudia.ui.navegacion

// Cada pantalla de la app se identifica con una ruta de texto.
// Navigation Compose usa esa ruta dentro de NavHost / navigate().
// Se usa sealed class (como en la materia) en lugar de enum.
sealed class Screen(val ruta: String) {
    object Bienvenida : Screen("bienvenida")
    object Login : Screen("login")
    object Registro : Screen("registro")
    object Inicio : Screen("inicio")
    object Materias : Screen("materias")
    object Calendario : Screen("calendario")
    object Notas : Screen("notas")
    object Perfil : Screen("perfil")
    object Ayuda : Screen("ayuda")
    object Temporizador : Screen("temporizador")
}

// Pantallas que muestran la barra inferior.
val pantallasConBarra = listOf(
    Screen.Inicio,
    Screen.Materias,
    Screen.Calendario,
    Screen.Notas,
    Screen.Perfil
)

// Pantallas con botón flotante: muestran los avisos en su propio Scaffold
// (ver SnackbarDeAvisos), para que el aviso no tape el botón.
val pantallasConBotonFlotante = listOf(
    Screen.Materias,
    Screen.Calendario,
    Screen.Notas
)
