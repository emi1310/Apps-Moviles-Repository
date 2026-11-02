package com.example.estudia.datos.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

// Operaciones sobre la tabla "eventos".
@Dao
interface EventoDao {

    @Insert
    suspend fun insertar(evento: EventoEntity): Long

    @Update
    suspend fun actualizar(evento: EventoEntity)

    @Delete
    suspend fun eliminar(evento: EventoEntity)

    // Lo usa el recordatorio de cada evento (EventoWorker), que corre sin
    // pantalla y solo conoce el id del evento.
    @Query(
        """
        SELECT eventos.*, materias.nombre AS nombreMateria, materias.usuarioId AS usuarioId
        FROM eventos INNER JOIN materias ON materias.id = eventos.materiaId
        WHERE eventos.id = :id
        """
    )
    suspend fun buscarConMateria(id: Int): EventoConMateriaFila?
}
