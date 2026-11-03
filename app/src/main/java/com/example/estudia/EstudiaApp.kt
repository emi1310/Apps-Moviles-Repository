package com.example.estudia

import android.app.Application
import android.app.UiModeManager
import android.os.Build
import com.example.estudia.datos.AlmacenamientoArchivos
import com.example.estudia.datos.PreferenciasDataStore
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.datos.RepositorioEstudiaImpl
import com.example.estudia.datos.SesionDataStore
import com.example.estudia.datos.TemporizadorDataStore
import com.example.estudia.datos.local.AppDatabase
import com.example.estudia.modelo.PreferenciaTema
import com.example.estudia.notificaciones.AvisosSistema
import com.example.estudia.notificaciones.AvisosSistemaImpl
import com.example.estudia.notificaciones.NotificacionTemporizador
import com.example.estudia.notificaciones.Notificaciones
import com.example.estudia.notificaciones.ProgramadorRecordatorios
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Clase Application: se crea una sola vez cuando arranca el proceso de la
// app, antes que cualquier pantalla. Funciona como "contenedor" de las
// dependencias compartidas (inyección de dependencias manual): el
// Repository y AvisosSistema viven acá y los usan todos los ViewModels,
// los Workers y TemporizadorReceiver.
class EstudiaApp : Application() {

    private val preferencias by lazy { PreferenciasDataStore(this) }

    // by lazy: se crean la primera vez que alguien los pide.
    val repositorio: RepositorioEstudia by lazy {
        val db = AppDatabase.obtenerInstancia(this)
        RepositorioEstudiaImpl(
            usuarioDao = db.usuarioDao(),
            materiaDao = db.materiaDao(),
            tareaDao = db.tareaDao(),
            eventoDao = db.eventoDao(),
            notaDao = db.notaDao(),
            archivoDao = db.archivoDao(),
            calificacionDao = db.calificacionDao(),
            sesion = SesionDataStore(this),
            preferencias = preferencias,
            almacenamiento = AlmacenamientoArchivos(this),
            temporizador = TemporizadorDataStore(this),
            tituloSesionEstudio = getString(R.string.sesion_estudio_titulo)
        )
    }

    val avisos: AvisosSistema by lazy {
        AvisosSistemaImpl(
            programador = ProgramadorRecordatorios(this),
            notificacionTemporizador = NotificacionTemporizador(this),
            recordatoriosActivos = preferencias.recordatoriosActivos
        )
    }

    override fun onCreate() {
        super.onCreate()
        // El canal tiene que existir antes de mostrar cualquier notificación.
        Notificaciones.crearCanal(this)
        // Reiniciar el teléfono borra las notificaciones, pero la sesión del
        // temporizador sigue guardada: al arrancar la app se vuelve a mostrar.
        // Si ya terminó, no se toca: la guarda TemporizadorWorker.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            val sesion = repositorio.sesionEstudio.first() ?: return@launch
            if (!sesion.terminada(System.currentTimeMillis())) avisos.actualizarTemporizador(sesion)
        }
        // En Android 12+ se le avisa al sistema el tema elegido: así la
        // pantalla de carga (que se dibuja antes que Compose) sale oscura o
        // clara según la app, y no según el teléfono.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            scope.launch {
                repositorio.tema.distinctUntilChanged().collect { tema ->
                    getSystemService(UiModeManager::class.java).setApplicationNightMode(
                        when (tema) {
                            PreferenciaTema.CLARO -> UiModeManager.MODE_NIGHT_NO
                            PreferenciaTema.OSCURO -> UiModeManager.MODE_NIGHT_YES
                            PreferenciaTema.SISTEMA -> UiModeManager.MODE_NIGHT_AUTO
                        }
                    )
                }
            }
        }
    }
}
