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
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.StickyNote2
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
import com.example.estudia.ui.componentes.progresoAnimado
import com.example.estudia.ui.componentes.TarjetaEstadistica
import com.example.estudia.ui.componentes.TarjetaSeccion
import com.example.estudia.ui.componentes.IconoCircular
import com.example.estudia.ui.theme.LocalDegradado
import com.example.estudia.ui.theme.VioletaPrincipal
import com.example.estudia.viewmodel.PerfilViewModel

// Pantalla "Mi Perfil": datos del usuario, estadísticas, meta semanal de
// estudio, recordatorios, tema (claro/oscuro), ayuda y cerrar sesión.
// "Recordatorios" pide el permiso POST_NOTIFICATIONS cuando hace falta.
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PerfilScreen(
    viewModel: PerfilViewModel,
    onAbrirAyuda: () -> Unit,
    onAbrirEstadisticas: () -> Unit,
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
        // ---------- Encabezado con degradado: avatar + datos principales ----------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(LocalDegradado.current)
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .border(3.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                    .padding(5.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = usuario.nombre.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    // El círculo es blanco también en modo oscuro: se usa el
                    // violeta fuerte fijo (el primary oscuro es lila claro y no se lee).
                    color = VioletaPrincipal
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = usuario.nombre, style = MaterialTheme.typography.titleLarge, color = Color.White)
            val carrera = usuario.carrera ?: stringResource(R.string.carrera_no_especificada)
            val anio = usuario.anio
            Text(
                text = if (anio != null) stringResource(R.string.anio_carrera, anio, carrera) else carrera,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = usuario.email ?: stringResource(R.string.email_no_especificado),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------- Estadísticas ----------
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TarjetaEstadistica(usuario.contarMaterias().toString(), stringResource(R.string.materias), Icons.AutoMirrored.Filled.MenuBook, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            TarjetaEstadistica(stringResource(R.string.horas_corto, usuario.calcularHorasEstudio()), stringResource(R.string.estudio), Icons.Filled.Timer, MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            TarjetaEstadistica(usuario.contarNotas().toString(), stringResource(R.string.notas), Icons.Filled.StickyNote2, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------- Meta semanal de estudio ----------
        TarjetaSeccion(
            titulo = stringResource(R.string.meta_semanal),
            icono = Icons.Filled.Flag,
            color = MaterialTheme.colorScheme.secondary,
            detalle = estado.progresoMeta?.let { "${(it * 100).toInt()}%" }
        ) {
            val meta = usuario.metaHorasSemanales
            val progreso = estado.progresoMeta
            if (meta == null || progreso == null) {
                Text(
                    text = stringResource(R.string.sin_meta),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(text = stringResource(R.string.meta_progreso, estado.horasEstaSemana, meta), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val avance = progresoAnimado(progreso)
                LinearProgressIndicator(
                    progress = { avance },
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier.fillMaxWidth().height(10.dp)
                )
                if (progreso >= 1f) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = stringResource(R.string.meta_cumplida), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TituloGrupo(stringResource(R.string.preferencias))

        // ---------- Recordatorios ----------
        // Se muestran activos solo si el usuario los eligió Y hay permiso.
        val activos = estado.recordatoriosActivos && permitidas
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconoCircular(Icons.Filled.NotificationsActive, MaterialTheme.colorScheme.tertiary, tamano = 36.dp)
                Spacer(modifier = Modifier.width(14.dp))
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = activos,
                    onCheckedChange = { activar -> if (activar) activarRecordatorios() else viewModel.cambiarRecordatorios(false) }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // ---------- Tema ----------
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconoCircular(Icons.Filled.Palette, MaterialTheme.colorScheme.primary, tamano = 36.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(text = stringResource(R.string.tema), style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(modifier = Modifier.height(10.dp))
                val opciones = listOf(
                    PreferenciaTema.SISTEMA to R.string.tema_sistema,
                    PreferenciaTema.CLARO to R.string.tema_claro,
                    PreferenciaTema.OSCURO to R.string.tema_oscuro
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    opciones.forEachIndexed { indice, (tema, texto) ->
                        SegmentedButton(
                            selected = estado.tema == tema,
                            onClick = { viewModel.cambiarTema(tema) },
                            shape = SegmentedButtonDefaults.itemShape(indice, opciones.size)
                        ) { Text(stringResource(texto)) }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TituloGrupo(stringResource(R.string.cuenta))

        // ---------- Menú de opciones ----------
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                OpcionPerfil(stringResource(R.string.editar_perfil), Icons.Filled.Edit) { mostrarEditarPerfil = true }
                OpcionPerfil(stringResource(R.string.metas_de_estudio), Icons.Filled.Flag) { mostrarMeta = true }
                OpcionPerfil(stringResource(R.string.estadisticas_titulo), Icons.Filled.Insights, alTocar = onAbrirEstadisticas)
                OpcionPerfil(stringResource(R.string.ayuda_y_soporte), Icons.AutoMirrored.Filled.HelpOutline, alTocar = onAbrirAyuda)
                OpcionPerfil(stringResource(R.string.cerrar_sesion), Icons.AutoMirrored.Filled.Logout, esDestructiva = true) {
                    confirmarCierreSesion = true
                }
            }
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

// Título chico que agrupa las tarjetas de opciones ("Preferencias", "Cuenta").
@Composable
private fun TituloGrupo(texto: String) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}
