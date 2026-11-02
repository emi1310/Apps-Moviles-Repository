package com.example.estudia.notificaciones

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.estudia.EstudiaApp
import com.example.estudia.R
import com.example.estudia.util.aTexto

// Trabajo en segundo plano (WorkManager) que se ejecuta una vez por día:
// pide al Repository el resumen del día y, si hay algo para recordar,
// muestra una notificación. WorkManager lo sigue ejecutando aunque la app
// esté cerrada o el teléfono se reinicie.
class RecordatorioWorker(
    context: Context,
    parametros: WorkerParameters
) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val repositorio = (applicationContext as EstudiaApp).repositorio
        // Si no hay nada pendiente no se notifica (no molestar al usuario).
        val resumen = repositorio.resumenDelDia() ?: return Result.success()

        val contexto = applicationContext
        val lineas = mutableListOf<String>()
        if (resumen.tareasPendientes > 0) {
            lineas.add(
                contexto.resources.getQuantityString(
                    R.plurals.notificacion_tareas_pendientes, resumen.tareasPendientes, resumen.tareasPendientes
                )
            )
        }
        if (resumen.eventosHoy.isNotEmpty()) {
            lineas.add(contexto.getString(R.string.notificacion_hoy, resumen.eventosHoy.joinToString { "${it.titulo} (${it.horaInicio.aTexto()})" }))
        }
        if (resumen.eventosManana.isNotEmpty()) {
            lineas.add(contexto.getString(R.string.notificacion_manana, resumen.eventosManana.joinToString { "${it.titulo} (${it.horaInicio.aTexto()})" }))
        }
        Notificaciones.mostrar(
            contexto,
            Notificaciones.ID_RESUMEN_DIARIO,
            contexto.getString(R.string.notificacion_resumen_titulo),
            lineas.joinToString("\n")
        )
        return Result.success()
    }
}
