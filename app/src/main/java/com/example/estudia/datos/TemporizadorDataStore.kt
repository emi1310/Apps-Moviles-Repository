package com.example.estudia.datos

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.estudia.modelo.SesionEstudio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Guarda la sesión del temporizador que está en curso (si hay una), para
// que sobreviva a cerrar la app o a que Android la cierre. Es un dato
// temporal, por eso va en DataStore y no en Room.
private val Context.temporizadorDataStore by preferencesDataStore(name = "temporizador")

private val CLAVE_MATERIA_ID = intPreferencesKey("materia_id")
private val CLAVE_NOMBRE_MATERIA = stringPreferencesKey("nombre_materia")
private val CLAVE_MINUTOS = intPreferencesKey("minutos")
private val CLAVE_FIN = longPreferencesKey("fin_millis")            // no está = en pausa
private val CLAVE_RESTANTE_PAUSA = longPreferencesKey("restante_pausa_millis")

class TemporizadorDataStore(private val context: Context) {

    val sesion: Flow<SesionEstudio?> = context.temporizadorDataStore.data.map { datos ->
        val materiaId = datos[CLAVE_MATERIA_ID] ?: return@map null
        SesionEstudio(
            materiaId = materiaId,
            nombreMateria = datos[CLAVE_NOMBRE_MATERIA] ?: "",
            minutos = datos[CLAVE_MINUTOS] ?: 25,
            finMillis = datos[CLAVE_FIN],
            restantePausaMillis = datos[CLAVE_RESTANTE_PAUSA] ?: 0
        )
    }

    suspend fun actual(): SesionEstudio? = sesion.first()

    suspend fun guardar(sesion: SesionEstudio) {
        context.temporizadorDataStore.edit { datos ->
            datos[CLAVE_MATERIA_ID] = sesion.materiaId
            datos[CLAVE_NOMBRE_MATERIA] = sesion.nombreMateria
            datos[CLAVE_MINUTOS] = sesion.minutos
            val fin = sesion.finMillis
            if (fin != null) datos[CLAVE_FIN] = fin else datos.remove(CLAVE_FIN)
            datos[CLAVE_RESTANTE_PAUSA] = sesion.restantePausaMillis
        }
    }

    suspend fun borrar() {
        context.temporizadorDataStore.edit { it.clear() }
    }
}
