package com.example.estudia.widget

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.example.estudia.EstudiaApp
import com.example.estudia.R
import com.example.estudia.ui.navegacion.AccionExterna
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Widget "Hoy": las clases, exámenes, entregas y bloques de estudio de hoy,
// por hora, y las tareas que vencen hoy. Tocarlo abre el Calendario.
class WidgetHoy : WidgetDeEstudia() {

    override suspend fun vistas(context: Context, app: EstudiaApp): RemoteViews {
        val hoy = LocalDate.now()
        val resumen = app.repositorio.usuarioActual.first()?.let { armarResumenHoy(it, hoy) }
        val vistas = RemoteViews(context.packageName, R.layout.widget_hoy)
        vistas.setOnClickPendingIntent(R.id.hoy_raiz, abrirEnLaApp(context, AccionExterna.CALENDARIO))
        vistas.setTextViewText(R.id.hoy_fecha, hoy.format(DateTimeFormatter.ofPattern("EEEE d/M", Locale.forLanguageTag("es-AR"))))

        // Sin sesión o sin nada para hoy: un solo mensaje en la primera línea.
        val lineas = when {
            resumen == null -> listOf(context.getString(R.string.widget_sin_sesion))
            resumen.lineas.isEmpty() -> listOf(context.getString(R.string.widget_hoy_libre))
            else -> resumen.lineas
        }
        LINEAS.forEachIndexed { i, id ->
            val texto = lineas.getOrNull(i)
            vistas.setViewVisibility(id, if (texto == null) View.GONE else View.VISIBLE)
            if (texto != null) vistas.setTextViewText(id, texto)
        }
        val mas = resumen?.mas ?: 0
        vistas.setViewVisibility(R.id.hoy_mas, if (mas > 0) View.VISIBLE else View.GONE)
        if (mas > 0) vistas.setTextViewText(R.id.hoy_mas, context.getString(R.string.widget_hoy_mas, mas))
        return vistas
    }

    companion object {
        private val LINEAS = intArrayOf(R.id.hoy_linea_1, R.id.hoy_linea_2, R.id.hoy_linea_3, R.id.hoy_linea_4, R.id.hoy_linea_5)
    }
}
