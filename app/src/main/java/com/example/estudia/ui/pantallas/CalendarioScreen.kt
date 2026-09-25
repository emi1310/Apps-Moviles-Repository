package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.ui.componentes.CasillaDia
import com.example.estudia.ui.componentes.DialogoNuevoEvento
import com.example.estudia.ui.componentes.TarjetaEventoCalendario
import com.example.estudia.util.*

private data class EventoConMateria(val evento: Evento, val nombreMateria: String)

@Composable
fun CalendarioScreen(
    usuario: Usuario,
    onAgregarEvento: (Materia, String, TipoEvento, String, String, String) -> Unit
) {
    var anioMostrado by remember { mutableStateOf(obtenerAnioActual()) }
    var mesMostrado by remember { mutableStateOf(obtenerMesActual()) }
    var diaSeleccionado by remember { mutableStateOf(obtenerDiaActual()) }
    var mostrarDialogoNuevoEvento by remember { mutableStateOf(false) }

    val diasDelMes = obtenerDiasDelMes(anioMostrado, mesMostrado)
    val diasSemana = listOf("D", "L", "M", "M", "J", "V", "S")

    val semanas = mutableListOf<List<Int?>>()
    var semana = mutableListOf<Int?>()
    for (dia in diasDelMes) {
        semana.add(dia)
        if (semana.size == 7) {
            semanas.add(semana)
            semana = mutableListOf()
        }
    }
    if (semana.isNotEmpty()) {
        while (semana.size < 7) {
            semana.add(null)
        }
        semanas.add(semana)
    }

    val esMesActual = anioMostrado == obtenerAnioActual() && mesMostrado == obtenerMesActual()

    val todosLosEventos: List<EventoConMateria> = usuario.materias.flatMap { materia ->
        materia.eventos.map { evento -> EventoConMateria(evento, materia.nombre) }
    }

    val fechaSeleccionada = formatearFecha(anioMostrado, mesMostrado, diaSeleccionado)
    val eventosSeleccionados = todosLosEventos
        .filter { it.evento.fecha == fechaSeleccionada }
        .sortedBy { it.evento.horaInicio }

    val mesYAnioMostrados = formatearFecha(anioMostrado, mesMostrado, 1).substringAfter('/')
    val diasConEventos = mutableListOf<Int>()
    for (eventoConMateria in todosLosEventos) {
        val fecha = eventoConMateria.evento.fecha
        if (fecha.substringAfter('/') == mesYAnioMostrados) {
            diasConEventos.add(fecha.substringBefore('/').toInt())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp)
    ) {
        item {
            Text(text = "Mi Calendario", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (mesMostrado == 0) { mesMostrado = 11; anioMostrado-- }
                    else mesMostrado--
                    diaSeleccionado = 1
                }) { Text("‹", style = MaterialTheme.typography.headlineSmall) }

                Text(text = "${nombreDelMes(mesMostrado)} $anioMostrado", style = MaterialTheme.typography.titleMedium)

                IconButton(onClick = {
                    if (mesMostrado == 11) { mesMostrado = 0; anioMostrado++ }
                    else mesMostrado++
                    diaSeleccionado = 1
                }) { Text("›", style = MaterialTheme.typography.headlineSmall) }
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

        items(semanas) { diasDeLaSemana ->
            Row(modifier = Modifier.fillMaxWidth()) {
                diasDeLaSemana.forEach { dia ->
                    CasillaDia(
                        dia = dia,
                        esHoy = esMesActual && dia == obtenerDiaActual(),
                        seleccionado = dia == diaSeleccionado,
                        tieneEventos = dia in diasConEventos,
                        modifier = Modifier.weight(1f),
                        onSeleccionar = { if (dia != null) diaSeleccionado = dia }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = fechaSeleccionada, style = MaterialTheme.typography.titleMedium)
            if (eventosSeleccionados.isEmpty()) {
                Text(text = "No hay eventos para esta fecha", style = MaterialTheme.typography.bodySmall)
            } else {
                eventosSeleccionados.forEach {
                    TarjetaEventoCalendario(it.evento.titulo, it.nombreMateria, it.evento.horaInicio, it.evento.horaFin)
                }
            }
        }
    }

    FloatingActionButton(
        onClick = { mostrarDialogoNuevoEvento = true },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
    ) {
        Text("+", style = MaterialTheme.typography.headlineSmall)
    }
    }

    if (mostrarDialogoNuevoEvento) {
        DialogoNuevoEvento(
            materiasDisponibles = usuario.materias.filter { !it.archivada },
            fechaInicial = fechaSeleccionada,
            onConfirmar = { materiaElegida, titulo, tipo, fecha, horaInicio, horaFin ->
                onAgregarEvento(materiaElegida, titulo, tipo, fecha, horaInicio, horaFin)
                mostrarDialogoNuevoEvento = false
            },
            onCancelar = { mostrarDialogoNuevoEvento = false }
        )
    }
}
