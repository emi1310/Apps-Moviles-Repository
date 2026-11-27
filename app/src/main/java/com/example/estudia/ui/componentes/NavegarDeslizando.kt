package com.example.estudia.ui.componentes

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import kotlin.math.abs

// Pasar de una sección del dock a otra deslizando el dedo: hacia la izquierda
// va a la siguiente y hacia la derecha a la anterior.
//
// Anda arrastrando sobre cualquier cosa (tarjetas, botones, el fondo): apenas
// el dedo se mueve claramente de costado, el gesto es de la navegación y lo de
// abajo no recibe el toque (no se abre la tarjeta al soltar). Solo se deja
// pasar si algo de adentro lo agarró primero: el scroll de una lista, una fila
// de pastillas o una tarjeta deslizable. En las tarjetas deslizables
// (Deslizable), un deslizón rápido y corto también cambia de sección; para
// borrar o completar hay que arrastrarlas despacio más de la mitad.
//
// Cuenta si es bien horizontal y se recorren al menos 90 dp, o 40 dp si fue
// rápido.

// Lo que hace un deslizón rápido de costado (paso: 1 = siguiente, -1 =
// anterior). Lo da AppNavegacion; null donde no hay dock.
val LocalCambiarSeccion = staticCompositionLocalOf<((Int) -> Unit)?> { null }

// Velocidad (dp por segundo) desde la que un deslizón cuenta como rápido.
const val VELOCIDAD_DESLIZON = 900f

@Composable
fun Modifier.deslizarEntreSecciones(activo: Boolean, alDeslizar: (paso: Int) -> Unit): Modifier {
    if (!activo) return this
    val accion by rememberUpdatedState(alDeslizar)
    return this.pointerInput(Unit) {
        val minimo = 90.dp.toPx()
        val minimoRapido = 40.dp.toPx()
        val rapido = VELOCIDAD_DESLIZON.dp.toPx()
        val umbral = viewConfiguration.touchSlop
        awaitEachGesture {
            // Pasada Main del padre: llega después de los de adentro, así se
            // sabe si alguno ya usó el movimiento.
            val inicio = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
            val velocidad = VelocityTracker()
            velocidad.addPosition(inicio.uptimeMillis, inicio.position)
            var dx = 0f
            var dy = 0f
            var mio = false
            while (true) {
                val evento = awaitPointerEvent(PointerEventPass.Main)
                val cambio = evento.changes.firstOrNull { it.id == inicio.id } ?: break
                if (evento.changes.size > 1) break
                // Algo de adentro se quedó con el gesto antes: es de ellos.
                if (!mio && cambio.isConsumed) break
                if (!cambio.pressed) {
                    if (mio) {
                        val vx = velocidad.calculateVelocity().x
                        val lejos = abs(dx) >= minimo || (abs(dx) >= minimoRapido && abs(vx) >= rapido)
                        if (lejos && abs(dx) > 2 * abs(dy)) accion(if (dx < 0) 1 else -1)
                        cambio.consume()
                    }
                    break
                }
                dx += cambio.position.x - cambio.previousPosition.x
                dy += cambio.position.y - cambio.previousPosition.y
                velocidad.addPosition(cambio.uptimeMillis, cambio.position)
                if (!mio) {
                    // Primero vertical: es scroll, no navegación.
                    if (abs(dy) > umbral && abs(dy) >= abs(dx)) break
                    if (abs(dx) > umbral && abs(dx) > 2 * abs(dy)) mio = true
                }
                // Al quedárselo, lo de abajo deja de recibir el toque.
                if (mio) cambio.consume()
            }
        }
    }
}

// Para Deslizable: mira (sin quedárselo) el arrastre sobre la tarjeta y, si
// fue un deslizón rápido de costado que no llegó a la mitad del ancho (la
// tarjeta vuelve a su lugar), cambia de sección.
@Composable
fun Modifier.deslizonCambiaSeccion(recorridoParaAccion: () -> Float): Modifier {
    val cambiar = LocalCambiarSeccion.current ?: return this
    val accion by rememberUpdatedState(cambiar)
    val limite by rememberUpdatedState(recorridoParaAccion)
    return this.pointerInput(Unit) {
        val minimoRapido = 40.dp.toPx()
        val rapido = VELOCIDAD_DESLIZON.dp.toPx()
        awaitEachGesture {
            val inicio = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            val velocidad = VelocityTracker()
            velocidad.addPosition(inicio.uptimeMillis, inicio.position)
            var dx = 0f
            var dy = 0f
            while (true) {
                val evento = awaitPointerEvent(PointerEventPass.Final)
                val cambio = evento.changes.firstOrNull { it.id == inicio.id } ?: break
                if (evento.changes.size > 1) break
                if (!cambio.pressed) {
                    val vx = velocidad.calculateVelocity().x
                    val corto = abs(dx) < limite()
                    if (corto && abs(dx) >= minimoRapido && abs(vx) >= rapido && abs(dx) > 2 * abs(dy)) {
                        accion(if (dx < 0) 1 else -1)
                    }
                    break
                }
                dx += cambio.position.x - cambio.previousPosition.x
                dy += cambio.position.y - cambio.previousPosition.y
                velocidad.addPosition(cambio.uptimeMillis, cambio.position)
            }
        }
    }
}
