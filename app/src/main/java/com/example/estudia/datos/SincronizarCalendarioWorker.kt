package com.example.estudia.datos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.estudia.EstudiaApp
import com.example.estudia.R
import java.util.concurrent.TimeUnit

// Cada una hora (con la app cerrada también) sincroniza con el calendario
// del teléfono, para traer a Estudia+ lo que se cambió en Google Calendar.
// Lo que cambia en la app ya se pasa enseguida (EstudiaApp).
class SincronizarCalendarioWorker(context: Context, parametros: WorkerParameters) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val app = applicationContext as EstudiaApp
        val permiso = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            .all { ContextCompat.checkSelfPermission(app, it) == PackageManager.PERMISSION_GRANTED }
        if (!permiso) return Result.success()
        val resultado = app.repositorio.sincronizarCalendario(app.getString(R.string.calendario_estudia)) ?: return Result.success()
        if (resultado.cambiados.isNotEmpty()) app.avisos.programarEventos(resultado.cambiados)
        return Result.success()
    }

    companion object {
        private const val TRABAJO = "sincronizar_calendario"

        fun programar(context: Context, activa: Boolean) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            if (!activa) {
                workManager.cancelUniqueWork(TRABAJO)
                return
            }
            val cadaHora = PeriodicWorkRequestBuilder<SincronizarCalendarioWorker>(1, TimeUnit.HOURS).build()
            workManager.enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, cadaHora)
        }
    }
}
