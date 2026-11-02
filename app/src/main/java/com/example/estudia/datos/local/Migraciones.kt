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
