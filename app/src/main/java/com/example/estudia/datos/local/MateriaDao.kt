package com.example.estudia.datos.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update

// Operaciones sobre la tabla "materias". Al eliminar una materia,
// el CASCADE de las claves foráneas borra sus tareas, eventos y notas.
@Dao
interface MateriaDao {

    @Insert
    suspend fun insertar(materia: MateriaEntity): Long

    @Update
    suspend fun actualizar(materia: MateriaEntity)

    @Delete
    suspend fun eliminar(materia: MateriaEntity)
}
