package com.example.estudia.datos

import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import com.example.estudia.datos.local.ArchivoDao
import com.example.estudia.datos.local.NotaVersionEntity
import com.example.estudia.datos.local.FeriadoDao
import com.example.estudia.datos.local.AsistenciaEntity
import com.example.estudia.datos.local.CarreraDao
import com.example.estudia.datos.local.EvaluacionEntity
import com.example.estudia.modelo.EstadoCopiaAutomatica
import com.example.estudia.modelo.calendarioIcs
import com.example.estudia.modelo.eventosParaCalendario
import java.time.Instant
import java.time.ZoneId
import com.example.estudia.modelo.Evaluacion
import com.example.estudia.modelo.CategoriaArchivo
import com.example.estudia.modelo.Feriado
import com.example.estudia.modelo.MateriaPlan
import com.example.estudia.modelo.MesaFinal
import com.example.estudia.modelo.Fuente
import com.example.estudia.modelo.MapaMental
import java.time.format.DateTimeFormatter
import com.example.estudia.datos.local.CalificacionDao
import com.example.estudia.datos.local.EventoDao
import com.example.estudia.datos.local.MateriaDao
import com.example.estudia.datos.local.NotaDao
import com.example.estudia.datos.local.SubtareaEntity
import com.example.estudia.datos.local.TareaDao
import com.example.estudia.datos.local.TemaDao
import com.example.estudia.datos.local.TarjetaDao
import com.example.estudia.datos.local.TarjetaEntity
import com.example.estudia.modelo.Tarjeta
import com.example.estudia.datos.local.TemaEntity
import com.example.estudia.modelo.Tema
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
import com.example.estudia.modelo.VersionNota
import com.example.estudia.modelo.Receso
import com.example.estudia.modelo.siguienteTareaRepetida
import com.example.estudia.modelo.cambioParaHistorial
import com.example.estudia.modelo.MAXIMO_VERSIONES_NOTA
import com.example.estudia.modelo.PreferenciaTema
import com.example.estudia.modelo.Prioridad
import com.example.estudia.modelo.SesionEstudio
import com.example.estudia.modelo.Subtarea
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
    private val temaDao: TemaDao,
    private val tarjetaDao: TarjetaDao,
    private val sesion: SesionDataStore,
    private val preferencias: PreferenciasDataStore,
    private val almacenamiento: AlmacenamientoArchivos,
    private val temporizador: TemporizadorDataStore,
    private val copiaDeSeguridad: CopiaDeSeguridad,
    private val calendarioTelefono: CalendarioTelefono,
    private val lectorTexto: LectorTexto,
    private val carreraDao: CarreraDao,
    private val exportadorPdf: ExportadorPdf,
    private val feriadoDao: FeriadoDao,
    private val fuenteFeriados: FuenteFeriados,
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
    override val permisoArchivosPedido: Flow<Boolean> = preferencias.permisoArchivosPedido
    override val silenciarAlEstudiar: Flow<Boolean> = preferencias.silenciarAlEstudiar
    override val feriados: Flow<List<Feriado>> = feriadoDao.observarTodos().map { lista -> lista.map { it.aModelo() } }

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
        // Las fotos de perfil también viven en esa carpeta: no se tocan.
        almacenamiento.limpiarHuerfanos((archivoDao.todasLasRutas() + usuarioDao.todasLasFotos()).toSet())
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

    override suspend fun cambiarFotoPerfil(usuarioId: Int, uri: String?): Boolean {
        val anterior = usuarioDao.fotoPerfil(usuarioId)
        val nueva = if (uri == null) null else (almacenamiento.copiarDesde(Uri.parse(uri)) ?: return false).ruta
        usuarioDao.actualizarFotoPerfil(usuarioId, nueva)
        if (anterior != null) almacenamiento.eliminar(anterior)
        return true
    }

    override fun uriDeFoto(ruta: String): String = almacenamiento.uriParaAbrir(ruta).toString()

    // ---------- Materias ----------

    override suspend fun agregarMateria(
        usuarioId: Int,
        nombre: String,
        color: Long,
        asistenciaMinima: Int,
        promocional: Boolean,
        notaPromocion: Double,
        tieneFinal: Boolean
    ) {
        materiaDao.insertar(
            Materia(
                id = 0, nombre = nombre, color = color,
                asistenciaMinima = asistenciaMinima, promocional = promocional,
                notaPromocion = notaPromocion, tieneFinal = tieneFinal
            ).aEntidad(usuarioId)
        )
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
        materia.tareas.forEach { restaurarTarea(materia.id, it) }
        materia.eventos.forEach { restaurarEvento(materia.id, it) }
        materia.notas.forEach { notaDao.insertar(it.aEntidad(materia.id)) }
        archivoDao.insertarTodos(materia.todosLosArchivos().map { it.aEntidad(materia.id) })
        materia.calificaciones.forEach { calificacionDao.insertar(it.aEntidad(materia.id)) }
        materia.tarjetas.forEach { tarjetaDao.insertar(it.aEntidad(materia.id)) }
        materia.evaluaciones.forEach { carreraDao.insertarEvaluacion(it.aEntidad(materia.id)) }
    }

    // ---------- Tareas ----------

    override suspend fun agregarTarea(
        materiaId: Int,
        titulo: String,
        fechaLimite: LocalDate?,
        prioridad: Prioridad,
        subtareas: List<String>,
        avisoDiasAntes: Int?
    ): Tarea {
        val nueva = Tarea(id = 0, titulo = titulo, fechaLimite = fechaLimite, prioridad = prioridad, avisoDiasAntes = avisoDiasAntes)
        val id = tareaDao.insertarConSubtareas(nueva.aEntidad(materiaId), subtareas.map { SubtareaEntity(tareaId = 0, titulo = it) })
        return nueva.copy(id = id)
    }

    override suspend fun actualizarTarea(materiaId: Int, tarea: Tarea): Tarea? {
        // Si se completa una que se repite, la repetición pasa a la siguiente.
        val anterior = tareaDao.obtener(tarea.id)?.aModelo()
        val siguiente = siguienteTareaRepetida(anterior, tarea, LocalDate.now())
        tareaDao.actualizar((if (siguiente != null) tarea.copy(repeticion = null) else tarea).aEntidad(materiaId))
        if (siguiente == null) return null
        val creada = agregarTarea(
            materiaId, siguiente.titulo, siguiente.fechaLimite, siguiente.prioridad,
            siguiente.subtareas.map { it.titulo }, siguiente.avisoDiasAntes
        ).copy(repeticion = siguiente.repeticion)
        tareaDao.actualizar(creada.aEntidad(materiaId))
        return creada
    }

    override suspend fun guardarSubtareas(tareaId: Int, subtareas: List<Subtarea>) {
        tareaDao.reemplazarSubtareas(tareaId, subtareas.map { it.aEntidad(tareaId) })
    }

    override suspend fun alternarSubtarea(tareaId: Int, subtarea: Subtarea) {
        tareaDao.actualizarSubtarea(subtarea.copy(completada = !subtarea.completada).aEntidad(tareaId))
    }

    override suspend fun eliminarTarea(materiaId: Int, tarea: Tarea) {
        tareaDao.eliminar(tarea.aEntidad(materiaId))
    }

    // Se inserta con el mismo id que tenía (y sus subtareas también), así
    // queda igual que antes.
    override suspend fun restaurarTarea(materiaId: Int, tarea: Tarea) {
        tareaDao.insertarConSubtareas(tarea.aEntidad(materiaId), tarea.subtareas.map { it.aEntidad(tarea.id) })
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

    // ---------- Tarjetas de estudio ----------

    // Una tarjeta nueva arranca en la caja 1, para repasarla hoy.
    override suspend fun agregarTarjeta(materiaId: Int, pregunta: String, respuesta: String) {
        tarjetaDao.insertar(TarjetaEntity(materiaId = materiaId, pregunta = pregunta, respuesta = respuesta, caja = 1, proximoRepaso = LocalDate.now()))
    }

    override suspend fun actualizarTarjeta(materiaId: Int, tarjeta: Tarjeta) {
        tarjetaDao.actualizar(tarjeta.aEntidad(materiaId))
    }

    override suspend fun eliminarTarjeta(materiaId: Int, tarjeta: Tarjeta) {
        tarjetaDao.eliminar(tarjeta.aEntidad(materiaId))
    }

    override suspend fun restaurarTarjeta(materiaId: Int, tarjeta: Tarjeta) {
        tarjetaDao.insertar(tarjeta.aEntidad(materiaId))
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

    // Con su temario (los temas vuelven con los mismos ids).
    override suspend fun restaurarEvento(materiaId: Int, evento: Evento) {
        eventoDao.insertar(evento.aEntidad(materiaId))
        temaDao.insertarTodos(evento.temas.map { it.aEntidad(evento.id) })
        carreraDao.guardarAsistencias(evento.asistencias.map { it.aEntidad(evento.id) })
    }

    // ---------- Temario de los exámenes ----------

    override suspend fun agregarTema(eventoId: Int, titulo: String) {
        temaDao.insertar(TemaEntity(eventoId = eventoId, titulo = titulo))
    }

    override suspend fun alternarTema(eventoId: Int, tema: Tema) {
        temaDao.actualizar(tema.copy(estudiado = !tema.estudiado).aEntidad(eventoId))
    }

    override suspend fun eliminarTema(eventoId: Int, tema: Tema) {
        temaDao.eliminar(tema.aEntidad(eventoId))
    }

    override suspend fun restaurarTema(eventoId: Int, tema: Tema) {
        temaDao.insertar(tema.aEntidad(eventoId))
    }

    // ---------- Notas ----------

    override suspend fun agregarNota(materiaId: Int, titulo: String, contenido: String) {
        notaDao.insertar(
            Nota(id = 0, titulo = titulo, contenido = contenido, fechaCreacion = LocalDate.now()).aEntidad(materiaId)
        )
    }

    override suspend fun actualizarNota(materiaId: Int, nota: Nota) {
        // Antes de pisarla, si cambió el título o el texto, se guarda cómo era.
        val anterior = notaDao.obtener(nota.id)
        if (anterior != null && cambioParaHistorial(anterior.titulo, anterior.contenido, nota.titulo, nota.contenido)) {
            notaDao.insertarVersion(
                NotaVersionEntity(notaId = nota.id, titulo = anterior.titulo, contenido = anterior.contenido, fecha = System.currentTimeMillis())
            )
            notaDao.recortarVersiones(nota.id, MAXIMO_VERSIONES_NOTA)
        }
        notaDao.actualizar(nota.aEntidad(materiaId))
    }

    override fun versionesDeNota(notaId: Int): Flow<List<VersionNota>> =
        notaDao.versiones(notaId).map { lista ->
            lista.map { VersionNota(it.id, it.titulo, it.contenido, LocalDateTime.ofInstant(Instant.ofEpochMilli(it.fecha), ZoneId.systemDefault())) }
        }

    override suspend fun eliminarNota(materiaId: Int, nota: Nota) {
        notaDao.eliminar(nota.aEntidad(materiaId))
    }

    override suspend fun restaurarNota(materiaId: Int, nota: Nota) {
        notaDao.insertar(nota.aEntidad(materiaId))
        archivoDao.insertarTodos(nota.archivos.map { it.aEntidad(materiaId) })
    }

    // ---------- Archivos adjuntos ----------

    override suspend fun adjuntarArchivo(materiaId: Int, notaId: Int?, uri: String, categoria: CategoriaArchivo): Boolean {
        val copiado = almacenamiento.copiarDesde(Uri.parse(uri)) ?: return false
        archivoDao.insertar(
            Archivo(
                id = 0,
                nombre = copiado.nombre,
                tipoMime = copiado.tipoMime,
                ruta = copiado.ruta,
                tamanoBytes = copiado.tamanoBytes,
                fechaAgregado = LocalDate.now(),
                notaId = notaId,
                categoria = categoria
            ).aEntidad(materiaId)
        )
        return true
    }

    override suspend fun cambiarCategoriaArchivo(materiaId: Int, archivo: Archivo, categoria: CategoriaArchivo) {
        archivoDao.actualizar(archivo.copy(categoria = categoria).aEntidad(materiaId))
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

    override suspend fun leerTextoDeImagen(archivo: Archivo): String? =
        if (!archivo.esImagen) null else lectorTexto.leer(almacenamiento.uriParaAbrir(archivo.ruta))

    // ---------- Preferencias ----------

    override suspend fun cambiarRecordatorios(activos: Boolean) {
        preferencias.guardarRecordatoriosActivos(activos)
    }

    override suspend fun cambiarTema(tema: PreferenciaTema) {
        preferencias.guardarTema(tema)
    }

    override suspend fun marcarPermisoArchivosPedido() {
        preferencias.guardarPermisoArchivosPedido()
    }

    override suspend fun cambiarSilenciarAlEstudiar(activo: Boolean) {
        preferencias.guardarSilenciarAlEstudiar(activo)
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

    override suspend fun tareaParaRecordatorio(tareaId: Int): TareaParaAviso? {
        if (!preferencias.recordatoriosActivos.first()) return null
        val fila = tareaDao.buscarConMateria(tareaId) ?: return null
        if (sesion.idUsuarioActivo.first() != fila.usuarioId) return null
        val tarea = fila.tarea.aModelo()
        if (tarea.completada) return null
        return TareaParaAviso(tarea, fila.nombreMateria)
    }

    override suspend fun rachaEnRiesgo(hoy: LocalDate): Int? {
        if (!preferencias.recordatoriosActivos.first()) return null
        val usuario = usuarioActual.first() ?: return null
        val estudioHoy = usuario.materias.any { m -> m.eventos.any { it.tipo == TipoEvento.BLOQUE_ESTUDIO && it.fecha == hoy } }
        if (estudioHoy) return null
        return usuario.rachaDeEstudio(hoy).takeIf { it > 0 }
    }

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
        insertarMaterias(usuarioId, usuario.materias)
        insertarCarrera(usuarioId, usuario.planEstudio, usuario.finales, usuario.fuentes, usuario.mapas, usuario.recesos)
    }

    // Inserta el plan de estudios, los finales, la bibliografía y los mapas
    // mentales como nuevos. Los ids de las
    // correlativas se traducen a los ids nuevos que genera SQLite.
    private suspend fun insertarCarrera(usuarioId: Int, plan: List<MateriaPlan>, finales: List<MesaFinal>, fuentes: List<Fuente>, mapas: List<MapaMental>, recesos: List<Receso> = emptyList()) {
        val idsNuevos = mutableMapOf<Int, Int>()
        plan.forEach { materia ->
            idsNuevos[materia.id] = carreraDao.insertarMateriaPlan(
                materia.aEntidad(usuarioId).copy(id = 0, correlativas = "")
            ).toInt()
        }
        plan.forEach { materia ->
            val id = idsNuevos.getValue(materia.id)
            carreraDao.actualizarMateriaPlan(
                materia.copy(id = id, correlativas = materia.correlativas.mapNotNull { idsNuevos[it] }).aEntidad(usuarioId)
            )
        }
        finales.forEach { carreraDao.insertarFinal(it.aEntidad(usuarioId).copy(id = 0)) }
        fuentes.forEach { carreraDao.insertarFuente(it.aEntidad(usuarioId).copy(id = 0)) }
        mapas.forEach { carreraDao.insertarMapa(it.aEntidad(usuarioId).copy(id = 0)) }
        recesos.forEach { carreraDao.insertarReceso(it.aEntidad(usuarioId).copy(id = 0)) }
    }

    // Inserta materias con todo su contenido como nuevas (los ids del objeto
    // se ignoran). Los archivos de las notas se enlazan con el id nuevo de su
    // nota. La usan los datos de prueba y la copia de seguridad.
    private suspend fun insertarMaterias(usuarioId: Int, materias: List<Materia>) {
        for (materia in materias) {
            val materiaId = materiaDao.insertar(materia.aEntidad(usuarioId).copy(id = 0)).toInt()
            materia.tareas.forEach { tarea ->
                tareaDao.insertarConSubtareas(
                    tarea.aEntidad(materiaId).copy(id = 0),
                    tarea.subtareas.map { it.aEntidad(0).copy(id = 0) }
                )
            }
            materia.eventos.forEach { evento ->
                val eventoId = eventoDao.insertar(evento.aEntidad(materiaId).copy(id = 0)).toInt()
                temaDao.insertarTodos(evento.temas.map { it.aEntidad(eventoId).copy(id = 0) })
                carreraDao.guardarAsistencias(evento.asistencias.map { it.aEntidad(eventoId).copy(id = 0) })
            }
            materia.notas.forEach { nota ->
                val notaId = notaDao.insertar(nota.aEntidad(materiaId).copy(id = 0)).toInt()
                archivoDao.insertarTodos(nota.archivos.map { it.aEntidad(materiaId).copy(id = 0, notaId = notaId) })
            }
            archivoDao.insertarTodos(materia.archivos.map { it.aEntidad(materiaId).copy(id = 0, notaId = null) })
            materia.calificaciones.forEach { calificacionDao.insertar(it.aEntidad(materiaId).copy(id = 0)) }
            materia.tarjetas.forEach { tarjetaDao.insertar(it.aEntidad(materiaId).copy(id = 0)) }
            materia.evaluaciones.forEach { carreraDao.insertarEvaluacion(it.aEntidad(materiaId).copy(id = 0)) }
        }
    }

    // ---------- Mi carrera (plan de estudios) ----------

    override suspend fun agregarMateriaPlan(usuarioId: Int, materia: MateriaPlan) {
        carreraDao.insertarMateriaPlan(materia.aEntidad(usuarioId).copy(id = 0))
    }

    override suspend fun actualizarMateriaPlan(usuarioId: Int, materia: MateriaPlan) {
        carreraDao.actualizarMateriaPlan(materia.aEntidad(usuarioId))
    }

    // Las otras materias pueden seguir teniéndola como correlativa: como ya
    // no está en el plan, se ignora (MateriaPlan.faltanParaCursar).
    override suspend fun eliminarMateriaPlan(usuarioId: Int, materia: MateriaPlan) {
        carreraDao.eliminarMateriaPlan(materia.aEntidad(usuarioId))
    }

    override suspend fun restaurarMateriaPlan(usuarioId: Int, materia: MateriaPlan) {
        carreraDao.insertarMateriaPlan(materia.aEntidad(usuarioId))
    }

    // ---------- Asistencia ----------

    override suspend fun marcarAsistencia(eventoId: Int, fecha: LocalDate, presente: Boolean?) {
        if (presente == null) carreraDao.borrarAsistencia(eventoId, fecha)
        else carreraDao.guardarAsistencia(AsistenciaEntity(eventoId = eventoId, fecha = fecha, presente = presente))
    }

    // ---------- ¿Qué nota necesito? ----------

    override suspend fun agregarEvaluacion(materiaId: Int, nombre: String, peso: Int, nota: Double?) {
        carreraDao.insertarEvaluacion(EvaluacionEntity(materiaId = materiaId, nombre = nombre, peso = peso, nota = nota))
    }

    override suspend fun actualizarEvaluacion(materiaId: Int, evaluacion: Evaluacion) {
        carreraDao.actualizarEvaluacion(evaluacion.aEntidad(materiaId))
    }

    override suspend fun eliminarEvaluacion(materiaId: Int, evaluacion: Evaluacion) {
        carreraDao.eliminarEvaluacion(evaluacion.aEntidad(materiaId))
    }

    override suspend fun restaurarEvaluacion(materiaId: Int, evaluacion: Evaluacion) {
        carreraDao.insertarEvaluacion(evaluacion.aEntidad(materiaId))
    }

    // ---------- Mesas de finales ----------

    override suspend fun agregarFinal(usuarioId: Int, final: MesaFinal): MesaFinal {
        val id = carreraDao.insertarFinal(final.aEntidad(usuarioId).copy(id = 0))
        return final.copy(id = id.toInt())
    }

    override suspend fun actualizarFinal(usuarioId: Int, final: MesaFinal) {
        carreraDao.actualizarFinal(final.aEntidad(usuarioId))
    }

    override suspend fun eliminarFinal(usuarioId: Int, final: MesaFinal) {
        carreraDao.eliminarFinal(final.aEntidad(usuarioId))
    }

    override suspend fun restaurarFinal(usuarioId: Int, final: MesaFinal) {
        carreraDao.insertarFinal(final.aEntidad(usuarioId))
    }

    // ---------- Bibliografía ----------

    override suspend fun agregarFuente(usuarioId: Int, fuente: Fuente): Fuente {
        val id = carreraDao.insertarFuente(fuente.aEntidad(usuarioId).copy(id = 0))
        return fuente.copy(id = id.toInt())
    }

    override suspend fun actualizarFuente(usuarioId: Int, fuente: Fuente) {
        carreraDao.actualizarFuente(fuente.aEntidad(usuarioId))
    }

    override suspend fun eliminarFuente(usuarioId: Int, fuente: Fuente) {
        carreraDao.eliminarFuente(fuente.aEntidad(usuarioId))
    }

    override suspend fun restaurarFuente(usuarioId: Int, fuente: Fuente) {
        carreraDao.insertarFuente(fuente.aEntidad(usuarioId))
    }

    // ---------- Recesos ----------

    // id 0 = nuevo; si no, se actualiza.
    override suspend fun guardarReceso(usuarioId: Int, receso: Receso) {
        if (receso.id == 0) carreraDao.insertarReceso(receso.aEntidad(usuarioId)) else carreraDao.actualizarReceso(receso.aEntidad(usuarioId))
    }

    override suspend fun eliminarReceso(usuarioId: Int, receso: Receso) {
        carreraDao.eliminarReceso(receso.aEntidad(usuarioId))
    }

    // ---------- Mapas mentales ----------

    override suspend fun agregarMapa(usuarioId: Int, mapa: MapaMental): MapaMental {
        val id = carreraDao.insertarMapa(mapa.aEntidad(usuarioId).copy(id = 0))
        return mapa.copy(id = id.toInt())
    }

    override suspend fun actualizarMapa(usuarioId: Int, mapa: MapaMental) {
        carreraDao.actualizarMapa(mapa.aEntidad(usuarioId))
    }

    override suspend fun eliminarMapa(usuarioId: Int, mapa: MapaMental) {
        carreraDao.eliminarMapa(mapa.aEntidad(usuarioId))
    }

    override suspend fun restaurarMapa(usuarioId: Int, mapa: MapaMental) {
        carreraDao.insertarMapa(mapa.aEntidad(usuarioId))
    }

    // ---------- Exportar a PDF ----------

    override suspend fun exportarNotaPdf(nota: Nota, nombreMateria: String): String? =
        exportadorPdf.exportar(nota, nombreMateria, nota.fechaCreacion.format(DateTimeFormatter.ofPattern("d/M/yyyy")))

    // ---------- Calendario del teléfono ----------

    override suspend fun sincronizarCalendario(nombreCalendario: String): Int? {
        val usuario = usuarioActual.first() ?: return null
        val cantidad = calendarioTelefono.sincronizar(eventosParaCalendario(usuario, LocalDate.now()), nombreCalendario)
        if (cantidad != null) preferencias.guardarCalendarioAutomatico(true)
        return cantidad
    }

    override val calendarioAutomatico: Flow<Boolean> = preferencias.calendarioAutomatico

    override val guiasVistas: Flow<Set<Int>> = preferencias.guiasVistas
    override fun planRevisadoHasta(usuarioId: Int): Flow<LocalDate?> = preferencias.planRevisadoHasta(usuarioId)
    override suspend fun marcarPlanRevisado(usuarioId: Int, fecha: LocalDate) = preferencias.marcarPlanRevisado(usuarioId, fecha)
    override suspend fun marcarGuiaVista(usuarioId: Int) = preferencias.marcarGuiaVista(usuarioId)

    override suspend fun dejarDeSincronizarCalendario(): Boolean {
        preferencias.guardarCalendarioAutomatico(false)
        return calendarioTelefono.borrarCalendario()
    }

    override suspend fun exportarCalendarioIcs(uri: String, nombreCalendario: String): Int? {
        val usuario = usuarioActual.first() ?: return null
        val eventos = eventosParaCalendario(usuario, LocalDate.now())
        val texto = calendarioIcs(eventos, nombreCalendario, ZoneId.systemDefault(), Instant.now())
        return if (calendarioTelefono.guardarIcs(uri, texto)) eventos.size else null
    }

    // ---------- Feriados ----------

    override suspend fun actualizarFeriados(usarCalendario: Boolean): ResultadoFeriados? {
        val anio = LocalDate.now().year
        val desde = LocalDate.of(anio, 1, 1)
        val hasta = LocalDate.of(anio + 1, 12, 31)
        // Primero el calendario de Google del teléfono; si no está o no tiene
        // ninguno, internet (este año y el que viene).
        val delCalendario = if (usarCalendario) fuenteFeriados.delCalendario(desde, hasta)?.takeIf { it.isNotEmpty() } else null
        val feriados = delCalendario ?: run {
            val este = fuenteFeriados.deInternet(anio) ?: return null
            este + (fuenteFeriados.deInternet(anio + 1) ?: emptyList())
        }
        feriadoDao.reemplazar(feriados.map { it.aEntidad() })
        return ResultadoFeriados(feriados.size, delCalendario != null)
    }

    // ---------- Copia de seguridad ----------

    override suspend fun exportarCopia(uri: String): Boolean {
        val usuario = usuarioActual.first() ?: return false
        return copiaDeSeguridad.exportar(usuario, uri)
    }

    override val copiaAutomatica: Flow<EstadoCopiaAutomatica> = preferencias.copiaAutomatica

    override suspend fun activarCopiaAutomatica(carpeta: String): Boolean {
        if (!copiaDeSeguridad.conservarPermiso(carpeta)) return false
        preferencias.copiaAutomatica.first().carpeta?.takeIf { it != carpeta }?.let { copiaDeSeguridad.soltarPermiso(it) }
        preferencias.guardarCarpetaCopia(carpeta)
        return hacerCopiaAutomatica()
    }

    override suspend fun desactivarCopiaAutomatica() {
        preferencias.copiaAutomatica.first().carpeta?.let { copiaDeSeguridad.soltarPermiso(it) }
        preferencias.guardarCarpetaCopia(null)
    }

    override suspend fun hacerCopiaAutomatica(): Boolean {
        val carpeta = preferencias.copiaAutomatica.first().carpeta ?: return false
        val usuario = usuarioActual.first() ?: return false
        val hoy = LocalDate.now()
        val ok = copiaDeSeguridad.exportarACarpeta(usuario, carpeta, hoy)
        if (ok) preferencias.guardarUltimaCopia(hoy)
        return ok
    }

    // Reemplaza las materias del usuario por las de la copia y trae su
    // perfil y su foto. Las materias viejas se borran (con CASCADE); sus
    // archivos en disco los limpia limpiarHuerfanos la próxima vez que abre la app.
    override suspend fun importarCopia(uri: String): Boolean {
        val usuario = usuarioActual.first() ?: return false
        val copia = copiaDeSeguridad.importar(uri) ?: return false
        usuario.materias.forEach { materiaDao.eliminar(it.aEntidad(usuario.id)) }
        usuario.planEstudio.forEach { carreraDao.eliminarMateriaPlan(it.aEntidad(usuario.id)) }
        usuario.finales.forEach { carreraDao.eliminarFinal(it.aEntidad(usuario.id)) }
        usuario.fuentes.forEach { carreraDao.eliminarFuente(it.aEntidad(usuario.id)) }
        usuario.mapas.forEach { carreraDao.eliminarMapa(it.aEntidad(usuario.id)) }
        usuario.recesos.forEach { carreraDao.eliminarReceso(it.aEntidad(usuario.id)) }
        usuarioDao.actualizarPerfil(usuario.id, copia.nombre, copia.carrera, copia.anio, copia.email)
        usuarioDao.actualizarMetaSemanal(usuario.id, copia.metaHorasSemanales)
        usuarioDao.actualizarFotoPerfil(usuario.id, copia.fotoPerfil)
        insertarMaterias(usuario.id, copia.materias)
        insertarCarrera(usuario.id, copia.planEstudio, copia.finales, copia.fuentes, copia.mapas, copia.recesos)
        return true
    }
}
