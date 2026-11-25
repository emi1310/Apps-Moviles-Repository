package com.example.estudia.ui.pantallas

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.estudia.ui.componentes.DialogoApp
import com.example.estudia.ui.componentes.TextoDialogo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.ui.componentes.LocalEspacioDock
import com.example.estudia.ui.componentes.texto
import com.example.estudia.ui.componentes.Deslizable
import com.example.estudia.ui.componentes.CasillaDia
import com.example.estudia.ui.componentes.DialogoEvento
import com.example.estudia.ui.componentes.DialogoTemario
import com.example.estudia.modelo.TipoEvento
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import com.example.estudia.ui.componentes.EncabezadoPantalla
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.RecolectarAvisos
import com.example.estudia.ui.componentes.TarjetaEventoCalendario
import com.example.estudia.ui.componentes.textoFecha
import com.example.estudia.viewmodel.CalendarioViewModel
import com.example.estudia.viewmodel.EventoConMateria

// Pantalla "Mi Calendario": la grilla del mes y, debajo, los eventos del
// día elegido (con opción de editarlos o eliminarlos).
@Composable
fun CalendarioScreen(
    viewModel: CalendarioViewModel,
    // true cuando se eligió crear desde el botón "+" del dock.
    crearAlAbrir: Boolean = false,
    onCreacionAbierta: () -> Unit = {},
    onAbrirHorario: () -> Unit = {}
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RecolectarAvisos(viewModel.avisos)

    // Diálogo de evento: eventoParaEditar == null significa "nuevo evento".
    var mostrarDialogo by remember { mutableStateOf(false) }
    var eventoParaEditar by remember { mutableStateOf<EventoConMateria?>(null) }
    // Examen cuyo temario está abierto (se busca en la lista del día, así el
    // diálogo se actualiza solo cuando cambian los temas).
    var temarioDeEventoId by rememberSaveable { mutableStateOf<Int?>(null) }

    val meses = stringArrayResource(R.array.meses)
    val diasSemana = stringArrayResource(R.array.dias_semana_corto)

    // ---------- Calendario del teléfono (permiso de calendario) ----------
    // Como con los demás permisos: primero se explica, después se pide, y si
    // lo bloqueó se ofrece ir a Ajustes. Sin permiso no se toca el calendario.
    val context = LocalContext.current
    val nombreCalendario = stringResource(R.string.calendario_estudia)
    var explicarCalendario by remember { mutableStateOf(false) }
    var calendarioBloqueado by remember { mutableStateOf(false) }
    val pedirCalendario = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        when {
            resultado.values.all { it } -> viewModel.sincronizarConTelefono(nombreCalendario)
            context.buscarActividad()?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.WRITE_CALENDAR)
            } == true -> calendarioBloqueado = true
        }
    }
    val pasarAlTelefono: () -> Unit = {
        if (PERMISOS_CALENDARIO.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            viewModel.sincronizarConTelefono(nombreCalendario)
        } else {
            explicarCalendario = true
        }
    }
    if (explicarCalendario) {
        DialogoApp(
            titulo = stringResource(R.string.permiso_calendario_titulo),
            icono = Icons.Filled.EventRepeat,
            textoConfirmar = stringResource(R.string.continuar),
            onConfirmar = {
                explicarCalendario = false
                pedirCalendario.launch(PERMISOS_CALENDARIO)
            },
            textoCancelar = stringResource(R.string.ahora_no),
            onCancelar = { explicarCalendario = false }
        ) {
            TextoDialogo(stringResource(R.string.explicacion_permiso_calendario))
        }
    }
    if (calendarioBloqueado) {
        DialogoApp(
            titulo = stringResource(R.string.sin_permiso_calendario_titulo),
            icono = Icons.Filled.EventBusy,
            textoConfirmar = stringResource(R.string.ir_a_ajustes),
            onConfirmar = {
                calendarioBloqueado = false
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            },
            onCancelar = { calendarioBloqueado = false }
        ) {
            TextoDialogo(stringResource(R.string.sin_permiso_calendario))
        }
    }

    LaunchedEffect(crearAlAbrir) {
        if (crearAlAbrir) {
            eventoParaEditar = null; mostrarDialogo = true
            onCreacionAbierta()
        }
    }

    // La lista pasa por detrás del dock flotante: abajo deja su espacio.
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = LocalEspacioDock.current + 16.dp)
        ) {
            item {
                EncabezadoPantalla(
                    titulo = stringResource(R.string.mi_calendario),
                    subtitulo = stringResource(R.string.calendario_subtitulo)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalIconButton(onClick = pasarAlTelefono) {
                            Icon(Icons.Filled.EventRepeat, contentDescription = stringResource(R.string.calendario_telefono))
                        }
                        FilledTonalIconButton(onClick = onAbrirHorario) {
                            Icon(Icons.Filled.ViewWeek, contentDescription = stringResource(R.string.horario_abrir))
                        }
                        FilledTonalIconButton(onClick = viewModel::irAHoy) {
                            Icon(Icons.Filled.Today, contentDescription = stringResource(R.string.ir_a_hoy))
                        }
                    }
                }

                // ---------- El mes, dentro de un bloque ----------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Mes y año a la izquierda; flechas en círculos a la derecha.
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meses[estado.mes.monthValue - 1],
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = estado.mes.year.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalIconButton(onClick = viewModel::mesAnterior) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.mes_anterior))
                            }
                            FilledTonalIconButton(onClick = viewModel::mesSiguiente) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.mes_siguiente))
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            diasSemana.forEach { letra ->
                                Text(
                                    text = letra,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        estado.semanas.forEach { semana ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                semana.forEach { fecha ->
                                    CasillaDia(
                                        fecha = fecha,
                                        esHoy = fecha == estado.hoy,
                                        seleccionado = fecha == estado.seleccionado,
                                        coloresEventos = estado.coloresPorDia[fecha].orEmpty(),
                                        modifier = Modifier.weight(1f),
                                        onSeleccionar = { if (fecha != null) viewModel.seleccionar(fecha) }
                                    )
                                }
                            }
                        }
                    }
                }

                // ---------- Día elegido: fecha y cuántos eventos tiene ----------
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(
                        text = textoFecha(estado.seleccionado, estado.hoy),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (estado.eventosDelDia.isNotEmpty()) {
                        val cantidad = estado.eventosDelDia.size
                        Text(
                            text = pluralStringResource(R.plurals.calendario_eventos_dia, cantidad, cantidad),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (estado.eventosDelDia.isEmpty()) {
                    EstadoVacio(
                        Icons.Filled.EventAvailable,
                        stringResource(R.string.sin_eventos_fecha),
                        stringResource(R.string.sin_eventos_fecha_sugerencia)
                    )
                }
            }

            items(estado.eventosDelDia, key = { it.evento.id }) { item ->
                // Deslizar hacia la izquierda elimina el evento (con "Deshacer").
                Deslizable(
                    modifier = Modifier.animateItem(),
                    onEliminar = { viewModel.eliminarEvento(item) },
                    forma = MaterialTheme.shapes.large,
                    margenVertical = 4.dp
                ) {
                    TarjetaEventoCalendario(
                        item = item,
                        hoy = estado.hoy,
                        onEditar = {
                            eventoParaEditar = item
                            mostrarDialogo = true
                        },
                        onEliminar = { viewModel.eliminarEvento(item) },
                        // Solo los exámenes tienen temario.
                        onAbrirTemario = if (item.evento.tipo == TipoEvento.EXAMEN) {
                            { temarioDeEventoId = item.evento.id }
                        } else null
                    )
                }
            }
        }
    }

    estado.eventosDelDia.firstOrNull { it.evento.id == temarioDeEventoId }?.let { item ->
        DialogoTemario(
            evento = item.evento,
            color = Color(item.colorMateria),
            hoy = estado.hoy,
            onAgregar = { viewModel.agregarTema(item.evento.id, it) },
            onAlternar = { viewModel.alternarTema(item.evento.id, it) },
            onEliminar = { viewModel.eliminarTema(item.evento.id, it) },
            onCerrar = { temarioDeEventoId = null }
        )
    }

    if (mostrarDialogo) {
        val editando = eventoParaEditar
        DialogoEvento(
            evento = editando?.evento,
            materiaIdInicial = editando?.materiaId,
            // Al editar se puede mover el evento a otra materia activa.
            materias = estado.materiasActivas,
            fechaInicial = estado.seleccionado,
            onConfirmar = { materiaId, titulo, tipo, fecha, inicio, fin, semanal, repetirHasta ->
                val error = viewModel.guardarEvento(
                    materiaId, editando?.evento, titulo, tipo, fecha, inicio, fin, semanal, repetirHasta
                )
                if (error == null) mostrarDialogo = false
                error?.texto()
            },
            onCancelar = { mostrarDialogo = false }
        )
    }
}

private val PERMISOS_CALENDARIO = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)

// La Activity detrás del Context (para saber si el permiso quedó bloqueado).
private fun Context.buscarActividad(): Activity? {
    var actual: Context? = this
    while (actual is ContextWrapper) {
        if (actual is Activity) return actual
        actual = actual.baseContext
    }
    return null
}
