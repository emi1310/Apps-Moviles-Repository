package com.example.estudia.widget

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.example.estudia.EstudiaApp
import com.example.estudia.R
import com.example.estudia.ui.navegacion.AccionExterna
import kotlinx.coroutines.flow.first
import java.time.LocalDate

// Widget "Próximo examen": cuántos días faltan, cuál es y cuánto del temario
// está estudiado. Tocarlo abre el Calendario.
class WidgetExamen : WidgetDeEstudia() {

    override suspend fun vistas(context: Context, app: EstudiaApp): RemoteViews {
        val vistas = RemoteViews(context.packageName, R.layout.widget_examen)
        vistas.setOnClickPendingIntent(R.id.examen_raiz, abrirEnLaApp(context, AccionExterna.CALENDARIO))
        val usuario = app.repositorio.usuarioActual.first()
        val examen = usuario?.let { proximoExamen(it, LocalDate.now()) }

        if (examen == null) {
            vistas.setTextViewText(R.id.examen_dias, "🎉")
            vistas.setTextViewText(R.id.examen_dias_texto, "")
            vistas.setTextViewText(
                R.id.examen_nombre,
                context.getString(if (usuario == null) R.string.widget_sin_sesion else R.string.widget_examen_ninguno)
            )
            vistas.setViewVisibility(R.id.examen_materia, View.GONE)
            vistas.setViewVisibility(R.id.examen_avance, View.GONE)
            vistas.setViewVisibility(R.id.examen_temas, View.GONE)
            return vistas
        }
        val dias = examen.dias.toInt()
        vistas.setTextViewText(R.id.examen_dias, if (dias == 0) context.getString(R.string.widget_examen_hoy) else dias.toString())
        vistas.setTextViewText(R.id.examen_dias_texto, if (dias == 0) "" else context.resources.getQuantityString(R.plurals.widget_examen_dias, dias))
        vistas.setTextViewText(R.id.examen_nombre, examen.titulo)
        vistas.setViewVisibility(R.id.examen_materia, View.VISIBLE)
        vistas.setTextViewText(R.id.examen_materia, examen.materia)
        val conTemario = examen.temasTotal > 0
        vistas.setViewVisibility(R.id.examen_avance, if (conTemario) View.VISIBLE else View.GONE)
        vistas.setViewVisibility(R.id.examen_temas, View.VISIBLE)
        if (conTemario) {
            vistas.setProgressBar(R.id.examen_avance, 100, examen.temasEstudiados * 100 / examen.temasTotal, false)
            vistas.setTextViewText(R.id.examen_temas, context.getString(R.string.widget_examen_temas, examen.temasEstudiados, examen.temasTotal))
        } else {
            vistas.setTextViewText(R.id.examen_temas, context.getString(R.string.widget_examen_sin_temario))
        }
        return vistas
    }
}
