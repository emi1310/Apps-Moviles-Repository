package com.example.estudia.datos

import android.content.Context
import android.net.Uri
import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.ASISTENCIA_MINIMA
import com.example.estudia.modelo.CategoriaArchivo
import com.example.estudia.modelo.NOTA_PROMOCION
import com.example.estudia.modelo.Asistencia
import com.example.estudia.modelo.EstadoPlan
import com.example.estudia.modelo.Evaluacion
import com.example.estudia.modelo.MateriaPlan
import com.example.estudia.modelo.MesaFinal
import com.example.estudia.modelo.Fuente
import com.example.estudia.modelo.TipoFuente
import com.example.estudia.modelo.MapaMental
import com.example.estudia.modelo.codificarNodos
import com.example.estudia.modelo.decodificarNodos
import com.example.estudia.modelo.Calificacion
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.Prioridad
import com.example.estudia.modelo.Subtarea
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.Tarjeta
import com.example.estudia.modelo.Tema
import com.example.estudia.datos.local.textoATipo
import com.example.estudia.datos.local.tipoATexto
import com.example.estudia.modelo.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// Copia de seguridad: un archivo .zip que el usuario guarda donde quiera
// (con el selector del sistema, sin permisos) y después puede restaurar en
// este u otro teléfono. Adentro tiene:
//  - datos.json: el perfil y todas las materias, con sus tareas (y pasos),
//    eventos (y temarios y asistencias), notas, notas de exámenes,
//    evaluaciones, la lista de archivos, el plan de estudios, los finales,
//    la bibliografía y los mapas mentales.
//  - archivos/...: el contenido de los adjuntos y de la foto de perfil.
// La contraseña no se guarda nunca. Es una fuente de datos más del Repository.
class CopiaDeSeguridad(context: Context) {

    private val contexto = context.applicationContext
    private val carpeta = File(contexto.filesDir, "archivos")

