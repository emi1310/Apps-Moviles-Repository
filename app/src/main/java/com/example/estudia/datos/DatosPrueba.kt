package com.example.estudia.datos

import com.example.estudia.modelo.*
import java.time.LocalDate
import java.time.LocalTime

// Genera un Usuario de demostración con Materias, Tareas, Eventos, Notas y Calificaciones
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
            // Clase semanal desde hace 4 semanas, con la asistencia marcada (una falta).
            Evento(
                0, "Clase: Ecuaciones diferenciales", TipoEvento.CLASE, hoy.minusWeeks(4), LocalTime.of(10, 0), LocalTime.of(11, 30),
                semanal = true, repetirHasta = hoy.plusWeeks(11),
                lugar = "Aula 12",
                asistencias = listOf(
                    Asistencia(0, hoy.minusWeeks(4), true),
                    Asistencia(0, hoy.minusWeeks(3), true),
                    Asistencia(0, hoy.minusWeeks(2), false),
                    Asistencia(0, hoy.minusWeeks(1), true)
                )
            ),
            Evento(0, "Examen parcial", TipoEvento.EXAMEN, manana, LocalTime.of(9, 0), LocalTime.of(11, 0)),
            Evento(0, "Estudio: Ecuaciones", TipoEvento.BLOQUE_ESTUDIO, hoy, LocalTime.of(18, 0), LocalTime.of(20, 0)),
            // Días anteriores seguidos: para que se vea la racha en Estadísticas.
            Evento(0, "Estudio: Integrales", TipoEvento.BLOQUE_ESTUDIO, hoy.minusDays(1), LocalTime.of(18, 0), LocalTime.of(19, 30)),
            Evento(0, "Estudio: Derivadas", TipoEvento.BLOQUE_ESTUDIO, hoy.minusDays(2), LocalTime.of(17, 0), LocalTime.of(18, 0)),
            Evento(0, "Estudio: Límites", TipoEvento.BLOQUE_ESTUDIO, hoy.minusDays(9), LocalTime.of(16, 0), LocalTime.of(19, 0))
        ),
        calificaciones = listOf(
            Calificacion(0, "Trabajo práctico 1", 9.0, hoy.minusDays(40)),
            Calificacion(0, "Primer parcial", 7.5, hoy.minusDays(20))
        ),
        // Cómo se compone la nota, para "¿Qué nota necesito?".
        evaluaciones = listOf(
            Evaluacion(0, "Trabajo práctico 1", 20, 9.0),
            Evaluacion(0, "Primer parcial", 40, 7.5),
            Evaluacion(0, "Segundo parcial", 40, null)
        )
    )

    // ---------- Materia: Física I ----------
    val fisica = Materia(
        id = 0,
        nombre = "Física I",
        color = 0xFF00B894,
        // Sin promoción: pide 80 % de asistencia y va a final.
        asistenciaMinima = 80,
        promocional = false,
        tareas = listOf(
            Tarea(id = 0, titulo = "Entregar Figma", fechaLimite = manana)
        ),
        eventos = listOf(
            // Clase virtual, con el link para entrar.
            Evento(0, "Laboratorio: Ondas", TipoEvento.CLASE, hoy, LocalTime.of(14, 0), LocalTime.of(15, 30), virtual = true, enlace = "https://meet.google.com/abc-defg-hij"),
            Evento(0, "Entrega: informe de laboratorio", TipoEvento.ENTREGA, manana, LocalTime.of(23, 0), LocalTime.of(23, 59)),
            Evento(0, "Estudio: Cinemática", TipoEvento.BLOQUE_ESTUDIO, hoy.minusDays(15), LocalTime.of(10, 0), LocalTime.of(12, 0))
        ),
        calificaciones = listOf(
            Calificacion(0, "Primer parcial", 3.0, hoy.minusDays(25)),
            Calificacion(0, "Recuperatorio", 6.0, hoy.minusDays(10))
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
        materias = listOf(matematica, fisica, diseno, matematicaI),
        planEstudio = planDePrueba(),
        finales = listOf(
            MesaFinal(0, "Matemáticas I", hoy.plusDays(12), cierreInscripcion = hoy.plusDays(2), vencimientoRegularidad = hoy.plusMonths(8)),
            MesaFinal(0, "Programación I", hoy.minusDays(30), inscripto = true, nota = 8.0)
        ),
        fuentes = listOf(
            Fuente(0, "Matemática II", TipoFuente.LIBRO, "Stewart, J.", 2018, "Cálculo de varias variables", origen = "Cengage Learning", comentario = "Capítulos 14 y 15 para el parcial"),
            Fuente(0, "Física I", TipoFuente.APUNTE, "Cátedra de Física I", 2025, "Guía de trabajos prácticos", origen = "Facultad de Ingeniería"),
            Fuente(0, "Diseño web", TipoFuente.WEB, "", null, "Guía de CSS", origen = "MDN Web Docs", enlace = "https://developer.mozilla.org/es/docs/Web/CSS")
        ),
        mapas = listOf(mapaDePrueba(hoy))
    )
}

// Plan de estudios de ejemplo (los ids son temporales: solo sirven para
// las correlativas; al guardarlo, el Repository los traduce a los reales).
private fun planDePrueba(): List<MateriaPlan> = listOf(
    MateriaPlan(1, "Matemáticas I", 1, EstadoPlan.REGULAR),
    MateriaPlan(2, "Programación I", 1, EstadoPlan.APROBADA, notaFinal = 8.0),
    MateriaPlan(3, "Inglés técnico I", 1, EstadoPlan.APROBADA, notaFinal = 9.0),
    MateriaPlan(4, "Matemática II", 2, EstadoPlan.CURSANDO, correlativas = listOf(1)),
    MateriaPlan(5, "Física I", 2, EstadoPlan.CURSANDO, correlativas = listOf(1)),
    MateriaPlan(6, "Diseño web", 2, EstadoPlan.CURSANDO, correlativas = listOf(2)),
    MateriaPlan(7, "Programación II", 2, EstadoPlan.PENDIENTE, correlativas = listOf(2)),
    MateriaPlan(8, "Base de datos", 3, EstadoPlan.PENDIENTE, correlativas = listOf(7)),
    MateriaPlan(9, "Práctica profesional", 3, EstadoPlan.PENDIENTE, correlativas = listOf(4, 7))
)

// Mapa mental de ejemplo: la idea central con tres ramas (las posiciones
// las calcula agregarIdea, como cuando se arma desde la app).
private fun mapaDePrueba(hoy: LocalDate): MapaMental {
    var mapa = MapaMental.nuevo(0, "Física I", "Leyes de Newton", hoy)
    mapa = mapa.agregarIdea(1, "1ª: Inercia").first
    mapa = mapa.agregarIdea(1, "2ª: F = m · a").first
    mapa = mapa.agregarIdea(1, "3ª: Acción y reacción").first
    mapa = mapa.agregarIdea(2, "Un cuerpo sigue como está si nada lo empuja").first
    mapa = mapa.agregarIdea(3, "Más masa, más fuerza para acelerar").first
    mapa = mapa.agregarIdea(4, "Ej: al remar, el agua empuja el bote").first
    return mapa
}
