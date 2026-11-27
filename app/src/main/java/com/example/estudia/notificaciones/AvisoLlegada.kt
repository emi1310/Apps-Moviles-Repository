package com.example.estudia.notificaciones

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.estudia.EstudiaApp
import com.example.estudia.R
import com.example.estudia.modelo.diasSinClase
import com.example.estudia.modelo.resumenDeLlegada
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDate
import kotlin.coroutines.resume

// Aviso al llegar a la facultad: se guarda dónde queda (la ubicación del
// teléfono cuando el usuario lo activa) y Google Play Services avisa cuando
// el teléfono entra en un radio de RADIO_METROS (una "geovalla"), aunque la
// app esté cerrada. Llega una sola notificación por día con las clases de
// hoy y las tareas que vencen. El lugar se guarda solo en el teléfono.
object AvisoLlegada {

    const val RADIO_METROS = 250f
    private const val PREFERENCIAS = "aviso_llegada"
    private const val CLAVE_LATITUD = "latitud"
    private const val CLAVE_LONGITUD = "longitud"
    private const val CLAVE_ULTIMO_AVISO = "ultimo_aviso"
    private const val ID_GEOVALLA = "facultad"

    data class Lugar(val latitud: Double, val longitud: Double)

    private fun preferencias(context: Context) = context.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)

    fun lugarGuardado(context: Context): Lugar? {
        val p = preferencias(context)
        if (!p.contains(CLAVE_LATITUD)) return null
        return Lugar(
            Double.fromBits(p.getLong(CLAVE_LATITUD, 0)),
            Double.fromBits(p.getLong(CLAVE_LONGITUD, 0))
        )
    }

    // Ubicación precisa o aproximada, y (desde Android 10) "todo el tiempo".
    fun tieneUbicacion(context: Context): Boolean =
        concedido(context, Manifest.permission.ACCESS_FINE_LOCATION) || concedido(context, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun tieneSegundoPlano(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || concedido(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    private fun concedido(context: Context, permiso: String) =
        ContextCompat.checkSelfPermission(context, permiso) == PackageManager.PERMISSION_GRANTED

    // Dónde está el teléfono ahora (null si no se pudo: sin permiso, GPS
    // apagado o sin Google Play Services).
    @SuppressLint("MissingPermission")
    suspend fun ubicacionActual(context: Context): Lugar? {
        if (!tieneUbicacion(context)) return null
        val cliente = runCatching { LocationServices.getFusedLocationProviderClient(context) }.getOrNull() ?: return null
        return suspendCancellableCoroutine { continuacion ->
            cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { ubicacion -> continuacion.resume(ubicacion?.let { Lugar(it.latitude, it.longitude) }) }
                .addOnFailureListener { continuacion.resume(null) }
        }
    }

    // Guarda el lugar y arma el aviso. false si no se pudo armar.
    suspend fun activar(context: Context, lugar: Lugar): Boolean {
        preferencias(context).edit()
            .putLong(CLAVE_LATITUD, lugar.latitud.toRawBits())
            .putLong(CLAVE_LONGITUD, lugar.longitud.toRawBits())
            .apply()
        return registrar(context)
    }

    // Arma (o vuelve a armar, después de reiniciar) la geovalla del lugar guardado.
    @SuppressLint("MissingPermission")
    suspend fun registrar(context: Context): Boolean {
        val lugar = lugarGuardado(context) ?: return false
        if (!tieneUbicacion(context) || !tieneSegundoPlano(context)) return false
        val geovalla = Geofence.Builder()
            .setRequestId(ID_GEOVALLA)
            .setCircularRegion(lugar.latitud, lugar.longitud, RADIO_METROS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()
        val pedido = GeofencingRequest.Builder()
            // Si ya está adentro al activarlo, no avisa: espera a la próxima llegada.
            .setInitialTrigger(0)
            .addGeofence(geovalla)
            .build()
        val cliente = runCatching { LocationServices.getGeofencingClient(context) }.getOrNull() ?: return false
        return suspendCancellableCoroutine { continuacion ->
            runCatching {
                cliente.addGeofences(pedido, intentDeLlegada(context))
                    .addOnSuccessListener { continuacion.resume(true) }
                    .addOnFailureListener { continuacion.resume(false) }
            }.onFailure { continuacion.resume(false) }
        }
    }

    fun desactivar(context: Context) {
        runCatching { LocationServices.getGeofencingClient(context).removeGeofences(intentDeLlegada(context)) }
        preferencias(context).edit().clear().apply()
    }

    // Las geovallas necesitan un PendingIntent mutable (Play Services le
    // agrega qué pasó).
    private fun intentDeLlegada(context: Context): PendingIntent {
        val bandera = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(
            context, 0, Intent(context, LlegadaReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or bandera
        )
    }

    // Una vez por día como mucho (entrar y salir de la zona no repite el aviso).
    fun yaAvisoHoy(context: Context, hoy: LocalDate): Boolean =
        preferencias(context).getLong(CLAVE_ULTIMO_AVISO, -1) == hoy.toEpochDay()

    fun marcarAviso(context: Context, hoy: LocalDate) {
        preferencias(context).edit().putLong(CLAVE_ULTIMO_AVISO, hoy.toEpochDay()).apply()
    }
}

// Llegó a la facultad: avisa las clases de hoy y las tareas que vencen.
class LlegadaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val evento = GeofencingEvent.fromIntent(intent) ?: return
        if (evento.hasError() || evento.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return
        val hoy = LocalDate.now()
        if (AvisoLlegada.yaAvisoHoy(context, hoy)) return
        val app = context.applicationContext as EstudiaApp
        val pendiente = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val usuario = app.repositorio.usuarioActual.first() ?: return@launch
                val sinClase = diasSinClase(app.repositorio.feriados.first(), usuario.recesos).keys
                val resumen = resumenDeLlegada(usuario, hoy, sinClase)
                val lineas = buildList {
                    if (resumen.clases.isNotEmpty()) add(context.getString(R.string.llegada_clases, resumen.clases.joinToString(", ")))
                    if (resumen.tareas.isNotEmpty()) add(context.getString(R.string.llegada_tareas, resumen.tareas.joinToString(", ")))
                }
                val texto = if (resumen.vacio) context.getString(R.string.llegada_nada) else lineas.joinToString("\n")
                Notificaciones.mostrar(context, Notificaciones.ID_LLEGADA, context.getString(R.string.llegada_titulo), texto)
                AvisoLlegada.marcarAviso(context, hoy)
            } finally {
                pendiente.finish()
            }
        }
    }
}

// Al reiniciar el teléfono se borran las geovallas: se vuelve a armar.
class ArranqueReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (AvisoLlegada.lugarGuardado(context) == null) return
        val pendiente = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AvisoLlegada.registrar(context)
            } finally {
                pendiente.finish()
            }
        }
    }
}
