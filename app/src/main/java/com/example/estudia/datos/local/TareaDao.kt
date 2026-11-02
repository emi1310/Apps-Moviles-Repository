package com.example.estudia.datos.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Operaciones sobre la tabla "tareas".
@Dao
interface TareaDao {

    // Para "Deshacer" se vuelve a insertar la tarea con el mismo id.
    @Insert
    suspend fun insertar(tarea: TareaEntity)

    @Update
    suspend fun actualizar(tarea: TareaEntity)

    @Delete
    suspend fun eliminar(tarea: TareaEntity)

    // Buscador: tareas del usuario cuyo título contiene el texto.
    // El parámetro :texto lo vincula Room de forma segura (sin inyección SQL).
    // Las que no tienen fecha van al final (fechaLimite IS NULL vale 1).
    @Query(
        """
        SELECT tareas.*, materias.nombre AS nombreMateria, materias.color AS colorMateria
        FROM tareas INNER JOIN materias ON materias.id = tareas.materiaId
        WHERE materias.usuarioId = :usuarioId AND tareas.titulo LIKE '%' || :texto || '%'
        ORDER BY tareas.completada, tareas.fechaLimite IS NULL, tareas.fechaLimite
        """
    )
    fun buscar(usuarioId: Int, texto: String): Flow<List<TareaConMateriaFila>>
}
