package com.example.estudia.datos.local

import androidx.room.Embedded
import androidx.room.Relation

// Clases que Room arma juntando varias tablas con @Relation, o con un JOIN.
// No son tablas: son el resultado de una consulta.

// Una materia con todas sus tareas, eventos, notas, archivos y calificaciones (los archivos
// incluyen los de sus notas: Mapeos.kt los reparte según notaId).
data class MateriaConDetalle(
    @Embedded val materia: MateriaEntity,
    @Relation(parentColumn = "id", entityColumn = "materiaId")
    val tareas: List<TareaEntity>,
    @Relation(parentColumn = "id", entityColumn = "materiaId")
    val eventos: List<EventoEntity>,
    @Relation(parentColumn = "id", entityColumn = "materiaId")
    val notas: List<NotaEntity>,
    @Relation(parentColumn = "id", entityColumn = "materiaId")
    val archivos: List<ArchivoEntity>,
    @Relation(parentColumn = "id", entityColumn = "materiaId")
    val calificaciones: List<CalificacionEntity>
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

data class EventoConMateriaFila(
    @Embedded val evento: EventoEntity,
    val nombreMateria: String,
    val usuarioId: Int
)
