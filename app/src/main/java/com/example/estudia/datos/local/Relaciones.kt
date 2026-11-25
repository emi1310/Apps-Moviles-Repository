package com.example.estudia.datos.local

import androidx.room.Embedded
import androidx.room.Relation

// Clases que Room arma juntando varias tablas con @Relation, o con un JOIN.
// No son tablas: son el resultado de una consulta.

// Una materia con todas sus tareas, eventos, notas, archivos y calificaciones (los archivos
// incluyen los de sus notas: Mapeos.kt los reparte según notaId).
data class MateriaConDetalle(
    @Embedded val materia: MateriaEntity,
    // Cada tarea viene con sus subtareas (una relación dentro de otra).
    @Relation(entity = TareaEntity::class, parentColumn = "id", entityColumn = "materiaId")
    val tareas: List<TareaConSubtareas>,
    // Cada evento viene con los temas de su temario (versión 7).
    @Relation(entity = EventoEntity::class, parentColumn = "id", entityColumn = "materiaId")
    val eventos: List<EventoConTemas>,
    @Relation(parentColumn = "id", entityColumn = "materiaId")
    val notas: List<NotaEntity>,
    @Relation(parentColumn = "id", entityColumn = "materiaId")
    val archivos: List<ArchivoEntity>,
    @Relation(parentColumn = "id", entityColumn = "materiaId")
    val calificaciones: List<CalificacionEntity>,
    @Relation(parentColumn = "id", entityColumn = "materiaId")
    val tarjetas: List<TarjetaEntity>
)

// Un evento con los temas de su temario (versión 7).
data class EventoConTemas(
    @Embedded val evento: EventoEntity,
    @Relation(parentColumn = "id", entityColumn = "eventoId")
    val temas: List<TemaEntity>
)

// Una tarea con sus subtareas (versión 6).
data class TareaConSubtareas(
    @Embedded val tarea: TareaEntity,
    @Relation(parentColumn = "id", entityColumn = "tareaId")
    val subtareas: List<SubtareaEntity>
)

// Un usuario con todas sus materias (y, dentro de cada una, su detalle).
data class UsuarioConMaterias(
    @Embedded val usuario: UsuarioEntity,
    @Relation(
        entity = MateriaEntity::class,
        parentColumn = "id",
        entityColumn = "usuarioId"
    )
    val materias: List<MateriaConDetalle>
)

// Resultado de un JOIN: una fila de otra tabla + datos de su materia.
data class NotaConMateriaFila(
    @Embedded val nota: NotaEntity,
    val nombreMateria: String
)

data class TareaConMateriaFila(
    @Embedded val tarea: TareaEntity,
    val nombreMateria: String,
    val colorMateria: Long
)

data class TareaConUsuarioFila(
    @Embedded val tarea: TareaEntity,
    val nombreMateria: String,
    val usuarioId: Int
)

data class EventoConMateriaFila(
    @Embedded val evento: EventoEntity,
    val nombreMateria: String,
    val usuarioId: Int
)
