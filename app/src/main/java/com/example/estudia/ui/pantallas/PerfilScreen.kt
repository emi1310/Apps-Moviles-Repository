package com.example.estudia.ui.pantallas

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.estudia.ui.componentes.BloqueColor
import com.example.estudia.ui.componentes.DialogoApp
import com.example.estudia.ui.componentes.Avatar
import com.example.estudia.ui.componentes.colorAcentoTexto
import com.example.estudia.ui.componentes.DialogoFotoPerfil
import com.example.estudia.ui.componentes.Medalla
import com.example.estudia.ui.componentes.camaraDeFotos
import com.example.estudia.ui.componentes.selectorDeArchivos
import com.example.estudia.viewmodel.Logro
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import java.time.LocalDate
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.example.estudia.ui.componentes.IconoCircular
import com.example.estudia.ui.componentes.TextoDialogo
import androidx.compose.material.icons.filled.NotificationsOff
import com.example.estudia.ui.componentes.CirculoBloque
import com.example.estudia.ui.theme.BloqueAzul
import com.example.estudia.ui.theme.BloqueNaranja
import com.example.estudia.ui.theme.BloqueVerde
import com.example.estudia.ui.theme.BloqueVioleta
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
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
import com.example.estudia.ui.componentes.LocalEspacioDock
import com.example.estudia.ui.componentes.DialogoConfirmacion
import com.example.estudia.ui.componentes.DialogoEditarPerfil
import com.example.estudia.ui.componentes.DialogoMeta
import com.example.estudia.ui.componentes.RecolectarAvisos
import com.example.estudia.ui.componentes.progresoAnimado
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
        // ---------- Bloque del usuario: avatar, datos y botón de editar ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(LocalDegradado.current)
                .padding(start = 18.dp, top = 18.dp, end = 8.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar: la foto de perfil o la inicial. Al tocarlo se elige la foto.
            // Con la cámara como insignia, para que se note que se puede tocar.
            Box {
                Avatar(
                    nombre = usuario.nombre,
                    fotoUri = estado.fotoUri,
                    tamano = 68.dp,
                    fondo = SolidColor(Color.White),
                    // El círculo es blanco también en modo oscuro: se usa el
                    // violeta fuerte fijo (el primary oscuro es lila claro y no se lee).
                    colorInicial = VioletaPrincipal,
                    estiloInicial = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.clickable(
                        onClickLabel = stringResource(R.string.foto_cambiar),
                        role = Role.Button
                    ) { mostrarFoto = true }
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = VioletaPrincipal, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = usuario.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { mostrarEditarPerfil = true }) {
                CirculoBloque(tamano = 40.dp) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.editar_perfil), tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---------- Estadísticas: tres bloques de color ----------
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BloqueEstadistica(usuario.contarMaterias().toString(), stringResource(R.string.materias), Icons.AutoMirrored.Filled.MenuBook, BloqueVioleta, Modifier.weight(1f))
            BloqueEstadistica(stringResource(R.string.horas_corto, usuario.calcularHorasEstudio()), stringResource(R.string.estudio), Icons.Filled.Timer, BloqueVerde, Modifier.weight(1f))
            BloqueEstadistica(usuario.contarNotas().toString(), stringResource(R.string.notas), Icons.Filled.StickyNote2, BloqueNaranja, Modifier.weight(1f))
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
                        Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(22.dp))
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
        BloqueAjuste(Icons.Filled.NotificationsActive, BloqueNaranja) {
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

        // ---------- Tema: sol (claro) o luna (oscuro) ----------
        // Si todavía sigue al teléfono (SISTEMA), se marca el que se ve ahora.
        val oscuroActivo = when (estado.tema) {
            PreferenciaTema.CLARO -> false
            PreferenciaTema.OSCURO -> true
            PreferenciaTema.SISTEMA -> isSystemInDarkTheme()
        }
        BloqueAjuste(Icons.Filled.Palette, BloqueVioleta) {
            Text(text = stringResource(R.string.tema), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BotonTema(Icons.Filled.LightMode, stringResource(R.string.tema_claro), elegido = !oscuroActivo) {
                    viewModel.cambiarTema(PreferenciaTema.CLARO)
                }
                BotonTema(Icons.Filled.DarkMode, stringResource(R.string.tema_oscuro), elegido = oscuroActivo) {
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
            BloqueCuenta(stringResource(R.string.estadisticas_titulo), Icons.Filled.Insights, BloqueVerde, Modifier.weight(1f), onAbrirEstadisticas)
            BloqueCuenta(stringResource(R.string.ayuda_y_soporte), Icons.AutoMirrored.Filled.HelpOutline, BloqueNaranja, Modifier.weight(1f), onAbrirAyuda)
        }
        Spacer(modifier = Modifier.height(10.dp))
        // Copia de seguridad: el archivo se elige con el selector del sistema
        // (Descargas, Drive...), sin pedir permisos.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BloqueCuenta(stringResource(R.string.copia_guardar), Icons.Filled.Backup, BloqueAzul, Modifier.weight(1f)) {
                guardarCopia.launch(nombreCopia)
            }
            BloqueCuenta(stringResource(R.string.copia_restaurar), Icons.Filled.Restore, BloqueVioleta, Modifier.weight(1f)) {
                confirmarRestaurar = true
            }
        }
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
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = rojo, modifier = Modifier.size(20.dp))
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
            icono = Icons.Filled.Restore,
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
            icono = Icons.Filled.NotificationsActive,
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
            icono = Icons.Filled.NotificationsOff,
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
            Text(text = valor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = colorAcentoTexto(color), maxLines = 1)
            Text(text = etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

// Bloque claro de una preferencia: el ícono en un círculo de color a la
// izquierda y el resto (texto, interruptor o selector) a la derecha.
@Composable
private fun BloqueAjuste(icono: ImageVector, color: Color, contenido: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
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
            .height(116.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = alTocar)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconoCircular(icono, Color.White, fondo = color)
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(text = texto, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClickLabel = stringResource(R.string.logros_ver_todos), role = Role.Button, onClick = onAbrir)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoCircular(Icons.Filled.EmojiEvents, Color.White, fondo = BloqueNaranja)
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
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
