package com.example.estudia.datos.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update

// Operaciones sobre la tabla "tarjetas" (tarjetas de estudio, versión 8).
// Se leen junto con la materia (MateriaConDetalle), por eso no hay consultas.
@Dao
interface TarjetaDao {

    @Insert
    suspend fun insertar(tarjeta: TarjetaEntity): Long

    @Update
    suspend fun actualizar(tarjeta: TarjetaEntity)

    @Delete
    suspend fun eliminar(tarjeta: TarjetaEntity)
}
