package com.example.estudia.datos.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.estudia.util.Contrasenas

// Migraciones de la base de datos: pasan el esquema de una versión a la
// siguiente SIN perder los datos que el usuario ya tenía guardados.
// Las prueba MigracionesTest (androidTest) con los esquemas de app/schemas.

// Versión 1 -> 2: se agrega la tabla "archivos" (adjuntos de cada materia).
val MIGRACION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `archivos` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`materiaId` INTEGER NOT NULL, " +
                "`nombre` TEXT NOT NULL, " +
                "`tipoMime` TEXT NOT NULL, " +
                "`ruta` TEXT NOT NULL, " +
                "`tamanoBytes` INTEGER NOT NULL, " +
                "`fechaAgregado` TEXT NOT NULL, " +
                "FOREIGN KEY(`materiaId`) REFERENCES `materias`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_archivos_materiaId` ON `archivos` (`materiaId`)")
    }
}

// Versión 2 -> 3:
//  - Las contraseñas pasan de texto plano a hash (columna contrasenaHash).
//  - Usuario: nueva columna metaHorasSemanales (metas de estudio).
//  - Las fechas pasan de texto ("17/06/2026") a número de día (epochDay)
//    y las horas de texto ("18:30") a minutos desde la medianoche.
//  - Archivos: nueva columna notaId (adjuntos dentro de una nota).
//
// SQLite no permite cambiar el tipo de una columna, así que cada tabla se
// recrea: se crea la tabla nueva, se copian los datos convirtiéndolos,
// se borra la vieja y se renombra la nueva (procedimiento oficial de SQLite).
val MIGRACION_2_3 = object : Migration(2, 3) {

    // Texto "dd/MM/yyyy" -> número de día desde 1970 (null si no tiene ese formato).
    // julianday() da el día juliano; restarle 2440587.5 lo lleva al epochDay.
    private fun fechaSql(columna: String) =
        "CASE WHEN $columna LIKE '__/__/____' THEN CAST(julianday(substr($columna,7,4) || '-' || " +
            "substr($columna,4,2) || '-' || substr($columna,1,2)) - 2440587.5 AS INTEGER) END"

    private val hoySql = "CAST(julianday('now','localtime','start of day') - 2440587.5 AS INTEGER)"

    // Texto "HH:mm" -> minutos desde la medianoche.
    private fun horaSql(columna: String) =
        "(CAST(substr($columna, 1, instr($columna, ':') - 1) AS INTEGER) * 60 + " +
            "CAST(substr($columna, instr($columna, ':') + 1) AS INTEGER))"

    override fun migrate(db: SupportSQLiteDatabase) {
        // ---------- usuarios ----------
        db.execSQL(
            "CREATE TABLE `usuarios_nueva` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`nombreUsuario` TEXT NOT NULL, `contrasenaHash` TEXT, `metodoAuth` TEXT NOT NULL, " +
                "`nombre` TEXT NOT NULL, `carrera` TEXT, `anio` INTEGER, `email` TEXT, " +
                "`metaHorasSemanales` INTEGER)"
        )
        db.execSQL(
            "INSERT INTO `usuarios_nueva` (id, nombreUsuario, contrasenaHash, metodoAuth, nombre, carrera, anio, email) " +
                "SELECT id, nombreUsuario, contrasena, metodoAuth, nombre, carrera, anio, email FROM `usuarios`"
        )
        // Las contraseñas viejas estaban en texto plano: se reemplazan por su hash.
        // Esto no se puede hacer en SQL, por eso se recorren las filas en Kotlin.
        db.query("SELECT id, contrasenaHash FROM `usuarios_nueva` WHERE contrasenaHash IS NOT NULL").use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getInt(0)
                val hash = Contrasenas.hashear(cursor.getString(1))
                db.execSQL("UPDATE `usuarios_nueva` SET contrasenaHash = ? WHERE id = ?", arrayOf<Any>(hash, id))
            }
        }

        // ---------- tareas ----------
        // La fecha límite era texto libre: "Hoy" y "Mañana" se convierten,
        // "dd/MM/yyyy" también, y lo demás (ej. "25 May") queda sin fecha.
        db.execSQL(
            "CREATE TABLE `tareas_nueva` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`materiaId` INTEGER NOT NULL, `titulo` TEXT NOT NULL, `fechaLimite` INTEGER, " +
                "`completada` INTEGER NOT NULL, FOREIGN KEY(`materiaId`) REFERENCES `materias`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "INSERT INTO `tareas_nueva` (id, materiaId, titulo, fechaLimite, completada) " +
                "SELECT id, materiaId, titulo, CASE " +
                "WHEN fechaLimite = 'Hoy' THEN $hoySql " +
                "WHEN fechaLimite = 'Mañana' THEN $hoySql + 1 " +
                "ELSE ${fechaSql("fechaLimite")} END, completada FROM `tareas`"
        )

        // ---------- eventos ----------
        db.execSQL(
            "CREATE TABLE `eventos_nueva` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`materiaId` INTEGER NOT NULL, `titulo` TEXT NOT NULL, `tipo` TEXT NOT NULL, " +
                "`fecha` INTEGER NOT NULL, `horaInicio` INTEGER NOT NULL, `horaFin` INTEGER NOT NULL, " +
                "FOREIGN KEY(`materiaId`) REFERENCES `materias`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "INSERT INTO `eventos_nueva` (id, materiaId, titulo, tipo, fecha, horaInicio, horaFin) " +
                "SELECT id, materiaId, titulo, tipo, COALESCE(${fechaSql("fecha")}, $hoySql), " +
                "${horaSql("horaInicio")}, ${horaSql("horaFin")} FROM `eventos`"
        )

        // ---------- notas ----------
        db.execSQL(
            "CREATE TABLE `notas_nueva` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`materiaId` INTEGER NOT NULL, `titulo` TEXT NOT NULL, `contenido` TEXT NOT NULL, " +
                "`fechaCreacion` INTEGER NOT NULL, `esFavorita` INTEGER NOT NULL, " +
                "FOREIGN KEY(`materiaId`) REFERENCES `materias`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "INSERT INTO `notas_nueva` (id, materiaId, titulo, contenido, fechaCreacion, esFavorita) " +
                "SELECT id, materiaId, titulo, contenido, COALESCE(${fechaSql("fechaCreacion")}, $hoySql), " +
                "esFavorita FROM `notas`"
        )

        // ---------- archivos ----------
        db.execSQL(
            "CREATE TABLE `archivos_nueva` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`materiaId` INTEGER NOT NULL, `notaId` INTEGER, `nombre` TEXT NOT NULL, " +
                "`tipoMime` TEXT NOT NULL, `ruta` TEXT NOT NULL, `tamanoBytes` INTEGER NOT NULL, " +
                "`fechaAgregado` INTEGER NOT NULL, " +
                "FOREIGN KEY(`materiaId`) REFERENCES `materias`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`notaId`) REFERENCES `notas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "INSERT INTO `archivos_nueva` (id, materiaId, notaId, nombre, tipoMime, ruta, tamanoBytes, fechaAgregado) " +
                "SELECT id, materiaId, NULL, nombre, tipoMime, ruta, tamanoBytes, " +
                "COALESCE(${fechaSql("fechaAgregado")}, $hoySql) FROM `archivos`"
        )

        // ---------- reemplazo de tablas e índices ----------
        // Primero se borran las hijas y después las madres.
        for (tabla in listOf("archivos", "tareas", "eventos", "notas", "usuarios")) {
            db.execSQL("DROP TABLE `$tabla`")
        }
        for (tabla in listOf("usuarios", "tareas", "eventos", "notas", "archivos")) {
            db.execSQL("ALTER TABLE `${tabla}_nueva` RENAME TO `$tabla`")
        }
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_usuarios_nombreUsuario` ON `usuarios` (`nombreUsuario`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tareas_materiaId` ON `tareas` (`materiaId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_eventos_materiaId` ON `eventos` (`materiaId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notas_materiaId` ON `notas` (`materiaId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_archivos_materiaId` ON `archivos` (`materiaId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_archivos_notaId` ON `archivos` (`notaId`)")
    }
}

// Versión 3 -> 4: clases que se repiten cada semana. Solo se agregan dos
// columnas a "eventos"; los eventos que ya existían quedan sin repetir.
// repetirHasta se guarda como epochDay (igual que las demás fechas).
val MIGRACION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `eventos` ADD COLUMN `semanal` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `eventos` ADD COLUMN `repetirHasta` INTEGER")
    }
}

