package com.example.estudia.ui.navegacion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

// Transiciones entre pantallas, con las curvas "enfatizadas" de Material 3
// (arrancan rápido y frenan suave, se sienten más naturales que una lineal):
//  - Entre secciones del dock (Inicio, Materias, Calendario, Notas...): la
//    nueva entra desde el lado de la sección elegida (ir a la derecha en el
//    dock la trae desde la derecha) y apenas más chica; la anterior se corre
//    hacia el otro lado y se desvanece rápido. Así no se ven las dos a la vez.
//  - Pantallas de detalle (Estadísticas, Horario, Temporizador...): entran
//    desde la derecha sobre la anterior, que se corre un poco a la izquierda y
//    se oscurece; al volver es al revés. Con el gesto de "atrás" de Android
//    (atrás predictivo) la animación sigue al dedo.
//  - La nota abierta: solo un fundido; el movimiento lo hace la tarjeta, que
//    se agranda hasta ser la nota (TransicionCompartida.kt).

private const val DURACION = 380
private const val DURACION_SALIDA = 160

private val Enfatizada = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val EnfatizadaEntra = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EnfatizadaSale = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

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

// Abrir o cerrar una nota (de la lista a la nota o al revés).
private fun Alcance.esNota(): Boolean =
    initialState.destination.route == Screen.Nota.ruta || targetState.destination.route == Screen.Nota.ruta

fun Alcance.entrar(): EnterTransition {
    if (esNota()) return fadeIn(tween(DURACION, easing = Enfatizada))
    val direccion = direccionEntreSecciones()
    return if (direccion != null) {
        fadeIn(tween(DURACION / 2, delayMillis = 60)) +
            scaleIn(tween(DURACION, easing = EnfatizadaEntra), initialScale = 0.97f) +
            slideInHorizontally(tween(DURACION, easing = EnfatizadaEntra)) { ancho -> direccion * ancho / 6 }
    } else {
        fadeIn(tween(DURACION / 2)) +
            slideInHorizontally(tween(DURACION, easing = EnfatizadaEntra)) { ancho -> ancho / 4 }
    }
}

fun Alcance.salir(): ExitTransition {
    if (esNota()) return fadeOut(tween(DURACION, easing = Enfatizada))
    val direccion = direccionEntreSecciones()
    return if (direccion != null) {
        fadeOut(tween(DURACION_SALIDA, easing = EnfatizadaSale)) +
            slideOutHorizontally(tween(DURACION, easing = Enfatizada)) { ancho -> -direccion * ancho / 10 }
    } else {
        // La de abajo se corre un poco y se desvanece mientras entra la de detalle.
        fadeOut(tween(DURACION, easing = Enfatizada), targetAlpha = 0.2f) +
            slideOutHorizontally(tween(DURACION, easing = Enfatizada)) { ancho -> -ancho / 10 }
    }
}

// Al volver atrás desde una pantalla de detalle, la de abajo vuelve a su lugar.
fun Alcance.volverAEntrar(): EnterTransition {
    if (esNota()) return fadeIn(tween(DURACION, easing = Enfatizada))
    return if (direccionEntreSecciones() != null) entrar()
    else fadeIn(tween(DURACION, easing = Enfatizada), initialAlpha = 0.2f) +
        slideInHorizontally(tween(DURACION, easing = Enfatizada)) { ancho -> -ancho / 10 }
}

// ...y la de detalle se va hacia la derecha, achicándose apenas.
fun Alcance.volverASalir(): ExitTransition {
    if (esNota()) return fadeOut(tween(DURACION_SALIDA))
    return if (direccionEntreSecciones() != null) salir()
    else fadeOut(tween(DURACION, easing = EnfatizadaSale)) +
        scaleOut(tween(DURACION, easing = Enfatizada), targetScale = 0.94f) +
        slideOutHorizontally(tween(DURACION, easing = Enfatizada)) { ancho -> ancho / 4 }
}
