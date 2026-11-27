package com.example.estudia.widget

import android.content.Context
import android.widget.RemoteViews
import com.example.estudia.EstudiaApp
import com.example.estudia.R
import com.example.estudia.ui.navegacion.AccionExterna
import kotlinx.coroutines.flow.first
import java.time.LocalDate

// Widget "Panel": un tablero con la racha, lo próximo que hay y ocho botones
// que abren la app directo en una función.
class WidgetPanel : WidgetDeEstudia() {

    override suspend fun vistas(context: Context, app: EstudiaApp): RemoteViews {
        val hoy = LocalDate.now()
        val usuario = app.repositorio.usuarioActual.first()
        val vistas = RemoteViews(context.packageName, R.layout.widget_panel)
        BOTONES.forEach { (id, accion) -> vistas.setOnClickPendingIntent(id, abrirEnLaApp(context, accion)) }

        if (usuario == null) {
            vistas.setTextViewText(R.id.panel_racha, "")
            vistas.setTextViewText(R.id.panel_proximo, context.getString(R.string.widget_sin_sesion))
            return vistas
        }
        vistas.setTextViewText(R.id.panel_racha, context.getString(R.string.widget_racha, usuario.rachaDeEstudio(hoy)))
        // Lo próximo: lo primero de hoy; si no hay nada, el próximo examen; si
        // tampoco, cuántas tareas quedan.
        val deHoy = armarResumenHoy(usuario, hoy).lineas.firstOrNull()
        val examen = proximoExamen(usuario, hoy)
        val pendientes = armarResumenWidget(usuario, hoy).pendientes
        val proximo = when {
            deHoy != null -> context.getString(R.string.widget_panel_hoy, deHoy)
            examen != null && examen.dias == 0L -> context.getString(R.string.widget_panel_examen_hoy, examen.titulo)
            examen != null -> context.resources.getQuantityString(R.plurals.widget_panel_examen, examen.dias.toInt(), examen.titulo, examen.dias.toInt())
            pendientes > 0 -> context.resources.getQuantityString(R.plurals.widget_pendientes, pendientes, pendientes)
            else -> context.getString(R.string.widget_al_dia)
        }
        vistas.setTextViewText(R.id.panel_proximo, proximo)
        return vistas
    }

    companion object {
        private val BOTONES = listOf(
            R.id.panel_nota to AccionExterna.NUEVA_NOTA,
            R.id.panel_evento to AccionExterna.NUEVO_EVENTO,
            R.id.panel_materia to AccionExterna.NUEVA_MATERIA,
            R.id.panel_estudiar to AccionExterna.TEMPORIZADOR,
            R.id.panel_ia to AccionExterna.ASISTENTE,
            R.id.panel_horario to AccionExterna.HORARIO,
            R.id.panel_calendario to AccionExterna.CALENDARIO,
            R.id.panel_buscar to AccionExterna.BUSCAR
        )
    }
}
