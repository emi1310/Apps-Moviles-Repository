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

// Widget de la pantalla de inicio: racha y próximas tareas. El sistema lo
// actualiza cada 30 minutos (info_widget.xml) y además EstudiaApp lo
// refresca cada vez que cambian los datos del usuario.
class WidgetEstudia : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // onUpdate corre en el hilo principal y no puede esperar a Room:
        // goAsync deja terminar el trabajo en una corrutina.
        val pendiente = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as EstudiaApp
                val usuario = app.repositorio.usuarioActual.first()
                val resumen = usuario?.let { armarResumenWidget(it, LocalDate.now()) }
                ids.forEach { manager.updateAppWidget(it, vistas(context, resumen)) }
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        private val IDS_TAREAS = intArrayOf(R.id.widget_tarea_1, R.id.widget_tarea_2, R.id.widget_tarea_3)

        // Pide al sistema que redibuje todos los widgets puestos (si no hay
        // ninguno, no hace nada).
        fun actualizarTodos(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WidgetEstudia::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, WidgetEstudia::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            )
        }

        private fun vistas(context: Context, resumen: ResumenWidget?): RemoteViews {
            val vistas = RemoteViews(context.packageName, R.layout.widget_estudia)
            // Tocar cualquier parte del widget abre la app.
            val abrir = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            vistas.setOnClickPendingIntent(R.id.widget_raiz, abrir)

            if (resumen == null) {
                vistas.setViewVisibility(R.id.widget_racha, View.GONE)
                vistas.setTextViewText(R.id.widget_subtitulo, context.getString(R.string.widget_sin_sesion))
                IDS_TAREAS.forEach { vistas.setViewVisibility(it, View.GONE) }
                return vistas
            }
            vistas.setViewVisibility(R.id.widget_racha, View.VISIBLE)
            vistas.setTextViewText(R.id.widget_racha, context.getString(R.string.widget_racha, resumen.racha))
            vistas.setTextViewText(
                R.id.widget_subtitulo,
                if (resumen.pendientes == 0) context.getString(R.string.widget_al_dia)
                else context.resources.getQuantityString(R.plurals.widget_pendientes, resumen.pendientes, resumen.pendientes)
            )
            IDS_TAREAS.forEachIndexed { i, id ->
                val texto = resumen.proximas.getOrNull(i)
                vistas.setViewVisibility(id, if (texto == null) View.GONE else View.VISIBLE)
                if (texto != null) vistas.setTextViewText(id, "• $texto")
            }
            return vistas
        }
    }
}
