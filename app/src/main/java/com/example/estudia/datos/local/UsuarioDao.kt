package com.example.estudia.datos.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// Operaciones sobre la tabla "usuarios".
// Regla práctica de la guía: Flow para observar datos que cambian,
// suspend para operaciones puntuales.
@Dao
interface UsuarioDao {

    // Devuelve el usuario con todas sus materias, tareas, eventos y notas.
    // @Transaction porque Room hace varias consultas (una por @Relation).
    // Como es un Flow, vuelve a emitir cuando cambia cualquiera de esas tablas.
    @Transaction
    @Query("SELECT * FROM usuarios WHERE id = :id")
    fun observarConMaterias(id: Int): Flow<UsuarioConMaterias?>

    // La contraseña no se compara en SQL: se trae el hash y se verifica
    // en el Repository con Contrasenas.verificar().
    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :nombreUsuario LIMIT 1")
    suspend fun buscarPorNombreUsuario(nombreUsuario: String): UsuarioEntity?

    @Query("SELECT COUNT(*) FROM usuarios WHERE nombreUsuario = :nombreUsuario")
    suspend fun contarPorNombreUsuario(nombreUsuario: String): Int

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int

    // Devuelve el id generado por SQLite.
    @Insert
    suspend fun insertar(usuario: UsuarioEntity): Long

    @Query("UPDATE usuarios SET nombre = :nombre, carrera = :carrera, anio = :anio, email = :email WHERE id = :id")
    suspend fun actualizarPerfil(id: Int, nombre: String, carrera: String?, anio: Int?, email: String?)

    @Query("UPDATE usuarios SET metaHorasSemanales = :horas WHERE id = :id")
    suspend fun actualizarMetaSemanal(id: Int, horas: Int?)
}
