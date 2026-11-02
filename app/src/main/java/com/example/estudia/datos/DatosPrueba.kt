package com.example.estudia.datos

import com.example.estudia.modelo.*
import java.time.LocalDate
import java.time.LocalTime

// Genera un Usuario de demostración con Materias, Tareas, Eventos y Notas
// ya cargadas. Se usa solo la primera vez que se abre la app (cuando la
// base de datos Room todavía está vacía), para poder ver la app funcionando.
// Las fechas son relativas a hoy, así siempre se ven en el calendario.
// Los id van en 0: al insertarlos, SQLite genera los reales.
//
// Cuenta de prueba: usuario "demo" / contraseña "demo" (la contraseña la
// agrega RepositorioEstudiaImpl, ya convertida en hash).
// Para no dejar ninguna cuenta por defecto, sacá la llamada a
// crearDatosDePruebaSiHaceFalta() en SesionViewModel.

fun crearUsuarioDePrueba(hoy: LocalDate = LocalDate.now()): Usuario {
    val manana = hoy.plusDays(1)

    // ---------- Materia: Matemática II ----------
    val matematica = Materia(
        id = 0,
        nombre = "Matemática II",
        color = 0xFF6C5CE7,
        tareas = listOf(
            Tarea(id = 0, titulo = "Ejercicios 3 y 4", fechaLimite = hoy),
            Tarea(id = 0, titulo = "Repasar teoría", fechaLimite = hoy.minusDays(3), completada = true)
        ),
        notas = listOf(
            Nota(
                id = 0,
                titulo = "Definición de estadística",
                contenido = "La estadística permite analizar datos y obtener conclusiones...",
                fechaCreacion = hoy.minusDays(10),
                esFavorita = true
            )
        ),
        eventos = listOf(
            Evento(0, "Clase: Ecuaciones diferenciales", TipoEvento.CLASE, hoy, LocalTime.of(10, 0), LocalTime.of(11, 30)),
            Evento(0, "Examen parcial", TipoEvento.EXAMEN, manana, LocalTime.of(9, 0), LocalTime.of(11, 0)),
            Evento(0, "Estudio: Ecuaciones", TipoEvento.BLOQUE_ESTUDIO, hoy, LocalTime.of(18, 0), LocalTime.of(20, 0))
        )
    )

    // ---------- Materia: Física I ----------
    val fisica = Materia(
        id = 0,
        nombre = "Física I",
        color = 0xFF00B894,
        tareas = listOf(
            Tarea(id = 0, titulo = "Entregar Figma", fechaLimite = manana)
        ),
        eventos = listOf(
            Evento(0, "Laboratorio: Ondas", TipoEvento.CLASE, hoy, LocalTime.of(14, 0), LocalTime.of(15, 30)),
            Evento(0, "Entrega: informe de laboratorio", TipoEvento.ENTREGA, manana, LocalTime.of(23, 0), LocalTime.of(23, 59))
        )
    )

    // ---------- Materia: Diseño web ----------
    val diseno = Materia(
        id = 0,
        nombre = "Diseño web",
        color = 0xFFFDA43C,
        notas = listOf(
            Nota(
                id = 0,
                titulo = "Diseños de páginas web con CSS",
                contenido = "Para las páginas web usar html y para el estilo usar CSS...",
                fechaCreacion = hoy.minusDays(30),
                esFavorita = true
            )
        )
    )

    // ---------- Materia: Matemáticas I (archivada, para probar el filtro) ----------
    val matematicaI = Materia(
        id = 0,
        nombre = "Matemáticas I",
        color = 0xFFE74C3C,
        archivada = true
    )

    // ---------- Usuario ----------
    return Usuario(
        id = 0,
        nombreUsuario = "demo",
        metodoAuth = MetodoAuth.LOCAL,
        nombre = "Usuario Demo",
        carrera = "Desarrollo de Software",
        anio = 2,
        email = "demo@ejemplo.com",
        metaHorasSemanales = 10,
        materias = listOf(matematica, fisica, diseno, matematicaI)
    )
}
