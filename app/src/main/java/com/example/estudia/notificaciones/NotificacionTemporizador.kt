package com.example.estudia.notificaciones

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.estudia.MainActivity
import com.example.estudia.R
import com.example.estudia.modelo.SesionEstudio

// Notificación fija mientras hay una sesión del temporizador en curso.
//
// La cuenta regresiva la dibuja Android con un cronómetro (setWhen +
// setChronometerCountDown): no hace falta actualizarla cada segundo ni un
// servicio en primer plano, y sigue contando aunque la app esté cerrada.
// Los botones (Pausar / Seguir / Terminar) los atiende TemporizadorReceiver.
// La muestra y la oculta AvisosSistema cada vez que cambia la sesión.
class NotificacionTemporizador(private val context: Context) {

    fun mostrar(sesion: SesionEstudio) {
        if (!Notificaciones.permitidas(context)) return
        val constructor = NotificationCompat.Builder(context, Notificaciones.CANAL_TEMPORIZADOR)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(context.getString(R.string.temporizador_estudiando, sesion.nombreMateria))
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(abrirTemporizador())

        val fin = sesion.finMillis
        if (fin == null) {
            val minutos = sesion.minutosRestantes(System.currentTimeMillis())
            constructor
                .setContentText(context.getString(R.string.notificacion_temporizador_pausa, minutos))
                .setShowWhen(false)
                .addAction(R.drawable.ic_notificacion, context.getString(R.string.temporizador_reanudar), accion(TemporizadorReceiver.ACCION_REANUDAR))
        } else {
            constructor
                .setContentText(context.getString(R.string.notificacion_temporizador_corriendo, sesion.minutos))
                .setWhen(fin)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(R.drawable.ic_notificacion, context.getString(R.string.temporizador_pausar), accion(TemporizadorReceiver.ACCION_PAUSAR))
        }
        constructor.addAction(R.drawable.ic_notificacion, context.getString(R.string.temporizador_terminar), accion(TemporizadorReceiver.ACCION_TERMINAR))

        try {
            NotificationManagerCompat.from(context).notify(Notificaciones.ID_TEMPORIZADOR_EN_CURSO, constructor.build())
        } catch (e: SecurityException) {
            // El usuario revocó el permiso justo ahora: no se muestra nada.
        }
    }

    fun ocultar() {
        NotificationManagerCompat.from(context).cancel(Notificaciones.ID_TEMPORIZADOR_EN_CURSO)
    }

    // Al tocar la notificación se abre la app directo en el temporizador.
    // SINGLE_TOP: si la app ya estaba abierta, no se crea otra pantalla
    // (le llega el pedido a MainActivity.onNewIntent).
    private fun abrirTemporizador(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ABRIR_TEMPORIZADOR, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context, CODIGO_ABRIR, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    // Intent explícito (solo lo recibe nuestra app) para cada botón.
    private fun accion(accion: String): PendingIntent {
        val intent = Intent(context, TemporizadorReceiver::class.java).setAction(accion)
        return PendingIntent.getBroadcast(
            context, accion.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        private const val CODIGO_ABRIR = 1
    }
}
