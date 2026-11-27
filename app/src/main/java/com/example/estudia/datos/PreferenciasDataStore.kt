package com.example.estudia.datos

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.estudia.modelo.EstadoCopiaAutomatica
import com.example.estudia.modelo.PreferenciaTema
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Preferencias de la app (clave-valor) que no son datos del estudio:
// si el usuario quiere recibir recordatorios, qué tema prefiere y si ya
// se le pidió el permiso de archivos.
private val Context.preferenciasDataStore by preferencesDataStore(name = "preferencias")

private val CLAVE_RECORDATORIOS = booleanPreferencesKey("recordatorios_activos")
private val CLAVE_TEMA = stringPreferencesKey("tema")
private val CLAVE_PERMISO_ARCHIVOS = booleanPreferencesKey("permiso_archivos_pedido")
private val CLAVE_SILENCIAR = booleanPreferencesKey("silenciar_al_estudiar")
private val CLAVE_META_DIARIA = intPreferencesKey("meta_diaria_minutos")
private val CLAVE_AVISAR_SALIR = booleanPreferencesKey("avisar_al_salir")
private val CLAVE_CARPETA_COPIA = stringPreferencesKey("carpeta_copia_automatica")
private val CLAVE_ULTIMA_COPIA = stringPreferencesKey("ultima_copia_automatica")
private val CLAVE_CALENDARIO_AUTO = booleanPreferencesKey("calendario_telefono_automatico")
private val CLAVE_GUIAS_VISTAS = stringSetPreferencesKey("guia_vista_usuarios")

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

    // Si ya se le explicó al usuario para qué es el permiso de fotos y
    // archivos (la explicación se muestra una sola vez).
    val permisoArchivosPedido: Flow<Boolean> = context.preferenciasDataStore.data
        .map { preferencias -> preferencias[CLAVE_PERMISO_ARCHIVOS] ?: false }

    // Si el usuario quiere silenciar el teléfono (No molestar) mientras
    // corre el temporizador. Por defecto no: necesita un acceso especial.
    val silenciarAlEstudiar: Flow<Boolean> = context.preferenciasDataStore.data
        .map { preferencias -> preferencias[CLAVE_SILENCIAR] ?: false }

    // Meta de minutos de estudio por día (0 = sin meta).
    val metaDiariaMinutos: Flow<Int> = context.preferenciasDataStore.data
        .map { preferencias -> preferencias[CLAVE_META_DIARIA] ?: 0 }

    suspend fun guardarMetaDiaria(minutos: Int) {
        context.preferenciasDataStore.edit { preferencias -> preferencias[CLAVE_META_DIARIA] = minutos.coerceAtLeast(0) }
    }

    // Aviso si sale de la app con el temporizador corriendo (por defecto sí).
    val avisarAlSalir: Flow<Boolean> = context.preferenciasDataStore.data
        .map { preferencias -> preferencias[CLAVE_AVISAR_SALIR] ?: true }

    suspend fun guardarAvisarAlSalir(activo: Boolean) {
        context.preferenciasDataStore.edit { preferencias -> preferencias[CLAVE_AVISAR_SALIR] = activo }
    }

    // Mejor tiempo del juego "Unir pares" por materia.
    fun mejorTiempoPares(materiaId: Int): Flow<Long?> = context.preferenciasDataStore.data
        .map { preferencias -> preferencias[longPreferencesKey("pares_mejor_$materiaId")] }

    suspend fun guardarMejorTiempoPares(materiaId: Int, millis: Long) {
        context.preferenciasDataStore.edit { preferencias -> preferencias[longPreferencesKey("pares_mejor_$materiaId")] = millis }
    }

    suspend fun guardarSilenciarAlEstudiar(activo: Boolean) {
        context.preferenciasDataStore.edit { preferencias ->
            preferencias[CLAVE_SILENCIAR] = activo
        }
    }

    // Copia automática: la carpeta elegida y la fecha de la última copia.
    val copiaAutomatica: Flow<EstadoCopiaAutomatica> = context.preferenciasDataStore.data
        .map { preferencias ->
            EstadoCopiaAutomatica(
                carpeta = preferencias[CLAVE_CARPETA_COPIA],
                ultima = preferencias[CLAVE_ULTIMA_COPIA]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            )
        }

    suspend fun guardarCarpetaCopia(carpeta: String?) {
        context.preferenciasDataStore.edit { preferencias ->
            if (carpeta == null) {
                preferencias.remove(CLAVE_CARPETA_COPIA)
                preferencias.remove(CLAVE_ULTIMA_COPIA)
            } else {
                preferencias[CLAVE_CARPETA_COPIA] = carpeta
            }
        }
    }

    suspend fun guardarUltimaCopia(fecha: LocalDate) {
        context.preferenciasDataStore.edit { preferencias ->
            preferencias[CLAVE_ULTIMA_COPIA] = fecha.toString()
        }
    }

    // Si el calendario "Estudia+" del teléfono se mantiene al día solo.
    val calendarioAutomatico: Flow<Boolean> = context.preferenciasDataStore.data
        .map { preferencias -> preferencias[CLAVE_CALENDARIO_AUTO] ?: false }

    suspend fun guardarCalendarioAutomatico(activo: Boolean) {
        context.preferenciasDataStore.edit { preferencias ->
            preferencias[CLAVE_CALENDARIO_AUTO] = activo
        }
    }

    // Hasta qué día el usuario ya revisó las sesiones del plan que pasaron
    // (para no volver a preguntarle por las mismas).
    fun planRevisadoHasta(usuarioId: Int): Flow<LocalDate?> = context.preferenciasDataStore.data
        .map { preferencias -> preferencias[stringPreferencesKey("plan_revisado_$usuarioId")]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } }

    suspend fun marcarPlanRevisado(usuarioId: Int, fecha: LocalDate) {
        context.preferenciasDataStore.edit { preferencias ->
            preferencias[stringPreferencesKey("plan_revisado_$usuarioId")] = fecha.toString()
        }
    }

    // Usuarios (id) que ya vieron la guía de primera vez.
    val guiasVistas: Flow<Set<Int>> = context.preferenciasDataStore.data
        .map { preferencias -> preferencias[CLAVE_GUIAS_VISTAS].orEmpty().mapNotNull { it.toIntOrNull() }.toSet() }

    suspend fun marcarGuiaVista(usuarioId: Int) {
        context.preferenciasDataStore.edit { preferencias ->
            preferencias[CLAVE_GUIAS_VISTAS] = preferencias[CLAVE_GUIAS_VISTAS].orEmpty() + usuarioId.toString()
        }
    }

    suspend fun guardarPermisoArchivosPedido() {
        context.preferenciasDataStore.edit { preferencias ->
            preferencias[CLAVE_PERMISO_ARCHIVOS] = true
        }
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
