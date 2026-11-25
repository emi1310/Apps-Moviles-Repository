package com.example.estudia.ui.navegacion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

// Transiciones entre pantallas. Dos tipos:
//  - Entre secciones del dock (Inicio, Materias, Calendario, Notas, Perfil):
//    un deslizamiento lateral corto con fundido, hacia el lado de la
//    sección elegida (ir a la derecha en el dock desliza a la izquierda).
//  - Pantallas de detalle (Estadísticas, Horario, Temporizador...): entran
//    desde la derecha sobre la anterior, que se aleja un poco; al volver
//    salen hacia la derecha.
// Duran menos de 350 ms para que la app se sienta ágil.

private const val DURACION = 320
private const val DURACION_SALIDA = 200

typealias Alcance = AnimatedContentTransitionScope<NavBackStackEntry>

private fun indiceEnDock(entrada: NavBackStackEntry): Int =
    pantallasConBarra.indexOfFirst { it.ruta == entrada.destination.route }

// -1 = la sección nueva está a la izquierda, 1 = a la derecha, null = no es
// un cambio entre dos secciones del dock.
private fun Alcance.direccionEntreSecciones(): Int? {
    val desde = indiceEnDock(initialState)
    val hacia = indiceEnDock(targetState)
    if (desde < 0 || hacia < 0) return null
    return if (hacia > desde) 1 else -1
}

fun Alcance.entrar(): EnterTransition {
    val direccion = direccionEntreSecciones()
    return if (direccion != null) {
        fadeIn(tween(DURACION, easing = FastOutSlowInEasing)) +
            slideInHorizontally(tween(DURACION, easing = FastOutSlowInEasing)) { ancho -> direccion * ancho / 8 }
    } else {
        fadeIn(tween(DURACION)) +
            slideInHorizontally(tween(DURACION, easing = FastOutSlowInEasing)) { ancho -> ancho / 3 }
    }
}

fun Alcance.salir(): ExitTransition {
    val direccion = direccionEntreSecciones()
    return if (direccion != null) {
        fadeOut(tween(DURACION_SALIDA)) +
            slideOutHorizontally(tween(DURACION, easing = FastOutSlowInEasing)) { ancho -> -direccion * ancho / 8 }
    } else {
        // La pantalla de abajo se achica apenas mientras entra la de detalle.
        fadeOut(tween(DURACION)) + scaleOut(tween(DURACION), targetScale = 0.96f)
    }
}

// Al volver atrás desde una pantalla de detalle, la de abajo reaparece.
fun Alcance.volverAEntrar(): EnterTransition =
    if (direccionEntreSecciones() != null) entrar()
    else fadeIn(tween(DURACION)) + scaleIn(tween(DURACION), initialScale = 0.96f)

// ...y la de detalle se va hacia la derecha.
fun Alcance.volverASalir(): ExitTransition =
    if (direccionEntreSecciones() != null) salir()
    else fadeOut(tween(DURACION_SALIDA)) +
        slideOutHorizontally(tween(DURACION, easing = FastOutSlowInEasing)) { ancho -> ancho / 3 }
