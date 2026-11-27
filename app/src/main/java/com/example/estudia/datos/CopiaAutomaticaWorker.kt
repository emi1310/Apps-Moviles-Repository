package com.example.estudia.datos

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.estudia.EstudiaApp
import java.util.concurrent.TimeUnit

// Copia de seguridad automática: una vez por semana guarda la copia en la
// carpeta que eligió el usuario (con la batería no baja). Si falla (por
// ejemplo, se borró la carpeta), lo vuelve a intentar más tarde.
class CopiaAutomaticaWorker(context: Context, parametros: WorkerParameters) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val app = applicationContext as EstudiaApp
        return if (app.repositorio.hacerCopiaAutomatica()) Result.success() else Result.retry()
    }

    companion object {
        private const val TRABAJO = "copia_automatica_semanal"

        // EstudiaApp lo llama cada vez que se prende o se apaga.
        fun programar(context: Context, activa: Boolean) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            if (!activa) {
                workManager.cancelUniqueWork(TRABAJO)
                return
            }
            val semanal = PeriodicWorkRequestBuilder<CopiaAutomaticaWorker>(7, TimeUnit.DAYS)
                // La primera ya se hizo al activarla.
                .setInitialDelay(7, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            workManager.enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, semanal)
        }
    }
}
