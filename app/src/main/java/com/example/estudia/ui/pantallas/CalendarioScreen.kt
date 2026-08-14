package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Usuario
import com.example.estudia.ui.componentes.CasillaDia
import com.example.estudia.ui.componentes.TarjetaEventoCalendario
import com.example.estudia.util.*

// Representa un Evento junto con el nombre de la materia a la que
// pertenece (Evento por sí solo no lo sabe, hay que buscarlo a
// través de la Materia dueña).
private data class EventoConMateria(val evento: Evento, val nombreMateria: String)

// Pantalla "Mi Calendario": muestra una grilla del mes actual y,
// debajo, los eventos de hoy y de mañana agrupados por fecha.
@Composable
fun CalendarioScreen(usuario: Usuario) {
    var anioMostrado by remember { mutableStateOf(obtenerAnioActual()) }
    var mesMostrado by remember { mutableStateOf(obtenerMesActual()) }

    val diasDelMes = obtenerDiasDelMes(anioMostrado, mesMostrado)
    val diasSemana = listOf("D", "L", "M", "M", "J", "V", "S")

    val esMesActual = anioMostrado == obtenerAnioActual() && mesMostrado == obtenerMesActual()

    val todosLosEventos: List<EventoConMateria> = usuario.materias.flatMap { materia ->
        materia.eventos.map { evento -> EventoConMateria(evento, materia.nombre) }
    }

    val fechaHoy = obtenerFechaHoyComoTexto()
    val fechaManiana = obtenerFechaMananaComoTexto()

    val eventosHoy = todosLosEventos.filter { it.evento.fecha == fechaHoy }
    val eventosManiana = todosLosEventos.filter { it.evento.fecha == fechaManiana }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Mi Calendario", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        // ---------- Encabezado del mes con flechas para cambiar ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                if (mesMostrado == 0) { mesMostrado = 11; anioMostrado-- }
                else mesMostrado--
            }) { Text("<") }

            Text(text = "${nombreDelMes(mesMostrado)} $anioMostrado", style = MaterialTheme.typography.titleMedium)

            IconButton(onClick = {
                if (mesMostrado == 11) { mesMostrado = 0; anioMostrado++ }
                else mesMostrado++
            }) { Text(">") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ---------- Nombres de los días de la semana ----------
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            diasSemana.forEach { letra -> Text(text = letra, style = MaterialTheme.typography.labelMedium) }
        }

        // ---------- Grilla de días ----------
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(220.dp)
        ) {
            items(diasDelMes) { dia ->
                val esHoy = esMesActual && dia == obtenerDiaActual()
                CasillaDia(dia = dia, esHoy = esHoy)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------- Eventos de hoy ----------
        Text(text = "Hoy", style = MaterialTheme.typography.titleMedium)
        if (eventosHoy.isEmpty()) {
            Text(text = "No hay eventos para hoy", style = MaterialTheme.typography.bodySmall)
        } else {
            eventosHoy.forEach {
                TarjetaEventoCalendario(it.evento.titulo, it.nombreMateria, it.evento.horaInicio, it.evento.horaFin)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---------- Eventos de mañana ----------
        Text(text = "Mañana", style = MaterialTheme.typography.titleMedium)
        if (eventosManiana.isEmpty()) {
            Text(text = "No hay eventos para mañana", style = MaterialTheme.typography.bodySmall)
        } else {
            eventosManiana.forEach {
                TarjetaEventoCalendario(it.evento.titulo, it.nombreMateria, it.evento.horaInicio, it.evento.horaFin)
            }
        }
    }
}