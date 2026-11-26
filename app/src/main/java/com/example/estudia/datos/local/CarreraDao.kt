package com.example.estudia.datos.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

// Operaciones de las tablas de la versión 9: plan de estudios, asistencias,
// evaluaciones y finales. Se leen junto con el usuario (UsuarioConMaterias),
// por eso casi no hay consultas de lectura.
@Dao
interface CarreraDao {

    // ---------- Plan de estudios ----------

    @Insert
    suspend fun insertarMateriaPlan(materia: MateriaPlanEntity): Long

    @Update
    suspend fun actualizarMateriaPlan(materia: MateriaPlanEntity)

    @Delete
    suspend fun eliminarMateriaPlan(materia: MateriaPlanEntity)

    // ---------- Asistencias ----------

    // REPLACE: si ya había una para esa clase y ese día, se reemplaza.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarAsistencia(asistencia: AsistenciaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarAsistencias(asistencias: List<AsistenciaEntity>)

    @Query("DELETE FROM asistencias WHERE eventoId = :eventoId AND fecha = :fecha")
    suspend fun borrarAsistencia(eventoId: Int, fecha: java.time.LocalDate)

    // ---------- Evaluaciones ----------

    @Insert
    suspend fun insertarEvaluacion(evaluacion: EvaluacionEntity): Long

    @Update
    suspend fun actualizarEvaluacion(evaluacion: EvaluacionEntity)

    @Delete
    suspend fun eliminarEvaluacion(evaluacion: EvaluacionEntity)

    // ---------- Finales ----------

    @Insert
    suspend fun insertarFinal(final: FinalEntity): Long

    @Update
    suspend fun actualizarFinal(final: FinalEntity)

    @Delete
    suspend fun eliminarFinal(final: FinalEntity)

    // ---------- Bibliografía (versión 11) ----------

    @Insert
    suspend fun insertarFuente(fuente: FuenteEntity): Long

    @Update
    suspend fun actualizarFuente(fuente: FuenteEntity)

    @Delete
    suspend fun eliminarFuente(fuente: FuenteEntity)

    // ---------- Mapas mentales (versión 12) ----------

    @Insert
    suspend fun insertarMapa(mapa: MapaEntity): Long

    @Update
    suspend fun actualizarMapa(mapa: MapaEntity)

    @Delete
    suspend fun eliminarMapa(mapa: MapaEntity)
}
