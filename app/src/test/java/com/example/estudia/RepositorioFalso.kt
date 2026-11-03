package com.example.estudia

import com.example.estudia.datos.EventoParaAviso
import com.example.estudia.datos.NotaEncontrada
import com.example.estudia.datos.RepositorioEstudia
import com.example.estudia.datos.ResumenDia
import com.example.estudia.datos.TareaEncontrada
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

    // ---------- Materias ----------

    override suspend fun agregarMateria(usuarioId: Int, nombre: String, color: Long) {
        cambiarMaterias { it + Materia(siguienteId++, nombre, color) }
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

    override suspend fun agregarTarea(materiaId: Int, titulo: String, fechaLimite: LocalDate?) =
        cambiarMateria(materiaId) { it.copy(tareas = it.tareas + Tarea(siguienteId++, titulo, fechaLimite)) }

    override suspend fun actualizarTarea(materiaId: Int, tarea: Tarea) =
        cambiarMateria(materiaId) { m -> m.copy(tareas = m.tareas.map { if (it.id == tarea.id) tarea else it }) }

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

    // ---------- Notas ----------

    override suspend fun agregarNota(materiaId: Int, titulo: String, contenido: String) =
        cambiarMateria(materiaId) { it.copy(notas = it.notas + Nota(siguienteId++, titulo, contenido, LocalDate.now())) }

    override suspend fun actualizarNota(materiaId: Int, nota: Nota) =
        cambiarMateria(materiaId) { m -> m.copy(notas = m.notas.map { if (it.id == nota.id) nota else it }) }

    override suspend fun eliminarNota(materiaId: Int, nota: Nota) =
        cambiarMateria(materiaId) { m -> m.copy(notas = m.notas.filter { it.id != nota.id }) }

    override suspend fun restaurarNota(materiaId: Int, nota: Nota) =
        cambiarMateria(materiaId) { it.copy(notas = it.notas + nota) }

    // ---------- Archivos ----------

    val archivosBorradosDelDisco = mutableListOf<Archivo>()

    override suspend fun adjuntarArchivo(materiaId: Int, notaId: Int?, uri: String): Boolean = true

    override suspend fun eliminarArchivo(materiaId: Int, archivo: Archivo) =
        cambiarMateria(materiaId) { m -> m.copy(archivos = m.archivos.filter { it.id != archivo.id }) }

    override suspend fun restaurarArchivo(materiaId: Int, archivo: Archivo) =
        cambiarMateria(materiaId) { it.copy(archivos = it.archivos + archivo) }

    override suspend fun borrarArchivosDelDisco(archivos: List<Archivo>) {
        archivosBorradosDelDisco += archivos
    }

    override fun uriParaAbrir(archivo: Archivo): String = throw UnsupportedOperationException("No se usa en los tests")

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

    // ---------- Ayudantes ----------

    private fun cambiarMaterias(cambio: (List<Materia>) -> List<Materia>) {
        usuario.value = usuario.value?.let { it.copy(materias = cambio(it.materias)) }
    }

    private fun cambiarMateria(materiaId: Int, cambio: (Materia) -> Materia) {
        cambiarMaterias { lista -> lista.map { if (it.id == materiaId) cambio(it) else it } }
    }
}
