package com.example.estudia.ui.pantallas

import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import java.time.format.DateTimeFormatter
import com.example.estudia.modelo.EstadoCopiaAutomatica
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.estudia.ui.componentes.BloqueColor
import com.example.estudia.ui.componentes.superficieVidrio
import com.example.estudia.ui.componentes.DialogoApp
import com.example.estudia.ui.componentes.Avatar
import com.example.estudia.ui.componentes.colorAcentoTexto
import com.example.estudia.ui.componentes.DialogoFotoPerfil
import com.example.estudia.ui.componentes.Medalla
import com.example.estudia.ui.componentes.camaraDeFotos
import com.example.estudia.ui.componentes.selectorDeArchivos
import com.example.estudia.viewmodel.Logro
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
import java.time.LocalDate
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.example.estudia.ui.componentes.IconoCircular
import com.example.estudia.ui.componentes.TextoDialogo
import androidx.compose.material.icons.outlined.NotificationsOff
import com.example.estudia.ui.componentes.CirculoBloque
import com.example.estudia.ui.componentes.EncabezadoPantalla
import com.example.estudia.ui.componentes.TextoAjustable
import androidx.compose.ui.text.style.TextAlign
import com.example.estudia.ui.theme.BloqueAzul
import com.example.estudia.notificaciones.SilencioEnClase
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import com.example.estudia.ui.theme.BloqueNaranja
import com.example.estudia.ui.theme.BloqueVerde
import com.example.estudia.ui.theme.BloqueVioleta
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.modelo.PreferenciaTema
import com.example.estudia.notificaciones.Notificaciones
import com.example.estudia.notificaciones.AvisoLlegada
import com.example.estudia.ui.componentes.LocalAvisos
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import com.example.estudia.ui.componentes.LocalEspacioDock
import com.example.estudia.ui.componentes.DialogoConfirmacion
import com.example.estudia.ui.componentes.DialogoEditarPerfil
import com.example.estudia.ui.componentes.DialogoMeta
import com.example.estudia.ui.componentes.RecolectarAvisos
import com.example.estudia.ui.componentes.progresoAnimado
import com.example.estudia.ui.theme.LocalDegradado
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
    onAbrirLogros: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RecolectarAvisos(viewModel.avisos)
    val usuario = estado.usuario ?: return

    var confirmarCierreSesion by remember { mutableStateOf(false) }
    var mostrarEditarPerfil by remember { mutableStateOf(false) }
    var mostrarMeta by remember { mutableStateOf(false) }
    var mostrarFoto by remember { mutableStateOf(false) }
    var confirmarRestaurar by remember { mutableStateOf(false) }

    // ---------- Copia de seguridad ----------
    // CreateDocument / OpenDocument: el usuario elige dónde guardarla o cuál
    // abrir, y la app recibe acceso solo a ese archivo (no hace falta permiso).
    val nombreCopia = "estudia_copia_${LocalDate.now()}.zip"
    val guardarCopia = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.exportarCopia(uri.toString())
    }
    val abrirCopia = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importarCopia(uri.toString())
    }
    // Copia automática: se elige una carpeta (OpenDocumentTree) y la app
    // guarda ahí una copia por semana.
    val elegirCarpeta = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.activarCopiaAutomatica(uri.toString())
    }

    // ---------- Foto de perfil: cámara o galería ----------
    // Cada una pide su permiso (cámara, o fotos y archivos) antes de abrirse.
    val sacarFoto = camaraDeFotos { viewModel.cambiarFotoPerfil(it) }
    val elegirDeGaleria = selectorDeArchivos(
        estado.permisoArchivosPedido,
        viewModel::marcarPermisoArchivosPedido,
        tipos = arrayOf("image/*")
    ) { uri -> if (uri != null) viewModel.cambiarFotoPerfil(uri.toString()) }

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
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = LocalEspacioDock.current + 16.dp)
    ) {
        // ---------- Encabezado, como en las demás pantallas ----------
        EncabezadoPantalla(
            titulo = stringResource(R.string.perfil_titulo),
            subtitulo = stringResource(R.string.perfil_subtitulo)
        ) {
            FilledTonalIconButton(onClick = { mostrarEditarPerfil = true }) {
                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.editar_perfil))
            }
        }

        // ---------- Tus datos: la foto grande al centro, el nombre, la carrera y el mail ----------
        // Sin bloque de fondo: la foto lleva un aro con el degradado violeta.
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar: la foto de perfil o la inicial. Al tocarlo se elige la foto.
            // Con la cámara como insignia, para que se note que se puede tocar.
            Box {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(LocalDegradado.current)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(4.dp)
                ) {
                    Avatar(
                        nombre = usuario.nombre,
                        fotoUri = estado.fotoUri,
                        tamano = 88.dp,
                        fondo = LocalDegradado.current,
                        colorInicial = Color.White,
                        estiloInicial = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.clickable(
                            onClickLabel = stringResource(R.string.foto_cambiar),
                            role = Role.Button
                        ) { mostrarFoto = true }
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = usuario.nombre,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val carrera = usuario.carrera ?: stringResource(R.string.carrera_no_especificada)
            val anio = usuario.anio
            Text(
                text = if (anio != null) stringResource(R.string.anio_carrera, anio, carrera) else carrera,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = usuario.email ?: stringResource(R.string.email_no_especificado),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------- Estadísticas: tres bloques de color ----------
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BloqueEstadistica(usuario.contarMaterias().toString(), stringResource(R.string.materias), Icons.AutoMirrored.Outlined.MenuBook, BloqueVioleta, Modifier.weight(1f))
            BloqueEstadistica(stringResource(R.string.horas_corto, usuario.calcularHorasEstudio()), stringResource(R.string.estudio), Icons.Outlined.Timer, BloqueVerde, Modifier.weight(1f))
            BloqueEstadistica(usuario.contarNotas().toString(), stringResource(R.string.notas), Icons.Outlined.StickyNote2, BloqueNaranja, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---------- Meta semanal: bloque de color que se toca para editarla ----------
        val meta = usuario.metaHorasSemanales
        val progreso = estado.progresoMeta
        BloqueColor(
            color = BloqueAzul,
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable(onClickLabel = stringResource(R.string.metas_de_estudio), role = Role.Button) { mostrarMeta = true }
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.meta_semanal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (meta == null || progreso == null) stringResource(R.string.sin_meta)
                            else stringResource(R.string.meta_progreso, estado.horasEstaSemana, meta),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    CirculoBloque(tamano = 44.dp) {
                        Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(22.dp))
                    }
                }
                if (meta != null && progreso != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${(progreso * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorAcentoTexto(BloqueAzul),
                            modifier = Modifier.weight(1f)
                        )
                        if (progreso >= 1f) {
                            Text(text = stringResource(R.string.meta_cumplida), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val avance = progresoAnimado(progreso)
                    LinearProgressIndicator(
                        progress = { avance },
                        color = BloqueAzul,
                        trackColor = BloqueAzul.copy(alpha = 0.16f),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---------- Tus logros: cuántos van y las medallas conseguidas ----------
        BloqueLogros(estado.logros, onAbrirLogros)

        Spacer(modifier = Modifier.height(20.dp))
        TituloGrupo(stringResource(R.string.preferencias))

        // ---------- Recordatorios ----------
        // Se muestran activos solo si el usuario los eligió Y hay permiso.
        val activos = estado.recordatoriosActivos && permitidas
        BloqueAjuste(Icons.Outlined.NotificationsActive, BloqueNaranja) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.recordatorios), style = MaterialTheme.typography.titleSmall)
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
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = activos,
                onCheckedChange = { activar -> if (activar) activarRecordatorios() else viewModel.cambiarRecordatorios(false) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        // ---------- Aviso al llegar a la facultad ----------
        AjusteAvisoLlegada()
        Spacer(modifier = Modifier.height(10.dp))
        AjusteSilencioClase()
        Spacer(modifier = Modifier.height(8.dp))

        // ---------- Tema: sol (claro) o luna (oscuro) ----------
        // Si todavía sigue al teléfono (SISTEMA), se marca el que se ve ahora.
        val oscuroActivo = when (estado.tema) {
            PreferenciaTema.CLARO -> false
            PreferenciaTema.OSCURO -> true
            PreferenciaTema.SISTEMA -> isSystemInDarkTheme()
        }
        BloqueAjuste(Icons.Outlined.Palette, BloqueVioleta) {
            Text(text = stringResource(R.string.tema), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BotonTema(Icons.Outlined.LightMode, stringResource(R.string.tema_claro), elegido = !oscuroActivo) {
                    viewModel.cambiarTema(PreferenciaTema.CLARO)
                }
                BotonTema(Icons.Outlined.DarkMode, stringResource(R.string.tema_oscuro), elegido = oscuroActivo) {
                    viewModel.cambiarTema(PreferenciaTema.OSCURO)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        TituloGrupo(stringResource(R.string.cuenta))

        // ---------- Cuenta: dos bloques y, abajo, cerrar sesión ----------
        // Editar perfil está en el lápiz del bloque del usuario y la meta se
        // edita tocando su bloque, así que acá no se repiten.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BloqueCuenta(stringResource(R.string.estadisticas_titulo), Icons.Outlined.Insights, BloqueVerde, Modifier.weight(1f), onAbrirEstadisticas)
            BloqueCuenta(stringResource(R.string.ayuda_y_soporte), Icons.AutoMirrored.Outlined.HelpOutline, BloqueNaranja, Modifier.weight(1f), onAbrirAyuda)
        }
        Spacer(modifier = Modifier.height(10.dp))
        // Copia de seguridad: el archivo se elige con el selector del sistema
        // (Descargas, Drive...), sin pedir permisos.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BloqueCuenta(stringResource(R.string.copia_guardar), Icons.Outlined.Backup, BloqueAzul, Modifier.weight(1f)) {
                guardarCopia.launch(nombreCopia)
            }
            BloqueCuenta(stringResource(R.string.copia_restaurar), Icons.Outlined.Restore, BloqueVioleta, Modifier.weight(1f)) {
                confirmarRestaurar = true
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        BloqueCopiaAutomatica(
            estado = estado.copiaAutomatica,
            onElegirCarpeta = { elegirCarpeta.launch(null) },
            onCopiarAhora = viewModel::copiarAhora,
            onDesactivar = viewModel::desactivarCopiaAutomatica
        )
        Spacer(modifier = Modifier.height(10.dp))
        BloqueCopiaGoogle()
        Spacer(modifier = Modifier.height(10.dp))
        val rojo = MaterialTheme.colorScheme.error
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(rojo.copy(alpha = 0.12f))
                .clickable(role = Role.Button) { confirmarCierreSesion = true }
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = rojo, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.cerrar_sesion), style = MaterialTheme.typography.titleSmall, color = rojo)
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

    if (confirmarRestaurar) {
        DialogoApp(
            titulo = stringResource(R.string.copia_restaurar),
            icono = Icons.Outlined.Restore,
            textoConfirmar = stringResource(R.string.restaurar),
            onConfirmar = {
                confirmarRestaurar = false
                abrirCopia.launch(arrayOf("application/zip", "application/octet-stream"))
            },
            onCancelar = { confirmarRestaurar = false },
            destructivo = true
        ) {
            TextoDialogo(stringResource(R.string.confirmar_restaurar_copia))
        }
    }

    if (mostrarFoto) {
        DialogoFotoPerfil(
            tieneFoto = estado.fotoUri != null,
            onSacarFoto = {
                mostrarFoto = false
                sacarFoto()
            },
            onElegirDeGaleria = {
                mostrarFoto = false
                elegirDeGaleria()
            },
            onQuitar = {
                mostrarFoto = false
                viewModel.cambiarFotoPerfil(null)
            },
            onCerrar = { mostrarFoto = false }
        )
    }

    if (mostrarExplicacion) {
        DialogoApp(
            titulo = stringResource(R.string.activar_recordatorios),
            icono = Icons.Outlined.NotificationsActive,
            textoConfirmar = stringResource(R.string.continuar),
            onConfirmar = {
                mostrarExplicacion = false
                solicitarPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            textoCancelar = stringResource(R.string.ahora_no),
            onCancelar = { mostrarExplicacion = false }
        ) {
            TextoDialogo(stringResource(R.string.explicacion_permiso_notificaciones))
        }
    }

    if (mostrarIrAAjustes) {
        DialogoApp(
            titulo = stringResource(R.string.notificaciones_desactivadas),
            icono = Icons.Outlined.NotificationsOff,
            textoConfirmar = stringResource(R.string.ir_a_ajustes),
            onConfirmar = {
                mostrarIrAAjustes = false
                abrirAjustesDeNotificaciones(context)
            },
            onCancelar = { mostrarIrAAjustes = false }
        ) {
            TextoDialogo(stringResource(R.string.notificaciones_bloqueadas))
        }
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

// Botón redondo del selector de tema (sol o luna). El elegido se pinta con
// el violeta de la app.
@Composable
private fun BotonTema(icono: ImageVector, descripcion: String, elegido: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (elegido) MaterialTheme.colorScheme.primary else Color.Transparent)
            .selectable(selected = elegido, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icono,
            contentDescription = descripcion,
            tint = if (elegido) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Bloque con un número grande (en su color) y su etiqueta (materias, horas
// de estudio, notas), con el ícono en un círculo del color arriba.
@Composable
private fun BloqueEstadistica(valor: String, etiqueta: String, icono: ImageVector, color: Color, modifier: Modifier) {
    BloqueColor(color = color, modifier = modifier.semantics(mergeDescendants = true) {}) {
        Column(modifier = Modifier.padding(14.dp)) {
            CirculoBloque(tamano = 36.dp) {
                Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextoAjustable(valor, MaterialTheme.typography.headlineSmall, color = colorAcentoTexto(color), fontWeight = FontWeight.ExtraBold, minimo = 14.sp)
            TextoAjustable(etiqueta, MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

// Bloque claro de una preferencia: el ícono en un círculo de color a la
// izquierda y el resto (texto, interruptor o selector) a la derecha.
@Composable
internal fun BloqueAjuste(icono: ImageVector, color: Color, contenido: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(RoundedCornerShape(22.dp), borde = false)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoCircular(icono, Color.White, fondo = color)
        Spacer(modifier = Modifier.width(14.dp))
        contenido()
    }
}

// Bloque cuadrado de la grilla de Cuenta: ícono en un círculo de color
// arriba, flecha en la esquina y el nombre de la opción abajo.
@Composable
private fun BloqueCuenta(texto: String, icono: ImageVector, color: Color, modifier: Modifier, alTocar: () -> Unit) {
    Column(
        modifier = modifier
            .heightIn(min = 116.dp)
            .superficieVidrio(RoundedCornerShape(22.dp), borde = false)
            .clickable(role = Role.Button, onClick = alTocar)
            .padding(14.dp),
        // El nombre va abajo; si ocupa dos renglones, el bloque crece.
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconoCircular(icono, Color.White, fondo = color)
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextoAjustable(texto, MaterialTheme.typography.titleSmall, maxLines = 2)
    }
}

// Bloque "Tus logros": cuántos se consiguieron, una barra de avance y las
// medallas (las conseguidas en color). Al tocarlo se abre la pantalla de Logros.
@Composable
private fun BloqueLogros(logros: List<Logro>, onAbrir: () -> Unit) {
    val conseguidos = logros.count { it.conseguido }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(RoundedCornerShape(22.dp), borde = false)
            .clickable(onClickLabel = stringResource(R.string.logros_ver_todos), role = Role.Button, onClick = onAbrir)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoCircular(Icons.Outlined.EmojiEvents, Color.White, fondo = BloqueNaranja)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.logros_tus_logros), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.logros_resumen, conseguidos, logros.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        val avance = progresoAnimado(if (logros.isEmpty()) 0f else conseguidos.toFloat() / logros.size)
        LinearProgressIndicator(
            progress = { avance },
            color = BloqueNaranja,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Las primeras medallas (las conseguidas van primero).
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.clearAndSetSemantics { }) {
            logros.take(6).forEach { Medalla(it.tipo, it.conseguido, tamano = 36.dp) }
        }
    }
}

// Copia automática semanal: apagada, invita a elegir una carpeta; prendida,
// muestra la última copia y deja copiar ahora, cambiar la carpeta o apagarla.
@Composable
private fun BloqueCopiaAutomatica(
    estado: EstadoCopiaAutomatica,
    onElegirCarpeta: () -> Unit,
    onCopiarAhora: () -> Unit,
    onDesactivar: () -> Unit
) {
    val formato = remember { DateTimeFormatter.ofPattern("d/M/yyyy") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(RoundedCornerShape(22.dp), borde = false)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoCircular(Icons.Outlined.CloudSync, Color.White, fondo = BloqueVerde)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.copia_auto_titulo), style = MaterialTheme.typography.titleSmall)
                Text(
                    when {
                        !estado.activa -> stringResource(R.string.copia_auto_apagada)
                        estado.ultima != null -> stringResource(R.string.copia_auto_ultima, estado.ultima.format(formato))
                        else -> stringResource(R.string.copia_auto_nunca)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (estado.activa) {
                FilledTonalButton(onClick = onCopiarAhora) { Text(stringResource(R.string.copia_auto_ahora)) }
                TextButton(onClick = onElegirCarpeta) { Text(stringResource(R.string.copia_auto_elegir)) }
                TextButton(onClick = onDesactivar) { Text(stringResource(R.string.copia_auto_desactivar)) }
            } else {
                FilledTonalButton(onClick = onElegirCarpeta) { Text(stringResource(R.string.copia_auto_elegir)) }
            }
        }
    }
}

// Aviso al llegar a la facultad. Al activarlo: se explica, se piden la
// ubicación y (desde Android 10) la ubicación "todo el tiempo", y se guarda
// dónde está el teléfono en ese momento como la facultad.
@Composable
private fun AjusteAvisoLlegada() {
    val context = LocalContext.current
    val avisos = LocalAvisos.current
    val alcance = rememberCoroutineScope()
    var activo by remember { mutableStateOf(AvisoLlegada.lugarGuardado(context) != null) }
    var explicando by rememberSaveable { mutableStateOf(false) }
    var pidiendoSegundoPlano by rememberSaveable { mutableStateOf(false) }
    var buscando by remember { mutableStateOf(false) }
    val textoListo = stringResource(R.string.llegada_activado)
    val textoSinUbicacion = stringResource(R.string.llegada_sin_ubicacion)
    val textoSinPermiso = stringResource(R.string.llegada_sin_permiso)

    val guardarAca: () -> Unit = {
        buscando = true
        alcance.launch {
            val lugar = AvisoLlegada.ubicacionActual(context)
            val ok = lugar != null && AvisoLlegada.activar(context, lugar)
            if (!ok) AvisoLlegada.desactivar(context)
            activo = ok
            buscando = false
            avisos.mostrarTexto(if (ok) textoListo else textoSinUbicacion)
        }
    }
    val pedirSegundoPlano = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) guardarAca() else avisos.mostrarTexto(textoSinPermiso)
    }
    val pedirUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        when {
            resultado.values.none { it } -> avisos.mostrarTexto(textoSinPermiso)
            AvisoLlegada.tieneSegundoPlano(context) -> guardarAca()
            else -> pidiendoSegundoPlano = true
        }
    }

    BloqueAjuste(Icons.Outlined.LocationOn, BloqueVerde) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.llegada_ajuste), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(if (activo) R.string.llegada_ajuste_activo else R.string.llegada_ajuste_descripcion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = activo,
            enabled = !buscando,
            onCheckedChange = { activar ->
                if (activar) explicando = true
                else {
                    AvisoLlegada.desactivar(context)
                    activo = false
                }
            }
        )
    }

    if (explicando) {
        DialogoApp(
            titulo = stringResource(R.string.llegada_ajuste),
            icono = Icons.Outlined.LocationOn,
            textoConfirmar = stringResource(R.string.llegada_estoy_aca),
            onConfirmar = {
                explicando = false
                when {
                    !AvisoLlegada.tieneUbicacion(context) -> pedirUbicacion.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                    !AvisoLlegada.tieneSegundoPlano(context) -> pidiendoSegundoPlano = true
                    else -> guardarAca()
                }
            },
            onCancelar = { explicando = false }
        ) {
            TextoDialogo(stringResource(R.string.llegada_explicacion))
        }
    }

    // Android 10 o más: "Permitir todo el tiempo" se elige en una pantalla aparte.
    if (pidiendoSegundoPlano && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        DialogoApp(
            titulo = stringResource(R.string.llegada_segundo_plano_titulo),
            icono = Icons.Outlined.LocationOn,
            textoConfirmar = stringResource(R.string.continuar),
            onConfirmar = {
                pidiendoSegundoPlano = false
                pedirSegundoPlano.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            },
            onCancelar = { pidiendoSegundoPlano = false }
        ) {
            TextoDialogo(stringResource(R.string.llegada_segundo_plano))
        }
    }
}

// Silenciar en clase: No molestar automático durante las clases y exámenes
// del horario. Necesita el acceso a No molestar (se da en Ajustes): antes se
// explica, y al volver a la app, si lo dio, se activa solo.
@Composable
private fun AjusteSilencioClase() {
    val context = LocalContext.current
    var activo by remember { mutableStateOf(SilencioEnClase.activo(context) && SilencioEnClase.accesoConcedido(context)) }
    var explicando by rememberSaveable { mutableStateOf(false) }
    var esperandoAcceso by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        val acceso = SilencioEnClase.accesoConcedido(context)
        if (esperandoAcceso && acceso) SilencioEnClase.cambiar(context, true)
        esperandoAcceso = false
        activo = SilencioEnClase.activo(context) && acceso
        onPauseOrDispose { }
    }

    BloqueAjuste(Icons.Outlined.DoNotDisturbOn, BloqueAzul) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.silencio_clase_titulo), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.silencio_clase_descripcion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = activo,
            onCheckedChange = { activar ->
                when {
                    !activar -> { SilencioEnClase.cambiar(context, false); activo = false }
                    SilencioEnClase.accesoConcedido(context) -> { SilencioEnClase.cambiar(context, true); activo = true }
                    else -> explicando = true
                }
            }
        )
    }

    if (explicando) {
        DialogoApp(
            titulo = stringResource(R.string.silencio_clase_titulo),
            icono = Icons.Outlined.DoNotDisturbOn,
            textoConfirmar = stringResource(R.string.ir_a_ajustes),
            onConfirmar = {
                explicando = false
                esperandoAcceso = true
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            },
            textoCancelar = stringResource(R.string.ahora_no),
            onCancelar = { explicando = false }
        ) {
            TextoDialogo(stringResource(R.string.silencio_clase_explicacion))
        }
    }
}

// Copia en la cuenta de Google: la hace Android solo (una vez por día, con
// wifi y cargando) y la guarda en Google Drive sin ocupar el espacio del
// usuario. Al instalar la app en otro teléfono con la misma cuenta, se
// restaura. Acá solo se explica y se abre el ajuste del sistema.
@Composable
private fun BloqueCopiaGoogle() {
    val context = LocalContext.current
    val avisos = LocalAvisos.current
    val textoSinAjuste = stringResource(R.string.copia_google_sin_ajuste)
    BloqueAjuste(Icons.Outlined.CloudDone, BloqueVerde) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.copia_google_titulo), style = MaterialTheme.typography.titleSmall)
            Text(
                text = stringResource(R.string.copia_google_descripcion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = { if (!abrirAjusteCopiaGoogle(context)) avisos.mostrarTexto(textoSinAjuste) },
                contentPadding = PaddingValues(0.dp)
            ) { Text(stringResource(R.string.copia_google_ver)) }
        }
    }
}

// La pantalla de "Copia de seguridad" de Google; si el teléfono no la tiene
// (sin Google Play Services), los Ajustes generales.
private fun abrirAjusteCopiaGoogle(context: android.content.Context): Boolean {
    val intentos = listOf(
        Intent().setClassName("com.google.android.gms", "com.google.android.gms.backup.component.BackupSettingsActivity"),
        Intent("android.settings.BACKUP_AND_RESET_SETTINGS"),
        Intent(Settings.ACTION_SETTINGS)
    )
    return intentos.any { intent ->
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    }
}