    // Escribe la copia del usuario en la uri elegida. false si falló.
    suspend fun exportar(usuario: Usuario, uri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val salida = contexto.contentResolver.openOutputStream(Uri.parse(uri)) ?: return@withContext false
            ZipOutputStream(salida).use { zip ->
                zip.putNextEntry(ZipEntry(DATOS))
                zip.write(usuarioAJson(usuario).toString(2).toByteArray())
                zip.closeEntry()
                val rutas = usuario.materias.flatMap { it.todosLosArchivos() }.map { it.ruta } + listOfNotNull(usuario.fotoPerfil)
                for (ruta in rutas.distinct()) {
                    val archivo = File(carpeta, ruta)
                    if (!archivo.exists()) continue
                    zip.putNextEntry(ZipEntry(CARPETA + ruta))
                    archivo.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // Lee una copia: deja los archivos en la carpeta interna de la app y
    // devuelve el usuario con sus materias (los ids son los de la copia; el
    // Repository los inserta como nuevos). null si el archivo no es una copia válida.
    suspend fun importar(uri: String): Usuario? = withContext(Dispatchers.IO) {
        try {
            val entrada = contexto.contentResolver.openInputStream(Uri.parse(uri)) ?: return@withContext null
            var json: String? = null
            carpeta.mkdirs()
            ZipInputStream(entrada).use { zip ->
                var entrada2 = zip.nextEntry
                while (entrada2 != null) {
                    when {
                        entrada2.name == DATOS -> json = zip.readBytes().decodeToString()
                        // Solo el nombre del archivo: nunca se escribe fuera de la carpeta.
                        entrada2.name.startsWith(CARPETA) && !entrada2.isDirectory -> {
                            val nombre = File(entrada2.name).name
                            File(carpeta, nombre).outputStream().use { zip.copyTo(it) }
                        }
                    }
                    entrada2 = zip.nextEntry
                }
            }
            json?.let { jsonAUsuario(JSONObject(it)) }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val DATOS = "datos.json"
        private const val CARPETA = "archivos/"
    }
}

// ---------- Modelo -> JSON ----------

// Versión del formato de la copia, por si cambia en el futuro.
private const val VERSION_COPIA = 1

private fun usuarioAJson(u: Usuario) = JSONObject()
    .put("version", VERSION_COPIA)
    .put("nombre", u.nombre)
    .put("carrera", u.carrera ?: JSONObject.NULL)
    .put("anio", u.anio ?: JSONObject.NULL)
    .put("email", u.email ?: JSONObject.NULL)
    .put("metaHorasSemanales", u.metaHorasSemanales ?: JSONObject.NULL)
    .put("fotoPerfil", u.fotoPerfil ?: JSONObject.NULL)
    .put("materias", JSONArray(u.materias.map { materiaAJson(it) }))
    // Versión 9: plan de estudios (con su id, para las correlativas) y finales.
    .put("planEstudio", JSONArray(u.planEstudio.map { p ->
        JSONObject().put("id", p.id).put("nombre", p.nombre).put("anio", p.anio).put("estado", p.estado.name)
            .put("correlativas", JSONArray(p.correlativas)).put("notaFinal", p.notaFinal ?: JSONObject.NULL)
    }))
    .put("finales", JSONArray(u.finales.map { f ->
        JSONObject().put("materia", f.materia).put("fechaMesa", f.fechaMesa.toString())
            .put("cierreInscripcion", f.cierreInscripcion?.toString() ?: JSONObject.NULL)
            .put("vencimientoRegularidad", f.vencimientoRegularidad?.toString() ?: JSONObject.NULL)
            .put("inscripto", f.inscripto).put("nota", f.nota ?: JSONObject.NULL)
    }))
    // Versión 11: bibliografía.
    .put("fuentes", JSONArray(u.fuentes.map { f ->
        JSONObject().put("materia", f.materia).put("tipo", f.tipo.name).put("autores", f.autores)
            .put("anio", f.anio ?: JSONObject.NULL).put("titulo", f.titulo).put("origen", f.origen)
            .put("enlace", f.enlace).put("paginas", f.paginas).put("comentario", f.comentario)
    }))
    // Versión 12: mapas mentales (las ideas, como texto: ver codificarNodos).
    .put("mapas", JSONArray(u.mapas.map { m ->
        JSONObject().put("materia", m.materia).put("titulo", m.titulo)
            .put("nodos", codificarNodos(m.nodos)).put("modificado", m.modificado.toString())
    }))

private fun materiaAJson(m: Materia) = JSONObject()
    .put("id", m.id)
    .put("nombre", m.nombre)
    .put("color", m.color)
    .put("archivada", m.archivada)
    .put("asistenciaMinima", m.asistenciaMinima)
    .put("promocional", m.promocional)
    .put("notaPromocion", m.notaPromocion)
    .put("tieneFinal", m.tieneFinal)
    .put("tareas", JSONArray(m.tareas.map { t ->
        JSONObject()
            .put("titulo", t.titulo)
            .put("fechaLimite", t.fechaLimite?.toString() ?: JSONObject.NULL)
            .put("completada", t.completada)
            .put("prioridad", t.prioridad.peso)
            .put("avisoDiasAntes", t.avisoDiasAntes ?: JSONObject.NULL)
            .put("subtareas", JSONArray(t.subtareas.map { JSONObject().put("titulo", it.titulo).put("completada", it.completada) }))
    }))
    .put("eventos", JSONArray(m.eventos.map { e ->
        JSONObject()
            .put("titulo", e.titulo)
            .put("tipo", tipoATexto(e.tipo))
            .put("fecha", e.fecha.toString())
            .put("horaInicio", e.horaInicio.toString())
            .put("horaFin", e.horaFin.toString())
            .put("semanal", e.semanal)
            .put("repetirHasta", e.repetirHasta?.toString() ?: JSONObject.NULL)
            .put("temas", JSONArray(e.temas.map { JSONObject().put("titulo", it.titulo).put("estudiado", it.estudiado) }))
            .put("asistencias", JSONArray(e.asistencias.map { JSONObject().put("fecha", it.fecha.toString()).put("presente", it.presente) }))
            .put("virtual", e.virtual)
            .put("lugar", e.lugar ?: JSONObject.NULL)
            .put("enlace", e.enlace ?: JSONObject.NULL)
    }))
    .put("notas", JSONArray(m.notas.map { n ->
        JSONObject()
            .put("id", n.id)
            .put("titulo", n.titulo)
            .put("contenido", n.contenido)
            .put("fechaCreacion", n.fechaCreacion.toString())
            .put("esFavorita", n.esFavorita)
    }))
    .put("archivos", JSONArray(m.todosLosArchivos().map { a ->
        JSONObject()
            .put("nombre", a.nombre)
            .put("tipoMime", a.tipoMime)
            .put("ruta", a.ruta)
            .put("tamanoBytes", a.tamanoBytes)
            .put("fechaAgregado", a.fechaAgregado.toString())
            .put("notaId", a.notaId ?: JSONObject.NULL)
            .put("categoria", a.categoria.name)
    }))
    .put("tarjetas", JSONArray(m.tarjetas.map { t ->
        JSONObject().put("pregunta", t.pregunta).put("respuesta", t.respuesta).put("caja", t.caja).put("proximoRepaso", t.proximoRepaso.toString())
    }))
    .put("evaluaciones", JSONArray(m.evaluaciones.map { e ->
        JSONObject().put("nombre", e.nombre).put("peso", e.peso).put("nota", e.nota ?: JSONObject.NULL)
    }))
    .put("calificaciones", JSONArray(m.calificaciones.map { c ->
        JSONObject().put("titulo", c.titulo).put("nota", c.nota).put("fecha", c.fecha.toString())
    }))

// ---------- JSON -> modelo ----------
// Los ids que no hacen falta van en 0: el Repository inserta todo como nuevo.

private fun JSONObject.textoONull(clave: String): String? = if (isNull(clave)) null else getString(clave)
private fun JSONObject.enteroONull(clave: String): Int? = if (isNull(clave)) null else getInt(clave)
private fun JSONObject.doubleONull(clave: String): Double? = if (!has(clave) || isNull(clave)) null else getDouble(clave)
private fun <T> JSONArray.mapear(f: (JSONObject) -> T): List<T> = (0 until length()).map { f(getJSONObject(it)) }

private fun jsonAUsuario(j: JSONObject): Usuario = Usuario(
    id = 0,
    nombreUsuario = "",
    metodoAuth = com.example.estudia.modelo.MetodoAuth.LOCAL,
    nombre = j.getString("nombre"),
    carrera = j.textoONull("carrera"),
    anio = j.enteroONull("anio"),
    email = j.textoONull("email"),
    metaHorasSemanales = j.enteroONull("metaHorasSemanales"),
    fotoPerfil = j.textoONull("fotoPerfil")?.let { File(it).name },
    materias = j.getJSONArray("materias").mapear { jsonAMateria(it) },
    // Las copias anteriores a la versión 9 no tienen plan ni finales.
    planEstudio = j.optJSONArray("planEstudio")?.mapear { p ->
        MateriaPlan(
            id = p.getInt("id"),
            nombre = p.getString("nombre"),
            anio = p.getInt("anio"),
            estado = EstadoPlan.entries.firstOrNull { it.name == p.getString("estado") } ?: EstadoPlan.PENDIENTE,
            correlativas = p.getJSONArray("correlativas").let { a -> (0 until a.length()).map { a.getInt(it) } },
            notaFinal = p.doubleONull("notaFinal")
        )
    } ?: emptyList(),
    finales = j.optJSONArray("finales")?.mapear { f ->
        MesaFinal(
            id = 0,
            materia = f.getString("materia"),
            fechaMesa = LocalDate.parse(f.getString("fechaMesa")),
            cierreInscripcion = f.textoONull("cierreInscripcion")?.let { LocalDate.parse(it) },
            vencimientoRegularidad = f.textoONull("vencimientoRegularidad")?.let { LocalDate.parse(it) },
            inscripto = f.getBoolean("inscripto"),
            nota = f.doubleONull("nota")
        )
    } ?: emptyList(),
    // Las copias anteriores a la versión 11 no tienen bibliografía.
    fuentes = j.optJSONArray("fuentes")?.mapear { f ->
        Fuente(
            id = 0,
            materia = f.optString("materia"),
            tipo = TipoFuente.desdeTexto(f.textoONull("tipo")),
            autores = f.optString("autores"),
            anio = f.enteroONull("anio"),
            titulo = f.getString("titulo"),
            origen = f.optString("origen"),
            enlace = f.optString("enlace"),
            paginas = f.optString("paginas"),
            comentario = f.optString("comentario")
        )
    } ?: emptyList(),
    // Las copias anteriores a la versión 12 no tienen mapas.
    mapas = j.optJSONArray("mapas")?.mapear { m ->
        MapaMental(
            id = 0,
            materia = m.optString("materia"),
            titulo = m.getString("titulo"),
            nodos = decodificarNodos(m.getString("nodos")),
            modificado = m.textoONull("modificado")?.let { LocalDate.parse(it) } ?: LocalDate.now()
        )
    }?.filter { mapa -> mapa.nodos.any { it.padreId == null } } ?: emptyList()
)

private fun jsonAMateria(j: JSONObject): Materia {
    // Los archivos de las notas se reparten por notaId (el de la copia).
    val archivos = j.getJSONArray("archivos").mapear { a ->
        Archivo(
            id = 0,
            nombre = a.getString("nombre"),
            tipoMime = a.getString("tipoMime"),
            ruta = File(a.getString("ruta")).name,
            tamanoBytes = a.getLong("tamanoBytes"),
            fechaAgregado = LocalDate.parse(a.getString("fechaAgregado")),
            notaId = a.enteroONull("notaId"),
            categoria = CategoriaArchivo.desdeTexto(a.textoONull("categoria"))
        )
    }
    return Materia(
        id = j.getInt("id"),
        nombre = j.getString("nombre"),
        color = j.getLong("color"),
        archivada = j.getBoolean("archivada"),
        // Versión 10 (las copias viejas no lo tienen: quedan los valores de siempre).
        asistenciaMinima = j.optInt("asistenciaMinima", ASISTENCIA_MINIMA),
        promocional = j.optBoolean("promocional", true),
        notaPromocion = j.optDouble("notaPromocion", NOTA_PROMOCION),
        tieneFinal = j.optBoolean("tieneFinal", true),
        tareas = j.getJSONArray("tareas").mapear { t ->
            Tarea(
                id = 0,
                titulo = t.getString("titulo"),
                fechaLimite = t.textoONull("fechaLimite")?.let { LocalDate.parse(it) },
                completada = t.getBoolean("completada"),
                prioridad = Prioridad.desdePeso(t.getInt("prioridad")),
                subtareas = t.getJSONArray("subtareas").mapear { Subtarea(0, it.getString("titulo"), it.getBoolean("completada")) },
                avisoDiasAntes = t.enteroONull("avisoDiasAntes")
            )
        },
        eventos = j.getJSONArray("eventos").mapear { e ->
            Evento(
                id = 0,
                titulo = e.getString("titulo"),
                tipo = textoATipo(e.getString("tipo")),
                fecha = LocalDate.parse(e.getString("fecha")),
                horaInicio = LocalTime.parse(e.getString("horaInicio")),
                horaFin = LocalTime.parse(e.getString("horaFin")),
                semanal = e.getBoolean("semanal"),
                repetirHasta = e.textoONull("repetirHasta")?.let { LocalDate.parse(it) },
                temas = e.getJSONArray("temas").mapear { Tema(0, it.getString("titulo"), it.getBoolean("estudiado")) },
                asistencias = e.optJSONArray("asistencias")?.mapear { Asistencia(0, LocalDate.parse(it.getString("fecha")), it.getBoolean("presente")) } ?: emptyList(),
                virtual = e.optBoolean("virtual", false),
                lugar = e.textoONull("lugar"),
                enlace = e.textoONull("enlace")
            )
        },
        notas = j.getJSONArray("notas").mapear { n ->
            val idNota = n.getInt("id")
            Nota(
                id = idNota,
                titulo = n.getString("titulo"),
                contenido = n.getString("contenido"),
                fechaCreacion = LocalDate.parse(n.getString("fechaCreacion")),
                esFavorita = n.getBoolean("esFavorita"),
                archivos = archivos.filter { it.notaId == idNota }
            )
        },
        archivos = archivos.filter { it.notaId == null },
        // Las copias anteriores a la versión 8 no tienen tarjetas.
        tarjetas = j.optJSONArray("tarjetas")?.mapear { t ->
            Tarjeta(0, t.getString("pregunta"), t.getString("respuesta"), t.getInt("caja"), LocalDate.parse(t.getString("proximoRepaso")))
        } ?: emptyList(),
        evaluaciones = j.optJSONArray("evaluaciones")?.mapear { Evaluacion(0, it.getString("nombre"), it.getInt("peso"), it.doubleONull("nota")) } ?: emptyList(),
        calificaciones = j.getJSONArray("calificaciones").mapear { c ->
            Calificacion(0, c.getString("titulo"), c.getDouble("nota"), LocalDate.parse(c.getString("fecha")))
        }
    )
}
