package com.example.estudia

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.estudia.datos.local.AppDatabase
import com.example.estudia.datos.local.MIGRACION_2_3
import com.example.estudia.datos.local.MIGRACION_3_4
import com.example.estudia.datos.local.MIGRACION_4_5
import com.example.estudia.datos.local.MIGRACION_5_6
import com.example.estudia.datos.local.MIGRACION_6_7
import com.example.estudia.datos.local.MIGRACION_7_8
import com.example.estudia.datos.local.MIGRACION_8_9
import com.example.estudia.datos.local.MIGRACION_9_10
import com.example.estudia.datos.local.MIGRACION_10_11
import com.example.estudia.datos.local.MIGRACION_11_12
import com.example.estudia.datos.local.MIGRACION_12_13
import com.example.estudia.datos.local.MIGRACION_13_14
import com.example.estudia.util.Contrasenas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

// Tests de las migraciones. El de 2 -> 3 crea una base con el esquema viejo (sacado de
// app/schemas/.../2.json), le carga datos como los guardaba la versión 2
// (contraseña en texto plano, fechas como texto) y verifica que después de
// migrar los datos siguen ahí, convertidos, y que el esquema es el correcto.
@RunWith(AndroidJUnit4::class)
class MigracionesTest {

    private val nombreBase = "migracion-test"

    @get:Rule
    val ayudante = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrar2a3ConservaYConvierteLosDatos() {
        ayudante.createDatabase(nombreBase, 2).apply {
            execSQL("INSERT INTO usuarios (id, nombreUsuario, contrasena, metodoAuth, nombre) VALUES (1, 'ana', 'clave1', 'LOCAL', 'Ana')")
            execSQL("INSERT INTO materias (id, usuarioId, nombre, color, archivada) VALUES (1, 1, 'Física', 123, 0)")
            execSQL("INSERT INTO tareas (id, materiaId, titulo, fechaLimite, completada) VALUES (1, 1, 'Con fecha', '25/12/2026', 0)")
            execSQL("INSERT INTO tareas (id, materiaId, titulo, fechaLimite, completada) VALUES (2, 1, 'Texto libre', '25 May', 1)")
            execSQL("INSERT INTO tareas (id, materiaId, titulo, fechaLimite, completada) VALUES (3, 1, 'Para hoy', 'Hoy', 0)")
            execSQL("INSERT INTO eventos (id, materiaId, titulo, tipo, fecha, horaInicio, horaFin) VALUES (1, 1, 'Parcial', 'EXAMEN', '17/06/2026', '18:30', '20:00')")
            execSQL("INSERT INTO notas (id, materiaId, titulo, contenido, fechaCreacion, esFavorita) VALUES (1, 1, 'Ondas', 'texto', '04/06/2026', 1)")
            execSQL("INSERT INTO archivos (id, materiaId, nombre, tipoMime, ruta, tamanoBytes, fechaAgregado) VALUES (1, 1, 'a.pdf', 'application/pdf', 'ruta_a', 10, '02/11/2026')")
            close()
        }

        // Migra y además valida que el esquema resultante sea igual al de 3.json.
        val db = ayudante.runMigrationsAndValidate(nombreBase, 3, true, MIGRACION_2_3)

        // La contraseña ya no está en texto plano, pero sigue sirviendo para entrar.
        db.query("SELECT contrasenaHash FROM usuarios WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            val hash = c.getString(0)
            assertFalse(hash.contains("clave1"))
            assertTrue(Contrasenas.verificar("clave1", hash))
        }

        // Fechas convertidas a número de día; las que no se entendían quedan sin fecha.
        db.query("SELECT fechaLimite FROM tareas ORDER BY id").use { c ->
            c.moveToNext(); assertEquals(LocalDate.of(2026, 12, 25).toEpochDay(), c.getLong(0))
            c.moveToNext(); assertTrue(c.isNull(0))
            c.moveToNext(); assertEquals(LocalDate.now().toEpochDay(), c.getLong(0))
        }

        // Horas convertidas a minutos desde la medianoche.
        db.query("SELECT fecha, horaInicio, horaFin FROM eventos").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(LocalDate.of(2026, 6, 17).toEpochDay(), c.getLong(0))
            assertEquals(18 * 60 + 30, c.getInt(1))
            assertEquals(20 * 60, c.getInt(2))
        }

