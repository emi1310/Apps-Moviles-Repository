package com.example.estudia.notificaciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.estudia.MainActivity
import com.example.estudia.R

// Todo lo relacionado con mostrar notificaciones.
//
// Desde Android 13 (API 33) mostrar notificaciones requiere el permiso
// POST_NOTIFICATIONS, que se pide en tiempo de ejecución (ver PerfilScreen).
// En versiones anteriores están permitidas por defecto, pero el usuario
// igual puede apagarlas desde Ajustes: por eso siempre se revisa con
// areNotificationsEnabled() antes de mostrar una.
object Notificaciones {

    const val CANAL_RECORDATORIOS = "recordatorios"
    // Canal aparte para la cuenta regresiva: importancia baja (sin sonido ni
    // vibración), así el usuario puede silenciarlo sin perder los recordatorios.
    const val CANAL_TEMPORIZADOR = "temporizador"
    const val ID_RESUMEN_DIARIO = 1
    // Cada evento usa su propio id (1000 + id del evento), así los avisos
    // de dos exámenes distintos no se pisan entre sí.
    const val ID_BASE_EVENTOS = 1000
    const val ID_TEMPORIZADOR = 2
    const val ID_TEMPORIZADOR_EN_CURSO = 3

    // Desde Android 8 cada notificación pertenece a un canal, que el usuario
    // puede configurar por separado en Ajustes. Crearlos varias veces no pasa nada.
    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_RECORDATORIOS,
                context.getString(R.string.canal_recordatorios_nombre),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.canal_recordatorios_descripcion)
            }
            val canalTemporizador = NotificationChannel(
                CANAL_TEMPORIZADOR,
                context.getString(R.string.canal_temporizador_nombre),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.canal_temporizador_descripcion)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannels(listOf(canal, canalTemporizador))
        }
    }

    // true si la app puede mostrar notificaciones ahora mismo.
    fun permitidas(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun mostrar(context: Context, id: Int, titulo: String, texto: String) {
        if (!permitidas(context)) return

        // Al tocar la notificación se abre la app.
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val alTocar = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificacion = NotificationCompat.Builder(context, CANAL_RECORDATORIOS)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(alTocar)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notificacion)
        } catch (e: SecurityException) {
            // El usuario revocó el permiso justo ahora: no se muestra nada.
        }
    }
}
