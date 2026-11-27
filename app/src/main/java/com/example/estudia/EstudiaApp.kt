package com.example.estudia

import android.app.Application
import android.app.UiModeManager
import android.os.Build
import com.example.estudia.datos.AsistenteIA
import com.example.estudia.datos.ClavesIA
import com.example.estudia.datos.ClavesIACifradas
import com.example.estudia.datos.GeminiCliente
import com.example.estudia.datos.AlmacenamientoArchivos
import com.example.estudia.datos.CopiaDeSeguridad
import com.example.estudia.datos.ExportadorPdf
import com.example.estudia.datos.FuenteFeriados
import com.example.estudia.datos.CalendarioTelefono
import com.example.estudia.datos.LectorTexto
import com.example.estudia.datos.PreferenciasDataStore
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.datos.RepositorioEstudiaImpl
import com.example.estudia.datos.SesionDataStore
import com.example.estudia.datos.TemporizadorDataStore
import com.example.estudia.datos.local.AppDatabase
import com.example.estudia.modelo.PreferenciaTema
import com.example.estudia.notificaciones.AvisosSistema
import com.example.estudia.notificaciones.AvisosSistemaImpl
import com.example.estudia.notificaciones.ModoNoMolestar
import com.example.estudia.notificaciones.NotificacionTemporizador
import com.example.estudia.notificaciones.Notificaciones
import com.example.estudia.notificaciones.ProgramadorRecordatorios
import com.example.estudia.widget.WidgetEstudia
import com.example.estudia.widget.WidgetHoy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.example.estudia.datos.CopiaAutomaticaWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.estudia.modelo.eventosParaCalendario
import java.time.LocalDate

// Clase Application: se crea una sola vez cuando arranca el proceso de la
// app, antes que cualquier pantalla. Funciona como "contenedor" de las
// dependencias compartidas (inyección de dependencias manual): el
// Repository y AvisosSistema viven acá y los usan todos los ViewModels,
// los Workers y TemporizadorReceiver.
@OptIn(kotlinx.coroutines.FlowPreview::class)
class EstudiaApp : Application() {

    private val preferencias by lazy { PreferenciasDataStore(this) }

    // by lazy: se crean la primera vez que alguien los pide.
    // Asistente IA (Gemini, plan gratis). Cada usuario conecta su propia clave
    // (guardada cifrada en clavesIA); la de gemini.properties, si existe, es
    // la de la app y se usa cuando el usuario no conectó la suya.
    val asistente: AsistenteIA by lazy { GeminiCliente(BuildConfig.GEMINI_MODELO) }
    val clavesIA: ClavesIA by lazy { ClavesIACifradas(this) }
    // Charlas guardadas y simulacros del Asistente (un archivo por usuario).
    val historialIA: com.example.estudia.datos.HistorialIA by lazy { com.example.estudia.datos.HistorialIAEnArchivo(this) }

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
            temaDao = db.temaDao(),
            tarjetaDao = db.tarjetaDao(),
            sesion = SesionDataStore(this),
            preferencias = preferencias,
            almacenamiento = AlmacenamientoArchivos(this),
            temporizador = TemporizadorDataStore(this),
            copiaDeSeguridad = CopiaDeSeguridad(this),
            calendarioTelefono = CalendarioTelefono(this),
            lectorTexto = LectorTexto(this),
            carreraDao = db.carreraDao(),
            exportadorPdf = ExportadorPdf(this),
            feriadoDao = db.feriadoDao(),
            fuenteFeriados = FuenteFeriados(this),
            tituloSesionEstudio = getString(R.string.sesion_estudio_titulo)
        )
    }

    private val noMolestar by lazy { ModoNoMolestar(this) }

    val avisos: AvisosSistema by lazy {
        AvisosSistemaImpl(
            programador = ProgramadorRecordatorios(this),
            notificacionTemporizador = NotificacionTemporizador(this),
            noMolestar = noMolestar,
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
        // "Silenciar el teléfono" se lee de DataStore y se mantiene al día en
        // ModoNoMolestar, que lo necesita al instante (sin corrutinas).
        scope.launch {
            preferencias.silenciarAlEstudiar.collect { noMolestar.elegido = it }
        }
        // El widget se redibuja cada vez que cambian los datos del usuario
        // (una tarea hecha, una sesión de estudio, cerrar sesión...).
        scope.launch {
            repositorio.usuarioActual.distinctUntilChanged().collect {
                WidgetEstudia.actualizarTodos(this@EstudiaApp)
                WidgetHoy.actualizarTodos(this@EstudiaApp)
            }
        }
        // Copia automática semanal: se programa (o se cancela) al prenderla o apagarla.
        scope.launch {
            repositorio.copiaAutomatica.map { it.activa }.distinctUntilChanged().collect { activa ->
                CopiaAutomaticaWorker.programar(this@EstudiaApp, activa)
            }
        }
        // Calendario del teléfono: una vez activado, cada cambio en los eventos
        // (o el paso de los días) lo actualiza solo, unos segundos después.
        // Sin el permiso de calendario no se toca.
        scope.launch {
            combine(
                repositorio.calendarioAutomatico,
                repositorio.usuarioActual.map { u -> u?.let { eventosParaCalendario(it, LocalDate.now()) } }.distinctUntilChanged()
            ) { activo, eventos -> activo && eventos != null }
                .debounce(3_000)
                .collect { sincronizar ->
                    val permiso = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                        .all { ContextCompat.checkSelfPermission(this@EstudiaApp, it) == PackageManager.PERMISSION_GRANTED }
                    if (sincronizar && permiso) repositorio.sincronizarCalendario(getString(R.string.calendario_estudia))
                }
        }
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
