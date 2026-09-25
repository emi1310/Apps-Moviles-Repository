package com.example.estudia.datos

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.estudia.modelo.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class RepositorioDatos(carpeta: File) {

    private val archivo = File(carpeta, "estudia_datos.json")

    fun cargar(): List<Usuario> {
        if (!archivo.exists()) {
            val iniciales = listOf(crearUsuarioDePrueba())
            guardar(iniciales)
            return iniciales
        }
        return try {
            leerUsuarios(archivo.readText())
        } catch (e: Exception) {
            listOf(crearUsuarioDePrueba())
        }
    }

    fun guardar(usuarios: List<Usuario>) {
        archivo.writeText(usuariosAJson(usuarios))
    }
}

private fun usuariosAJson(usuarios: List<Usuario>): String {
    val lista = JSONArray()
    for (usuario in usuarios) {
        lista.put(usuarioAJson(usuario))
    }
    return lista.toString()
}

private fun usuarioAJson(usuario: Usuario): JSONObject {
    val json = JSONObject()
    json.put("id", usuario.id)
    json.put("nombreUsuario", usuario.nombreUsuario)
    json.put("contrasena", usuario.contrasena)
    json.put("metodoAuth", if (usuario.metodoAuth == MetodoAuth.GOOGLE) "GOOGLE" else "LOCAL")
    json.put("nombre", usuario.nombre)
    json.put("carrera", usuario.carrera)
    json.put("anio", usuario.anio)
    json.put("email", usuario.email)

    val materias = JSONArray()
    for (materia in usuario.materias) {
        materias.put(materiaAJson(materia))
    }
    json.put("materias", materias)
    return json
}

private fun materiaAJson(materia: Materia): JSONObject {
    val json = JSONObject()
    json.put("id", materia.id)
    json.put("nombre", materia.nombre)
    json.put("color", materia.color.toArgb())
    json.put("archivada", materia.archivada)

    val tareas = JSONArray()
    for (tarea in materia.tareas) {
        val t = JSONObject()
        t.put("id", tarea.id)
        t.put("titulo", tarea.titulo)
        t.put("fechaLimite", tarea.fechaLimite)
        t.put("completada", tarea.completada)
        tareas.put(t)
    }
    json.put("tareas", tareas)

    val eventos = JSONArray()
    for (evento in materia.eventos) {
        val e = JSONObject()
        e.put("id", evento.id)
        e.put("titulo", evento.titulo)
        e.put("tipo", tipoATexto(evento.tipo))
        e.put("fecha", evento.fecha)
        e.put("horaInicio", evento.horaInicio)
        e.put("horaFin", evento.horaFin)
        eventos.put(e)
    }
    json.put("eventos", eventos)

    val notas = JSONArray()
    for (nota in materia.notas) {
        val n = JSONObject()
        n.put("id", nota.id)
        n.put("titulo", nota.titulo)
        n.put("contenido", nota.contenido)
        n.put("fechaCreacion", nota.fechaCreacion)
        n.put("esFavorita", nota.esFavorita)
        notas.put(n)
    }
    json.put("notas", notas)
    return json
}

private fun tipoATexto(tipo: TipoEvento): String {
    return when (tipo) {
        TipoEvento.CLASE -> "CLASE"
        TipoEvento.EXAMEN -> "EXAMEN"
        TipoEvento.ENTREGA -> "ENTREGA"
        else -> "BLOQUE_ESTUDIO"
    }
}

private fun leerUsuarios(texto: String): List<Usuario> {
    val usuarios = mutableListOf<Usuario>()
    val lista = JSONArray(texto)
    for (i in 0 until lista.length()) {
        usuarios.add(leerUsuario(lista.getJSONObject(i)))
    }
    return usuarios
}

private fun leerUsuario(json: JSONObject): Usuario {
    val usuario = Usuario(
        id = json.getInt("id"),
        nombreUsuario = json.getString("nombreUsuario"),
        contrasena = leerTexto(json, "contrasena"),
        metodoAuth = if (json.getString("metodoAuth") == "GOOGLE") MetodoAuth.GOOGLE else MetodoAuth.LOCAL,
        nombre = json.getString("nombre"),
        carrera = leerTexto(json, "carrera"),
        anio = leerEntero(json, "anio"),
        email = leerTexto(json, "email")
    )
    val materias = json.getJSONArray("materias")
    for (i in 0 until materias.length()) {
        usuario.materias.add(leerMateria(materias.getJSONObject(i)))
    }
    return usuario
}

private fun leerMateria(json: JSONObject): Materia {
    val materia = Materia(
        id = json.getInt("id"),
        nombre = json.getString("nombre"),
        color = Color(json.getInt("color")),
        archivada = json.getBoolean("archivada")
    )

    val tareas = json.getJSONArray("tareas")
    for (i in 0 until tareas.length()) {
        val t = tareas.getJSONObject(i)
        materia.tareas.add(
            Tarea(
                id = t.getInt("id"),
                titulo = t.getString("titulo"),
                fechaLimite = t.getString("fechaLimite"),
                completada = t.getBoolean("completada")
            )
        )
    }

    val eventos = json.getJSONArray("eventos")
    for (i in 0 until eventos.length()) {
        val e = eventos.getJSONObject(i)
        materia.eventos.add(
            Evento(
                id = e.getInt("id"),
                titulo = e.getString("titulo"),
                tipo = textoATipo(e.getString("tipo")),
                fecha = e.getString("fecha"),
                horaInicio = e.getString("horaInicio"),
                horaFin = e.getString("horaFin")
            )
        )
    }

    val notas = json.getJSONArray("notas")
    for (i in 0 until notas.length()) {
        val n = notas.getJSONObject(i)
        materia.notas.add(
            Nota(
                id = n.getInt("id"),
                titulo = n.getString("titulo"),
                contenido = n.getString("contenido"),
                fechaCreacion = n.getString("fechaCreacion"),
                esFavorita = n.getBoolean("esFavorita")
            )
        )
    }
    return materia
}

private fun textoATipo(texto: String): TipoEvento {
    return when (texto) {
        "CLASE" -> TipoEvento.CLASE
        "EXAMEN" -> TipoEvento.EXAMEN
        "ENTREGA" -> TipoEvento.ENTREGA
        else -> TipoEvento.BLOQUE_ESTUDIO
    }
}

private fun leerTexto(json: JSONObject, clave: String): String? {
    if (json.has(clave)) {
        return json.getString(clave)
    }
    return null
}

private fun leerEntero(json: JSONObject, clave: String): Int? {
    if (json.has(clave)) {
        return json.getInt(clave)
    }
    return null
}
