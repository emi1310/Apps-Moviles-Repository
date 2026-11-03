package com.example.estudia.notificaciones

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.estudia.EstudiaApp
import com.example.estudia.R
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.util.aTexto
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

// Aviso de un evento puntual. Lo programa ProgramadorRecordatorios con el id
// del evento; al ejecutarse vuelve a leer el evento de la base, porque pudo
// haberse editado o borrado desde que se programó.
//  - Exámenes y entregas: "Examen de Física · mañana a las 9:00".
//  - Clases: "Clase de Física en 15 min". Si la clase se repite, al final
//    deja programado el aviso de la semana siguiente.
class EventoWorker(
    context: Context,
    parametros: WorkerParameters
) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val eventoId = inputData.getInt(CLAVE_EVENTO_ID, -1)
        val app = applicationContext as EstudiaApp
        val repositorio = app.repositorio
        val datos = repositorio.eventoParaRecordatorio(eventoId)

        if (datos != null) {
            val contexto = applicationContext
            val evento = datos.evento
            val titulo: String
            val texto: String
            if (evento.tipo == TipoEvento.CLASE) {
                // Los minutos que faltan de verdad: WorkManager puede ejecutar el aviso
                // un poco más tarde de lo programado.
                val faltan = ((Duration.between(LocalDateTime.now(), datos.inicio).seconds + 59) / 60).coerceAtLeast(1)
                titulo = contexto.getString(R.string.notificacion_clase_titulo, datos.nombreMateria, faltan)
                texto = contexto.getString(
                    R.string.notificacion_clase_texto, evento.titulo, evento.horaInicio.aTexto(), evento.horaFin.aTexto()
                )
            } else {
                val tipo = contexto.getString(
                    if (evento.tipo == TipoEvento.EXAMEN) R.string.tipo_examen else R.string.tipo_entrega
                )
                val cuando = contexto.getString(
                    if (evento.fecha == LocalDate.now()) R.string.notificacion_evento_hoy else R.string.notificacion_evento_manana,
                    evento.horaInicio.aTexto()
                )
                titulo = contexto.getString(R.string.notificacion_evento_titulo, tipo, datos.nombreMateria)
                texto = "${evento.titulo} · $cuando"
            }
            Notificaciones.mostrar(contexto, Notificaciones.ID_BASE_EVENTOS + evento.id, titulo, texto)
        }

        // Aunque este aviso no se haya mostrado (llegó tarde), la clase de la
        // semana que viene se sigue avisando (si todavía existe).
        val evento = repositorio.buscarEvento(eventoId)
        if (evento != null && evento.tipo == TipoEvento.CLASE) app.avisos.programarClaseSiguiente(evento)
        return Result.success()
    }

    companion object {
        const val CLAVE_EVENTO_ID = "evento_id"
    }
}
