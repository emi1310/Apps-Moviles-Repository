package com.example.estudia.datos.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Operaciones sobre las tablas "tareas" y "subtareas".
// Es una clase abstracta (y no una interfaz) para poder escribir métodos
// con @Transaction que hacen varias operaciones juntas: si una falla, no
// se guarda ninguna.
@Dao
abstract class TareaDao {

    // Para "Deshacer" se vuelve a insertar la tarea con el mismo id.
    // Devuelve el id (el generado por SQLite si era una tarea nueva).
    @Insert
    abstract suspend fun insertar(tarea: TareaEntity): Long

    @Update
    abstract suspend fun actualizar(tarea: TareaEntity)

    @Delete
    abstract suspend fun eliminar(tarea: TareaEntity)

    // ---------- Subtareas ----------

    @Insert
    abstract suspend fun insertarSubtareas(subtareas: List<SubtareaEntity>)

    @Update
    abstract suspend fun actualizarSubtarea(subtarea: SubtareaEntity)

    @Query("DELETE FROM subtareas WHERE tareaId = :tareaId")
    abstract suspend fun eliminarSubtareasDe(tareaId: Int)

    // Tarea nueva con sus subtareas: primero la tarea (para tener su id) y
    // después los pasos apuntando a ese id.
    @Transaction
    open suspend fun insertarConSubtareas(tarea: TareaEntity, subtareas: List<SubtareaEntity>): Int {
        val id = insertar(tarea).toInt()
        insertarSubtareas(subtareas.map { it.copy(tareaId = id) })
        return id
    }

    // Al editar una tarea se reemplaza la lista completa de subtareas (las
    // que ya existían conservan su id y si estaban hechas).
    @Transaction
    open suspend fun reemplazarSubtareas(tareaId: Int, subtareas: List<SubtareaEntity>) {
        eliminarSubtareasDe(tareaId)
        insertarSubtareas(subtareas.map { it.copy(tareaId = tareaId) })
    }

    // Lo usa el aviso de cada tarea (TareaWorker), que solo conoce el id.
    @Query(
        """
        SELECT tareas.*, materias.nombre AS nombreMateria, materias.usuarioId AS usuarioId
        FROM tareas INNER JOIN materias ON materias.id = tareas.materiaId
        WHERE tareas.id = :id
        """
    )
    abstract suspend fun buscarConMateria(id: Int): TareaConUsuarioFila?

    // Buscador: tareas del usuario cuyo título contiene el texto.
    // El parámetro :texto lo vincula Room de forma segura (sin inyección SQL).
    // Las que no tienen fecha van al final (fechaLimite IS NULL vale 1).
    @Query(
        """
        SELECT tareas.*, materias.nombre AS nombreMateria, materias.color AS colorMateria
        FROM tareas INNER JOIN materias ON materias.id = tareas.materiaId
        WHERE materias.usuarioId = :usuarioId AND tareas.titulo LIKE '%' || :texto || '%'
        ORDER BY tareas.completada, tareas.prioridad DESC, tareas.fechaLimite IS NULL, tareas.fechaLimite
        """
    )
    abstract fun buscar(usuarioId: Int, texto: String): Flow<List<TareaConMateriaFila>>
}
