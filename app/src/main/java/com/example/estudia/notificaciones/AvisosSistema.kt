package com.example.estudia.notificaciones

import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.SesionEstudio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

// Avisos que la app le deja programados al sistema: recordatorios
// (WorkManager) y la notificación fija del temporizador.
//
// Está separado del Repository a propósito (apunte de MVVM, 5.5): el
// Repository solo guarda y entrega datos, y quien cambia algo (un
// ViewModel, un Worker o un botón de la notificación) le avisa después a
// AvisosSistema. Es una interfaz para que los tests de los ViewModels
// usen una versión falsa, igual que con el Repository.
interface AvisosSistema {

    // Los recordatorios de eventos solo se programan si el usuario los
    // tiene activados; si no, estas llamadas no hacen nada.
    suspend fun programarEvento(evento: Evento)
    suspend fun programarEventos(eventos: List<Evento>)

    // Lo llama EventoWorker después de avisar una clase semanal, para dejar
    // programado el aviso de la semana siguiente.
    suspend fun programarClaseSiguiente(evento: Evento)

    fun cancelarEvento(eventoId: Int)

    // Al activar: el resumen diario y los avisos de todos los eventos.
    // Al desactivar: se cancela todo.
    suspend fun activarRecordatorios(eventos: List<Evento>)
    fun desactivarRecordatorios()

    // Deja la notificación y el aviso de fin del temporizador de acuerdo con
    // la sesión: corriendo, en pausa o sin sesión (null = se ocultan).
    fun actualizarTemporizador(sesion: SesionEstudio?, ahora: Long = System.currentTimeMillis())
}

class AvisosSistemaImpl(
    private val programador: ProgramadorRecordatorios,
    private val notificacionTemporizador: NotificacionTemporizador,
    // La preferencia del usuario (DataStore), solo para leerla.
    private val recordatoriosActivos: Flow<Boolean>
) : AvisosSistema {

    override suspend fun programarEvento(evento: Evento) {
        if (recordatoriosActivos.first()) programador.programarEvento(evento)
    }

    override suspend fun programarEventos(eventos: List<Evento>) {
        if (recordatoriosActivos.first()) eventos.forEach { programador.programarEvento(it) }
    }

    override suspend fun programarClaseSiguiente(evento: Evento) {
        if (recordatoriosActivos.first()) programador.programarEvento(evento, desdeElAviso = true)
    }

    override fun cancelarEvento(eventoId: Int) {
        programador.cancelarEvento(eventoId)
    }

    override suspend fun activarRecordatorios(eventos: List<Evento>) {
        programador.programarDiario()
        programarEventos(eventos)
    }

    override fun desactivarRecordatorios() {
        programador.cancelarTodo()
    }

    override fun actualizarTemporizador(sesion: SesionEstudio?, ahora: Long) {
        when {
            sesion == null -> {
                programador.cancelarFinTemporizador()
                notificacionTemporizador.ocultar()
            }
            sesion.enPausa -> {
                programador.cancelarFinTemporizador()
                notificacionTemporizador.mostrar(sesion)
            }
            else -> {
                programador.programarFinTemporizador(sesion.restanteMillis(ahora))
                notificacionTemporizador.mostrar(sesion)
            }
        }
    }
}
