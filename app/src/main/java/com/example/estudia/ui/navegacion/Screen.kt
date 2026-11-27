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
    object Asistente : Screen("asistente")
    object Perfil : Screen("perfil")
    object Ayuda : Screen("ayuda")
    object Temporizador : Screen("temporizador")
    object Estadisticas : Screen("estadisticas")
    object Horario : Screen("horario")
    object Logros : Screen("logros")
    object Carrera : Screen("carrera")
    object Asistencia : Screen("asistencia")
    object Finales : Screen("finales")
    object Bibliografia : Screen("bibliografia")
    object Mapas : Screen("mapas")
    // Editor de un mapa mental.
    object Mapa : Screen("mapa/{mapaId}") {
        fun de(mapaId: Int) = "mapa/$mapaId"
    }
    // Una nota abierta, con todos sus datos y el texto completo.
    object Nota : Screen("nota/{notaId}") {
        const val ARGUMENTO = "notaId"
        fun de(notaId: Int) = "nota/$notaId"
    }
    object ResumenSemanal : Screen("resumen_semanal")
    object Busqueda : Screen("busqueda")
    object Matriz : Screen("matriz")
    // ¿Qué nota necesito?: el id de la materia (-1 = la primera).
    object NotaNecesaria : Screen("nota_necesaria/{materiaId}") {
        fun de(materiaId: Int) = "nota_necesaria/$materiaId"
    }
    // Quiz con las tarjetas de una materia.
    object Quiz : Screen("quiz/{materiaId}") {
        fun de(materiaId: Int) = "quiz/$materiaId"
    }
    // Juego "Unir pares" con las tarjetas de una materia.
    object Pares : Screen("pares/{materiaId}") {
        fun de(materiaId: Int) = "pares/$materiaId"
    }
    // Repaso de tarjetas: lleva el id de la materia (-1 = todas).
    object Repaso : Screen("repaso/{materiaId}") {
        const val ARGUMENTO = "materiaId"
        fun de(materiaId: Int) = "repaso/$materiaId"
    }
}

// Pantallas que muestran la barra inferior.
val pantallasConBarra = listOf(
    Screen.Inicio,
    Screen.Materias,
    Screen.Calendario,
    Screen.Notas,
    Screen.Asistente,
    Screen.Perfil
)

