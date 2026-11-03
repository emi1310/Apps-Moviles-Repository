package com.example.estudia.datos.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update

// Operaciones sobre la tabla "calificaciones" (notas de los exámenes).
// Se leen junto con la materia (MateriaConDetalle), por eso no hay consultas.
@Dao
interface CalificacionDao {

    @Insert
    suspend fun insertar(calificacion: CalificacionEntity): Long

    @Update
    suspend fun actualizar(calificacion: CalificacionEntity)

    @Delete
    suspend fun eliminar(calificacion: CalificacionEntity)
}
