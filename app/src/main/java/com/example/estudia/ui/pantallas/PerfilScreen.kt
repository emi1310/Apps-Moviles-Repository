package com.example.estudia.ui.pantallas

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.modelo.PreferenciaTema
import com.example.estudia.notificaciones.Notificaciones
import com.example.estudia.ui.componentes.DialogoConfirmacion
import com.example.estudia.ui.componentes.DialogoEditarPerfil
import com.example.estudia.ui.componentes.DialogoMeta
import com.example.estudia.ui.componentes.OpcionPerfil
import com.example.estudia.ui.componentes.RecolectarAvisos
import com.example.estudia.ui.componentes.TarjetaEstadistica
import com.example.estudia.viewmodel.PerfilViewModel

// Pantalla "Mi Perfil": datos del usuario, estadísticas, meta semanal de
// estudio, recordatorios, tema (claro/oscuro), ayuda y cerrar sesión.
// "Recordatorios" pide el permiso POST_NOTIFICATIONS cuando hace falta.
@Composable
fun PerfilScreen(
    viewModel: PerfilViewModel,
    onAbrirAyuda: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RecolectarAvisos(viewModel.avisos)
    val usuario = estado.usuario ?: return

    var confirmarCierreSesion by remember { mutableStateOf(false) }
    var mostrarEditarPerfil by remember { mutableStateOf(false) }
    var mostrarMeta by remember { mutableStateOf(false) }

    // ---------- Permiso de notificaciones ----------
    // Buenas prácticas: el permiso se pide recién cuando el usuario activa
    // los recordatorios (en contexto), antes se explica para qué se usa, y si
    // lo rechazó para siempre se ofrece ir a Ajustes en vez de insistir.
    val context = LocalContext.current
    var permitidas by remember { mutableStateOf(Notificaciones.permitidas(context)) }
    var mostrarExplicacion by remember { mutableStateOf(false) }
    var mostrarIrAAjustes by remember { mutableStateOf(false) }

    // Al volver a la app (por ejemplo desde Ajustes) se revisa otra vez el
    // permiso, porque el usuario lo puede haber cambiado afuera de la app.
    LifecycleResumeEffect(Unit) {
        permitidas = Notificaciones.permitidas(context)
        onPauseOrDispose { }
    }

    val solicitarPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        permitidas = Notificaciones.permitidas(context)
        when {
            concedido -> viewModel.cambiarRecordatorios(true)
            // Rechazado con "No volver a preguntar": solo se puede desde Ajustes.
            !debeExplicarPermiso(context) -> mostrarIrAAjustes = true
            else -> viewModel.avisarSinPermiso()
        }
    }

    val activarRecordatorios: () -> Unit = {
        when {
            permitidas -> viewModel.cambiarRecordatorios(true)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> mostrarExplicacion = true
            // Antes de Android 13 no hay permiso que pedir: si están apagadas,
            // el usuario las apagó en Ajustes.
            else -> mostrarIrAAjustes = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = stringResource(R.string.mi_perfil), style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // ---------- Avatar + datos principales ----------
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = usuario.nombre.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = usuario.nombre, style = MaterialTheme.typography.titleLarge)
            val carrera = usuario.carrera ?: stringResource(R.string.carrera_no_especificada)
            val anio = usuario.anio
            Text(
                text = if (anio != null) stringResource(R.string.anio_carrera, anio, carrera) else carrera,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(text = usuario.email ?: stringResource(R.string.email_no_especificado), style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ---------- Estadísticas ----------
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TarjetaEstadistica(valor = usuario.contarMaterias().toString(), etiqueta = stringResource(R.string.materias))
            TarjetaEstadistica(valor = stringResource(R.string.horas_corto, usuario.calcularHorasEstudio()), etiqueta = stringResource(R.string.estudio))
            TarjetaEstadistica(valor = usuario.contarNotas().toString(), etiqueta = stringResource(R.string.notas))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ---------- Meta semanal de estudio ----------
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = stringResource(R.string.meta_semanal), style = MaterialTheme.typography.titleMedium)
                val meta = usuario.metaHorasSemanales
                val progreso = estado.progresoMeta
                if (meta == null || progreso == null) {
                    Text(text = stringResource(R.string.sin_meta), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        text = stringResource(R.string.meta_progreso, estado.horasEstaSemana, meta),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth())
                    if (progreso >= 1f) {
                        Text(text = stringResource(R.string.meta_cumplida), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---------- Recordatorios ----------
        // Se muestran activos solo si el usuario los eligió Y hay permiso.
        val activos = estado.recordatoriosActivos && permitidas
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.recordatorios), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(
                        when {
                            activos -> R.string.recordatorios_descripcion
                            estado.recordatoriosActivos -> R.string.sin_permiso_notificaciones
                            else -> R.string.desactivados
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = activos,
                onCheckedChange = { activar -> if (activar) activarRecordatorios() else viewModel.cambiarRecordatorios(false) }
            )
        }

        // ---------- Tema ----------
        Text(text = stringResource(R.string.tema), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
        Row {
            listOf(
                PreferenciaTema.SISTEMA to R.string.tema_sistema,
                PreferenciaTema.CLARO to R.string.tema_claro,
                PreferenciaTema.OSCURO to R.string.tema_oscuro
            ).forEach { (tema, texto) ->
                FilterChip(
                    selected = estado.tema == tema,
                    onClick = { viewModel.cambiarTema(tema) },
                    label = { Text(stringResource(texto)) }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()

        // ---------- Menú de opciones ----------
        OpcionPerfil(stringResource(R.string.editar_perfil), Icons.Filled.Edit) { mostrarEditarPerfil = true }
        OpcionPerfil(stringResource(R.string.metas_de_estudio), Icons.Filled.Flag) { mostrarMeta = true }
        OpcionPerfil(stringResource(R.string.ayuda_y_soporte), Icons.AutoMirrored.Filled.HelpOutline, alTocar = onAbrirAyuda)
        OpcionPerfil(stringResource(R.string.cerrar_sesion), Icons.AutoMirrored.Filled.Logout, esDestructiva = true) {
            confirmarCierreSesion = true
        }
    }

    if (confirmarCierreSesion) {
        DialogoConfirmacion(
            mensaje = stringResource(R.string.confirmar_cerrar_sesion),
            textoConfirmar = stringResource(R.string.cerrar_sesion),
            onConfirmar = onCerrarSesion,
            onCancelar = { confirmarCierreSesion = false }
        )
    }

    if (mostrarEditarPerfil) {
        DialogoEditarPerfil(
            usuario = usuario,
            onConfirmar = { nombre, carrera, anio, email ->
                viewModel.editarPerfil(nombre, carrera, anio, email)
                mostrarEditarPerfil = false
            },
            onCancelar = { mostrarEditarPerfil = false }
        )
    }

    if (mostrarMeta) {
        DialogoMeta(
            metaActual = usuario.metaHorasSemanales,
            onConfirmar = {
                viewModel.cambiarMetaSemanal(it)
                mostrarMeta = false
            },
            onCancelar = { mostrarMeta = false }
        )
    }

    if (mostrarExplicacion) {
        AlertDialog(
            onDismissRequest = { mostrarExplicacion = false },
            title = { Text(stringResource(R.string.activar_recordatorios)) },
            text = { Text(stringResource(R.string.explicacion_permiso_notificaciones)) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarExplicacion = false
                    solicitarPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text(stringResource(R.string.continuar)) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarExplicacion = false }) { Text(stringResource(R.string.ahora_no)) }
            }
        )
    }

    if (mostrarIrAAjustes) {
        AlertDialog(
            onDismissRequest = { mostrarIrAAjustes = false },
            title = { Text(stringResource(R.string.notificaciones_desactivadas)) },
            text = { Text(stringResource(R.string.notificaciones_bloqueadas)) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarIrAAjustes = false
                    abrirAjustesDeNotificaciones(context)
                }) { Text(stringResource(R.string.ir_a_ajustes)) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarIrAAjustes = false }) { Text(stringResource(R.string.cancelar)) }
            }
        )
    }
}

// true si Android recomienda explicar el permiso antes de volver a pedirlo
// (el usuario lo rechazó una vez). Si es false después de un rechazo,
// significa que eligió "No volver a preguntar".
private fun debeExplicarPermiso(context: Context): Boolean {
    val activity = context.buscarActivity() ?: return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
}

// Abre la pantalla de Ajustes con las notificaciones de esta app.
private fun abrirAjustesDeNotificaciones(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }
    context.startActivity(intent)
}

private fun Context.buscarActivity(): Activity? {
    var actual: Context? = this
    while (actual is ContextWrapper) {
        if (actual is Activity) return actual
        actual = actual.baseContext
    }
    return null
}
