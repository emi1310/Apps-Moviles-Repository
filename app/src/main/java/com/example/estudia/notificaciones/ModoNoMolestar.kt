package com.example.estudia.notificaciones

import android.app.NotificationManager
import android.content.Context

// Activa y apaga "No molestar" mientras corre el temporizador, si el
// usuario lo eligió y le dio a la app el acceso especial (ACCESS_NOTIFICATION_POLICY,
// que se da desde Ajustes, no con un diálogo).
//
// Para dejar el teléfono como estaba, se guarda el modo anterior en
// SharedPreferences: se lee al instante (sin corrutinas) desde cualquier
// lugar que termine la sesión, incluso TemporizadorWorker o el botón de la
// notificación con la app cerrada. Si el usuario ya tenía "No molestar"
// puesto, la app no lo toca (ni al empezar ni al terminar).
class ModoNoMolestar(context: Context) {

    private val contexto = context.applicationContext
    private val notificaciones = contexto.getSystemService(NotificationManager::class.java)
    private val guardado = contexto.getSharedPreferences("no_molestar", Context.MODE_PRIVATE)

    // La preferencia del usuario ("Silenciar el teléfono"). La mantiene al
    // día EstudiaApp, que la lee de DataStore.
    @Volatile
    var elegido: Boolean = false

    fun accesoConcedido(): Boolean = notificaciones.isNotificationPolicyAccessGranted

    // Silencia el teléfono si corresponde y todavía no lo hizo la app.
    fun activar() {
        if (!elegido || !accesoConcedido() || guardado.contains(CLAVE_ANTERIOR)) return
        val actual = notificaciones.currentInterruptionFilter
        if (actual != NotificationManager.INTERRUPTION_FILTER_ALL) return
        guardado.edit().putInt(CLAVE_ANTERIOR, actual).apply()
        notificaciones.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    // Vuelve al modo anterior, solo si lo había cambiado la app.
    fun restaurar() {
        if (!guardado.contains(CLAVE_ANTERIOR)) return
        val anterior = guardado.getInt(CLAVE_ANTERIOR, NotificationManager.INTERRUPTION_FILTER_ALL)
        guardado.edit().remove(CLAVE_ANTERIOR).apply()
        if (accesoConcedido()) notificaciones.setInterruptionFilter(anterior)
    }

    private companion object {
        const val CLAVE_ANTERIOR = "filtro_anterior"
    }
}
