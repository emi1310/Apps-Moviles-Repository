package com.example.estudia.datos.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

// Operaciones sobre la tabla "archivos".
@Dao
interface ArchivoDao {

    @Insert
    suspend fun insertar(archivo: ArchivoEntity)

    @Insert
    suspend fun insertarTodos(archivos: List<ArchivoEntity>)

    // Versión 10: para cambiar la categoría.
    @Update
    suspend fun actualizar(archivo: ArchivoEntity)

    @Delete
    suspend fun eliminar(archivo: ArchivoEntity)

    // Nombres de todos los archivos que la base conoce. Se usa para borrar
    // del disco los archivos que quedaron sin fila (ver limpiarArchivosHuerfanos).
    @Query("SELECT ruta FROM archivos")
    suspend fun todasLasRutas(): List<String>
}
