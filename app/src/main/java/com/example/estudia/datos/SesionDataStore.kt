package com.example.estudia.datos

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sesionDataStore by preferencesDataStore(name = "sesion")

private val CLAVE_ID_USUARIO_ACTIVO = intPreferencesKey("id_usuario_activo")

class SesionDataStore(private val context: Context) {

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
