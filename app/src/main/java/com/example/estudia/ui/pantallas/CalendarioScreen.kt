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
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.estudia.ui.componentes.DialogoApp
import com.example.estudia.ui.componentes.superficieVidrio
import com.example.estudia.ui.componentes.TextoDialogo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material.icons.outlined.HowToReg
import com.example.estudia.ui.componentes.TarjetaAcceso
import com.example.estudia.ui.theme.BloqueAzul
import com.example.estudia.ui.theme.BloqueVerde
import androidx.compose.material.icons.outlined.EventAvailable
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
import com.example.estudia.ui.componentes.DialogoRecesos
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
import com.example.estudia.viewmodel.EventoEncontrado
import com.example.estudia.ui.componentes.ChipMateria
import com.example.estudia.ui.componentes.FilaTarjeta
import com.example.estudia.ui.componentes.iconoTipo
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.outlined.SearchOff
import java.time.LocalDate

// Pantalla "Mi Calendario": la grilla del mes y, debajo, los eventos del
// día elegido (con opción de editarlos o eliminarlos).
@Composable
fun CalendarioScreen(
    viewModel: CalendarioViewModel,
    // true cuando se eligió crear desde el botón "+" del dock.
    crearAlAbrir: Boolean = false,
    onCreacionAbierta: () -> Unit = {},
    onAbrirHorario: () -> Unit = {},
    onAbrirAsistencia: () -> Unit = {},
    // Lo que se escribe en la lupa del dock: busca entre los eventos.
    busqueda: String = "",
    onCerrarBusqueda: () -> Unit = {}
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(busqueda) { viewModel.cambiarBusqueda(busqueda) }
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
    // El botón del calendario abre las opciones: pasar al teléfono (y que se
    // mantenga al día), exportar un .ics o dejar de sincronizar.
    var opcionesCalendario by remember { mutableStateOf(false) }
    val nombreArchivoIcs = stringResource(R.string.calendario_archivo_ics)
    val guardarIcs = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri ->
        if (uri != null) viewModel.exportarIcs(uri.toString(), nombreCalendario)
    }
    if (opcionesCalendario) {
        DialogoOpcionesCalendario(
            automatico = estado.calendarioAutomatico,
            onPasar = { opcionesCalendario = false; pasarAlTelefono() },
            onExportar = { opcionesCalendario = false; guardarIcs.launch(nombreArchivoIcs) },
            onDejar = { opcionesCalendario = false; viewModel.dejarDeSincronizar() },
            onCancelar = { opcionesCalendario = false }
        )
    }
    if (explicarCalendario) {
        DialogoApp(
            titulo = stringResource(R.string.permiso_calendario_titulo),
            icono = Icons.Outlined.EventRepeat,
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
            icono = Icons.Outlined.EventBusy,
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

    // ---------- Feriados ----------
    // Si ya hay permiso de leer el calendario, se traen del calendario de
    // Google del teléfono. Si no, se explica y se pide; si dice que no (o
    // elige "Desde internet"), se bajan de internet.
    var explicarFeriados by remember { mutableStateOf(false) }
    val pedirLectura = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        viewModel.importarFeriados(usarCalendario = concedido)
    }
    val importarFeriados: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            viewModel.importarFeriados(usarCalendario = true)
        } else {
            explicarFeriados = true
        }
    }
    var viendoRecesos by rememberSaveable { mutableStateOf(false) }
    if (viendoRecesos) {
        DialogoRecesos(
            recesos = estado.recesos,
            onGuardar = viewModel::guardarReceso,
            onEliminar = viewModel::eliminarReceso,
            onCerrar = { viendoRecesos = false }
        )
    }
    if (explicarFeriados) {
        DialogoApp(
            titulo = stringResource(R.string.feriados_titulo),
            icono = Icons.Outlined.Celebration,
            textoConfirmar = stringResource(R.string.continuar),
            onConfirmar = {
                explicarFeriados = false
                pedirLectura.launch(Manifest.permission.READ_CALENDAR)
            },
            textoCancelar = stringResource(R.string.feriados_desde_internet),
            onCancelar = {
                explicarFeriados = false
                viewModel.importarFeriados(usarCalendario = false)
            },
            // Tocar afuera solo cierra.
            onDismissRequest = { explicarFeriados = false }
        ) {
            TextoDialogo(stringResource(R.string.feriados_explicacion))
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
                        FilledTonalIconButton(onClick = importarFeriados, enabled = !estado.actualizandoFeriados) {
                            Icon(Icons.Outlined.Celebration, contentDescription = stringResource(R.string.feriados_importar))
                        }
                        // Recesos: vacaciones y semanas sin clases.
                        FilledTonalIconButton(onClick = { viendoRecesos = true }) {
                            Icon(Icons.Outlined.BeachAccess, contentDescription = stringResource(R.string.recesos_titulo))
                        }
                        FilledTonalIconButton(onClick = { opcionesCalendario = true }) {
                            Icon(Icons.Outlined.EventRepeat, contentDescription = stringResource(R.string.calendario_telefono))
                        }
                        FilledTonalIconButton(onClick = viewModel::irAHoy) {
                            Icon(Icons.Outlined.Today, contentDescription = stringResource(R.string.ir_a_hoy))
                        }
                    }
                }
                if (estado.buscando) {
                    // ---------- Resultados de la lupa del dock ----------
                    EncabezadoResultados(estado.resultadosBusqueda.size)
                    if (estado.resultadosBusqueda.isEmpty()) {
                        EstadoVacio(Icons.Outlined.SearchOff, stringResource(R.string.sin_resultados), stringResource(R.string.calendario_sin_resultados))
                    }
                } else {
                // Accesos de las clases: la asistencia y el horario de la semana.
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 12.dp).height(IntrinsicSize.Min)) {
                    TarjetaAcceso(stringResource(R.string.asistencia_titulo), stringResource(R.string.acceso_asistencia), Icons.Outlined.HowToReg, BloqueVerde, Modifier.weight(1f).fillMaxHeight(), alTocar = onAbrirAsistencia)
                    TarjetaAcceso(stringResource(R.string.acceso_horario_titulo), stringResource(R.string.acceso_horario), Icons.Outlined.ViewWeek, BloqueAzul, Modifier.weight(1f).fillMaxHeight(), alTocar = onAbrirHorario)
                }

                // ---------- El mes, dentro de un bloque ----------
                Card(
                    modifier = Modifier.fillMaxWidth().superficieVidrio(MaterialTheme.shapes.large, MaterialTheme.colorScheme.surfaceContainerHighest),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
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
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = stringResource(R.string.mes_anterior))
                            }
                            FilledTonalIconButton(onClick = viewModel::mesSiguiente) {
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = stringResource(R.string.mes_siguiente))
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
                                        feriado = fecha != null && fecha in estado.feriadosDelMes,
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
                // Feriado: su nombre, en un cartel arriba de los eventos del día.
                estado.feriadoDelDia?.let { nombre ->
                    Spacer(modifier = Modifier.height(8.dp))
                    CartelFeriado(nombre)
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (estado.eventosDelDia.isEmpty()) {
                    EstadoVacio(
                        Icons.Outlined.EventAvailable,
                        stringResource(R.string.sin_eventos_fecha),
                        stringResource(R.string.sin_eventos_fecha_sugerencia)
                    )
                }
                }
            }

            if (estado.buscando) {
                // Al tocar un resultado se cierra la búsqueda y el calendario
                // salta a ese día.
                items(estado.resultadosBusqueda, key = { "b" + it.item.evento.id }) { encontrado ->
                    FilaEventoEncontrado(encontrado, estado.hoy, Modifier.animateItem()) {
                        viewModel.irA(encontrado.fecha)
                        onCerrarBusqueda()
                    }
                }
            } else items(estado.eventosDelDia, key = { it.evento.id }) { item ->
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
                        } else null,
                        esFeriado = estado.feriadoDelDia != null
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
            onConfirmar = { materiaId, titulo, tipo, fecha, inicio, fin, semanal, repetirHasta, virtual, lugar, enlace, cadaSemanas ->
                val error = viewModel.guardarEvento(
                    materiaId, editando?.evento, titulo, tipo, fecha, inicio, fin, semanal, repetirHasta,
                    virtual, lugar, enlace, cadaSemanas
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

// Cartel del feriado del día elegido: el nombre y que no hay clases.
@Composable
private fun CartelFeriado(nombre: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(Icons.Outlined.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.feriado_nombre, nombre),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = stringResource(R.string.feriado_detalle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

// Título de los resultados de la búsqueda, con cuántos hay.
@Composable
private fun EncabezadoResultados(cantidad: Int) {
    Text(
        text = pluralStringResource(R.plurals.calendario_resultados, cantidad, cantidad),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)
    )
}

// Un evento encontrado: a la izquierda la fecha (día y mes) en un bloque del
// color de la materia; al lado el título, la materia, cuándo es y el horario.
// Los pasados se ven un poco más apagados.
@Composable
private fun FilaEventoEncontrado(encontrado: EventoEncontrado, hoy: LocalDate, modifier: Modifier = Modifier, alTocar: () -> Unit) {
    val (evento, _, nombreMateria, colorMateria) = encontrado.item
    val color = Color(colorMateria)
    val meses = stringArrayResource(R.array.meses)
    val pasado = encontrado.fecha.isBefore(hoy)
    FilaTarjeta(modifier = modifier.padding(vertical = 4.dp).alpha(if (pasado) 0.6f else 1f), onClick = alTocar) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(color.copy(alpha = 0.16f))
                .padding(vertical = 6.dp)
        ) {
            Text(encontrado.fecha.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(meses[encontrado.fecha.monthValue - 1].take(3).lowercase(), style = MaterialTheme.typography.labelSmall, color = color)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(evento.titulo, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            ChipMateria(nombreMateria, colorMateria)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${textoFecha(encontrado.fecha, hoy)} · %02d:%02d".format(evento.horaInicio.hour, evento.horaInicio.minute),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(iconoTipo(evento.tipo), contentDescription = null, tint = color, modifier = Modifier.padding(horizontal = 12.dp).size(22.dp))
    }
}

// Opciones del calendario fuera de la app. Cada una es un bloque con ícono,
// título y una línea que explica qué hace.
@Composable
private fun DialogoOpcionesCalendario(
    automatico: Boolean,
    onPasar: () -> Unit,
    onExportar: () -> Unit,
    onDejar: () -> Unit,
    onCancelar: () -> Unit
) {
    DialogoApp(
        titulo = stringResource(R.string.calendario_opciones_titulo),
        icono = Icons.Outlined.EventRepeat,
        textoConfirmar = null,
        onConfirmar = {},
        onCancelar = onCancelar
    ) {
        val opciones = buildList {
            add(
                Triple(
                    Icons.Outlined.EventRepeat,
                    stringResource(if (automatico) R.string.calendario_actualizar else R.string.calendario_telefono) to
                        stringResource(if (automatico) R.string.calendario_automatico_activo else R.string.calendario_telefono_detalle),
                    onPasar
                )
            )
            add(Triple(Icons.Outlined.FileDownload, stringResource(R.string.calendario_exportar_ics) to stringResource(R.string.calendario_exportar_ics_detalle), onExportar))
            if (automatico) {
                add(Triple(Icons.Outlined.EventBusy, stringResource(R.string.calendario_dejar) to stringResource(R.string.calendario_dejar_detalle), onDejar))
            }
        }
        opciones.forEach { (icono, textos, alTocar) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = alTocar)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(textos.first, style = MaterialTheme.typography.bodyLarge)
                    Text(textos.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
