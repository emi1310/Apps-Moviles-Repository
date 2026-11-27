package com.example.estudia.ui.componentes

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

// Pasar de una sección del dock a otra deslizando el dedo: hacia la izquierda
// va a la siguiente y hacia la derecha a la anterior.
//
// Solo cuenta un gesto que nadie más usó: si el dedo hace scroll en una lista,
// mueve una fila deslizable (completar o borrar) o una fila de pastillas, el
// gesto es de ellos y no se cambia de sección. Además tiene que ser bien
// horizontal y de al menos 90 dp.
@Composable
fun Modifier.deslizarEntreSecciones(activo: Boolean, alDeslizar: (paso: Int) -> Unit): Modifier {
    if (!activo) return this
    val accion by rememberUpdatedState(alDeslizar)
    return this.pointerInput(Unit) {
        val minimo = 90.dp.toPx()
        awaitEachGesture {
            // Se mira al final (después de los hijos) para saber si alguno lo usó.
            val inicio = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            var dx = 0f
            var dy = 0f
            while (true) {
                val evento = awaitPointerEvent(PointerEventPass.Final)
                val cambio = evento.changes.firstOrNull { it.id == inicio.id } ?: break
                // Otro dedo o algo de adentro se quedó con el gesto: no es para acá.
                if (evento.changes.size > 1 || cambio.isConsumed) break
                if (!cambio.pressed) {
                    if (abs(dx) >= minimo && abs(dx) > 2 * abs(dy)) accion(if (dx < 0) 1 else -1)
                    break
                }
                dx += cambio.position.x - cambio.previousPosition.x
                dy += cambio.position.y - cambio.previousPosition.y
            }
        }
    }
}
