package com.example.estudia.datos

import android.net.Uri
import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.PreferenciaTema
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.Usuario
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// Repository: centraliza el acceso a los datos y le oculta al ViewModel
// de dónde vienen (Room, DataStore, archivos, WorkManager).
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

    fun buscarNotas(usuarioId: Int, texto: String): Flow<List<NotaEncontrada>>
    fun buscarTareas(usuarioId: Int, texto: String): Flow<List<TareaEncontrada>>

    // ---------- Sesión y perfil ----------

    suspend fun crearDatosDePruebaSiHaceFalta()
    suspend fun iniciarSesion(nombreUsuario: String, contrasena: String): Boolean
    suspend fun registrarUsuario(nombre: String, nombreUsuario: String, contrasena: String): Boolean
    suspend fun cerrarSesion()
    suspend fun editarPerfil(usuarioId: Int, nombre: String, carrera: String?, anio: Int?, email: String?)
    suspend fun cambiarMetaSemanal(usuarioId: Int, horas: Int?)

    // ---------- Materias ----------

    suspend fun agregarMateria(usuarioId: Int, nombre: String, color: Long)
    suspend fun actualizarMateria(usuarioId: Int, materia: Materia)
    suspend fun eliminarMateria(usuarioId: Int, materia: Materia)

    // ---------- Tareas ----------

    suspend fun agregarTarea(materiaId: Int, titulo: String, fechaLimite: LocalDate?)
    suspend fun actualizarTarea(materiaId: Int, tarea: Tarea)
    suspend fun eliminarTarea(materiaId: Int, tarea: Tarea)
    suspend fun restaurarTarea(materiaId: Int, tarea: Tarea)

    // ---------- Eventos ----------

    suspend fun agregarEvento(materiaId: Int, evento: Evento)
    suspend fun actualizarEvento(materiaId: Int, evento: Evento)
    suspend fun eliminarEvento(materiaId: Int, evento: Evento)
    suspend fun restaurarEvento(materiaId: Int, evento: Evento)

    // ---------- Notas ----------

    suspend fun agregarNota(materiaId: Int, titulo: String, contenido: String)
    suspend fun actualizarNota(materiaId: Int, nota: Nota)
    // Borra la nota (y las filas de sus adjuntos) pero deja los archivos en
    // disco, por si el usuario toca "Deshacer". Ver borrarArchivosDelDisco.
    suspend fun eliminarNota(materiaId: Int, nota: Nota)
    suspend fun restaurarNota(materiaId: Int, nota: Nota)

    // ---------- Archivos adjuntos ----------

    // notaId null = adjunto de la materia. Devuelve false si no se pudo.
    suspend fun adjuntarArchivo(materiaId: Int, notaId: Int?, uri: Uri): Boolean
    suspend fun eliminarArchivo(materiaId: Int, archivo: Archivo)
    suspend fun restaurarArchivo(materiaId: Int, archivo: Archivo)
    suspend fun borrarArchivosDelDisco(archivos: List<Archivo>)
    fun uriParaAbrir(archivo: Archivo): Uri

    // ---------- Preferencias y recordatorios ----------

    suspend fun cambiarRecordatorios(activos: Boolean)
    suspend fun cambiarTema(tema: PreferenciaTema)
}

// Resultados del buscador (una nota o tarea junto con su materia).
data class NotaEncontrada(val nota: Nota, val materiaId: Int, val nombreMateria: String)

data class TareaEncontrada(val tarea: Tarea, val materiaId: Int, val nombreMateria: String, val colorMateria: Long)