// Versión 4 -> 5: tabla nueva con las notas de los exámenes de cada materia.
// El SQL es el mismo que genera Room (ver app/schemas/.../5.json).
val MIGRACION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `calificaciones` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`materiaId` INTEGER NOT NULL, " +
                "`titulo` TEXT NOT NULL, " +
                "`nota` REAL NOT NULL, " +
                "`fecha` INTEGER NOT NULL, " +
                "FOREIGN KEY(`materiaId`) REFERENCES `materias`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_calificaciones_materiaId` ON `calificaciones` (`materiaId`)")
    }
}

// Versión 5 -> 6: prioridad de las tareas, subtareas y foto de perfil.
// Las tareas que ya existían quedan con prioridad media (1) y sin
// subtareas; los usuarios, sin foto. El SQL es el que genera Room (6.json).
val MIGRACION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tareas` ADD COLUMN `prioridad` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `usuarios` ADD COLUMN `fotoPerfil` TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `subtareas` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`tareaId` INTEGER NOT NULL, " +
                "`titulo` TEXT NOT NULL, " +
                "`completada` INTEGER NOT NULL, " +
                "FOREIGN KEY(`tareaId`) REFERENCES `tareas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subtareas_tareaId` ON `subtareas` (`tareaId`)")
    }
}

// Versión 6 -> 7: aviso de cada tarea y temario de los exámenes. Las tareas
// que ya existían quedan sin aviso (null) y los exámenes, sin temas.
val MIGRACION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tareas` ADD COLUMN `avisoDiasAntes` INTEGER")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `temas` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`eventoId` INTEGER NOT NULL, " +
                "`titulo` TEXT NOT NULL, " +
                "`estudiado` INTEGER NOT NULL, " +
                "FOREIGN KEY(`eventoId`) REFERENCES `eventos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_temas_eventoId` ON `temas` (`eventoId`)")
    }
}

