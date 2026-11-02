package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Today
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
import com.example.estudia.ui.componentes.CasillaDia
import com.example.estudia.ui.componentes.DialogoEvento
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

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(onClick = {
                eventoParaEditar = null
                mostrarDialogo = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.nuevo_evento))
            }
        }
    ) { relleno ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(relleno),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.mi_calendario),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::irAHoy) {
                        Icon(Icons.Filled.Today, contentDescription = stringResource(R.string.ir_a_hoy))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
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
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    diasSemana.forEach { letra ->
                        Text(
                            text = letra,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            items(estado.semanas) { semana ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    semana.forEach { fecha ->
                        CasillaDia(
                            fecha = fecha,
                            esHoy = fecha == estado.hoy,
                            seleccionado = fecha == estado.seleccionado,
                            tieneEventos = fecha in estado.diasConEventos,
                            modifier = Modifier.weight(1f),
                            onSeleccionar = { if (fecha != null) viewModel.seleccionar(fecha) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = textoFecha(estado.seleccionado, estado.hoy), style = MaterialTheme.typography.titleMedium)
                if (estado.eventosDelDia.isEmpty()) {
                    Text(text = stringResource(R.string.sin_eventos_fecha), style = MaterialTheme.typography.bodySmall)
                }
            }

            items(estado.eventosDelDia, key = { it.evento.id }) { item ->
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

    if (mostrarDialogo) {
        val editando = eventoParaEditar
        DialogoEvento(
            evento = editando?.evento,
            materiaIdInicial = editando?.materiaId,
            // Al editar se puede mover el evento a otra materia activa.
            materias = estado.materiasActivas,
            fechaInicial = estado.seleccionado,
            onConfirmar = { materiaId, titulo, tipo, fecha, inicio, fin ->
                val error = viewModel.guardarEvento(materiaId, editando?.evento, titulo, tipo, fecha, inicio, fin)
                if (error == null) mostrarDialogo = false
                error
            },
            onCancelar = { mostrarDialogo = false }
        )
    }
}
