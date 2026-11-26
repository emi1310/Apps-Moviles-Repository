package com.example.estudia.notificaciones

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.estudia.EstudiaApp
import com.example.estudia.R
import com.example.estudia.util.aTexto
import com.example.estudia.modelo.TipoAvisoFinal
import com.example.estudia.modelo.avisosDeFinales
import kotlinx.coroutines.flow.first
import java.time.LocalDate

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
        // Los finales (cierre de inscripción y mesas) van en un aviso aparte.
        avisarFinales(repositorio)
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

// Aviso de los finales de hoy: inscripciones que cierran en 2 días o
// menos y mesas de hoy o mañana (ver avisosDeFinales). Solo con los
// recordatorios activados.
private suspend fun CoroutineWorker.avisarFinales(repositorio: com.example.estudia.datos.RepositorioEstudia) {
    if (!repositorio.recordatoriosActivos.first()) return
    val usuario = repositorio.usuarioActual.first() ?: return
    val hoy = LocalDate.now()
    val avisos = avisosDeFinales(usuario.finales, hoy)
    if (avisos.isEmpty()) return
    val contexto = applicationContext
    val lineas = avisos.map { aviso ->
        val f = aviso.final
        when (aviso.tipo) {
            TipoAvisoFinal.MESA_HOY -> contexto.getString(R.string.notificacion_final_hoy, f.materia)
            TipoAvisoFinal.MESA_MANANA -> contexto.getString(R.string.notificacion_final_manana, f.materia)
            TipoAvisoFinal.CIERRA_INSCRIPCION -> contexto.getString(
                R.string.notificacion_final_inscripcion, f.materia, f.cierreInscripcion?.aTexto() ?: ""
            )
        }
    }
    Notificaciones.mostrar(contexto, Notificaciones.ID_FINALES, contexto.getString(R.string.notificacion_finales_titulo), lineas.joinToString("\n"))
}
