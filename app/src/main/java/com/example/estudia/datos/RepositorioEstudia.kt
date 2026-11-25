package com.example.estudia.datos

import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.Calificacion
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.PreferenciaTema
import com.example.estudia.modelo.Prioridad
import com.example.estudia.modelo.SesionEstudio
import com.example.estudia.modelo.Subtarea
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.Tema
import com.example.estudia.modelo.Tarjeta
import com.example.estudia.modelo.Usuario
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

// Repository: centraliza el acceso a los datos y le oculta al ViewModel
// de dónde vienen (Room, DataStore, archivos). Los avisos al sistema
// (WorkManager y notificaciones) no van acá: ver AvisosSistema.
//
// Es una interfaz (como en el apunte de MVVM, cap. 5.6): la app usa
// RepositorioEstudiaImpl, y los tests de los ViewModels usan un repositorio
// falso que guarda todo en memoria, sin base de datos ni emulador.
interface RepositorioEstudia {

    // ---------- Observables (Flow) ----------

    // Usuario con la sesión abierta, con todas sus materias, o null.
    val usuarioActual: Flow<Usuario?>
    val recordatoriosActivos: Flow<Boolean>
    val tema: Flow<PreferenciaTema>
    val permisoArchivosPedido: Flow<Boolean>
    val silenciarAlEstudiar: Flow<Boolean>

    fun buscarNotas(usuarioId: Int, texto: String): Flow<List<NotaEncontrada>>
    fun buscarTareas(usuarioId: Int, texto: String): Flow<List<TareaEncontrada>>

    // ---------- Sesión y perfil ----------

    suspend fun crearDatosDePruebaSiHaceFalta()
    suspend fun iniciarSesion(nombreUsuario: String, contrasena: String): Boolean
    suspend fun registrarUsuario(nombre: String, nombreUsuario: String, contrasena: String): Boolean
    suspend fun cerrarSesion()
    suspend fun editarPerfil(usuarioId: Int, nombre: String, carrera: String?, anio: Int?, email: String?)
    suspend fun cambiarMetaSemanal(usuarioId: Int, horas: Int?)

    // Copia la foto (uri de la cámara o del selector, como texto) a la
    // carpeta interna y la deja como foto de perfil; uri null = quitarla.
    // La foto anterior se borra del disco. Devuelve false si no se pudo copiar.
    suspend fun cambiarFotoPerfil(usuarioId: Int, uri: String?): Boolean
    // Uri content:// de la foto, para que la UI la pueda leer y mostrar.
    fun uriDeFoto(ruta: String): String

    // ---------- Materias ----------

    suspend fun agregarMateria(usuarioId: Int, nombre: String, color: Long)
    suspend fun actualizarMateria(usuarioId: Int, materia: Materia)
    // Los archivos en disco no se borran acá, por si el usuario toca
    // "Deshacer": ver borrarArchivosDelDisco.
    suspend fun eliminarMateria(usuarioId: Int, materia: Materia)
    suspend fun restaurarMateria(usuarioId: Int, materia: Materia)

    // ---------- Tareas ----------

    // subtareas: los títulos de los pasos (se crean sin hacer).
    // Devuelve la tarea ya guardada, con su id (para programar su aviso).
    suspend fun agregarTarea(
        materiaId: Int,
        titulo: String,
        fechaLimite: LocalDate?,
        prioridad: Prioridad = Prioridad.MEDIA,
        subtareas: List<String> = emptyList(),
        avisoDiasAntes: Int? = null
    ): Tarea
    // Solo la fila de la tarea (título, fecha, prioridad, hecha): las
    // subtareas no se tocan. Para cambiarlas está guardarSubtareas.
    suspend fun actualizarTarea(materiaId: Int, tarea: Tarea)
    // Reemplaza la lista de subtareas de una tarea (las nuevas van con id 0).
    suspend fun guardarSubtareas(tareaId: Int, subtareas: List<Subtarea>)
    suspend fun alternarSubtarea(tareaId: Int, subtarea: Subtarea)
    suspend fun eliminarTarea(materiaId: Int, tarea: Tarea)
    suspend fun restaurarTarea(materiaId: Int, tarea: Tarea)

    // ---------- Calificaciones (notas de los exámenes) ----------

    suspend fun agregarCalificacion(materiaId: Int, titulo: String, nota: Double, fecha: LocalDate)
    suspend fun actualizarCalificacion(materiaId: Int, calificacion: Calificacion)
    suspend fun eliminarCalificacion(materiaId: Int, calificacion: Calificacion)
    suspend fun restaurarCalificacion(materiaId: Int, calificacion: Calificacion)

    // ---------- Tarjetas de estudio ----------

    suspend fun agregarTarjeta(materiaId: Int, pregunta: String, respuesta: String)
    // También guarda el resultado de un repaso (caja y próximo repaso).
    suspend fun actualizarTarjeta(materiaId: Int, tarjeta: Tarjeta)
    suspend fun eliminarTarjeta(materiaId: Int, tarjeta: Tarjeta)
    suspend fun restaurarTarjeta(materiaId: Int, tarjeta: Tarjeta)

    // ---------- Eventos ----------

    // Devuelve el evento ya guardado, con su id (para programar su aviso).
    suspend fun agregarEvento(materiaId: Int, evento: Evento): Evento
    suspend fun actualizarEvento(materiaId: Int, evento: Evento)
    suspend fun eliminarEvento(materiaId: Int, evento: Evento)
    suspend fun restaurarEvento(materiaId: Int, evento: Evento)

    // ---------- Temario de los exámenes ----------

