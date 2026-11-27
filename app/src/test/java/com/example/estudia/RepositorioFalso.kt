package com.example.estudia

import com.example.estudia.datos.EventoParaAviso
import com.example.estudia.datos.NotaEncontrada
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.datos.ResumenDia
import com.example.estudia.datos.TareaEncontrada
import com.example.estudia.datos.TareaParaAviso
import com.example.estudia.modelo.EstadoCopiaAutomatica
import com.example.estudia.modelo.calendarioIcs
import com.example.estudia.modelo.eventosParaCalendario
import com.example.estudia.modelo.Tema
import com.example.estudia.modelo.Tarjeta
import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.CategoriaArchivo
import com.example.estudia.modelo.Feriado
import com.example.estudia.datos.ResultadoFeriados
import com.example.estudia.modelo.Asistencia
import com.example.estudia.modelo.Evaluacion
import com.example.estudia.modelo.MateriaPlan
import com.example.estudia.modelo.MesaFinal
import com.example.estudia.modelo.Fuente
import com.example.estudia.modelo.MapaMental
import com.example.estudia.modelo.Calificacion
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MetodoAuth
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.VersionNota
import com.example.estudia.modelo.Receso
import com.example.estudia.modelo.siguienteTareaRepetida
import com.example.estudia.modelo.cambioParaHistorial
import com.example.estudia.modelo.PreferenciaTema
import com.example.estudia.modelo.Prioridad
import com.example.estudia.modelo.SesionEstudio
import com.example.estudia.modelo.Subtarea
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// Repositorio falso para los tests de los ViewModels: guarda todo en
// memoria (sin Room, sin DataStore, sin emulador). Es posible gracias a
// que RepositorioEstudia es una interfaz (apunte de MVVM, cap. 5.6).
class RepositorioFalso(
    usuarioInicial: Usuario? = null,
    private val usuariosRegistrados: MutableMap<String, String> = mutableMapOf("demo" to "demo")
) : RepositorioEstudia {

    val usuario = MutableStateFlow(usuarioInicial)
    private val recordatorios = MutableStateFlow(false)
    private val temaElegido = MutableStateFlow(PreferenciaTema.SISTEMA)
    val permisoArchivos = MutableStateFlow(false)
    private var siguienteId = 1000

    override val usuarioActual: Flow<Usuario?> = usuario
    override val recordatoriosActivos: Flow<Boolean> = recordatorios
    override val tema: Flow<PreferenciaTema> = temaElegido
    override val permisoArchivosPedido: Flow<Boolean> = permisoArchivos
    val silenciar = MutableStateFlow(false)
    override val silenciarAlEstudiar: Flow<Boolean> = silenciar
    val metaDiaria = MutableStateFlow(0)
    override val metaDiariaMinutos: Flow<Int> = metaDiaria
    override suspend fun cambiarMetaDiaria(minutos: Int) { metaDiaria.value = minutos }
    val avisarSalir = MutableStateFlow(true)
    override val avisarAlSalir: Flow<Boolean> = avisarSalir
    override suspend fun cambiarAvisarAlSalir(activo: Boolean) { avisarSalir.value = activo }
    val mejoresTiempos = MutableStateFlow<Map<Int, Long>>(emptyMap())
    override fun mejorTiempoPares(materiaId: Int): Flow<Long?> = mejoresTiempos.map { it[materiaId] }
    override suspend fun guardarMejorTiempoPares(materiaId: Int, millis: Long) { mejoresTiempos.value = mejoresTiempos.value + (materiaId to millis) }

    // Feriados: "feriadosParaTraer" es lo que devolvería la fuente (null = falla).
    val feriadosGuardados = MutableStateFlow<List<Feriado>>(emptyList())
    override val feriados: Flow<List<Feriado>> = feriadosGuardados
    var feriadosParaTraer: List<Feriado>? = emptyList()
    var feriadosEnElCalendario = false

    override suspend fun actualizarFeriados(usarCalendario: Boolean): ResultadoFeriados? {
        val lista = feriadosParaTraer ?: return null
        feriadosGuardados.value = lista
        return ResultadoFeriados(lista.size, usarCalendario && feriadosEnElCalendario)
    }

    override fun buscarNotas(usuarioId: Int, texto: String): Flow<List<NotaEncontrada>> = usuario.map { u ->
        u?.materias.orEmpty().flatMap { m ->
            m.notas.filter { it.titulo.contains(texto, true) || it.contenido.contains(texto, true) }
                .map { NotaEncontrada(it, m.id, m.nombre) }
        }
    }

    override fun buscarTareas(usuarioId: Int, texto: String): Flow<List<TareaEncontrada>> = usuario.map { u ->
        u?.materias.orEmpty().flatMap { m ->
            m.tareas.filter { it.titulo.contains(texto, true) }.map { TareaEncontrada(it, m.id, m.nombre, m.color) }
        }
    }

    // ---------- Sesión ----------

    override suspend fun crearDatosDePruebaSiHaceFalta() {}

    override suspend fun iniciarSesion(nombreUsuario: String, contrasena: String): Boolean {
        if (usuariosRegistrados[nombreUsuario] != contrasena) return false
        usuario.value = Usuario(id = 1, nombreUsuario = nombreUsuario, metodoAuth = MetodoAuth.LOCAL, nombre = nombreUsuario)
        return true
    }

    override suspend fun registrarUsuario(nombre: String, nombreUsuario: String, contrasena: String): Boolean {
        if (usuariosRegistrados.containsKey(nombreUsuario)) return false
        usuariosRegistrados[nombreUsuario] = contrasena
        usuario.value = Usuario(id = 2, nombreUsuario = nombreUsuario, metodoAuth = MetodoAuth.LOCAL, nombre = nombre)
        return true
    }

    override suspend fun cerrarSesion() {
        usuario.value = null
    }

    override suspend fun editarPerfil(usuarioId: Int, nombre: String, carrera: String?, anio: Int?, email: String?) {
        usuario.value = usuario.value?.copy(nombre = nombre, carrera = carrera, anio = anio, email = email)
    }

    override suspend fun cambiarMetaSemanal(usuarioId: Int, horas: Int?) {
        usuario.value = usuario.value?.copy(metaHorasSemanales = horas)
    }

    // La "ruta" guardada es la misma uri, así los tests pueden comprobarla.
    override suspend fun cambiarFotoPerfil(usuarioId: Int, uri: String?): Boolean {
        usuario.value = usuario.value?.copy(fotoPerfil = uri)
        return true
    }

    override fun uriDeFoto(ruta: String): String = ruta

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
        cambiarMaterias {
            it + Materia(
                siguienteId++, nombre, color,
                asistenciaMinima = asistenciaMinima, promocional = promocional,
                notaPromocion = notaPromocion, tieneFinal = tieneFinal
            )
        }
    }

    override suspend fun actualizarMateria(usuarioId: Int, materia: Materia) {
        cambiarMaterias { lista -> lista.map { if (it.id == materia.id) materia else it } }
    }

    override suspend fun eliminarMateria(usuarioId: Int, materia: Materia) {
        cambiarMaterias { lista -> lista.filter { it.id != materia.id } }
    }

    override suspend fun restaurarMateria(usuarioId: Int, materia: Materia) {
        cambiarMaterias { lista -> lista + materia }
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
        val pasos = subtareas.map { Subtarea(siguienteId++, it) }
        val nueva = Tarea(siguienteId++, titulo, fechaLimite, prioridad = prioridad, subtareas = pasos, avisoDiasAntes = avisoDiasAntes)
        cambiarMateria(materiaId) { it.copy(tareas = it.tareas + nueva) }
        return nueva
    }

    // Igual que en Room: solo cambia la tarea, sus subtareas quedan como estaban.
    // Como el real: si se completa una que se repite, crea la siguiente.
    override suspend fun actualizarTarea(materiaId: Int, tarea: Tarea): Tarea? {
        val anterior = usuario.value?.materias?.firstOrNull { it.id == materiaId }?.tareas?.firstOrNull { it.id == tarea.id }
        val siguiente = siguienteTareaRepetida(anterior, tarea.copy(subtareas = anterior?.subtareas ?: tarea.subtareas), LocalDate.now())
        val guardada = if (siguiente != null) tarea.copy(repeticion = null) else tarea
        cambiarMateria(materiaId) { m -> m.copy(tareas = m.tareas.map { if (it.id == tarea.id) guardada.copy(subtareas = it.subtareas) else it }) }
        if (siguiente == null) return null
        val creada = siguiente.copy(id = siguienteId++, subtareas = siguiente.subtareas.map { it.copy(id = siguienteId++) })
        cambiarMateria(materiaId) { it.copy(tareas = it.tareas + creada) }
        return creada
    }

    override suspend fun guardarSubtareas(tareaId: Int, subtareas: List<Subtarea>) =
        cambiarTarea(tareaId) { t -> t.copy(subtareas = subtareas.map { if (it.id == 0) it.copy(id = siguienteId++) else it }) }

    override suspend fun alternarSubtarea(tareaId: Int, subtarea: Subtarea) =
        cambiarTarea(tareaId) { t ->
            t.copy(subtareas = t.subtareas.map { if (it.id == subtarea.id) it.copy(completada = !it.completada) else it })
        }

    override suspend fun eliminarTarea(materiaId: Int, tarea: Tarea) =
        cambiarMateria(materiaId) { m -> m.copy(tareas = m.tareas.filter { it.id != tarea.id }) }

    override suspend fun restaurarTarea(materiaId: Int, tarea: Tarea) =
        cambiarMateria(materiaId) { it.copy(tareas = it.tareas + tarea) }

    // ---------- Calificaciones ----------

    override suspend fun agregarCalificacion(materiaId: Int, titulo: String, nota: Double, fecha: LocalDate) =
        cambiarMateria(materiaId) { it.copy(calificaciones = it.calificaciones + Calificacion(siguienteId++, titulo, nota, fecha)) }

    override suspend fun actualizarCalificacion(materiaId: Int, calificacion: Calificacion) =
        cambiarMateria(materiaId) { m -> m.copy(calificaciones = m.calificaciones.map { if (it.id == calificacion.id) calificacion else it }) }

    override suspend fun eliminarCalificacion(materiaId: Int, calificacion: Calificacion) =
        cambiarMateria(materiaId) { m -> m.copy(calificaciones = m.calificaciones.filter { it.id != calificacion.id }) }

    override suspend fun restaurarCalificacion(materiaId: Int, calificacion: Calificacion) =
        cambiarMateria(materiaId) { it.copy(calificaciones = it.calificaciones + calificacion) }

    // ---------- Tarjetas de estudio ----------

    override suspend fun agregarTarjeta(materiaId: Int, pregunta: String, respuesta: String, imagen: String?) =
        cambiarMateria(materiaId) { it.copy(tarjetas = it.tarjetas + Tarjeta(siguienteId++, pregunta, respuesta, proximoRepaso = LocalDate.now(), imagen = imagen)) }

    override suspend fun actualizarTarjeta(materiaId: Int, tarjeta: Tarjeta) =
        cambiarMateria(materiaId) { m -> m.copy(tarjetas = m.tarjetas.map { if (it.id == tarjeta.id) tarjeta else it }) }

    override suspend fun eliminarTarjeta(materiaId: Int, tarjeta: Tarjeta) =
        cambiarMateria(materiaId) { m -> m.copy(tarjetas = m.tarjetas.filter { it.id != tarjeta.id }) }

    override suspend fun restaurarTarjeta(materiaId: Int, tarjeta: Tarjeta) =
        cambiarMateria(materiaId) { it.copy(tarjetas = it.tarjetas + tarjeta) }

    // ---------- Eventos ----------

    override suspend fun agregarEvento(materiaId: Int, evento: Evento): Evento {
        val guardado = evento.copy(id = siguienteId++)
        cambiarMateria(materiaId) { it.copy(eventos = it.eventos + guardado) }
        return guardado
    }

    override suspend fun actualizarEvento(materiaId: Int, evento: Evento) =
        cambiarMateria(materiaId) { m -> m.copy(eventos = m.eventos.map { if (it.id == evento.id) evento else it }) }

    override suspend fun eliminarEvento(materiaId: Int, evento: Evento) =
        cambiarMateria(materiaId) { m -> m.copy(eventos = m.eventos.filter { it.id != evento.id }) }

    override suspend fun restaurarEvento(materiaId: Int, evento: Evento) =
        cambiarMateria(materiaId) { it.copy(eventos = it.eventos + evento) }

    // ---------- Temario ----------

    override suspend fun agregarTema(eventoId: Int, titulo: String) =
        cambiarEvento(eventoId) { it.copy(temas = it.temas + Tema(siguienteId++, titulo)) }

    override suspend fun alternarTema(eventoId: Int, tema: Tema) =
        cambiarEvento(eventoId) { e -> e.copy(temas = e.temas.map { if (it.id == tema.id) it.copy(estudiado = !it.estudiado) else it }) }

    override suspend fun eliminarTema(eventoId: Int, tema: Tema) =
        cambiarEvento(eventoId) { e -> e.copy(temas = e.temas.filter { it.id != tema.id }) }

    override suspend fun restaurarTema(eventoId: Int, tema: Tema) =
        cambiarEvento(eventoId) { it.copy(temas = it.temas + tema) }

    // ---------- Notas ----------

    override suspend fun agregarNota(materiaId: Int, titulo: String, contenido: String) =
        cambiarMateria(materiaId) { it.copy(notas = it.notas + Nota(siguienteId++, titulo, contenido, LocalDate.now())) }

    // Historial de cada nota (lo más nuevo primero), como en el repositorio real.
    val versiones = MutableStateFlow<Map<Int, List<VersionNota>>>(emptyMap())

    override suspend fun actualizarNota(materiaId: Int, nota: Nota) {
        val anterior = usuario.value?.materias?.flatMap { it.notas }?.firstOrNull { it.id == nota.id }
        if (anterior != null && cambioParaHistorial(anterior.titulo, anterior.contenido, nota.titulo, nota.contenido)) {
            val version = VersionNota(siguienteId++, anterior.titulo, anterior.contenido, java.time.LocalDateTime.now())
            versiones.value = versiones.value + (nota.id to (listOf(version) + versiones.value[nota.id].orEmpty()))
        }
        cambiarMateria(materiaId) { m -> m.copy(notas = m.notas.map { if (it.id == nota.id) nota else it }) }
    }

    override fun versionesDeNota(notaId: Int): Flow<List<VersionNota>> = versiones.map { it[notaId].orEmpty() }

    override suspend fun eliminarNota(materiaId: Int, nota: Nota) =
        cambiarMateria(materiaId) { m -> m.copy(notas = m.notas.filter { it.id != nota.id }) }

    override suspend fun restaurarNota(materiaId: Int, nota: Nota) =
        cambiarMateria(materiaId) { it.copy(notas = it.notas + nota) }

    // ---------- Archivos ----------

    val archivosBorradosDelDisco = mutableListOf<Archivo>()

    var ultimaCategoriaAdjuntada: CategoriaArchivo? = null

    override suspend fun adjuntarArchivo(materiaId: Int, notaId: Int?, uri: String, categoria: CategoriaArchivo): Boolean {
        ultimaCategoriaAdjuntada = categoria
        return true
    }

    override suspend fun cambiarCategoriaArchivo(materiaId: Int, archivo: Archivo, categoria: CategoriaArchivo) =
        cambiarMateria(materiaId) { m -> m.copy(archivos = m.archivos.map { if (it.id == archivo.id) it.copy(categoria = categoria) else it }) }

    override suspend fun eliminarArchivo(materiaId: Int, archivo: Archivo) =
        cambiarMateria(materiaId) { m -> m.copy(archivos = m.archivos.filter { it.id != archivo.id }) }

    override suspend fun restaurarArchivo(materiaId: Int, archivo: Archivo) =
        cambiarMateria(materiaId) { it.copy(archivos = it.archivos + archivo) }

    override suspend fun borrarArchivosDelDisco(archivos: List<Archivo>) {
        archivosBorradosDelDisco += archivos
    }

    override fun uriParaAbrir(archivo: Archivo): String = throw UnsupportedOperationException("No se usa en los tests")

    // Lo que "lee" el OCR falso: null = la imagen no se pudo leer.
    var textoDeLaFoto: String? = ""

    override suspend fun leerTextoDeImagen(archivo: Archivo): String? = textoDeLaFoto

    // ---------- Mi carrera ----------

    override suspend fun agregarMateriaPlan(usuarioId: Int, materia: MateriaPlan) =
        cambiarUsuario { it.copy(planEstudio = it.planEstudio + materia.copy(id = siguienteId++)) }

    override suspend fun actualizarMateriaPlan(usuarioId: Int, materia: MateriaPlan) =
        cambiarUsuario { u -> u.copy(planEstudio = u.planEstudio.map { if (it.id == materia.id) materia else it }) }

    override suspend fun eliminarMateriaPlan(usuarioId: Int, materia: MateriaPlan) =
        cambiarUsuario { u -> u.copy(planEstudio = u.planEstudio.filter { it.id != materia.id }) }

    override suspend fun restaurarMateriaPlan(usuarioId: Int, materia: MateriaPlan) =
        cambiarUsuario { it.copy(planEstudio = it.planEstudio + materia) }

    // ---------- Asistencia ----------

    override suspend fun marcarAsistencia(eventoId: Int, fecha: LocalDate, presente: Boolean?) =
        cambiarEvento(eventoId) { e ->
            val otras = e.asistencias.filter { it.fecha != fecha }
            e.copy(asistencias = if (presente == null) otras else otras + Asistencia(siguienteId++, fecha, presente))
        }

    // ---------- ¿Qué nota necesito? ----------

    override suspend fun agregarEvaluacion(materiaId: Int, nombre: String, peso: Int, nota: Double?) =
        cambiarMateria(materiaId) { it.copy(evaluaciones = it.evaluaciones + Evaluacion(siguienteId++, nombre, peso, nota)) }

    override suspend fun actualizarEvaluacion(materiaId: Int, evaluacion: Evaluacion) =
        cambiarMateria(materiaId) { m -> m.copy(evaluaciones = m.evaluaciones.map { if (it.id == evaluacion.id) evaluacion else it }) }

    override suspend fun eliminarEvaluacion(materiaId: Int, evaluacion: Evaluacion) =
        cambiarMateria(materiaId) { m -> m.copy(evaluaciones = m.evaluaciones.filter { it.id != evaluacion.id }) }

    override suspend fun restaurarEvaluacion(materiaId: Int, evaluacion: Evaluacion) =
        cambiarMateria(materiaId) { it.copy(evaluaciones = it.evaluaciones + evaluacion) }

    // ---------- Finales ----------

    override suspend fun agregarFinal(usuarioId: Int, final: MesaFinal): MesaFinal {
        val guardado = final.copy(id = siguienteId++)
        cambiarUsuario { it.copy(finales = it.finales + guardado) }
        return guardado
    }

    override suspend fun actualizarFinal(usuarioId: Int, final: MesaFinal) =
        cambiarUsuario { u -> u.copy(finales = u.finales.map { if (it.id == final.id) final else it }) }

    override suspend fun eliminarFinal(usuarioId: Int, final: MesaFinal) =
        cambiarUsuario { u -> u.copy(finales = u.finales.filter { it.id != final.id }) }

    override suspend fun restaurarFinal(usuarioId: Int, final: MesaFinal) =
        cambiarUsuario { it.copy(finales = it.finales + final) }

    // ---------- Bibliografía ----------

    override suspend fun agregarFuente(usuarioId: Int, fuente: Fuente): Fuente {
        val guardada = fuente.copy(id = siguienteId++)
        cambiarUsuario { it.copy(fuentes = it.fuentes + guardada) }
        return guardada
    }

    override suspend fun actualizarFuente(usuarioId: Int, fuente: Fuente) =
        cambiarUsuario { u -> u.copy(fuentes = u.fuentes.map { if (it.id == fuente.id) fuente else it }) }

    override suspend fun eliminarFuente(usuarioId: Int, fuente: Fuente) =
        cambiarUsuario { u -> u.copy(fuentes = u.fuentes.filter { it.id != fuente.id }) }

    override suspend fun restaurarFuente(usuarioId: Int, fuente: Fuente) =
        cambiarUsuario { it.copy(fuentes = it.fuentes + fuente) }

    override suspend fun guardarReceso(usuarioId: Int, receso: Receso) = cambiarUsuario { u ->
        if (receso.id == 0) u.copy(recesos = u.recesos + receso.copy(id = siguienteId++))
        else u.copy(recesos = u.recesos.map { if (it.id == receso.id) receso else it })
    }

    override suspend fun eliminarReceso(usuarioId: Int, receso: Receso) =
        cambiarUsuario { u -> u.copy(recesos = u.recesos.filter { it.id != receso.id }) }

    // ---------- Mapas mentales ----------

    override suspend fun agregarMapa(usuarioId: Int, mapa: MapaMental): MapaMental {
        val guardado = mapa.copy(id = siguienteId++)
        cambiarUsuario { it.copy(mapas = it.mapas + guardado) }
        return guardado
    }

    override suspend fun actualizarMapa(usuarioId: Int, mapa: MapaMental) =
        cambiarUsuario { u -> u.copy(mapas = u.mapas.map { if (it.id == mapa.id) mapa else it }) }

    override suspend fun eliminarMapa(usuarioId: Int, mapa: MapaMental) =
        cambiarUsuario { u -> u.copy(mapas = u.mapas.filter { it.id != mapa.id }) }

    override suspend fun restaurarMapa(usuarioId: Int, mapa: MapaMental) =
        cambiarUsuario { it.copy(mapas = it.mapas + mapa) }

    // ---------- PDF ----------

    var pdfExportado: String? = "content://pdf/nota.pdf"
    override suspend fun exportarNotaPdf(nota: Nota, nombreMateria: String): String? = pdfExportado

    // ---------- Calendario del teléfono ----------
    // Cuenta los eventos que se pasarían (sin bloques de estudio).

    val calendarioAuto = MutableStateFlow(false)
    var icsGuardado: String? = null
    override suspend fun sincronizarCalendario(nombreCalendario: String): com.example.estudia.modelo.ResultadoCalendario? {
        val u = usuario.value ?: return null
        calendarioAuto.value = true
        return com.example.estudia.modelo.ResultadoCalendario(eventosParaCalendario(u, LocalDate.now()).size, emptyList())
    }
    val destinoCalendario = MutableStateFlow<Long?>(null)
    override val calendarioDestino: Flow<Long?> = destinoCalendario
    var calendariosGoogle: List<com.example.estudia.modelo.CalendarioDisponible> = emptyList()
    override suspend fun calendariosDeGoogle() = calendariosGoogle
    override suspend fun cambiarCalendarioDestino(destino: Long?, nombreCalendario: String): com.example.estudia.modelo.ResultadoCalendario? {
        destinoCalendario.value = destino
        return sincronizarCalendario(nombreCalendario)
    }
    override val calendarioAutomatico: Flow<Boolean> = calendarioAuto
    override suspend fun dejarDeSincronizarCalendario(): Boolean {
        calendarioAuto.value = false
        return true
    }
    override suspend fun exportarCalendarioIcs(uri: String, nombreCalendario: String): Int? {
        val u = usuario.value ?: return null
        val eventos = eventosParaCalendario(u, LocalDate.now())
        icsGuardado = calendarioIcs(eventos, nombreCalendario, java.time.ZoneId.of("America/Argentina/Buenos_Aires"), java.time.Instant.EPOCH)
        return eventos.size
    }

    // ---------- Guía de primera vez ----------

    val planRevisado = MutableStateFlow<LocalDate?>(null)
    override fun planRevisadoHasta(usuarioId: Int): Flow<LocalDate?> = planRevisado
    override suspend fun marcarPlanRevisado(usuarioId: Int, fecha: LocalDate) {
        planRevisado.value = fecha
    }

    val guias = MutableStateFlow<Set<Int>>(emptySet())
    override val guiasVistas: Flow<Set<Int>> = guias
    override suspend fun marcarGuiaVista(usuarioId: Int) {
        guias.value = guias.value + usuarioId
    }

    // ---------- Copia de seguridad ----------
    // "Guarda" la copia en memoria y la restaura reemplazando las materias.

    private var copiaGuardada: Usuario? = null

    override suspend fun exportarCopia(uri: String): Boolean {
        copiaGuardada = usuario.value ?: return false
        return true
    }

    override suspend fun importarCopia(uri: String): Boolean {
        val copia = copiaGuardada ?: return false
        usuario.value = usuario.value?.copy(materias = copia.materias, nombre = copia.nombre)
        return true
    }

    val copiaAuto = MutableStateFlow(EstadoCopiaAutomatica())
    var carpetaValida = true
    override val copiaAutomatica: Flow<EstadoCopiaAutomatica> = copiaAuto
    override suspend fun activarCopiaAutomatica(carpeta: String): Boolean {
        if (!carpetaValida) return false
        copiaAuto.value = EstadoCopiaAutomatica(carpeta)
        return hacerCopiaAutomatica()
    }
    override suspend fun desactivarCopiaAutomatica() {
        copiaAuto.value = EstadoCopiaAutomatica()
    }
    override suspend fun hacerCopiaAutomatica(): Boolean {
        if (copiaAuto.value.carpeta == null || usuario.value == null) return false
        copiaAuto.value = copiaAuto.value.copy(ultima = LocalDate.of(2026, 11, 26))
        return true
    }

    // ---------- Preferencias ----------

    override suspend fun cambiarRecordatorios(activos: Boolean) {
        recordatorios.value = activos
    }

    override suspend fun cambiarTema(tema: PreferenciaTema) {
        temaElegido.value = tema
    }

    override suspend fun marcarPermisoArchivosPedido() {
        permisoArchivos.value = true
    }

    override suspend fun cambiarSilenciarAlEstudiar(activo: Boolean) {
        silenciar.value = activo
    }

    // ---------- Temporizador ----------
    // "reloj" lo controla cada test (en milisegundos), en lugar de la hora real.

    var reloj = 0L
    val sesion = MutableStateFlow<SesionEstudio?>(null)
    override val sesionEstudio: Flow<SesionEstudio?> = sesion

    override suspend fun empezarSesion(materia: Materia, minutos: Int): SesionEstudio {
        sesion.value = SesionEstudio.nueva(materia, minutos, reloj)
        return sesion.value!!
    }

    override suspend fun pausarSesion(): SesionEstudio? {
        sesion.value = sesion.value?.pausar(reloj)
        return sesion.value
    }

    override suspend fun reanudarSesion(): SesionEstudio? {
        sesion.value = sesion.value?.reanudar(reloj)
        return sesion.value
    }

    override suspend fun terminarSesion(guardar: Boolean): Int {
        val actual = sesion.value ?: return 0
        val minutos = if (guardar) actual.minutosEstudiados(reloj) else 0
        if (minutos > 0) guardarBloque(actual.materiaId, minutos)
        sesion.value = null
        return minutos
    }

    override suspend fun completarSesionSiTermino(): SesionEstudio? {
        val actual = sesion.value?.takeIf { it.terminada(reloj) } ?: return null
        guardarBloque(actual.materiaId, actual.minutos)
        sesion.value = null
        return actual
    }

    private suspend fun guardarBloque(materiaId: Int, minutos: Int) {
        val inicio = LocalTime.of(10, 0)
        agregarEvento(
            materiaId,
            Evento(0, "Sesión de estudio", TipoEvento.BLOQUE_ESTUDIO, LocalDate.now(), inicio, inicio.plusMinutes(minutos.toLong()))
        )
    }

    // ---------- Para los Workers (no los usan los tests de ViewModels) ----------

    override suspend fun resumenDelDia(hoy: LocalDate): ResumenDia? = null

    override suspend fun eventoParaRecordatorio(eventoId: Int, ahora: LocalDateTime): EventoParaAviso? = null

    override suspend fun buscarEvento(eventoId: Int): Evento? =
        usuario.value?.materias?.flatMap { it.eventos }?.firstOrNull { it.id == eventoId }

    override suspend fun tareaParaRecordatorio(tareaId: Int): TareaParaAviso? = null

    override suspend fun rachaEnRiesgo(hoy: LocalDate): Int? = null

    // ---------- Ayudantes ----------

    private fun cambiarUsuario(cambio: (Usuario) -> Usuario) {
        usuario.value = usuario.value?.let(cambio)
    }

    private fun cambiarMaterias(cambio: (List<Materia>) -> List<Materia>) {
        usuario.value = usuario.value?.let { it.copy(materias = cambio(it.materias)) }
    }

    private fun cambiarMateria(materiaId: Int, cambio: (Materia) -> Materia) {
        cambiarMaterias { lista -> lista.map { if (it.id == materiaId) cambio(it) else it } }
    }

    private fun cambiarEvento(eventoId: Int, cambio: (Evento) -> Evento) {
        cambiarMaterias { lista -> lista.map { m -> m.copy(eventos = m.eventos.map { if (it.id == eventoId) cambio(it) else it }) } }
    }

    private fun cambiarTarea(tareaId: Int, cambio: (Tarea) -> Tarea) {
        cambiarMaterias { lista -> lista.map { m -> m.copy(tareas = m.tareas.map { if (it.id == tareaId) cambio(it) else it }) } }
    }
}
