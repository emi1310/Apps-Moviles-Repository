package com.example.estudia

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.estudia.datos.local.AppDatabase
import com.example.estudia.datos.local.MIGRACION_2_3
import com.example.estudia.datos.local.MIGRACION_3_4
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
}
