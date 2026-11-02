package com.example.estudia.notificaciones

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.estudia.EstudiaApp
import com.example.estudia.R
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.util.aTexto
import java.time.LocalDate

// Aviso de un examen o entrega puntual. Lo programa ProgramadorRecordatorios
// con el id del evento; al ejecutarse vuelve a leer el evento de la base,
// porque pudo haberse editado o borrado desde que se programó.
class EventoWorker(
    context: Context,
    parametros: WorkerParameters
) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val eventoId = inputData.getInt(CLAVE_EVENTO_ID, -1)
        val repositorio = (applicationContext as EstudiaApp).repositorio
        val datos = repositorio.eventoParaRecordatorio(eventoId) ?: return Result.success()

        val contexto = applicationContext
        val evento = datos.evento
        val tipo = contexto.getString(
            if (evento.tipo == TipoEvento.EXAMEN) R.string.tipo_examen else R.string.tipo_entrega
        )
        val cuando = contexto.getString(
            if (evento.fecha == LocalDate.now()) R.string.notificacion_evento_hoy else R.string.notificacion_evento_manana,
            evento.horaInicio.aTexto()
        )
        Notificaciones.mostrar(
            contexto,
            Notificaciones.ID_BASE_EVENTOS + evento.id,
            contexto.getString(R.string.notificacion_evento_titulo, tipo, datos.nombreMateria),
            "${evento.titulo} · $cuando"
        )
        return Result.success()
    }

    companion object {
        const val CLAVE_EVENTO_ID = "evento_id"
    }
}
