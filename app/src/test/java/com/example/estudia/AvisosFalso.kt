package com.example.estudia

import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.SesionEstudio
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

    override suspend fun activarRecordatorios(eventos: List<Evento>) {
        recordatoriosActivos = true
        programarEventos(eventos)
    }

    override fun desactivarRecordatorios() {
        recordatoriosActivos = false
    }

    override fun actualizarTemporizador(sesion: SesionEstudio?, ahora: Long) {
        sesionEnNotificacion = sesion
    }
}
