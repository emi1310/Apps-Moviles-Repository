package com.example.estudia

import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.SesionEstudio
import com.example.estudia.modelo.Tarea
import com.example.estudia.notificaciones.AvisosSistema

// AvisosSistema falso para los tests: en vez de usar WorkManager y mostrar
// notificaciones, anota lo que se le pidió, así cada test puede revisarlo.
class AvisosFalso : AvisosSistema {

    val eventosProgramados = mutableListOf<Int>()
    val eventosCancelados = mutableListOf<Int>()
    var recordatoriosActivos = false

    // La última sesión que se mandó a la notificación (null = oculta).
    var sesionEnNotificacion: SesionEstudio? = null

    override suspend fun programarEvento(evento: Evento) {
        eventosProgramados += evento.id
    }

    override suspend fun programarEventos(eventos: List<Evento>) {
        eventos.forEach { programarEvento(it) }
    }

    override suspend fun programarClaseSiguiente(evento: Evento) {
        programarEvento(evento)
    }

    override fun cancelarEvento(eventoId: Int) {
        eventosCancelados += eventoId
    }

    val tareasProgramadas = mutableListOf<Tarea>()
    val tareasCanceladas = mutableListOf<Int>()

    override suspend fun programarTarea(tarea: Tarea) {
        tareasProgramadas += tarea
    }

    override fun cancelarTarea(tareaId: Int) {
        tareasCanceladas += tareaId
    }

    override suspend fun activarRecordatorios(eventos: List<Evento>, tareas: List<Tarea>) {
        recordatoriosActivos = true
        programarEventos(eventos)
        tareas.forEach { programarTarea(it) }
    }

    override fun desactivarRecordatorios() {
        recordatoriosActivos = false
    }

    override fun actualizarTemporizador(sesion: SesionEstudio?, ahora: Long) {
        sesionEnNotificacion = sesion
    }

    var silencioElegido: Boolean? = null

    override fun cambiarSilencio(activo: Boolean, sesion: SesionEstudio?) {
        silencioElegido = activo
    }
}
