package com.example.estudia.ui.componentes

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

// Transición compartida entre pantallas: un elemento de una pantalla "se
// convierte" en otro de la siguiente (por ejemplo, la tarjeta de una nota se
// agranda hasta ser la nota abierta). AppNavegacion envuelve todo en un
// SharedTransitionLayout y cada pantalla que participa da su alcance animado.

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalTransicionCompartida = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAlcanceAnimado = compositionLocalOf<AnimatedVisibilityScope?> { null }

// Curva "enfatizada" de Material 3: arranca rápido y frena suave.
val CurvaEnfatizada = CubicBezierEasing(0.2f, 0f, 0f, 1f)
const val DURACION_COMPARTIDA = 420

@OptIn(ExperimentalSharedTransitionApi::class)
private val transformarLimites = BoundsTransform { _, _ -> tween(DURACION_COMPARTIDA, easing = CurvaEnfatizada) }

// El contenedor (la tarjeta y la pantalla entera): se agranda o se achica de
// uno al otro mientras el contenido de cada uno aparece y desaparece.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.limitesCompartidos(clave: Any): Modifier {
    val transicion = LocalTransicionCompartida.current ?: return this
    val alcance = LocalAlcanceAnimado.current ?: return this
    return with(transicion) {
        this@limitesCompartidos.sharedBounds(
            rememberSharedContentState(clave),
            alcance,
            enter = fadeIn(tween(DURACION_COMPARTIDA / 2, delayMillis = DURACION_COMPARTIDA / 6)),
            exit = fadeOut(tween(DURACION_COMPARTIDA / 3)),
            boundsTransform = transformarLimites,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
        )
    }
}

// Un elemento igual en las dos pantallas (el título): viaja de un lugar al otro.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.elementoCompartido(clave: Any): Modifier {
    val transicion = LocalTransicionCompartida.current ?: return this
    val alcance = LocalAlcanceAnimado.current ?: return this
    return with(transicion) {
        this@elementoCompartido.sharedBounds(
            rememberSharedContentState(clave),
            alcance,
            boundsTransform = transformarLimites,
            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds()
        )
    }
}
