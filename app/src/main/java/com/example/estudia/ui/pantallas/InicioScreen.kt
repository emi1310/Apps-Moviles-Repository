package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.ui.componentes.GraficoBarrasProgreso
import com.example.estudia.ui.componentes.GraficoTortaHoras
import com.example.estudia.ui.componentes.TarjetaEventoResumen
import com.example.estudia.ui.componentes.TarjetaTareaPendiente
import com.example.estudia.util.minutosDelDia

private data class TareaPendiente(
    val titulo: String,
    val nombreMateria: String,
    val fechaLimite: String,
    val colorMateria: Color
)

@Composable
fun InicioScreen(usuario: Usuario) {

    val tareasPendientes = usuario.materias.flatMap { materia ->
        materia.tareas
            .filter { !it.completada }
            .map { tarea ->
                TareaPendiente(tarea.titulo, materia.nombre, tarea.fechaLimite, materia.color)
            }
    }

    val proximosEventos = usuario.materias.flatMap { materia ->
        materia.eventos.filter { it.tipo == TipoEvento.EXAMEN || it.tipo == TipoEvento.ENTREGA }
    }

    val notaDestacada = usuario.materias.flatMap { it.notas }.firstOrNull { it.esFavorita }

    val materiasActivas = usuario.materias.filter { !it.archivada }

    val minutosPorMateria = materiasActivas.map { materia ->
        val minutos = materia.eventos
            .filter { it.tipo == TipoEvento.BLOQUE_ESTUDIO }
            .sumOf { minutosDelDia(it.horaFin) - minutosDelDia(it.horaInicio) }
        materia to minutos
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "¡Hola, ${usuario.nombre.substringBefore(" ")}!", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Vamos por un día productivo", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Tareas pendientes", style = MaterialTheme.typography.titleMedium)
                    Text(text = "${tareasPendientes.size} pendientes", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (tareasPendientes.isEmpty()) {
                    Text(text = "No tenés tareas pendientes", style = MaterialTheme.typography.bodySmall)
                } else {
                    tareasPendientes.take(4).forEach { tarea ->
                        TarjetaTareaPendiente(
                            titulo = tarea.titulo,
                            nombreMateria = tarea.nombreMateria,
                            fechaLimite = tarea.fechaLimite,
                            colorMateria = tarea.colorMateria
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "Próximos eventos", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (proximosEventos.isEmpty()) {
                    Text(text = "No hay eventos próximos", style = MaterialTheme.typography.bodySmall)
                } else {
                    proximosEventos.take(3).forEach { evento ->
                        TarjetaEventoResumen(titulo = evento.titulo, fecha = evento.fecha)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (notaDestacada != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Nota destacada", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = notaDestacada.titulo, style = MaterialTheme.typography.bodyLarge)
                    Text(text = notaDestacada.contenido, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (materiasActivas.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Progreso por materia", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    GraficoBarrasProgreso(materias = materiasActivas)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Horas de estudio por materia", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    GraficoTortaHoras(datos = minutosPorMateria)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = "Horas de estudio planificadas: ${usuario.calcularHorasEstudio()}h",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
