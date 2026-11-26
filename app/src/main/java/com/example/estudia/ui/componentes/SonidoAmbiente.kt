package com.example.estudia.ui.componentes

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// Sonidos de concentración para el temporizador. No son grabaciones: el
// ruido se genera en el momento (como un sintetizador), así la app no pesa
// más. Cada tipo de ruido "suena" distinto según cómo se filtra:
//  - Ruido blanco: todas las frecuencias por igual (como una radio sin señal).
//  - Lluvia: ruido rosa, con menos agudos; se parece a la lluvia constante.
//  - Olas: ruido marrón (todavía más grave) que sube y baja cada ~8 segundos.
enum class TipoSonido { NINGUNO, LLUVIA, OLAS, RUIDO_BLANCO }

class SonidoAmbiente {

    @Volatile private var sonando: TipoSonido = TipoSonido.NINGUNO
    private var hilo: Thread? = null

    fun reproducir(tipo: TipoSonido) {
        if (tipo == sonando && hilo?.isAlive == true) return
        detener()
        if (tipo == TipoSonido.NINGUNO) return
        sonando = tipo
        hilo = Thread({ generar(tipo) }, "sonido-ambiente").apply {
            isDaemon = true
            start()
        }
    }

    fun detener() {
        sonando = TipoSonido.NINGUNO
        hilo?.join(500)
        hilo = null
    }

    // Llena el AudioTrack de a bloques mientras siga elegido el mismo sonido.
    private fun generar(tipo: TipoSonido) {
        val minimo = AudioTrack.getMinBufferSize(FRECUENCIA, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val pista = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(FRECUENCIA)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minimo, BLOQUE * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            return
        }
        val generador = GeneradorRuido(tipo)
        val bloque = ShortArray(BLOQUE)
        var muestras = 0L
        try {
            pista.play()
            while (sonando == tipo) {
                for (i in bloque.indices) {
                    // Sube el volumen de a poco al empezar (sin "clic").
                    val entrada = (muestras / (FRECUENCIA * 1.5f)).coerceAtMost(1f)
                    bloque[i] = (generador.siguiente() * entrada * VOLUMEN * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    muestras++
                }
                pista.write(bloque, 0, bloque.size)
            }
        } catch (e: Exception) {
            // Si el sistema corta el audio, simplemente se deja de sonar.
        } finally {
            runCatching { pista.stop() }
            pista.release()
        }
    }

    companion object {
        const val FRECUENCIA = 22_050
        private const val BLOQUE = 2_048
        private const val VOLUMEN = 0.5f
    }
}

// Genera una muestra por vez, entre -1 y 1. Separado de AudioTrack para
// poder probarlo en los tests.
class GeneradorRuido(private val tipo: TipoSonido, private val random: Random = Random.Default) {
    // Filtro de ruido rosa (método de Paul Kellet).
    private var b0 = 0.0; private var b1 = 0.0; private var b2 = 0.0
    private var b3 = 0.0; private var b4 = 0.0; private var b5 = 0.0; private var b6 = 0.0
    private var marron = 0.0
    private var n = 0L

    fun siguiente(): Float {
        val blanco = random.nextDouble(-1.0, 1.0)
        val valor = when (tipo) {
            TipoSonido.NINGUNO -> 0.0
            TipoSonido.RUIDO_BLANCO -> blanco * 0.35
            TipoSonido.LLUVIA -> {
                b0 = 0.99886 * b0 + blanco * 0.0555179
                b1 = 0.99332 * b1 + blanco * 0.0750759
                b2 = 0.96900 * b2 + blanco * 0.1538520
                b3 = 0.86650 * b3 + blanco * 0.3104856
                b4 = 0.55000 * b4 + blanco * 0.5329522
                b5 = -0.7616 * b5 - blanco * 0.0168980
                val rosa = b0 + b1 + b2 + b3 + b4 + b5 + b6 + blanco * 0.5362
                b6 = blanco * 0.115926
                rosa * 0.11
            }
            TipoSonido.OLAS -> {
                marron = (marron + 0.02 * blanco) / 1.02
                // Una ola cada 8 segundos: el volumen sube y baja suave.
                val ola = 0.55 + 0.45 * sin(2 * PI * n / (SonidoAmbiente.FRECUENCIA * 8.0))
                marron * 3.5 * ola
            }
        }
        n++
        return valor.toFloat().coerceIn(-1f, 1f)
    }
}

// Pastillas para elegir el sonido. Suena solo mientras la sesión corre y la
// pantalla está abierta; al pausar, terminar o salir se apaga.
@Composable
fun SelectorSonido(elegido: TipoSonido, corriendo: Boolean, onElegir: (TipoSonido) -> Unit) {
    val sonido = remember { SonidoAmbiente() }
    DisposableEffect(elegido, corriendo) {
        if (corriendo) sonido.reproducir(elegido) else sonido.detener()
        onDispose { sonido.detener() }
    }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TipoSonido.entries.forEach { tipo ->
            PastillaFiltro(stringResource(textoSonido(tipo)), elegida = tipo == elegido) { onElegir(tipo) }
        }
    }
}

fun textoSonido(tipo: TipoSonido): Int = when (tipo) {
    TipoSonido.NINGUNO -> R.string.sonido_ninguno
    TipoSonido.LLUVIA -> R.string.sonido_lluvia
    TipoSonido.OLAS -> R.string.sonido_olas
    TipoSonido.RUIDO_BLANCO -> R.string.sonido_ruido_blanco
}
