package com.example.estudia

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.estudia.datos.local.AppDatabase
import com.example.estudia.datos.local.ArchivoEntity
import com.example.estudia.datos.local.EventoEntity
import com.example.estudia.datos.local.MateriaEntity
import com.example.estudia.datos.local.NotaEntity
import com.example.estudia.datos.local.TareaEntity
import com.example.estudia.datos.local.UsuarioEntity
import com.example.estudia.datos.local.aModelo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime

// Tests de los DAOs con una base de datos en memoria: se crea vacía en
// cada test y desaparece al terminar (no toca los datos reales de la app).
@RunWith(AndroidJUnit4::class)
class DaosTest {

    private lateinit var db: AppDatabase
    private val hoy = LocalDate.of(2026, 11, 4)
    private var usuarioId = 0
    private var materiaId = 0

    @Before
    fun crearBase() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        usuarioId = db.usuarioDao().insertar(UsuarioEntity(nombreUsuario = "ana", contrasenaHash = "x", metodoAuth = "LOCAL", nombre = "Ana")).toInt()
        materiaId = db.materiaDao().insertar(MateriaEntity(usuarioId = usuarioId, nombre = "Física", color = 1)).toInt()
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun lasFechasYHorasSeGuardanYLeenIgual() = runTest {
        db.eventoDao().insertar(EventoEntity(materiaId = materiaId, titulo = "Parcial", tipo = "EXAMEN", fecha = hoy, horaInicio = LocalTime.of(18, 30), horaFin = LocalTime.of(20, 0)))
        db.tareaDao().insertar(TareaEntity(materiaId = materiaId, titulo = "Sin fecha", fechaLimite = null))

        val usuario = db.usuarioDao().observarConMaterias(usuarioId).first()!!.aModelo()
        val evento = usuario.materias.single().eventos.single()
        assertEquals(hoy, evento.fecha)
        assertEquals(LocalTime.of(18, 30), evento.horaInicio)
        assertEquals(null, usuario.materias.single().tareas.single().fechaLimite)
    }

    @Test
    fun elBuscadorUsaLike() = runTest {
        db.notaDao().insertar(NotaEntity(materiaId = materiaId, titulo = "Ondas", contenido = "frecuencia", fechaCreacion = hoy))
        db.notaDao().insertar(NotaEntity(materiaId = materiaId, titulo = "Óptica", contenido = "lentes", fechaCreacion = hoy))
        db.tareaDao().insertar(TareaEntity(materiaId = materiaId, titulo = "Leer capítulo 3", fechaLimite = hoy))

        assertEquals(listOf("Ondas"), db.notaDao().buscar(usuarioId, "FREC").first().map { it.nota.titulo })
        assertEquals(listOf("Física"), db.notaDao().buscar(usuarioId, "lentes").first().map { it.nombreMateria })
        assertEquals(1, db.tareaDao().buscar(usuarioId, "capítulo").first().size)
        // Otro usuario no ve las notas de Ana.
        assertTrue(db.notaDao().buscar(usuarioId + 1, "Ondas").first().isEmpty())
    }

    @Test
    fun borrarUnaMateriaBorraTodoSuContenido() = runTest {
        val notaId = db.notaDao().insertar(NotaEntity(materiaId = materiaId, titulo = "N", contenido = "", fechaCreacion = hoy)).toInt()
        db.tareaDao().insertar(TareaEntity(materiaId = materiaId, titulo = "T", fechaLimite = null))
        db.archivoDao().insertar(ArchivoEntity(materiaId = materiaId, notaId = notaId, nombre = "a.pdf", tipoMime = "application/pdf", ruta = "a", tamanoBytes = 1, fechaAgregado = hoy))

        val materia = db.usuarioDao().observarConMaterias(usuarioId).first()!!.materias.single().materia
        db.materiaDao().eliminar(materia)

        val usuario = db.usuarioDao().observarConMaterias(usuarioId).first()
        assertNotNull(usuario)
        assertTrue(usuario!!.materias.isEmpty())
        assertTrue(db.archivoDao().todasLasRutas().isEmpty())
    }

    @Test
    fun losAdjuntosDeUnaNotaVanASuNota() = runTest {
        val notaId = db.notaDao().insertar(NotaEntity(materiaId = materiaId, titulo = "N", contenido = "", fechaCreacion = hoy)).toInt()
        db.archivoDao().insertar(ArchivoEntity(materiaId = materiaId, notaId = notaId, nombre = "foto.jpg", tipoMime = "image/jpeg", ruta = "f", tamanoBytes = 1, fechaAgregado = hoy))
        db.archivoDao().insertar(ArchivoEntity(materiaId = materiaId, notaId = null, nombre = "programa.pdf", tipoMime = "application/pdf", ruta = "p", tamanoBytes = 1, fechaAgregado = hoy))

        val materia = db.usuarioDao().observarConMaterias(usuarioId).first()!!.aModelo().materias.single()
        assertEquals(listOf("programa.pdf"), materia.archivos.map { it.nombre })
        assertEquals(listOf("foto.jpg"), materia.notas.single().archivos.map { it.nombre })
    }
}
