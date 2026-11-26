package com.example.estudia.datos.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Base de datos Room de la app: conecta las entidades (tablas) con los
// DAO (operaciones). Se usa una única instancia (patrón singleton) para
// no crear la base de datos cada vez que se abre una pantalla.
//
// Versiones del esquema (cada una exportada a app/schemas):
//  1 - usuarios, materias, tareas, eventos y notas.
//  2 - se agrega la tabla "archivos" (adjuntos de cada materia).
//  3 - contraseñas con hash, fechas reales, metas de estudio y
//      archivos adjuntos a notas.
//  4 - clases que se repiten cada semana (eventos.semanal y repetirHasta).
//  5 - notas de los exámenes (tabla "calificaciones").
//  6 - prioridad de las tareas, subtareas (tabla "subtareas") y foto de
//      perfil del usuario.
//  7 - aviso de cada tarea (tareas.avisoDiasAntes) y temario de los
//      exámenes (tabla "temas").
//  8 - tarjetas de estudio con repetición espaciada (tabla "tarjetas").
//  9 - plan de estudios, asistencias, evaluaciones y mesas de finales.
// 10 - asistencia mínima, promoción y final de cada materia, clases
//      virtuales, categoría de los archivos y feriados.
// Cada vez que cambia el esquema se sube "version" y se agrega una
// migración (Migraciones.kt), para no perder los datos ya guardados.
@Database(
    entities = [
        UsuarioEntity::class,
        MateriaEntity::class,
        TareaEntity::class,
        EventoEntity::class,
        NotaEntity::class,
        ArchivoEntity::class,
        CalificacionEntity::class,
        SubtareaEntity::class,
        TemaEntity::class,
        TarjetaEntity::class,
        MateriaPlanEntity::class,
        AsistenciaEntity::class,
        EvaluacionEntity::class,
        FinalEntity::class,
        FeriadoEntity::class
    ],
    version = 10,
    exportSchema = true
)
@TypeConverters(Conversores::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun materiaDao(): MateriaDao
    abstract fun tareaDao(): TareaDao
    abstract fun eventoDao(): EventoDao
    abstract fun notaDao(): NotaDao
    abstract fun archivoDao(): ArchivoDao
    abstract fun calificacionDao(): CalificacionDao
    abstract fun temaDao(): TemaDao
    abstract fun tarjetaDao(): TarjetaDao
    abstract fun carreraDao(): CarreraDao
    abstract fun feriadoDao(): FeriadoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun obtenerInstancia(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "estudia_db"
                )
                    .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4, MIGRACION_4_5, MIGRACION_5_6, MIGRACION_6_7, MIGRACION_7_8, MIGRACION_8_9, MIGRACION_9_10)
                    .build()
                    .also {
                        INSTANCE = it
                    }
            }
        }
    }
}
