package com.example.estudia.ui.componentes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

// Valor de una barra de progreso que se llena de a poco: arranca en 0 la
// primera vez que se muestra y después se mueve suave cada vez que cambia
// el objetivo (ej: al marcar una tarea). animateFloatAsState no sirve para
// esto porque empieza directamente en el valor final.
@Composable
fun progresoAnimado(objetivo: Float, duracionMs: Int = 700): Float {
    val valor = remember { Animatable(0f) }
    LaunchedEffect(objetivo) {
        valor.animateTo(objetivo.coerceIn(0f, 1f), tween(duracionMs))
    }
    return valor.value
}
