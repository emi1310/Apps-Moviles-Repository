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
// Cada vez que cambia el esquema se sube "version" y se agrega una
// migración (Migraciones.kt), para no perder los datos ya guardados.
@Database(
    entities = [
        UsuarioEntity::class,
        MateriaEntity::class,
        TareaEntity::class,
        EventoEntity::class,
        NotaEntity::class,
        ArchivoEntity::class
    ],
    version = 3,
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
                    .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
                    .build()
                    .also {
                        INSTANCE = it
                    }
            }
        }
    }
}
