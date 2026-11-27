package com.example.estudia.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.estudia.EstudiaApp
import com.example.estudia.MainActivity
import com.example.estudia.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Widget "Hoy" de la pantalla de inicio: las clases, exámenes, entregas y
// bloques de estudio de hoy, por hora, y las tareas que vencen hoy. Como el
// otro widget, el sistema lo actualiza cada 30 minutos y la app cada vez que
// cambian los datos.
class WidgetHoy : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pendiente = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as EstudiaApp
                val hoy = LocalDate.now()
                val resumen = app.repositorio.usuarioActual.first()?.let { armarResumenHoy(it, hoy) }
                ids.forEach { manager.updateAppWidget(it, vistas(context, resumen, hoy)) }
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        private val IDS_LINEAS = intArrayOf(R.id.hoy_linea_1, R.id.hoy_linea_2, R.id.hoy_linea_3, R.id.hoy_linea_4, R.id.hoy_linea_5)

        fun actualizarTodos(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WidgetHoy::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, WidgetHoy::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            )
        }

        private fun vistas(context: Context, resumen: ResumenHoy?, hoy: LocalDate): RemoteViews {
            val vistas = RemoteViews(context.packageName, R.layout.widget_hoy)
            val abrir = PendingIntent.getActivity(
                context, 1,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            vistas.setOnClickPendingIntent(R.id.hoy_raiz, abrir)
            vistas.setTextViewText(R.id.hoy_fecha, hoy.format(DateTimeFormatter.ofPattern("EEEE d/M", Locale.forLanguageTag("es-AR"))))

            // Sin sesión o sin nada para hoy: un solo mensaje en la primera línea.
            val lineas = when {
                resumen == null -> listOf(context.getString(R.string.widget_sin_sesion))
                resumen.lineas.isEmpty() -> listOf(context.getString(R.string.widget_hoy_libre))
                else -> resumen.lineas
            }
            IDS_LINEAS.forEachIndexed { i, id ->
                val texto = lineas.getOrNull(i)
                vistas.setViewVisibility(id, if (texto == null) View.GONE else View.VISIBLE)
                if (texto != null) vistas.setTextViewText(id, texto)
            }
            val mas = resumen?.mas ?: 0
            vistas.setViewVisibility(R.id.hoy_mas, if (mas > 0) View.VISIBLE else View.GONE)
            if (mas > 0) vistas.setTextViewText(R.id.hoy_mas, context.getString(R.string.widget_hoy_mas, mas))
            return vistas
        }
    }
}
