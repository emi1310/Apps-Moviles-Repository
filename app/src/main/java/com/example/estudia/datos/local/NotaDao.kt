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

    @Query("SELECT * FROM notas WHERE id = :id")
    suspend fun obtener(id: Int): NotaEntity?

    // ---------- Historial de versiones ----------

    @Insert
    suspend fun insertarVersion(version: NotaVersionEntity)

    // De la más nueva a la más vieja.
    @Query("SELECT * FROM nota_versiones WHERE notaId = :notaId ORDER BY fecha DESC, id DESC")
    fun versiones(notaId: Int): Flow<List<NotaVersionEntity>>

    // Deja solo las últimas "dejar" versiones de la nota.
    @Query(
        """
        DELETE FROM nota_versiones WHERE notaId = :notaId AND id NOT IN (
            SELECT id FROM nota_versiones WHERE notaId = :notaId ORDER BY fecha DESC, id DESC LIMIT :dejar
        )
        """
    )
    suspend fun recortarVersiones(notaId: Int, dejar: Int)
}
