package com.example.estudia.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.estudia.EstudiaApp
import com.example.estudia.R
import com.example.estudia.ui.navegacion.AccionExterna
import kotlinx.coroutines.flow.first
import java.time.LocalDate

// Widget "Tareas": la racha y las próximas tareas. El círculo de cada tarea la
// completa ahí mismo (WidgetAccionesReceiver), sin abrir la app; tocar el
// texto abre Materias y el + crea una nota.
class WidgetEstudia : WidgetDeEstudia() {

    override suspend fun vistas(context: Context, app: EstudiaApp): RemoteViews {
        val resumen = app.repositorio.usuarioActual.first()?.let { armarResumenWidget(it, LocalDate.now()) }
        val vistas = RemoteViews(context.packageName, R.layout.widget_estudia)
        vistas.setOnClickPendingIntent(R.id.widget_raiz, abrirEnLaApp(context, AccionExterna.MATERIAS))
        vistas.setOnClickPendingIntent(R.id.widget_agregar, abrirEnLaApp(context, AccionExterna.NUEVA_NOTA))

        if (resumen == null) {
            vistas.setViewVisibility(R.id.widget_racha, View.GONE)
            vistas.setTextViewText(R.id.widget_subtitulo, context.getString(R.string.widget_sin_sesion))
            FILAS.forEach { vistas.setViewVisibility(it, View.GONE) }
            return vistas
        }
        vistas.setViewVisibility(R.id.widget_racha, View.VISIBLE)
        vistas.setTextViewText(R.id.widget_racha, context.getString(R.string.widget_racha, resumen.racha))
        vistas.setTextViewText(
            R.id.widget_subtitulo,
            if (resumen.pendientes == 0) context.getString(R.string.widget_al_dia)
            else context.resources.getQuantityString(R.plurals.widget_pendientes, resumen.pendientes, resumen.pendientes)
        )
        FILAS.forEachIndexed { i, fila ->
            val tarea = resumen.tareas.getOrNull(i)
            vistas.setViewVisibility(fila, if (tarea == null) View.GONE else View.VISIBLE)
            if (tarea != null) {
                vistas.setTextViewText(TEXTOS[i], tarea.texto)
                vistas.setOnClickPendingIntent(fila, abrirEnLaApp(context, AccionExterna.MATERIAS))
                val completar = Intent(context, WidgetAccionesReceiver::class.java)
                    .setAction(WidgetAccionesReceiver.ACCION_COMPLETAR_TAREA)
                    .putExtra(WidgetAccionesReceiver.EXTRA_TAREA, tarea.tareaId)
                    .putExtra(WidgetAccionesReceiver.EXTRA_MATERIA, tarea.materiaId)
                vistas.setOnClickPendingIntent(CHECKS[i], avisoAReceptor(context, 10_000 + tarea.tareaId, completar))
            }
        }
        return vistas
    }

    companion object {
        private val FILAS = intArrayOf(R.id.widget_fila_1, R.id.widget_fila_2, R.id.widget_fila_3)
        private val TEXTOS = intArrayOf(R.id.widget_tarea_1, R.id.widget_tarea_2, R.id.widget_tarea_3)
        private val CHECKS = intArrayOf(R.id.widget_check_1, R.id.widget_check_2, R.id.widget_check_3)
    }
}
