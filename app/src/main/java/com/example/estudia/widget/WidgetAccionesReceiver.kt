package com.example.estudia.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.estudia.EstudiaApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Lo que se hace desde un widget sin abrir la app: por ahora, completar una
// tarea (el círculo del widget "Tareas"). Usa el mismo Repository que la app.
// No es "exported": solo lo llaman los widgets de la propia app.
class WidgetAccionesReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACCION_COMPLETAR_TAREA) return
        val tareaId = intent.getIntExtra(EXTRA_TAREA, -1)
        val materiaId = intent.getIntExtra(EXTRA_MATERIA, -1)
        val app = context.applicationContext as EstudiaApp
        val pendiente = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val tarea = app.repositorio.usuarioActual.first()
                    ?.materias?.firstOrNull { it.id == materiaId }
                    ?.tareas?.firstOrNull { it.id == tareaId }
                if (tarea != null && !tarea.completada) {
                    app.repositorio.actualizarTarea(materiaId, tarea.copy(completada = true))
                    app.avisos.cancelarTarea(tarea.id)
                }
                // Por si la app no está abierta para enterarse del cambio.
                actualizarTodosLosWidgets(context)
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        const val ACCION_COMPLETAR_TAREA = "com.example.estudia.widget.COMPLETAR_TAREA"
        const val EXTRA_TAREA = "tarea"
        const val EXTRA_MATERIA = "materia"
    }
}
