package com.example.estudia.notificaciones

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.estudia.EstudiaApp
import com.example.estudia.R
import kotlinx.coroutines.flow.first

// Se ejecuta cuando el temporizador de estudio llega a cero (lo programa
// ProgramadorRecordatorios). Guarda la sesión como bloque de estudio y
// avisa con una notificación. Si la pantalla del temporizador ya la guardó
// (porque estaba abierta), no hace nada: completarSesionSiTermino devuelve null.
class TemporizadorWorker(
    context: Context,
    parametros: WorkerParameters
) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val app = applicationContext as EstudiaApp
        val sesion = app.repositorio.completarSesionSiTermino()
        if (sesion == null) {
            // WorkManager puede ejecutarlo unos segundos antes de hora: si la
            // sesión sigue corriendo, se vuelve a programar para lo que falta.
            val enCurso = app.repositorio.sesionEstudio.first()
            if (enCurso != null && !enCurso.enPausa) app.avisos.actualizarTemporizador(enCurso)
            return Result.success()
        }
        Notificaciones.mostrar(
            applicationContext,
            Notificaciones.ID_TEMPORIZADOR,
            applicationContext.getString(R.string.notificacion_temporizador_titulo),
            applicationContext.getString(R.string.notificacion_temporizador_texto, sesion.minutos, sesion.nombreMateria)
        )
        // Al final: oculta la notificación fija y cancela este mismo trabajo.
        app.avisos.actualizarTemporizador(null)
        return Result.success()
    }
}
