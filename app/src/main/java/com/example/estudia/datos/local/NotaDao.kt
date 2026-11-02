package com.example.estudia.datos.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Operaciones sobre la tabla "notas".
@Dao
interface NotaDao {

    @Insert
    suspend fun insertar(nota: NotaEntity): Long

    @Update
    suspend fun actualizar(nota: NotaEntity)

    // El CASCADE borra también las filas de sus archivos adjuntos.
    @Delete
    suspend fun eliminar(nota: NotaEntity)

    // Buscador: notas del usuario cuyo título o contenido contiene el texto,
    // de la más nueva a la más vieja.
    @Query(
        """
        SELECT notas.*, materias.nombre AS nombreMateria
        FROM notas INNER JOIN materias ON materias.id = notas.materiaId
        WHERE materias.usuarioId = :usuarioId
          AND (notas.titulo LIKE '%' || :texto || '%' OR notas.contenido LIKE '%' || :texto || '%')
        ORDER BY notas.fechaCreacion DESC
        """
    )
    fun buscar(usuarioId: Int, texto: String): Flow<List<NotaConMateriaFila>>
}