        db.query("SELECT fechaCreacion, esFavorita FROM notas").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(LocalDate.of(2026, 6, 4).toEpochDay(), c.getLong(0))
            assertEquals(1, c.getInt(1))
        }

        // El archivo sigue siendo de la materia (notaId vacío).
        db.query("SELECT notaId, fechaAgregado FROM archivos").use { c ->
            assertTrue(c.moveToFirst())
            assertTrue(c.isNull(0))
            assertEquals(LocalDate.of(2026, 11, 2).toEpochDay(), c.getLong(1))
        }
        db.close()
    }

    // 3 -> 4: los eventos que ya existían siguen igual y quedan sin repetir.
    @Test
    fun migrar3a4AgregaLaRepeticionSemanal() {
        val lunes = LocalDate.of(2026, 11, 2).toEpochDay()
        ayudante.createDatabase(nombreBase, 3).apply {
            execSQL("INSERT INTO usuarios (id, nombreUsuario, contrasenaHash, metodoAuth, nombre) VALUES (1, 'ana', 'x', 'LOCAL', 'Ana')")
            execSQL("INSERT INTO materias (id, usuarioId, nombre, color, archivada) VALUES (1, 1, 'Física', 123, 0)")
            execSQL("INSERT INTO eventos (id, materiaId, titulo, tipo, fecha, horaInicio, horaFin) VALUES (1, 1, 'Teórico', 'CLASE', $lunes, 540, 660)")
            close()
        }

        val db = ayudante.runMigrationsAndValidate(nombreBase, 4, true, MIGRACION_3_4)

        db.query("SELECT titulo, fecha, semanal, repetirHasta FROM eventos").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Teórico", c.getString(0))
            assertEquals(lunes, c.getLong(1))
            assertEquals(0, c.getInt(2))
            assertTrue(c.isNull(3))
        }
        db.close()
    }

    // 4 -> 5: aparece la tabla de calificaciones y se borra junto con su materia.
    @Test
    fun migrar4a5CreaLaTablaDeCalificaciones() {
        ayudante.createDatabase(nombreBase, 4).apply {
            execSQL("INSERT INTO usuarios (id, nombreUsuario, contrasenaHash, metodoAuth, nombre) VALUES (1, 'ana', 'x', 'LOCAL', 'Ana')")
            execSQL("INSERT INTO materias (id, usuarioId, nombre, color, archivada) VALUES (1, 1, 'Física', 123, 0)")
            close()
        }

        val db = ayudante.runMigrationsAndValidate(nombreBase, 5, true, MIGRACION_4_5)

        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("INSERT INTO calificaciones (materiaId, titulo, nota, fecha) VALUES (1, 'Parcial', 7.5, 0)")
        db.query("SELECT titulo, nota FROM calificaciones").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Parcial", c.getString(0))
            assertEquals(7.5, c.getDouble(1), 0.001)
        }
        db.execSQL("DELETE FROM materias WHERE id = 1")
        db.query("SELECT COUNT(*) FROM calificaciones").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    // 5 -> 6: las tareas que ya existían quedan con prioridad media, el
    // usuario sin foto, y las subtareas se borran junto con su tarea.
    @Test
    fun migrar5a6AgregaPrioridadSubtareasYFoto() {
        ayudante.createDatabase(nombreBase, 5).apply {
            execSQL("INSERT INTO usuarios (id, nombreUsuario, contrasenaHash, metodoAuth, nombre) VALUES (1, 'ana', 'x', 'LOCAL', 'Ana')")
            execSQL("INSERT INTO materias (id, usuarioId, nombre, color, archivada) VALUES (1, 1, 'Física', 123, 0)")
            execSQL("INSERT INTO tareas (id, materiaId, titulo, fechaLimite, completada) VALUES (1, 1, 'TP 1', NULL, 0)")
            close()
        }

        val db = ayudante.runMigrationsAndValidate(nombreBase, 6, true, MIGRACION_5_6)

        db.query("SELECT titulo, prioridad FROM tareas").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("TP 1", c.getString(0))
            assertEquals(1, c.getInt(1))
        }
        db.query("SELECT fotoPerfil FROM usuarios").use { c ->
            assertTrue(c.moveToFirst())
            assertTrue(c.isNull(0))
        }
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("INSERT INTO subtareas (tareaId, titulo, completada) VALUES (1, 'Leer el apunte', 0)")
        db.execSQL("DELETE FROM tareas WHERE id = 1")
        db.query("SELECT COUNT(*) FROM subtareas").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    // 6 -> 7: las tareas quedan sin aviso y los temas se borran con su examen.
    @Test
    fun migrar6a7AgregaAvisoDeTareasYTemario() {
        ayudante.createDatabase(nombreBase, 6).apply {
            execSQL("INSERT INTO usuarios (id, nombreUsuario, contrasenaHash, metodoAuth, nombre) VALUES (1, 'ana', 'x', 'LOCAL', 'Ana')")
            execSQL("INSERT INTO materias (id, usuarioId, nombre, color, archivada) VALUES (1, 1, 'Física', 123, 0)")
            execSQL("INSERT INTO tareas (id, materiaId, titulo, fechaLimite, completada, prioridad) VALUES (1, 1, 'TP 1', NULL, 0, 1)")
            execSQL("INSERT INTO eventos (id, materiaId, titulo, tipo, fecha, horaInicio, horaFin, semanal) VALUES (1, 1, 'Parcial', 'EXAMEN', 0, 540, 660, 0)")
            close()
        }

        val db = ayudante.runMigrationsAndValidate(nombreBase, 7, true, MIGRACION_6_7)

        db.query("SELECT avisoDiasAntes FROM tareas").use { c ->
            assertTrue(c.moveToFirst())
            assertTrue(c.isNull(0))
        }
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("INSERT INTO temas (eventoId, titulo, estudiado) VALUES (1, 'Unidad 1', 0)")
        db.execSQL("DELETE FROM eventos WHERE id = 1")
        db.query("SELECT COUNT(*) FROM temas").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    // 7 -> 8: aparece la tabla de tarjetas y se borra junto con su materia.
    @Test
    fun migrar7a8CreaLaTablaDeTarjetas() {
        ayudante.createDatabase(nombreBase, 7).apply {
            execSQL("INSERT INTO usuarios (id, nombreUsuario, contrasenaHash, metodoAuth, nombre) VALUES (1, 'ana', 'x', 'LOCAL', 'Ana')")
            execSQL("INSERT INTO materias (id, usuarioId, nombre, color, archivada) VALUES (1, 1, 'Física', 123, 0)")
            close()
        }

        val db = ayudante.runMigrationsAndValidate(nombreBase, 8, true, MIGRACION_7_8)

        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("INSERT INTO tarjetas (materiaId, pregunta, respuesta, caja, proximoRepaso) VALUES (1, '¿Qué es una onda?', 'Una perturbación', 1, 0)")
        db.execSQL("DELETE FROM materias WHERE id = 1")
        db.query("SELECT COUNT(*) FROM tarjetas").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    // 8 -> 9: aparecen el plan, las asistencias, las evaluaciones y los
    // finales; lo anterior queda igual y la asistencia es una por clase y día.
    @Test
    fun migrar8a9CreaLasTablasDeLaCursada() {
        ayudante.createDatabase(nombreBase, 8).apply {
            execSQL("INSERT INTO usuarios (id, nombreUsuario, contrasenaHash, metodoAuth, nombre) VALUES (1, 'ana', 'x', 'LOCAL', 'Ana')")
            execSQL("INSERT INTO materias (id, usuarioId, nombre, color, archivada) VALUES (1, 1, 'Física', 123, 0)")
            execSQL("INSERT INTO eventos (id, materiaId, titulo, tipo, fecha, horaInicio, horaFin, semanal) VALUES (1, 1, 'Clase', 'CLASE', 0, 600, 690, 1)")
            close()
        }

        val db = ayudante.runMigrationsAndValidate(nombreBase, 9, true, MIGRACION_8_9)

        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("INSERT INTO plan_materias (usuarioId, nombre, anio, estado, correlativas) VALUES (1, 'Física', 1, 'CURSANDO', '')")
        db.execSQL("INSERT INTO evaluaciones (materiaId, nombre, peso, nota) VALUES (1, 'Parcial', 50, NULL)")
        db.execSQL("INSERT INTO finales (usuarioId, materia, fechaMesa, inscripto) VALUES (1, 'Física', 100, 0)")
        db.execSQL("INSERT INTO asistencias (eventoId, fecha, presente) VALUES (1, 7, 1)")
        // Marcar el mismo día otra vez reemplaza la fila (índice único).
        db.execSQL("INSERT OR REPLACE INTO asistencias (eventoId, fecha, presente) VALUES (1, 7, 0)")
        db.query("SELECT COUNT(*), MIN(presente) FROM asistencias").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
            assertEquals(0, c.getInt(1))
        }
        // Al borrar la materia se van su evaluación y la asistencia de su clase.
        db.execSQL("DELETE FROM materias WHERE id = 1")
        db.query("SELECT (SELECT COUNT(*) FROM evaluaciones) + (SELECT COUNT(*) FROM asistencias)").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    // 9 -> 10: las materias, eventos y archivos que ya estaban quedan con los
    // valores de siempre (75 %, promocional con 7, con final, presencial y en
    // "Otros") y aparece la tabla de feriados.
    @Test
    fun migrar9a10AgregaReglasModalidadCategoriasYFeriados() {
        ayudante.createDatabase(nombreBase, 9).apply {
            execSQL("INSERT INTO usuarios (id, nombreUsuario, contrasenaHash, metodoAuth, nombre) VALUES (1, 'ana', 'x', 'LOCAL', 'Ana')")
            execSQL("INSERT INTO materias (id, usuarioId, nombre, color, archivada) VALUES (1, 1, 'Física', 123, 0)")
            execSQL("INSERT INTO eventos (id, materiaId, titulo, tipo, fecha, horaInicio, horaFin, semanal) VALUES (1, 1, 'Clase', 'CLASE', 0, 600, 690, 1)")
            execSQL("INSERT INTO archivos (id, materiaId, nombre, tipoMime, ruta, tamanoBytes, fechaAgregado) VALUES (1, 1, 'apunte.pdf', 'application/pdf', 'a.pdf', 10, 0)")
            close()
        }

        val db = ayudante.runMigrationsAndValidate(nombreBase, 10, true, MIGRACION_9_10)

        db.query("SELECT asistenciaMinima, promocional, notaPromocion, tieneFinal FROM materias WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals(75, c.getInt(0))
            assertEquals(1, c.getInt(1))
            assertEquals(7.0, c.getDouble(2), 0.001)
            assertEquals(1, c.getInt(3))
        }
        db.query("SELECT virtual, lugar, enlace FROM eventos WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
            assertEquals(true, c.isNull(1))
            assertEquals(true, c.isNull(2))
        }
        db.query("SELECT categoria FROM archivos WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals("OTROS", c.getString(0))
        }
        db.execSQL("INSERT INTO feriados (fecha, nombre) VALUES (100, 'Día de la Independencia')")
        // Un feriado por día: la fecha es la clave.
        db.execSQL("INSERT OR REPLACE INTO feriados (fecha, nombre) VALUES (100, 'Otro nombre')")
        db.query("SELECT COUNT(*), MAX(nombre) FROM feriados").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
            assertEquals("Otro nombre", c.getString(1))
        }
        db.close()
    }

    @Test
    fun migrar10a11CreaLaTablaDeBibliografia() {
        ayudante.createDatabase(nombreBase, 10).apply {
            execSQL("INSERT INTO usuarios (id, nombreUsuario, contrasenaHash, metodoAuth, nombre) VALUES (1, 'ana', 'x', 'LOCAL', 'Ana')")
            execSQL("INSERT INTO materias (id, usuarioId, nombre, color, archivada) VALUES (1, 1, 'Física', 123, 0)")
            close()
        }

        val db = ayudante.runMigrationsAndValidate(nombreBase, 11, true, MIGRACION_10_11)

        // Lo que ya estaba sigue igual.
        db.query("SELECT nombre FROM materias WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals("Física", c.getString(0))
        }
        db.execSQL(
            "INSERT INTO fuentes (usuarioId, materia, tipo, autores, anio, titulo, origen, enlace, paginas, comentario) " +
                "VALUES (1, 'Física', 'LIBRO', 'Resnick, R.', 2002, 'Física', 'Patria', '', '', '')"
        )
        // Al borrar el usuario se borra su bibliografía (CASCADE).
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM usuarios WHERE id = 1")
        db.query("SELECT COUNT(*) FROM fuentes").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrar11a12CreaLaTablaDeMapas() {
        ayudante.createDatabase(nombreBase, 11).apply {
            execSQL("INSERT INTO usuarios (id, nombreUsuario, contrasenaHash, metodoAuth, nombre) VALUES (1, 'ana', 'x', 'LOCAL', 'Ana')")
            execSQL("INSERT INTO fuentes (usuarioId, materia, tipo, autores, anio, titulo, origen, enlace, paginas, comentario) VALUES (1, '', 'LIBRO', '', NULL, 'Física', '', '', '', '')")
            close()
        }

        val db = ayudante.runMigrationsAndValidate(nombreBase, 12, true, MIGRACION_11_12)

        // La bibliografía sigue igual.
        db.query("SELECT titulo FROM fuentes").use { c ->
            c.moveToFirst()
            assertEquals("Física", c.getString(0))
        }
        db.execSQL("INSERT INTO mapas (usuarioId, materia, titulo, nodos, modificado) VALUES (1, '', 'Newton', '1||0.0|0.0||Newton', 0)")
        db.query("SELECT titulo FROM mapas").use { c ->
            c.moveToFirst()
            assertEquals("Newton", c.getString(0))
        }
        db.close()
    }

    @Test
    fun migrar12a13AgregaFijadaColorEHistorial() {
        ayudante.createDatabase(nombreBase, 12).apply {
            execSQL("INSERT INTO usuarios (id, nombreUsuario, contrasenaHash, metodoAuth, nombre) VALUES (1, 'ana', 'x', 'LOCAL', 'Ana')")
            execSQL("INSERT INTO materias (id, usuarioId, nombre, color, archivada) VALUES (1, 1, 'Física', 123, 0)")
            execSQL("INSERT INTO notas (id, materiaId, titulo, contenido, fechaCreacion, esFavorita) VALUES (1, 1, 'Ondas', 'texto', 20000, 0)")
            close()
        }

        val db = ayudante.runMigrationsAndValidate(nombreBase, 13, true, MIGRACION_12_13)

        // La nota que ya estaba queda sin fijar y con el color de la materia.
        db.query("SELECT titulo, fijada, color FROM notas").use { c ->
            c.moveToFirst()
            assertEquals("Ondas", c.getString(0))
            assertEquals(0, c.getInt(1))
            assertTrue(c.isNull(2))
        }
        db.execSQL("INSERT INTO nota_versiones (notaId, titulo, contenido, fecha) VALUES (1, 'Ondas viejas', '', 0)")
        // Si se borra la nota, se borra su historial.
        db.execSQL("DELETE FROM notas WHERE id = 1")
        db.query("SELECT COUNT(*) FROM nota_versiones").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrar13a14AgregaRepeticionSemanasYRecesos() {
        ayudante.createDatabase(nombreBase, 13).apply {
            execSQL("INSERT INTO usuarios (id, nombreUsuario, contrasenaHash, metodoAuth, nombre) VALUES (1, 'ana', 'x', 'LOCAL', 'Ana')")
            execSQL("INSERT INTO materias (id, usuarioId, nombre, color, archivada) VALUES (1, 1, 'Física', 123, 0)")
            execSQL("INSERT INTO tareas (id, materiaId, titulo, fechaLimite, completada, prioridad) VALUES (1, 1, 'TP', 20000, 0, 1)")
            execSQL("INSERT INTO eventos (id, materiaId, titulo, tipo, fecha, horaInicio, horaFin, semanal) VALUES (1, 1, 'Teoría', 'CLASE', 20000, 600, 720, 1)")
            close()
        }

        val db = ayudante.runMigrationsAndValidate(nombreBase, 14, true, MIGRACION_13_14)

        // Lo que ya estaba: la tarea no se repite y la clase es todas las semanas.
        db.query("SELECT repeticion FROM tareas").use { c ->
            c.moveToFirst()
            assertTrue(c.isNull(0))
        }
        db.query("SELECT cadaSemanas FROM eventos").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
        db.execSQL("INSERT INTO recesos (usuarioId, nombre, desde, hasta) VALUES (1, 'Invierno', 20640, 20651)")
        db.query("SELECT nombre FROM recesos").use { c ->
            c.moveToFirst()
            assertEquals("Invierno", c.getString(0))
        }
        db.close()
    }
}