// Versión 7 -> 8: tabla nueva con las tarjetas de estudio de cada materia.
val MIGRACION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tarjetas` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`materiaId` INTEGER NOT NULL, " +
                "`pregunta` TEXT NOT NULL, " +
                "`respuesta` TEXT NOT NULL, " +
                "`caja` INTEGER NOT NULL, " +
                "`proximoRepaso` INTEGER NOT NULL, " +
                "FOREIGN KEY(`materiaId`) REFERENCES `materias`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tarjetas_materiaId` ON `tarjetas` (`materiaId`)")
    }
}

// Versión 8 -> 9: plan de estudios, asistencias, evaluaciones (¿qué nota
// necesito?) y mesas de finales. Son tablas nuevas: lo que ya estaba no cambia.
val MIGRACION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `plan_materias` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`usuarioId` INTEGER NOT NULL, " +
                "`nombre` TEXT NOT NULL, " +
                "`anio` INTEGER NOT NULL, " +
                "`estado` TEXT NOT NULL, " +
                "`correlativas` TEXT NOT NULL, " +
                "`notaFinal` REAL, " +
                "FOREIGN KEY(`usuarioId`) REFERENCES `usuarios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_materias_usuarioId` ON `plan_materias` (`usuarioId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `asistencias` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`eventoId` INTEGER NOT NULL, " +
                "`fecha` INTEGER NOT NULL, " +
                "`presente` INTEGER NOT NULL, " +
                "FOREIGN KEY(`eventoId`) REFERENCES `eventos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_asistencias_eventoId_fecha` ON `asistencias` (`eventoId`, `fecha`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `evaluaciones` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`materiaId` INTEGER NOT NULL, " +
                "`nombre` TEXT NOT NULL, " +
                "`peso` INTEGER NOT NULL, " +
                "`nota` REAL, " +
                "FOREIGN KEY(`materiaId`) REFERENCES `materias`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_evaluaciones_materiaId` ON `evaluaciones` (`materiaId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `finales` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`usuarioId` INTEGER NOT NULL, " +
                "`materia` TEXT NOT NULL, " +
                "`fechaMesa` INTEGER NOT NULL, " +
                "`cierreInscripcion` INTEGER, " +
                "`vencimientoRegularidad` INTEGER, " +
                "`inscripto` INTEGER NOT NULL, " +
                "`nota` REAL, " +
                "FOREIGN KEY(`usuarioId`) REFERENCES `usuarios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_finales_usuarioId` ON `finales` (`usuarioId`)")
    }
}