    suspend fun agregarTema(eventoId: Int, titulo: String)
    suspend fun alternarTema(eventoId: Int, tema: Tema)
    suspend fun eliminarTema(eventoId: Int, tema: Tema)
    suspend fun restaurarTema(eventoId: Int, tema: Tema)

    // ---------- Notas ----------

    suspend fun agregarNota(materiaId: Int, titulo: String, contenido: String)
    suspend fun actualizarNota(materiaId: Int, nota: Nota)
    // Borra la nota (y las filas de sus adjuntos) pero deja los archivos en
    // disco, por si el usuario toca "Deshacer". Ver borrarArchivosDelDisco.
    suspend fun eliminarNota(materiaId: Int, nota: Nota)
    suspend fun restaurarNota(materiaId: Int, nota: Nota)

    // ---------- Archivos adjuntos ----------

    // notaId null = adjunto de la materia. Devuelve false si no se pudo.
    // uri: el que devuelve el selector de archivos, como texto (así el
    // ViewModel no depende de android.net.Uri).
    suspend fun adjuntarArchivo(materiaId: Int, notaId: Int?, uri: String): Boolean
    suspend fun eliminarArchivo(materiaId: Int, archivo: Archivo)
    suspend fun restaurarArchivo(materiaId: Int, archivo: Archivo)
    suspend fun borrarArchivosDelDisco(archivos: List<Archivo>)
    fun uriParaAbrir(archivo: Archivo): String

    // OCR: lee el texto de un adjunto que es imagen. "" = no encontró
    // texto; null = no se pudo leer la imagen.
    suspend fun leerTextoDeImagen(archivo: Archivo): String?

    // ---------- Calendario del teléfono ----------

    // Pasa las clases, exámenes y entregas de las materias activas al
    // calendario "Estudia+" del teléfono (necesita el permiso de calendario).
    // Devuelve cuántos eventos quedaron, o null si no se pudo.
    suspend fun sincronizarCalendario(nombreCalendario: String): Int?

    // ---------- Copia de seguridad ----------

    // uri: el archivo elegido con el selector del sistema (como texto).
    // Devuelven false si no se pudo escribir o leer.
    suspend fun exportarCopia(uri: String): Boolean
    // Reemplaza las materias y el perfil del usuario por los de la copia.
    suspend fun importarCopia(uri: String): Boolean

    // ---------- Preferencias y recordatorios ----------

    // Solo guarda la preferencia; los avisos los programa AvisosSistema.
    suspend fun cambiarRecordatorios(activos: Boolean)
    suspend fun cambiarTema(tema: PreferenciaTema)
    suspend fun marcarPermisoArchivosPedido()
    suspend fun cambiarSilenciarAlEstudiar(activo: Boolean)

    // ---------- Temporizador de estudio ----------
    // Al terminar, la sesión se guarda como un bloque de estudio de hoy, así
    // se suma sola a las horas de estudio y a la meta semanal.

    val sesionEstudio: Flow<SesionEstudio?>
    // Devuelven la sesión como quedó (null = no había sesión).
    suspend fun empezarSesion(materia: Materia, minutos: Int): SesionEstudio
    suspend fun pausarSesion(): SesionEstudio?
    suspend fun reanudarSesion(): SesionEstudio?

    // Corta la sesión antes de tiempo. Si guardar = true, registra lo que ya
    // se estudió (si es al menos 1 minuto). Devuelve los minutos guardados.
    suspend fun terminarSesion(guardar: Boolean): Int

    // Si el tiempo ya llegó a cero, guarda la sesión completa y la cierra.
    // Devuelve la sesión guardada, o null si no había nada que completar.
    // La usan la pantalla y TemporizadorWorker: solo uno de los dos la guarda.
    suspend fun completarSesionSiTermino(): SesionEstudio?

    // ---------- Para los Workers (recordatorios) ----------

    // Tareas pendientes y eventos de hoy y mañana del usuario con sesión
    // abierta. null si no hay nada que avisar, si nadie inició sesión o si
    // el usuario desactivó los recordatorios.
    suspend fun resumenDelDia(hoy: LocalDate = LocalDate.now()): ResumenDia?

    // El evento a recordar, solo si sigue existiendo, es del usuario con
    // sesión abierta, todavía no pasó y los recordatorios están activos.
    suspend fun eventoParaRecordatorio(eventoId: Int, ahora: LocalDateTime = LocalDateTime.now()): EventoParaAviso?

    // El evento tal como está guardado ahora, o null si se borró.
    suspend fun buscarEvento(eventoId: Int): Evento?

    // La tarea a recordar, solo si sigue existiendo, es del usuario con
    // sesión abierta, no está hecha y los recordatorios están activos.
    suspend fun tareaParaRecordatorio(tareaId: Int): TareaParaAviso?

    // Días de racha que se cortarían si hoy no estudia (null = no hay nada
    // que avisar: sin racha, ya estudió hoy o los recordatorios están apagados).
    suspend fun rachaEnRiesgo(hoy: LocalDate = LocalDate.now()): Int?
}

data class TareaParaAviso(val tarea: Tarea, val nombreMateria: String)

// Resultados del buscador (una nota o tarea junto con su materia).
data class NotaEncontrada(val nota: Nota, val materiaId: Int, val nombreMateria: String)

data class TareaEncontrada(val tarea: Tarea, val materiaId: Int, val nombreMateria: String, val colorMateria: Long)

// Lo que muestra el resumen diario.
data class ResumenDia(val tareasPendientes: Int, val eventosHoy: List<Evento>, val eventosManana: List<Evento>)

// inicio: cuándo empieza esta vez (en las clases semanales, la clase de hoy).
data class EventoParaAviso(val evento: Evento, val nombreMateria: String, val inicio: LocalDateTime)
