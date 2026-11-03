package com.example.estudia.notificaciones

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.estudia.EstudiaApp
import com.example.estudia.R

// Se ejecuta cuando el temporizador de estudio llega a cero (lo programa
// ProgramadorRecordatorios). Guarda la sesión como bloque de estudio y
// avisa con una notificación. Si la pantalla del temporizador ya la guardó
// (porque estaba abierta), no hace nada: completarSesionSiTermino devuelve null.
class TemporizadorWorker(
    context: Context,
    parametros: WorkerParameters
) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val repositorio = (applicationContext as EstudiaApp).repositorio
        val sesion = repositorio.completarSesionSiTermino() ?: return Result.success()
        Notificaciones.mostrar(
            applicationContext,
            Notificaciones.ID_TEMPORIZADOR,
            applicationContext.getString(R.string.notificacion_temporizador_titulo),
            applicationContext.getString(R.string.notificacion_temporizador_texto, sesion.minutos, sesion.nombreMateria)
        )
        return Result.success()
    }
}
