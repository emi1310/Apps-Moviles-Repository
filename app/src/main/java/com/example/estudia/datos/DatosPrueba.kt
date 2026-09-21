package com.example.estudia.datos

import androidx.compose.ui.graphics.Color
import com.example.estudia.modelo.*
import com.example.estudia.util.obtenerFechaHoyComoTexto
import com.example.estudia.util.obtenerFechaMananaComoTexto

// Genera un Usuario de demostración con Materias, Tareas, Eventos y Notas
// ya cargadas. Se usa solo la primera vez que se abre la app (cuando todavía
// no existe el archivo de datos), para poder ver la app funcionando.
// Las fechas de los eventos son de hoy y de mañana, así siempre se ven
// en el calendario.
//
// Cuenta de prueba: usuario "demo" / contraseña "demo".
// Para no dejar ninguna cuenta por defecto, sacá las llamadas a
// crearUsuarioDePrueba() en RepositorioDatos.

fun crearUsuarioDePrueba(): Usuario {
    val hoy = obtenerFechaHoyComoTexto()
    val manana = obtenerFechaMananaComoTexto()

    // ---------- Materia: Matemática II ----------
    val matematica = Materia(
        id = 1,
        nombre = "Matemática II",
        color = Color(0xFF6C5CE7)
    )
    matematica.tareas.add(Tarea(id = 1, titulo = "Ejercicios 3 y 4", fechaLimite = "Hoy"))
    matematica.tareas.add(Tarea(id = 2, titulo = "Repasar teoría", fechaLimite = "25 May", completada = true))
    matematica.notas.add(
        Nota(
            id = 1,
            titulo = "Definición de estadística",
            contenido = "La estadística permite analizar datos y obtener conclusiones...",
            fechaCreacion = "04/06/2026",
            esFavorita = true
        )
    )
    matematica.eventos.add(
        Evento(
            id = 1,
            titulo = "Clase: Ecuaciones diferenciales",
            tipo = TipoEvento.CLASE,
            fecha = hoy,
            horaInicio = "10:00",
            horaFin = "11:30"
        )
    )
    matematica.eventos.add(
        Evento(
            id = 2,
            titulo = "Examen parcial",
            tipo = TipoEvento.EXAMEN,
            fecha = manana,
            horaInicio = "09:00",
            horaFin = "11:00"
        )
    )
    matematica.eventos.add(
        Evento(
            id = 3,
            titulo = "Estudio: Ecuaciones",
            tipo = TipoEvento.BLOQUE_ESTUDIO,
            fecha = hoy,
            horaInicio = "18:00",
            horaFin = "20:00"
        )
    )

    // ---------- Materia: Física I ----------
    val fisica = Materia(
        id = 2,
        nombre = "Física I",
        color = Color(0xFF00B894)
    )
    fisica.tareas.add(Tarea(id = 3, titulo = "Entregar Figma", fechaLimite = "Mañana"))
    fisica.eventos.add(
        Evento(
            id = 4,
            titulo = "Laboratorio: Ondas",
            tipo = TipoEvento.CLASE,
            fecha = hoy,
            horaInicio = "14:00",
            horaFin = "15:30"
        )
    )
    fisica.eventos.add(
        Evento(
            id = 5,
            titulo = "Entrega: informe de laboratorio",
            tipo = TipoEvento.ENTREGA,
            fecha = manana,
            horaInicio = "23:00",
            horaFin = "23:59"
        )
    )

    // ---------- Materia: Diseño web ----------
    val diseno = Materia(
        id = 3,
        nombre = "Diseño web",
        color = Color(0xFFFDA43C)
    )
    diseno.notas.add(
        Nota(
            id = 1,
            titulo = "Diseños de páginas web con CSS",
            contenido = "Para las páginas web usar html y para el estilo usar CSS...",
            fechaCreacion = "17/11/2025",
            esFavorita = true
        )
    )

    // ---------- Materia: Matemáticas I (archivada, para probar el filtro) ----------
    val matematicaI = Materia(
        id = 4,
        nombre = "Matemáticas I",
        color = Color(0xFFE74C3C),
        archivada = true
    )

    // ---------- Usuario ----------
    val usuario = Usuario(
        id = 1,
        nombreUsuario = "demo",
        contrasena = "demo",
        metodoAuth = MetodoAuth.LOCAL,
        nombre = "Usuario Demo",
        carrera = "Desarrollo de Software",
        anio = 2,
        email = "demo@ejemplo.com"
    )

    usuario.materias.add(matematica)
    usuario.materias.add(fisica)
    usuario.materias.add(diseno)
    usuario.materias.add(matematicaI)

    return usuario
}
