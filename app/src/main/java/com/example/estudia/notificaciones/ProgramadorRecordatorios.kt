package com.example.estudia.notificaciones

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.estudia.modelo.Evento
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

// Programa (o cancela) los recordatorios con WorkManager:
//  - Un resumen diario a las 8:00 (RecordatorioWorker).
//  - Un aviso por cada examen o entrega: la noche anterior a las 20:00, o
//    2 horas antes si ya pasó ese momento (EventoWorker).
//  - El fin del temporizador de estudio (TemporizadorWorker).
class ProgramadorRecordatorios(context: Context) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    // Además del diario, se ejecuta una vez ahora para que el usuario vea
    // enseguida cómo es el aviso.
    fun programarDiario() {
        val diario = PeriodicWorkRequestBuilder<RecordatorioWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(milisHasta(proximaHora(LocalTime.of(8, 0))), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(TRABAJO_DIARIO, ExistingPeriodicWorkPolicy.UPDATE, diario)
        workManager.enqueue(OneTimeWorkRequestBuilder<RecordatorioWorker>().build())
    }

    fun programarEvento(evento: Evento, ahora: LocalDateTime = LocalDateTime.now()) {
        if (!evento.llevaRecordatorio) {
            cancelarEvento(evento.id)
            return
        }
        val nocheAnterior = evento.fecha.minusDays(1).atTime(20, 0)
        val momento = when {
            nocheAnterior.isAfter(ahora) -> nocheAnterior
            evento.inicio.minusHours(2).isAfter(ahora) -> evento.inicio.minusHours(2)
            else -> null
        }
        if (momento == null) {
            cancelarEvento(evento.id)
            return
        }
        val trabajo = OneTimeWorkRequestBuilder<EventoWorker>()
            .setInitialDelay(Duration.between(ahora, momento).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(EventoWorker.CLAVE_EVENTO_ID to evento.id))
            .addTag(ETIQUETA_EVENTOS)
            .build()
        // REPLACE: si el evento se edita, se reemplaza el aviso anterior.
        workManager.enqueueUniqueWork(nombreTrabajoEvento(evento.id), ExistingWorkPolicy.REPLACE, trabajo)
    }

    fun cancelarEvento(eventoId: Int) {
        workManager.cancelUniqueWork(nombreTrabajoEvento(eventoId))
    }

    fun cancelarTodo() {
        workManager.cancelUniqueWork(TRABAJO_DIARIO)
        workManager.cancelAllWorkByTag(ETIQUETA_EVENTOS)
    }

    // Fin del temporizador de estudio: aunque la app esté cerrada, al llegar
    // a cero TemporizadorWorker guarda la sesión y avisa con una notificación.
    // REPLACE: al pausar y reanudar se reemplaza el aviso por uno nuevo.
    fun programarFinTemporizador(retrasoMillis: Long) {
        val trabajo = OneTimeWorkRequestBuilder<TemporizadorWorker>()
            .setInitialDelay(retrasoMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(TRABAJO_TEMPORIZADOR, ExistingWorkPolicy.REPLACE, trabajo)
    }

    fun cancelarFinTemporizador() {
        workManager.cancelUniqueWork(TRABAJO_TEMPORIZADOR)
    }

    private fun proximaHora(hora: LocalTime): LocalDateTime {
        val hoy = LocalDateTime.now().toLocalDate().atTime(hora)
        return if (hoy.isAfter(LocalDateTime.now())) hoy else hoy.plusDays(1)
    }

    private fun milisHasta(momento: LocalDateTime): Long = Duration.between(LocalDateTime.now(), momento).toMillis()

    private fun nombreTrabajoEvento(id: Int) = "recordatorio_evento_$id"

    companion object {
        private const val TRABAJO_DIARIO = "recordatorio_diario"
        private const val ETIQUETA_EVENTOS = "recordatorio_evento"
        private const val TRABAJO_TEMPORIZADOR = "fin_temporizador"
    }
}
