package com.example.estudia.datos.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// Feriados guardados (versión 10). Cada vez que se actualizan se reemplazan
// todos por los que se trajeron.
@Dao
interface FeriadoDao {

    @Query("SELECT * FROM feriados ORDER BY fecha")
    fun observarTodos(): Flow<List<FeriadoEntity>>

    @Query("SELECT * FROM feriados ORDER BY fecha")
    suspend fun obtenerTodos(): List<FeriadoEntity>

    @Query("DELETE FROM feriados")
    suspend fun borrarTodos()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(feriados: List<FeriadoEntity>)

    @Transaction
    suspend fun reemplazar(feriados: List<FeriadoEntity>) {
        borrarTodos()
        insertar(feriados)
    }
}
