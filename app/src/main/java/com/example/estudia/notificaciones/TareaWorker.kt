package com.example.estudia.notificaciones

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.estudia.EstudiaApp
import com.example.estudia.R
import com.example.estudia.viewmodel.armarResumenSemanal
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Aviso de una tarea con fecha límite ("TP 2 de Física · vence mañana").
// Lo programa ProgramadorRecordatorios con el id de la tarea; al ejecutarse
// vuelve a leerla de la base: si se borró o ya está hecha, no avisa.
class TareaWorker(
    context: Context,
    parametros: WorkerParameters
) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val tareaId = inputData.getInt(CLAVE_TAREA_ID, -1)
        val datos = (applicationContext as EstudiaApp).repositorio.tareaParaRecordatorio(tareaId) ?: return Result.success()
        val fecha = datos.tarea.fechaLimite ?: return Result.success()
        val contexto = applicationContext
        val dias = ChronoUnit.DAYS.between(LocalDate.now(), fecha).toInt()
        val cuando = when {
            dias <= 0 -> contexto.getString(R.string.notificacion_tarea_hoy)
            dias == 1 -> contexto.getString(R.string.notificacion_tarea_manana)
            else -> contexto.resources.getQuantityString(R.plurals.notificacion_tarea_dias, dias, dias)
        }
        Notificaciones.mostrar(
            contexto,
            Notificaciones.ID_BASE_TAREAS + tareaId,
            contexto.getString(R.string.notificacion_tarea_titulo, datos.nombreMateria),
            "${datos.tarea.titulo} · $cuando"
        )
        return Result.success()
    }

    companion object {
        const val CLAVE_TAREA_ID = "tarea_id"
    }
}

// Se ejecuta todos los días a las 20:00: si el usuario tiene una racha y
// todavía no estudió hoy, le avisa para que no la pierda.
class RachaWorker(
    context: Context,
    parametros: WorkerParameters
) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val repositorio = (applicationContext as EstudiaApp).repositorio
        // Los domingos, además, el resumen de la semana.
        if (LocalDate.now().dayOfWeek == DayOfWeek.SUNDAY) avisarSemana(repositorio)
        val dias = repositorio.rachaEnRiesgo() ?: return Result.success()
        val contexto = applicationContext
        Notificaciones.mostrar(
            contexto,
            Notificaciones.ID_RACHA,
            contexto.getString(R.string.notificacion_racha_titulo),
            contexto.resources.getQuantityString(R.plurals.notificacion_racha_texto, dias, dias)
        )
        return Result.success()
    }
}

// Resumen de la semana (domingos a las 20:00): horas estudiadas y la
// comparación con la semana anterior. Solo con los recordatorios activados.
private suspend fun CoroutineWorker.avisarSemana(repositorio: com.example.estudia.datos.RepositorioEstudia) {
    if (!repositorio.recordatoriosActivos.first()) return
    val usuario = repositorio.usuarioActual.first() ?: return
    val resumen = armarResumenSemanal(usuario, LocalDate.now())
    val contexto = applicationContext
    val horas = String.format(java.util.Locale.getDefault(), "%.1f", resumen.minutos / 60.0)
    val variacion = resumen.variacion
    val texto = buildString {
        append(contexto.getString(R.string.notificacion_semana_texto, horas, resumen.diasEstudiados))
        if (variacion != null) {
            append(" ")
            append(
                if (variacion >= 0) contexto.getString(R.string.notificacion_semana_mas, variacion)
                else contexto.getString(R.string.notificacion_semana_menos, -variacion)
            )
        }
    }
    Notificaciones.mostrar(contexto, Notificaciones.ID_RESUMEN_SEMANAL, contexto.getString(R.string.notificacion_semana_titulo), texto)
}
