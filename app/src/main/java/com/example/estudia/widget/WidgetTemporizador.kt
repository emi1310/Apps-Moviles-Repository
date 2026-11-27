package com.example.estudia.widget

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.example.estudia.EstudiaApp
import com.example.estudia.R
import com.example.estudia.notificaciones.TemporizadorReceiver
import com.example.estudia.ui.navegacion.AccionExterna
import kotlinx.coroutines.flow.first

// Widget "Estudiar": la sesión del temporizador en curso, con el tiempo que
// falta (un Chronometer que cuenta para atrás solo, sin que la app tenga que
// redibujarlo cada segundo) y los botones Pausar o Seguir y Terminar, que
// usan el mismo receptor que la notificación. Sin sesión, "Empezar" abre el
// temporizador.
class WidgetTemporizador : WidgetDeEstudia() {

    override suspend fun vistas(context: Context, app: EstudiaApp): RemoteViews {
        val vistas = RemoteViews(context.packageName, R.layout.widget_temporizador)
        vistas.setOnClickPendingIntent(R.id.tempo_raiz, abrirEnLaApp(context, AccionExterna.TEMPORIZADOR))
        vistas.setOnClickPendingIntent(R.id.tempo_empezar, abrirEnLaApp(context, AccionExterna.TEMPORIZADOR))
        val sesion = if (app.repositorio.usuarioActual.first() == null) null else app.repositorio.sesionEstudio.first()
        val ahora = System.currentTimeMillis()

        if (sesion == null || sesion.terminada(ahora)) {
            vistas.setTextViewText(R.id.tempo_titulo, context.getString(R.string.widget_temporizador_libre))
            vistas.setViewVisibility(R.id.tempo_sesion, View.GONE)
            vistas.setViewVisibility(R.id.tempo_empezar, View.VISIBLE)
            return vistas
        }
        vistas.setViewVisibility(R.id.tempo_sesion, View.VISIBLE)
        vistas.setViewVisibility(R.id.tempo_empezar, View.GONE)
        vistas.setTextViewText(
            R.id.tempo_titulo,
            context.getString(if (sesion.enPausa) R.string.widget_temporizador_pausa else R.string.widget_temporizador_estudiando, sesion.nombreMateria)
        )
        val restante = sesion.restanteMillis(ahora)
        if (sesion.enPausa) {
            vistas.setViewVisibility(R.id.tempo_reloj, View.GONE)
            vistas.setViewVisibility(R.id.tempo_pausado, View.VISIBLE)
            val segundos = restante / 1000
            vistas.setTextViewText(R.id.tempo_pausado, "%02d:%02d".format(segundos / 60, segundos % 60))
            vistas.setImageViewResource(R.id.tempo_pausar, R.drawable.wi_seguir)
            vistas.setContentDescription(R.id.tempo_pausar, context.getString(R.string.widget_seguir))
        } else {
            vistas.setViewVisibility(R.id.tempo_pausado, View.GONE)
            vistas.setViewVisibility(R.id.tempo_reloj, View.VISIBLE)
            vistas.setChronometer(R.id.tempo_reloj, SystemClock.elapsedRealtime() + restante, null, true)
            vistas.setChronometerCountDown(R.id.tempo_reloj, true)
            vistas.setImageViewResource(R.id.tempo_pausar, R.drawable.wi_pausa)
            vistas.setContentDescription(R.id.tempo_pausar, context.getString(R.string.widget_pausar))
        }
        val accionPausa = if (sesion.enPausa) TemporizadorReceiver.ACCION_REANUDAR else TemporizadorReceiver.ACCION_PAUSAR
        vistas.setOnClickPendingIntent(
            R.id.tempo_pausar,
            avisoAReceptor(context, 20_001, Intent(context, TemporizadorReceiver::class.java).setAction(accionPausa))
        )
        vistas.setOnClickPendingIntent(
            R.id.tempo_terminar,
            avisoAReceptor(context, 20_002, Intent(context, TemporizadorReceiver::class.java).setAction(TemporizadorReceiver.ACCION_TERMINAR))
        )
        return vistas
    }
}
