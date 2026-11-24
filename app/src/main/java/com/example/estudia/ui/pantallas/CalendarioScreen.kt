package com.example.estudia.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Today
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
    onCreacionAbierta: () -> Unit = {}
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RecolectarAvisos(viewModel.avisos)

    // Diálogo de evento: eventoParaEditar == null significa "nuevo evento".
    var mostrarDialogo by remember { mutableStateOf(false) }
    var eventoParaEditar by remember { mutableStateOf<EventoConMateria?>(null) }

    val meses = stringArrayResource(R.array.meses)
    val diasSemana = stringArrayResource(R.array.dias_semana_corto)

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
                    FilledTonalIconButton(onClick = viewModel::irAHoy) {
                        Icon(Icons.Filled.Today, contentDescription = stringResource(R.string.ir_a_hoy))
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
                        onEditar = {
                            eventoParaEditar = item
                            mostrarDialogo = true
                        },
                        onEliminar = { viewModel.eliminarEvento(item) }
                    )
                }
            }
        }
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
