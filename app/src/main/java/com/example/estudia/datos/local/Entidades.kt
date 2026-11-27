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
    val metaHorasSemanales: Int? = null,
    // Versión 6: nombre del archivo de la foto dentro de filesDir/archivos.
    val fotoPerfil: String? = null
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
    val archivada: Boolean = false,
    // Versión 10: reglas de la cursada (ver Materia).
    @ColumnInfo(defaultValue = "75")
    val asistenciaMinima: Int = 75,
    @ColumnInfo(defaultValue = "1")
    val promocional: Boolean = true,
    @ColumnInfo(defaultValue = "7.0")
    val notaPromocion: Double = 7.0,
    @ColumnInfo(defaultValue = "1")
    val tieneFinal: Boolean = true
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
    val completada: Boolean = false,
    // Versión 6: 0 = baja, 1 = media, 2 = alta (ver Prioridad.peso). Las
    // tareas que ya existían quedan en media.
    @ColumnInfo(defaultValue = "1")
    val prioridad: Int = 1,
    // Versión 7: días antes de la fecha límite en que se avisa (null = sin aviso).
    val avisoDiasAntes: Int? = null,
    // Versión 14: cada cuánto se repite (nombre de Repeticion) o null.
    val repeticion: String? = null
)

// Versión 8: tarjetas de estudio de una materia (pregunta y respuesta), con
// su caja de Leitner y la fecha del próximo repaso.
@Entity(
    tableName = "tarjetas",
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
data class TarjetaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val materiaId: Int,
    val pregunta: String,
    val respuesta: String,
    val caja: Int,
    val proximoRepaso: LocalDate,
    // Versión 15: imagen de la pregunta (archivo en filesDir/tarjetas).
    val imagen: String? = null
)

// Versión 7: temas del temario de un examen. Se borran con su evento.
@Entity(
    tableName = "temas",
    foreignKeys = [
        ForeignKey(
            entity = EventoEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventoId")]
)
data class TemaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventoId: Int,
    val titulo: String,
    val estudiado: Boolean = false
)

// Versión 6: pasos de una tarea. Si se borra la tarea (o su materia), el
// CASCADE borra también sus subtareas.
@Entity(
    tableName = "subtareas",
    foreignKeys = [
        ForeignKey(
            entity = TareaEntity::class,
            parentColumns = ["id"],
            childColumns = ["tareaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tareaId")]
)
data class SubtareaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tareaId: Int,
    val titulo: String,
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
    val repetirHasta: LocalDate? = null,
    // Versión 10: clase virtual (con link) o presencial (con aula o lugar).
    @ColumnInfo(defaultValue = "0")
    val virtual: Boolean = false,
    val lugar: String? = null,
    val enlace: String? = null,
    // Versión 14: 1 = todas las semanas, 2 = semana por medio.
    @ColumnInfo(defaultValue = "1")
    val cadaSemanas: Int = 1
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
    val esFavorita: Boolean = false,
    val fijada: Boolean = false,
    val color: Long? = null
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
    val fechaAgregado: LocalDate,
    // Versión 10: "TEORIA", "TPS", "INFORMACION", "EXAMENES" u "OTROS".
    @ColumnInfo(defaultValue = "'OTROS'")
    val categoria: String = "OTROS"
)

// Versión 5: notas de los exámenes de cada materia (escala de 1 a 10).
@Entity(
    tableName = "calificaciones",
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
data class CalificacionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val materiaId: Int,
    val titulo: String,
    val nota: Double,
    val fecha: LocalDate
)

// ---------- Versión 9 ----------

// "Mi carrera": materias del plan de estudios del usuario. Las correlativas
// se guardan como texto con los ids separados por coma ("3,5"): son pocas y
// siempre se leen junto con todo el plan, no hace falta otra tabla.
@Entity(
    tableName = "plan_materias",
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
data class MateriaPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val usuarioId: Int,
    val nombre: String,
    val anio: Int,
    val estado: String,          // "PENDIENTE", "CURSANDO", "REGULAR" o "APROBADA"
    val correlativas: String,
    val notaFinal: Double? = null
)

// Asistencia a una clase en un día. Una sola fila por clase y día (índice
// único): marcar de nuevo reemplaza la anterior.
@Entity(
    tableName = "asistencias",
    foreignKeys = [
        ForeignKey(
            entity = EventoEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["eventoId", "fecha"], unique = true)]
)
data class AsistenciaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val eventoId: Int,
    val fecha: LocalDate,
    val presente: Boolean
)

// "¿Qué nota necesito?": evaluaciones de una materia con su peso (%).
@Entity(
    tableName = "evaluaciones",
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
data class EvaluacionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val materiaId: Int,
    val nombre: String,
    val peso: Int,
    val nota: Double? = null
)

// Mesas de finales. Son del usuario (no de una materia de la app): la
// materia puede estar archivada o no existir, se guarda su nombre.
@Entity(
    tableName = "finales",
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
data class FinalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val usuarioId: Int,
    val materia: String,
    val fechaMesa: LocalDate,
    val cierreInscripcion: LocalDate? = null,
    val vencimientoRegularidad: LocalDate? = null,
    val inscripto: Boolean = false,
    val nota: Double? = null
)

// Versión 11: bibliografía. Como los finales, es del usuario y guarda el
// nombre de la materia (vacío = general). tipo: LIBRO, ARTICULO, WEB o APUNTE.
@Entity(
    tableName = "fuentes",
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
data class FuenteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val usuarioId: Int,
    val materia: String,
    val tipo: String,
    val autores: String,
    val anio: Int? = null,
    val titulo: String,
    val origen: String = "",
    val enlace: String = "",
    val paginas: String = "",
    val comentario: String = ""
)

// Versión 12: mapas mentales. Como la bibliografía, son del usuario y
// guardan el nombre de la materia. Las ideas van en una sola columna de
// texto (ver codificarNodos): se leen y se guardan siempre todas juntas.
@Entity(
    tableName = "mapas",
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
data class MapaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val usuarioId: Int,
    val materia: String,
    val titulo: String,
    val nodos: String,
    val modificado: LocalDate
)

// Versión 10: feriados (traídos del calendario de Google del teléfono o de
// internet). Son de todos los usuarios del teléfono: no dependen de la cuenta.
@Entity(tableName = "feriados")
data class FeriadoEntity(
    @PrimaryKey
    val fecha: LocalDate,
    val nombre: String
)

// Historial de una nota: cómo era antes de cada cambio de título o texto.
// fecha es epoch en milisegundos. Se borra junto con la nota (CASCADE).
@Entity(
    tableName = "nota_versiones",
    foreignKeys = [
        ForeignKey(
            entity = NotaEntity::class,
            parentColumns = ["id"],
            childColumns = ["notaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notaId")]
)
data class NotaVersionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val notaId: Int,
    val titulo: String,
    val contenido: String,
    val fecha: Long
)

// Versión 14: recesos del usuario (días sin clases, de "desde" a "hasta").
@Entity(
    tableName = "recesos",
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
data class RecesoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val usuarioId: Int,
    val nombre: String,
    val desde: LocalDate,
    val hasta: LocalDate
)
