package com.example.estudia.notificaciones

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.estudia.EstudiaApp
import com.example.estudia.modelo.diasSinClase
import com.example.estudia.modelo.estadoSilencioClase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

// "Silenciar en clase": pone No molestar al empezar cada clase o examen del
// horario y lo saca al terminar. Usa una alarma de Android que se vuelve a
// programar sola para el próximo cambio (no hace falta que la app esté
// abierta). Es una alarma "inexacta" (no pide permisos especiales), así que
// puede correrse un par de minutos.
//
// Como con el temporizador, necesita el acceso a No molestar y deja el
// teléfono como estaba: si el usuario ya tenía No molestar puesto, no lo
// toca. La elección se guarda en SharedPreferences para leerla al instante
// desde la alarma, aunque la app esté cerrada.
object SilencioEnClase {

    private const val PREFERENCIAS = "silencio_clase"
    private const val CLAVE_ACTIVO = "activo"
    private const val CLAVE_ANTERIOR = "filtro_anterior"

    private fun preferencias(context: Context) = context.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)

    fun activo(context: Context): Boolean = preferencias(context).getBoolean(CLAVE_ACTIVO, false)

    fun accesoConcedido(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    fun cambiar(context: Context, activar: Boolean) {
        preferencias(context).edit().putBoolean(CLAVE_ACTIVO, activar).apply()
        if (!activar) {
            restaurar(context)
            cancelarAlarma(context)
        }
        actualizar(context)
    }

    // Revisa si ahora hay clase, silencia o restaura, y programa la próxima
    // revisión. Se llama al activarlo, al cambiar el horario, al reiniciar el
    // teléfono y desde la alarma.
    fun actualizar(context: Context, alTerminar: () -> Unit = {}) {
        val app = context.applicationContext as? EstudiaApp
        if (app == null || !activo(context)) {
            alTerminar()
            return
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val usuario = app.repositorio.usuarioActual.first()
                if (usuario == null) {
                    restaurar(context)
                    return@launch
                }
                val ahora = LocalDateTime.now()
                val sinClase = diasSinClase(app.repositorio.feriados.first(), usuario.recesos).keys
                val estado = estadoSilencioClase(usuario, ahora, sinClase)
                if (estado.enClase) silenciar(context) else restaurar(context)
                // Aunque no haya clases cerca, se revisa una vez por día.
                val proxima = listOfNotNull(estado.proximoCambio, ahora.plusDays(1)).min()
                programarAlarma(context, proxima)
            } finally {
                alTerminar()
            }
        }
    }

    private fun silenciar(context: Context) {
        val gestor = context.getSystemService(NotificationManager::class.java)
        val guardado = preferencias(context)
        if (!gestor.isNotificationPolicyAccessGranted || guardado.contains(CLAVE_ANTERIOR)) return
        val actual = gestor.currentInterruptionFilter
        if (actual != NotificationManager.INTERRUPTION_FILTER_ALL) return
        guardado.edit().putInt(CLAVE_ANTERIOR, actual).apply()
        gestor.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    private fun restaurar(context: Context) {
        val guardado = preferencias(context)
        if (!guardado.contains(CLAVE_ANTERIOR)) return
        val anterior = guardado.getInt(CLAVE_ANTERIOR, NotificationManager.INTERRUPTION_FILTER_ALL)
        guardado.edit().remove(CLAVE_ANTERIOR).apply()
        val gestor = context.getSystemService(NotificationManager::class.java)
        if (gestor.isNotificationPolicyAccessGranted) gestor.setInterruptionFilter(anterior)
    }

    private fun intentAlarma(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0, Intent(context, SilencioClaseReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun programarAlarma(context: Context, cuando: LocalDateTime) {
        val millis = cuando.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intentAlarma(context))
    }

    private fun cancelarAlarma(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(intentAlarma(context))
    }
}

// La alarma: empezó o terminó una clase.
class SilencioClaseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendiente = goAsync()
        SilencioEnClase.actualizar(context) { pendiente.finish() }
    }
}
