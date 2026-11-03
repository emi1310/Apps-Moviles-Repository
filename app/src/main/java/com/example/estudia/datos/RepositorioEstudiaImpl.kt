package com.example.estudia.datos

import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import com.example.estudia.datos.local.ArchivoDao
import com.example.estudia.datos.local.CalificacionDao
import com.example.estudia.datos.local.EventoDao
import com.example.estudia.datos.local.MateriaDao
import com.example.estudia.datos.local.NotaDao
import com.example.estudia.datos.local.TareaDao
import com.example.estudia.datos.local.UsuarioDao
import com.example.estudia.datos.local.UsuarioEntity
import com.example.estudia.datos.local.aEntidad
import com.example.estudia.datos.local.aModelo
import com.example.estudia.datos.local.metodoAuthATexto
import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.Calificacion
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.PreferenciaTema
import com.example.estudia.modelo.SesionEstudio
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.notificaciones.MINUTOS_AVISO_CLASE
import com.example.estudia.util.Contrasenas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

// Implementación real del Repository. Combina varias fuentes:
//  - Room (DAOs): usuarios, materias, tareas, eventos, notas y archivos.
//  - DataStore: el id del usuario con la sesión abierta, las preferencias
//    y la sesión del temporizador en curso.
//  - AlmacenamientoArchivos: el contenido de los adjuntos (filesDir).
// También convierte entidades de Room <-> modelo de dominio (Mapeos.kt).
// Solo maneja datos: los recordatorios y la notificación del temporizador
// están en AvisosSistema. Las dependencias las arma EstudiaApp.
// Arquitectura: UI -> ViewModel -> Repository -> Room / DataStore / archivos.
class RepositorioEstudiaImpl(
    private val usuarioDao: UsuarioDao,
    private val materiaDao: MateriaDao,
    private val tareaDao: TareaDao,
    private val eventoDao: EventoDao,
    private val notaDao: NotaDao,
    private val archivoDao: ArchivoDao,
    private val calificacionDao: CalificacionDao,
    private val sesion: SesionDataStore,
    private val preferencias: PreferenciasDataStore,
    private val almacenamiento: AlmacenamientoArchivos,
    private val temporizador: TemporizadorDataStore,
    // Título de los bloques de estudio que guarda el temporizador. Lo lee
    // EstudiaApp de strings.xml, así el Repository no necesita Context.
    private val tituloSesionEstudio: String
) : RepositorioEstudia {

    // ---------- Observables ----------

    // Cada vez que cambia el id guardado en DataStore, se pasa a observar ese
    // usuario en Room (flatMapLatest). Como ambos son Flow, la UI se actualiza
    // sola después de cada inserción, edición o borrado.
    @OptIn(ExperimentalCoroutinesApi::class)
    override val usuarioActual: Flow<Usuario?> = sesion.idUsuarioActivo.flatMapLatest { id ->
        if (id == null) {
            flowOf(null)
        } else {
            usuarioDao.observarConMaterias(id).map { it?.aModelo() }
        }
    }

    override val recordatoriosActivos: Flow<Boolean> = preferencias.recordatoriosActivos

    override val tema: Flow<PreferenciaTema> = preferencias.tema

    override fun buscarNotas(usuarioId: Int, texto: String): Flow<List<NotaEncontrada>> =
        notaDao.buscar(usuarioId, texto).map { filas ->
            filas.map { NotaEncontrada(it.nota.aModelo(), it.nota.materiaId, it.nombreMateria) }
        }

    override fun buscarTareas(usuarioId: Int, texto: String): Flow<List<TareaEncontrada>> =
        tareaDao.buscar(usuarioId, texto).map { filas ->
            filas.map { TareaEncontrada(it.tarea.aModelo(), it.tarea.materiaId, it.nombreMateria, it.colorMateria) }
        }

    // ---------- Sesión y perfil ----------

    // La primera vez que se abre la app (base vacía) carga el usuario de
    // demostración. Cuenta de prueba: usuario "demo" / contraseña "demo".
    // También aprovecha para borrar adjuntos que quedaron sin fila en la base.
    override suspend fun crearDatosDePruebaSiHaceFalta() {
        if (usuarioDao.contar() == 0) {
            insertarUsuarioCompleto(crearUsuarioDePrueba(), contrasena = "demo")
        }
        almacenamiento.limpiarHuerfanos(archivoDao.todasLasRutas().toSet())
    }

    // Se busca el usuario por nombre y se verifica la contraseña contra el hash.
    // PBKDF2 es lento a propósito: se calcula en Dispatchers.Default.
    override suspend fun iniciarSesion(nombreUsuario: String, contrasena: String): Boolean {
        val usuario = usuarioDao.buscarPorNombreUsuario(nombreUsuario) ?: return false
        val correcta = withContext(Dispatchers.Default) { Contrasenas.verificar(contrasena, usuario.contrasenaHash) }
        if (!correcta) return false
        sesion.guardarSesion(usuario.id)
        return true
    }

    // Devuelve false si el nombre de usuario ya existe.
    override suspend fun registrarUsuario(nombre: String, nombreUsuario: String, contrasena: String): Boolean {
        if (usuarioDao.contarPorNombreUsuario(nombreUsuario) > 0) return false
        val hash = withContext(Dispatchers.Default) { Contrasenas.hashear(contrasena) }
        val id = usuarioDao.insertar(
            UsuarioEntity(
                nombreUsuario = nombreUsuario,
                contrasenaHash = hash,
                metodoAuth = metodoAuthATexto(MetodoAuth.LOCAL),
                nombre = nombre
            )
        )
        sesion.guardarSesion(id.toInt())
        return true
    }

    override suspend fun cerrarSesion() {
        sesion.borrarSesion()
        // El temporizador es del usuario que se va: se descarta sin guardar.
        candadoTemporizador.withLock { temporizador.borrar() }
    }

    override suspend fun editarPerfil(usuarioId: Int, nombre: String, carrera: String?, anio: Int?, email: String?) {
        usuarioDao.actualizarPerfil(usuarioId, nombre, carrera, anio, email)
    }

    override suspend fun cambiarMetaSemanal(usuarioId: Int, horas: Int?) {
        usuarioDao.actualizarMetaSemanal(usuarioId, horas)
    }

    // ---------- Materias ----------

    override suspend fun agregarMateria(usuarioId: Int, nombre: String, color: Long) {
        materiaDao.insertar(Materia(id = 0, nombre = nombre, color = color).aEntidad(usuarioId))
    }

    override suspend fun actualizarMateria(usuarioId: Int, materia: Materia) {
        materiaDao.actualizar(materia.aEntidad(usuarioId))
    }

    // El CASCADE de Room borra sus tareas, eventos, notas y filas de archivos.
    // El contenido de los archivos en disco se borra después de que pasa el
    // "Deshacer" (borrarArchivosDelDisco).
    override suspend fun eliminarMateria(usuarioId: Int, materia: Materia) {
        materiaDao.eliminar(materia.aEntidad(usuarioId))
    }

    // Se vuelve a insertar todo con los mismos ids. El orden importa por las
    // ForeignKey: primero la materia, después las notas y al final los archivos.
    override suspend fun restaurarMateria(usuarioId: Int, materia: Materia) {
        materiaDao.insertar(materia.aEntidad(usuarioId))
        materia.tareas.forEach { tareaDao.insertar(it.aEntidad(materia.id)) }
        materia.eventos.forEach { eventoDao.insertar(it.aEntidad(materia.id)) }
        materia.notas.forEach { notaDao.insertar(it.aEntidad(materia.id)) }
        archivoDao.insertarTodos(materia.todosLosArchivos().map { it.aEntidad(materia.id) })
        materia.calificaciones.forEach { calificacionDao.insertar(it.aEntidad(materia.id)) }
    }

    // ---------- Tareas ----------

    override suspend fun agregarTarea(materiaId: Int, titulo: String, fechaLimite: LocalDate?) {
        tareaDao.insertar(Tarea(id = 0, titulo = titulo, fechaLimite = fechaLimite).aEntidad(materiaId))
    }

    override suspend fun actualizarTarea(materiaId: Int, tarea: Tarea) {
        tareaDao.actualizar(tarea.aEntidad(materiaId))
    }

    override suspend fun eliminarTarea(materiaId: Int, tarea: Tarea) {
        tareaDao.eliminar(tarea.aEntidad(materiaId))
    }

    // Se inserta con el mismo id que tenía, así queda igual que antes.
    override suspend fun restaurarTarea(materiaId: Int, tarea: Tarea) {
        tareaDao.insertar(tarea.aEntidad(materiaId))
    }

    // ---------- Calificaciones ----------

    override suspend fun agregarCalificacion(materiaId: Int, titulo: String, nota: Double, fecha: LocalDate) {
        calificacionDao.insertar(Calificacion(0, titulo, nota, fecha).aEntidad(materiaId))
    }

    override suspend fun actualizarCalificacion(materiaId: Int, calificacion: Calificacion) {
        calificacionDao.actualizar(calificacion.aEntidad(materiaId))
    }

    override suspend fun eliminarCalificacion(materiaId: Int, calificacion: Calificacion) {
        calificacionDao.eliminar(calificacion.aEntidad(materiaId))
    }

    override suspend fun restaurarCalificacion(materiaId: Int, calificacion: Calificacion) {
        calificacionDao.insertar(calificacion.aEntidad(materiaId))
    }

    // ---------- Eventos ----------

    override suspend fun agregarEvento(materiaId: Int, evento: Evento): Evento {
        val id = eventoDao.insertar(evento.copy(id = 0).aEntidad(materiaId)).toInt()
        return evento.copy(id = id)
    }

    override suspend fun actualizarEvento(materiaId: Int, evento: Evento) {
        eventoDao.actualizar(evento.aEntidad(materiaId))
    }

    override suspend fun eliminarEvento(materiaId: Int, evento: Evento) {
        eventoDao.eliminar(evento.aEntidad(materiaId))
    }

    override suspend fun restaurarEvento(materiaId: Int, evento: Evento) {
        eventoDao.insertar(evento.aEntidad(materiaId))
    }

    // ---------- Notas ----------

    override suspend fun agregarNota(materiaId: Int, titulo: String, contenido: String) {
        notaDao.insertar(
            Nota(id = 0, titulo = titulo, contenido = contenido, fechaCreacion = LocalDate.now()).aEntidad(materiaId)
        )
    }

    override suspend fun actualizarNota(materiaId: Int, nota: Nota) {
        notaDao.actualizar(nota.aEntidad(materiaId))
    }

    override suspend fun eliminarNota(materiaId: Int, nota: Nota) {
        notaDao.eliminar(nota.aEntidad(materiaId))
    }

    override suspend fun restaurarNota(materiaId: Int, nota: Nota) {
        notaDao.insertar(nota.aEntidad(materiaId))
        archivoDao.insertarTodos(nota.archivos.map { it.aEntidad(materiaId) })
    }

    // ---------- Archivos adjuntos ----------

    override suspend fun adjuntarArchivo(materiaId: Int, notaId: Int?, uri: String): Boolean {
        val copiado = almacenamiento.copiarDesde(Uri.parse(uri)) ?: return false
        archivoDao.insertar(
            Archivo(
                id = 0,
                nombre = copiado.nombre,
                tipoMime = copiado.tipoMime,
                ruta = copiado.ruta,
                tamanoBytes = copiado.tamanoBytes,
                fechaAgregado = LocalDate.now(),
                notaId = notaId
            ).aEntidad(materiaId)
        )
        return true
    }

    // Solo borra la fila: el archivo en disco se borra después de que pasa
    // el "Deshacer" (borrarArchivosDelDisco).
    override suspend fun eliminarArchivo(materiaId: Int, archivo: Archivo) {
        archivoDao.eliminar(archivo.aEntidad(materiaId))
    }

    override suspend fun restaurarArchivo(materiaId: Int, archivo: Archivo) {
        archivoDao.insertar(archivo.aEntidad(materiaId))
    }

    override suspend fun borrarArchivosDelDisco(archivos: List<Archivo>) {
        archivos.forEach { almacenamiento.eliminar(it.ruta) }
    }

    override fun uriParaAbrir(archivo: Archivo): String = almacenamiento.uriParaAbrir(archivo.ruta).toString()

    // ---------- Preferencias ----------

    override suspend fun cambiarRecordatorios(activos: Boolean) {
        preferencias.guardarRecordatoriosActivos(activos)
    }

    override suspend fun cambiarTema(tema: PreferenciaTema) {
        preferencias.guardarTema(tema)
    }

    // ---------- Temporizador de estudio ----------
    // El Mutex evita que la pantalla y TemporizadorWorker guarden la misma
    // sesión dos veces si terminan justo al mismo tiempo.

    private val candadoTemporizador = Mutex()

    override val sesionEstudio: Flow<SesionEstudio?> = temporizador.sesion

    override suspend fun empezarSesion(materia: Materia, minutos: Int): SesionEstudio = candadoTemporizador.withLock {
        val sesion = SesionEstudio.nueva(materia, minutos, ahora())
        temporizador.guardar(sesion)
        sesion
    }

    override suspend fun pausarSesion(): SesionEstudio? = candadoTemporizador.withLock {
        val pausada = temporizador.actual()?.pausar(ahora()) ?: return@withLock null
        temporizador.guardar(pausada)
        pausada
    }

    override suspend fun reanudarSesion(): SesionEstudio? = candadoTemporizador.withLock {
        val sesion = temporizador.actual()?.reanudar(ahora()) ?: return@withLock null
        temporizador.guardar(sesion)
        sesion
    }

    override suspend fun terminarSesion(guardar: Boolean): Int = candadoTemporizador.withLock {
        val sesion = temporizador.actual() ?: return@withLock 0
        val minutos = if (guardar) sesion.minutosEstudiados(ahora()) else 0
        if (minutos > 0) guardarBloqueDeEstudio(sesion.materiaId, minutos)
        temporizador.borrar()
        minutos
    }

    override suspend fun completarSesionSiTermino(): SesionEstudio? = candadoTemporizador.withLock {
        val sesion = temporizador.actual()?.takeIf { it.terminada(ahora()) } ?: return@withLock null
        guardarBloqueDeEstudio(sesion.materiaId, sesion.minutos)
        temporizador.borrar()
        sesion
    }

    // Bloque de estudio que termina ahora y empezó "minutos" antes (si la
    // sesión arrancó antes de la medianoche, se cuenta desde las 00:00).
    // Si mientras tanto se borró la materia, no hay dónde guardarlo.
    private suspend fun guardarBloqueDeEstudio(materiaId: Int, minutos: Int) {
        val fin = LocalTime.now().truncatedTo(ChronoUnit.MINUTES)
        val inicio = fin.minusMinutes(minutos.toLong()).let { if (it.isAfter(fin)) LocalTime.MIDNIGHT else it }
        val bloque = Evento(0, tituloSesionEstudio, TipoEvento.BLOQUE_ESTUDIO, LocalDate.now(), inicio, fin)
        try {
            eventoDao.insertar(bloque.aEntidad(materiaId))
        } catch (e: SQLiteConstraintException) {
            // La materia ya no existe: la sesión se descarta.
        }
    }

    private fun ahora() = System.currentTimeMillis()

    // ---------- Para los Workers ----------

    override suspend fun resumenDelDia(hoy: LocalDate): ResumenDia? {
        if (!preferencias.recordatoriosActivos.first()) return null
        val usuario = usuarioActual.first() ?: return null
        val materias = usuario.materias.filter { !it.archivada }
        val pendientes = materias.sumOf { materia -> materia.tareas.count { !it.completada } }
        val eventos = materias.flatMap { it.eventos }
        val eventosHoy = eventos.filter { it.ocurreEl(hoy) }
        val eventosManana = eventos.filter { it.ocurreEl(hoy.plusDays(1)) }
        if (pendientes == 0 && eventosHoy.isEmpty() && eventosManana.isEmpty()) return null
        return ResumenDia(pendientes, eventosHoy, eventosManana)
    }

    // Para las clases, además, tiene que empezar dentro de un rato (si el
    // aviso llegó muy tarde, por ejemplo con el teléfono apagado, no se muestra).
    override suspend fun eventoParaRecordatorio(eventoId: Int, ahora: LocalDateTime): EventoParaAviso? {
        if (!preferencias.recordatoriosActivos.first()) return null
        val fila = eventoDao.buscarConMateria(eventoId) ?: return null
        if (sesion.idUsuarioActivo.first() != fila.usuarioId) return null
        val evento = fila.evento.aModelo()
        if (evento.tipo == TipoEvento.CLASE) {
            val inicio = evento.proximoInicio(ahora.minusMinutes(1)) ?: return null
            if (inicio.isAfter(ahora.plusMinutes(MINUTOS_AVISO_CLASE * 2))) return null
            return EventoParaAviso(evento, fila.nombreMateria, inicio)
        }
        if (evento.inicio.isBefore(ahora)) return null
        return EventoParaAviso(evento, fila.nombreMateria, evento.inicio)
    }

    override suspend fun buscarEvento(eventoId: Int): Evento? =
        eventoDao.buscarConMateria(eventoId)?.evento?.aModelo()

    // ---------- Privados ----------

    // Inserta un usuario con todo su contenido. Los id del objeto se ignoran
    // (se insertan como 0) y SQLite genera los nuevos.
    private suspend fun insertarUsuarioCompleto(usuario: Usuario, contrasena: String) {
        val usuarioId = usuarioDao.insertar(
            UsuarioEntity(
                nombreUsuario = usuario.nombreUsuario,
                contrasenaHash = withContext(Dispatchers.Default) { Contrasenas.hashear(contrasena) },
                metodoAuth = metodoAuthATexto(usuario.metodoAuth),
                nombre = usuario.nombre,
                carrera = usuario.carrera,
                anio = usuario.anio,
                email = usuario.email,
                metaHorasSemanales = usuario.metaHorasSemanales
            )
        ).toInt()
        for (materia in usuario.materias) {
            val materiaId = materiaDao.insertar(materia.aEntidad(usuarioId).copy(id = 0)).toInt()
            materia.tareas.forEach { tareaDao.insertar(it.aEntidad(materiaId).copy(id = 0)) }
            materia.eventos.forEach { eventoDao.insertar(it.aEntidad(materiaId).copy(id = 0)) }
            materia.notas.forEach { notaDao.insertar(it.aEntidad(materiaId).copy(id = 0)) }
            materia.calificaciones.forEach { calificacionDao.insertar(it.aEntidad(materiaId).copy(id = 0)) }
        }
    }
}
