package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
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
fun CalendarioScreen(viewModel: CalendarioViewModel) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RecolectarAvisos(viewModel.avisos)

    // Diálogo de evento: eventoParaEditar == null significa "nuevo evento".
    var mostrarDialogo by remember { mutableStateOf(false) }
    var eventoParaEditar by remember { mutableStateOf<EventoConMateria?>(null) }

    val meses = stringArrayResource(R.array.meses)
    val diasSemana = stringArrayResource(R.array.dias_semana_corto)

    // Un solo Scaffold en la app (el de AppNavegacion): el botón flotante
    // va arriba de la lista, en la esquina de abajo a la derecha.
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp)
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

                // ---------- El mes, dentro de una tarjeta ----------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = viewModel::mesAnterior) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.mes_anterior))
                            }
                            Text(
                                text = "${meses[estado.mes.monthValue - 1]} ${estado.mes.year}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = viewModel::mesSiguiente) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.mes_siguiente))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
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

                Spacer(modifier = Modifier.height(20.dp))
                Text(text = textoFecha(estado.seleccionado, estado.hoy), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
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
        ExtendedFloatingActionButton(
            onClick = {
                eventoParaEditar = null
                mostrarDialogo = true
            },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.nuevo_evento)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
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
