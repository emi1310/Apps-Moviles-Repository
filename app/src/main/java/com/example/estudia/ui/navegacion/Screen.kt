package com.example.estudia.ui.navegacion

sealed class Screen(val ruta: String) {
    object Bienvenida : Screen("bienvenida")
    object Login : Screen("login")
    object Registro : Screen("registro")
    object Inicio : Screen("inicio")
    object Materias : Screen("materias")
    object Calendario : Screen("calendario")
    object Notas : Screen("notas")
    object Perfil : Screen("perfil")
}

val pantallasConBarra = listOf(
    Screen.Inicio,
    Screen.Materias,
    Screen.Calendario,
    Screen.Notas,
    Screen.Perfil
)
