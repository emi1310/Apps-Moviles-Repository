package com.example.estudia.notificaciones

import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.SesionEstudio
import com.example.estudia.modelo.Tarea
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

    // Aviso de una tarea (versión 7): solo con los recordatorios activados.
    suspend fun programarTarea(tarea: Tarea)
    fun cancelarTarea(tareaId: Int)

    // Al activar: el resumen diario, el aviso de racha y los avisos de todos
    // los eventos y tareas. Al desactivar: se cancela todo.
    suspend fun activarRecordatorios(eventos: List<Evento>, tareas: List<Tarea> = emptyList())
    fun desactivarRecordatorios()

    // Deja la notificación y el aviso de fin del temporizador de acuerdo con
    // la sesión: corriendo, en pausa o sin sesión (null = se ocultan).
    // También activa "No molestar" mientras la sesión corre (si el usuario
    // lo eligió) y lo apaga al pausar o terminar.
    fun actualizarTemporizador(sesion: SesionEstudio?, ahora: Long = System.currentTimeMillis())

    // El usuario prendió o apagó "Silenciar el teléfono": se aplica ya a la
    // sesión en curso (si hay).
    fun cambiarSilencio(activo: Boolean, sesion: SesionEstudio?)
}

class AvisosSistemaImpl(
    private val programador: ProgramadorRecordatorios,
    private val notificacionTemporizador: NotificacionTemporizador,
    private val noMolestar: ModoNoMolestar,
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

    override suspend fun programarTarea(tarea: Tarea) {
        if (recordatoriosActivos.first()) programador.programarTarea(tarea)
    }

    override fun cancelarTarea(tareaId: Int) {
        programador.cancelarTarea(tareaId)
    }

    override suspend fun activarRecordatorios(eventos: List<Evento>, tareas: List<Tarea>) {
        programador.programarDiario()
        programarEventos(eventos)
        tareas.forEach { programarTarea(it) }
    }

    override fun desactivarRecordatorios() {
        programador.cancelarTodo()
    }

    override fun actualizarTemporizador(sesion: SesionEstudio?, ahora: Long) {
        when {
            sesion == null -> {
                programador.cancelarFinTemporizador()
                notificacionTemporizador.ocultar()
                noMolestar.restaurar()
            }
            sesion.enPausa -> {
                programador.cancelarFinTemporizador()
                notificacionTemporizador.mostrar(sesion)
                noMolestar.restaurar()
            }
            else -> {
                programador.programarFinTemporizador(sesion.restanteMillis(ahora))
                notificacionTemporizador.mostrar(sesion)
                noMolestar.activar()
            }
        }
    }

    override fun cambiarSilencio(activo: Boolean, sesion: SesionEstudio?) {
        noMolestar.elegido = activo
        if (activo && sesion != null && !sesion.enPausa) noMolestar.activar() else noMolestar.restaurar()
    }
}
