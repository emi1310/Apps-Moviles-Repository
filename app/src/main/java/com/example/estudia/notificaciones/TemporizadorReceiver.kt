package com.example.estudia.notificaciones

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.estudia.EstudiaApp
import com.example.estudia.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Atiende los botones de la notificación del temporizador (Pausar, Seguir,
// Terminar), aunque la app esté cerrada. Usa el mismo Repository que la
// pantalla, así la pantalla se entera sola del cambio (la sesión es un Flow).
//
// goAsync(): el receptor tiene unos segundos para terminar trabajo en
// segundo plano (guardar en DataStore y Room) antes de que Android lo corte.
class TemporizadorReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repositorio = (context.applicationContext as EstudiaApp).repositorio
        val pendiente = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    ACCION_PAUSAR -> repositorio.pausarSesion()
                    ACCION_REANUDAR -> repositorio.reanudarSesion()
                    ACCION_TERMINAR -> {
                        val minutos = repositorio.terminarSesion(guardar = true)
                        if (minutos > 0) {
                            Notificaciones.mostrar(
                                context,
                                Notificaciones.ID_TEMPORIZADOR,
                                context.getString(R.string.notificacion_temporizador_guardada_titulo),
                                context.getString(R.string.aviso_sesion_guardada, minutos)
                            )
                        }
                    }
                }
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        const val ACCION_PAUSAR = "com.example.estudia.temporizador.PAUSAR"
        const val ACCION_REANUDAR = "com.example.estudia.temporizador.REANUDAR"
        const val ACCION_TERMINAR = "com.example.estudia.temporizador.TERMINAR"
    }
}