// Versión 9 -> 10: reglas de la cursada de cada materia (asistencia mínima,
// promoción y final), clases virtuales o presenciales, categoría de los
// archivos y la tabla de feriados. Las columnas nuevas tienen un valor por
// defecto, así lo que ya estaba guardado sigue igual (75 %, promocional con
// 7, con final, presencial y archivos en "Otros").
val MIGRACION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `materias` ADD COLUMN `asistenciaMinima` INTEGER NOT NULL DEFAULT 75")
        db.execSQL("ALTER TABLE `materias` ADD COLUMN `promocional` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `materias` ADD COLUMN `notaPromocion` REAL NOT NULL DEFAULT 7.0")
        db.execSQL("ALTER TABLE `materias` ADD COLUMN `tieneFinal` INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE `eventos` ADD COLUMN `virtual` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `eventos` ADD COLUMN `lugar` TEXT")
        db.execSQL("ALTER TABLE `eventos` ADD COLUMN `enlace` TEXT")
        db.execSQL("ALTER TABLE `archivos` ADD COLUMN `categoria` TEXT NOT NULL DEFAULT 'OTROS'")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `feriados` (" +
                "`fecha` INTEGER NOT NULL, " +
                "`nombre` TEXT NOT NULL, " +
                "PRIMARY KEY(`fecha`))"
        )
    }
}

// Versión 10 -> 11: bibliografía (tabla "fuentes"). Es una tabla nueva: lo
// que ya estaba no cambia.
val MIGRACION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `fuentes` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`usuarioId` INTEGER NOT NULL, " +
                "`materia` TEXT NOT NULL, " +
                "`tipo` TEXT NOT NULL, " +
                "`autores` TEXT NOT NULL, " +
                "`anio` INTEGER, " +
                "`titulo` TEXT NOT NULL, " +
                "`origen` TEXT NOT NULL, " +
                "`enlace` TEXT NOT NULL, " +
                "`paginas` TEXT NOT NULL, " +
                "`comentario` TEXT NOT NULL, " +
                "FOREIGN KEY(`usuarioId`) REFERENCES `usuarios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuentes_usuarioId` ON `fuentes` (`usuarioId`)")
    }
}

// Versión 11 -> 12: mapas mentales (tabla "mapas"). Tabla nueva: lo que ya
// estaba no cambia.
val MIGRACION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `mapas` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`usuarioId` INTEGER NOT NULL, " +
                "`materia` TEXT NOT NULL, " +
                "`titulo` TEXT NOT NULL, " +
                "`nodos` TEXT NOT NULL, " +
                "`modificado` INTEGER NOT NULL, " +
                "FOREIGN KEY(`usuarioId`) REFERENCES `usuarios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_mapas_usuarioId` ON `mapas` (`usuarioId`)")
    }
}

// Versión 12 -> 13: notas fijadas y con color propio, e historial de
// versiones de cada nota (tabla "nota_versiones"). Las notas que ya existían
// quedan sin fijar, con el color de su materia y sin historial.
val MIGRACION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `notas` ADD COLUMN `fijada` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `notas` ADD COLUMN `color` INTEGER")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `nota_versiones` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`notaId` INTEGER NOT NULL, " +
                "`titulo` TEXT NOT NULL, " +
                "`contenido` TEXT NOT NULL, " +
                "`fecha` INTEGER NOT NULL, " +
                "FOREIGN KEY(`notaId`) REFERENCES `notas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_nota_versiones_notaId` ON `nota_versiones` (`notaId`)")
    }
}

// Versión 13 -> 14: tareas que se repiten, clases de semana por medio y
// recesos (tabla "recesos"). Las tareas que ya existían no se repiten y las
// clases siguen siendo todas las semanas.
val MIGRACION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tareas` ADD COLUMN `repeticion` TEXT")
        db.execSQL("ALTER TABLE `eventos` ADD COLUMN `cadaSemanas` INTEGER NOT NULL DEFAULT 1")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recesos` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`usuarioId` INTEGER NOT NULL, " +
                "`nombre` TEXT NOT NULL, " +
                "`desde` INTEGER NOT NULL, " +
                "`hasta` INTEGER NOT NULL, " +
                "FOREIGN KEY(`usuarioId`) REFERENCES `usuarios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recesos_usuarioId` ON `recesos` (`usuarioId`)")
    }
}

// Versión 15: las tarjetas de estudio pueden tener una imagen.
val MIGRACION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tarjetas` ADD COLUMN `imagen` TEXT")
    }
}
