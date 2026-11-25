package com.example.estudia.datos.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update

// Operaciones sobre la tabla "temas" (el temario de cada examen, versión 7).
@Dao
interface TemaDao {

    @Insert
    suspend fun insertar(tema: TemaEntity): Long

    // Para "Deshacer" al restaurar un evento o una materia con sus temas.
    @Insert
    suspend fun insertarTodos(temas: List<TemaEntity>)

    @Update
    suspend fun actualizar(tema: TemaEntity)

    @Delete
    suspend fun eliminar(tema: TemaEntity)
}
