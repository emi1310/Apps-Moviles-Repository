package com.example.estudia.datos

import com.example.estudia.modelo.*

// Este archivo genera un Usuario de prueba con Materias, Tareas, Eventos y
// Notas ya cargadas, para poder ver la app funcionando con datos reales
// sin necesidad de una base de datos todavía.
//
// Más adelante, cuando el usuario pueda crear sus propios datos desde la
// app, este archivo se va a dejar de usar (o solo va a servir para testing).

fun crearUsuarioDePrueba(): Usuario {

    // ---------- Materia: Matemática II ----------
    val matematica = Materia(
        id = 1,
        nombre = "Matemática II",
        color = "#6C5CE7"
    )
    matematica.tareas.add(Tarea(id = 1, titulo = "Ejercicios 3 y 4", fechaLimite = "Hoy"))
    matematica.tareas.add(Tarea(id = 2, titulo = "Repasar teoría", fechaLimite = "25 May", completada = true))
    matematica.notas.add(
        Nota(
            id = 1,
            titulo = "Definición de estadística",
            contenido = "La estadística permite analizar datos y obtener conclusiones...",
            fechaCreacion = "4 de junio de 2026",
            esFavorita = true
        )
    )
    matematica.eventos.add(
        Evento(
            id = 1,
            titulo = "Clase: Ecuaciones diferenciales",
            tipo = TipoEvento.CLASE,
            fecha = "17/06/2026",
            horaInicio = "10:00",
            horaFin = "11:30"
        )
    )

    // ---------- Materia: Física I ----------
    val fisica = Materia(
        id = 2,
        nombre = "Física I",
        color = "#00B894"
    )
    fisica.tareas.add(Tarea(id = 3, titulo = "Entregar Figma", fechaLimite = "Mañana"))
    fisica.eventos.add(
        Evento(
            id = 2,
            titulo = "Laboratorio: Ondas",
            tipo = TipoEvento.CLASE,
            fecha = "17/06/2026",
            horaInicio = "14:00",
            horaFin = "15:30"
        )
    )

    // ---------- Materia: Diseño Web ----------
    val diseñoWeb = Materia(
        id = 3,
        nombre = "Diseño web",
        color = "#FDA43C"
    )
    diseñoWeb.notas.add(
        Nota(
            id = 2,
            titulo = "Diseños de páginas web con CSS",
            contenido = "Para las páginas web usar html y para el estilo usar CSS...",
            fechaCreacion = "17 de Noviembre de 2025",
            esFavorita = true
        )
    )

    // ---------- Materia: Matemáticas I (archivada, para probar el filtro) ----------
    val matematicaI = Materia(
        id = 4,
        nombre = "Matemáticas I",
        color = "#E74C3C",
        archivada = true
    )

    // ---------- Usuario ----------
    val usuario = Usuario(
        id = 1,
        nombreUsuario = "lucasardanza",
        contrasena = "1234",
        metodoAuth = MetodoAuth.LOCAL,
        nombre = "Lucas Ardanza",
        carrera = "Desarrollo de Software",
        anio = 2,
        email = "lucasardanza777@gmail.com"
    )

    usuario.materias.add(matematica)
    usuario.materias.add(fisica)
    usuario.materias.add(diseñoWeb)
    usuario.materias.add(matematicaI)

    return usuario
}