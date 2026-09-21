package com.example.estudia.datos

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore de preferencias clave-valor donde guardamos únicamente el id
// del usuario con la sesión activa. A diferencia del RepositorioDatos
// (que guarda todos los usuarios con sus materias en un archivo JSON),
// esto es solo "quién está logueado ahora", y se conecta con Flow.
private val Context.sesionDataStore by preferencesDataStore(name = "sesion")

private val CLAVE_ID_USUARIO_ACTIVO = intPreferencesKey("id_usuario_activo")

class SesionDataStore(private val context: Context) {

    // Emite el id del usuario con sesión guardada, o null si nadie inició sesión
    // (o si cerró sesión). Se usa una sola vez al arrancar la app para saber
    // si hay que mostrar el Login o ir directo a Inicio.
    val idUsuarioActivo: Flow<Int?> = context.sesionDataStore.data
        .map { preferencias -> preferencias[CLAVE_ID_USUARIO_ACTIVO] }

    suspend fun guardarSesion(idUsuario: Int) {
        context.sesionDataStore.edit { preferencias ->
            preferencias[CLAVE_ID_USUARIO_ACTIVO] = idUsuario
        }
    }

    suspend fun borrarSesion() {
        context.sesionDataStore.edit { preferencias ->
            preferencias.remove(CLAVE_ID_USUARIO_ACTIVO)
        }
    }
}
