package com.example.estudia.ui.componentes

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlin.math.abs

// De dónde viene la luz que se refleja en el vidrio líquido: x e y van de 0 a
// 1 dentro de cada pieza (0,0 = arriba a la izquierda). Por defecto la luz
// pega arriba a la derecha; al inclinar el teléfono, el reflejo se corre.
class Luz {
    var x by mutableFloatStateOf(0.82f)
    var y by mutableFloatStateOf(0f)
}

val LocalLuz = staticCompositionLocalOf<Luz?> { null }

// Lee el sensor de gravedad mientras la app está en primer plano. Con el
// ahorro de batería prendido (o sin sensor) la luz queda quieta.
@Composable
fun rememberLuzDelTelefono(): Luz? {
    if (!vidrioLiquidoDisponible()) return null
    val contexto = LocalContext.current
    val luz = remember { Luz() }
    LifecycleResumeEffect(luz) {
        val sensores = contexto.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val ahorro = (contexto.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
        val sensor = sensores?.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sensores?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val oyente = object : SensorEventListener {
            override fun onSensorChanged(evento: SensorEvent) {
                // Inclinar hacia un costado corre la luz para ese lado; inclinar
                // hacia adelante o atrás la sube o la baja un poco.
                val objetivoX = (0.5f - evento.values[0] / SensorManager.GRAVITY_EARTH * 0.6f).coerceIn(0.08f, 0.92f)
                val objetivoY = ((7f - evento.values[1]) / SensorManager.GRAVITY_EARTH * 0.5f).coerceIn(0f, 0.45f)
                // Suavizado para que no tiemble.
                val nuevoX = luz.x + (objetivoX - luz.x) * 0.15f
                val nuevoY = luz.y + (objetivoY - luz.y) * 0.15f
                if (abs(nuevoX - luz.x) > 0.004f) luz.x = nuevoX
                if (abs(nuevoY - luz.y) > 0.004f) luz.y = nuevoY
            }
            override fun onAccuracyChanged(sensor: Sensor?, precision: Int) {}
        }
        if (sensores != null && sensor != null && !ahorro) {
            sensores.registerListener(oyente, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onPauseOrDispose { sensores?.unregisterListener(oyente) }
    }
    return luz
}
