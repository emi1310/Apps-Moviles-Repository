package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.util.aTexto
import com.example.estudia.viewmodel.EventoConMateria
import com.example.estudia.viewmodel.TareaConMateria
import java.time.LocalDate

// Filas que se usan en Inicio y en el Calendario.

// Tarea pendiente en Inicio: checkbox, título, materia (como etiqueta de
// color) y fecha límite (en rojo si está vencida).
@Composable
fun TarjetaTareaPendiente(item: TareaConMateria, hoy: LocalDate, onAlternar: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = item.tarea.completada, onCheckedChange = { onAlternar() })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.tarea.titulo,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.tarea.completada) TextDecoration.LineThrough else null
            )
            ChipMateria(item.nombreMateria, item.colorMateria)
        }
        val fecha = item.tarea.fechaLimite
        if (fecha != null) {
            EtiquetaFecha(textoFecha(fecha, hoy), vencida = item.tarea.estaVencida(hoy))
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

// Fecha dentro de una pastilla; roja si la tarea está vencida.
@Composable
fun EtiquetaFecha(texto: String, vencida: Boolean) {
    val color = if (vencida) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

// Examen o entrega próximo en Inicio: ícono del tipo con el color de la
// materia, título, materia y cuándo es.
@Composable
fun TarjetaEventoResumen(item: EventoConMateria, hoy: LocalDate) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoCircular(iconoTipo(item.evento.tipo), Color(item.colorMateria), tamano = 40.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.evento.titulo, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = "${stringResource(nombreTipo(item.evento.tipo))} · ${item.nombreMateria}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = textoFecha(item.evento.fecha, hoy), style = MaterialTheme.typography.labelLarge)
            Text(
                text = item.evento.horaInicio.aTexto(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Evento del día elegido en el Calendario: la hora a la izquierda, una
// franja del color de la materia y el detalle, con menú para editar o eliminar.
@Composable
fun TarjetaEventoCalendario(item: EventoConMateria, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val color = Color(item.colorMateria)
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min).padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
                Text(text = item.evento.horaInicio.aTexto(), style = MaterialTheme.typography.labelLarge)
                Text(
                    text = item.evento.horaFin.aTexto(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(text = item.evento.titulo, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChipMateria(item.nombreMateria, item.colorMateria)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(nombreTipo(item.evento.tipo)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
}
