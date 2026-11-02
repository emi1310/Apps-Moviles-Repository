package com.example.estudia.datos

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.estudia.modelo.PreferenciaTema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Preferencias de la app (clave-valor) que no son datos del estudio:
// si el usuario quiere recibir recordatorios y qué tema prefiere.
private val Context.preferenciasDataStore by preferencesDataStore(name = "preferencias")

private val CLAVE_RECORDATORIOS = booleanPreferencesKey("recordatorios_activos")
private val CLAVE_TEMA = stringPreferencesKey("tema")

class PreferenciasDataStore(private val context: Context) {

    // Por defecto están desactivados: el permiso de notificaciones se pide
    // recién cuando el usuario los activa (no apenas se abre la app).
    val recordatoriosActivos: Flow<Boolean> = context.preferenciasDataStore.data
        .map { preferencias -> preferencias[CLAVE_RECORDATORIOS] ?: false }

    val tema: Flow<PreferenciaTema> = context.preferenciasDataStore.data
        .map { preferencias ->
            val guardado = preferencias[CLAVE_TEMA]
            PreferenciaTema.entries.firstOrNull { it.name == guardado } ?: PreferenciaTema.SISTEMA
        }

    suspend fun guardarRecordatoriosActivos(activos: Boolean) {
        context.preferenciasDataStore.edit { preferencias ->
            preferencias[CLAVE_RECORDATORIOS] = activos
        }
    }

    suspend fun guardarTema(tema: PreferenciaTema) {
        context.preferenciasDataStore.edit { preferencias ->
            preferencias[CLAVE_TEMA] = tema.name
        }
    }
}
