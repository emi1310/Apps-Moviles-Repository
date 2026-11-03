package com.example.estudia.datos.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

// Entidades de Room: cada clase es una tabla de la base SQLite y cada
// propiedad es una columna. Los id usan autoGenerate = true, así SQLite
// genera el identificador (por eso el valor inicial es 0).
//
// Las relaciones Usuario -> Materia -> Tarea/Evento/Nota/Archivo se guardan
// con claves foráneas (ForeignKey) y onDelete = CASCADE: si se borra una
// materia, SQLite borra solas sus tareas, eventos, notas y archivos.
//
// LocalDate y LocalTime no son tipos de SQLite: los convierte Conversores.kt
// (LocalDate -> número de día, LocalTime -> minutos desde las 00:00).

@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["nombreUsuario"], unique = true)]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombreUsuario: String,
    // Hash PBKDF2 de la contraseña (ver util/Contrasenas.kt), nunca la contraseña.
    val contrasenaHash: String?,
    val metodoAuth: String,      // "LOCAL" o "GOOGLE"
    val nombre: String,
    val carrera: String? = null,
    val anio: Int? = null,
    val email: String? = null,
    val metaHorasSemanales: Int? = null
)

@Entity(
    tableName = "materias",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("usuarioId")]
)
data class MateriaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val usuarioId: Int,
    val nombre: String,
    val color: Long,             // ARGB, ej: 0xFF6C5CE7
    val archivada: Boolean = false
)

@Entity(
    tableName = "tareas",
    foreignKeys = [
        ForeignKey(
            entity = MateriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["materiaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("materiaId")]
)
data class TareaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val materiaId: Int,
    val titulo: String,
    val fechaLimite: LocalDate?,
    val completada: Boolean = false
)

@Entity(
    tableName = "eventos",
    foreignKeys = [
        ForeignKey(
            entity = MateriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["materiaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("materiaId")]
)
data class EventoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val materiaId: Int,
    val titulo: String,
    val tipo: String,            // "CLASE", "EXAMEN", "ENTREGA" o "BLOQUE_ESTUDIO"
    val fecha: LocalDate,
    val horaInicio: LocalTime,
    val horaFin: LocalTime,
    // Versión 4: clases que se repiten cada semana (ver Evento.ocurreEl).
    @ColumnInfo(defaultValue = "0")
    val semanal: Boolean = false,
    val repetirHasta: LocalDate? = null
)

@Entity(
    tableName = "notas",
    foreignKeys = [
        ForeignKey(
            entity = MateriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["materiaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("materiaId")]
)
data class NotaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val materiaId: Int,
    val titulo: String,
    val contenido: String,
    val fechaCreacion: LocalDate,
    val esFavorita: Boolean = false
)

// Archivo adjunto a una materia o a una nota. En la base solo se guardan
// los datos del archivo: el contenido se copia al almacenamiento interno de
// la app (filesDir/archivos), y "ruta" es el nombre dentro de esa carpeta.
// notaId es null si el archivo es de la materia y no de una nota.
@Entity(
    tableName = "archivos",
    foreignKeys = [
        ForeignKey(
            entity = MateriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["materiaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = NotaEntity::class,
            parentColumns = ["id"],
            childColumns = ["notaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("materiaId"), Index("notaId")]
)
data class ArchivoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val materiaId: Int,
    val notaId: Int? = null,
    val nombre: String,          // nombre original, ej: "parcial.pdf"
    val tipoMime: String,        // ej: "application/pdf", "image/jpeg"
    val ruta: String,
    val tamanoBytes: Long,
    val fechaAgregado: LocalDate
)
