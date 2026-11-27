package com.example.estudia.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.estudia.EstudiaApp
import com.example.estudia.ui.navegacion.AccionExterna
import com.example.estudia.ui.navegacion.intentDeAccion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Base de los widgets de Estudia+. El sistema los actualiza cada 30 minutos
// (info_widget_*.xml) y además EstudiaApp los refresca cada vez que cambian
// los datos del usuario o la sesión del temporizador.
abstract class WidgetDeEstudia : AppWidgetProvider() {

    // Arma lo que muestra el widget. Corre en segundo plano (puede leer Room).
    abstract suspend fun vistas(context: Context, app: EstudiaApp): RemoteViews

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // onUpdate corre en el hilo principal y no puede esperar a Room:
        // goAsync deja terminar el trabajo en una corrutina.
        val pendiente = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val vistas = vistas(context, context.applicationContext as EstudiaApp)
                ids.forEach { manager.updateAppWidget(it, vistas) }
            } finally {
                pendiente.finish()
            }
        }
    }
}

private val TODOS = listOf(
    WidgetEstudia::class.java, WidgetHoy::class.java, WidgetPanel::class.java,
    WidgetTemporizador::class.java, WidgetExamen::class.java
)

// Pide al sistema que redibuje los widgets de esa clase que estén puestos.
fun actualizarWidget(context: Context, clase: Class<out AppWidgetProvider>) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(ComponentName(context, clase))
    if (ids.isEmpty()) return
    context.sendBroadcast(
        Intent(context, clase)
            .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
    )
}

fun actualizarTodosLosWidgets(context: Context) = TODOS.forEach { actualizarWidget(context, it) }

// Abre la app y hace la acción (crear una nota, ir al horario...).
fun abrirEnLaApp(context: Context, accion: AccionExterna): PendingIntent =
    PendingIntent.getActivity(
        context, 100 + accion.ordinal, intentDeAccion(context, accion),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

// Un aviso a un receptor de la app (completar una tarea, pausar...).
fun avisoAReceptor(context: Context, codigo: Int, intent: Intent): PendingIntent =
    PendingIntent.getBroadcast(context, codigo, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
