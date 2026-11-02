package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.util.aTexto
import com.example.estudia.viewmodel.EventoConMateria
import com.example.estudia.viewmodel.TareaConMateria
import java.time.LocalDate

// Filas que se usan en Inicio y en el Calendario.

// Tarea pendiente en Inicio: círculo del color de la materia, título,
// materia y fecha límite (en rojo si está vencida). Se puede completar.
@Composable
fun TarjetaTareaPendiente(item: TareaConMateria, hoy: LocalDate, onAlternar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.tarea.completada, onCheckedChange = { onAlternar() })
        Box(modifier = Modifier.size(10.dp).background(Color(item.colorMateria), CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.tarea.titulo, style = MaterialTheme.typography.bodyLarge)
            Text(text = item.nombreMateria, style = MaterialTheme.typography.bodySmall)
        }
        val fecha = item.tarea.fechaLimite
        if (fecha != null) {
            Text(
                text = textoFecha(fecha, hoy),
                style = MaterialTheme.typography.bodySmall,
                color = if (item.tarea.estaVencida(hoy)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Examen o entrega próximo en Inicio.
@Composable
fun TarjetaEventoResumen(item: EventoConMateria, hoy: LocalDate) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).background(Color(item.colorMateria), CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.evento.titulo, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${stringResource(nombreTipo(item.evento.tipo))} · ${item.nombreMateria}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = "${textoFecha(item.evento.fecha, hoy)} ${item.evento.horaInicio.aTexto()}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// Evento del día elegido en el Calendario, con menú para editar o eliminar.
@Composable
fun TarjetaEventoCalendario(item: EventoConMateria, onEditar: () -> Unit, onEliminar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).background(Color(item.colorMateria), CircleShape))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.evento.titulo, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${item.nombreMateria} · ${stringResource(nombreTipo(item.evento.tipo))} · " +
                    "${item.evento.horaInicio.aTexto()} - ${item.evento.horaFin.aTexto()}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        MenuOpciones(
            listOf(
                OpcionMenu(stringResource(R.string.editar), Icons.Filled.Edit, onEditar),
                OpcionMenu(stringResource(R.string.eliminar), Icons.Filled.Delete, onEliminar)
            ),
            descripcion = stringResource(R.string.opciones_de, item.evento.titulo)
        )
    }
}
