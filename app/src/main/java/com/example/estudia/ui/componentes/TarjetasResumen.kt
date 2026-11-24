package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

// Evento del día elegido en el Calendario: un bloque del color de la
// materia con la hora en un bloquecito translúcido a la izquierda, el título,
// el tipo y la materia, el ícono del tipo y el menú para editar o eliminar.
@Composable
fun TarjetaEventoCalendario(
    item: EventoConMateria,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    BloqueColor(color = Color(item.colorMateria), modifier = modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Horario: inicio en grande y fin debajo.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(BlancoTranslucido)
                    .padding(vertical = 10.dp)
            ) {
                Text(text = item.evento.horaInicio.aTexto(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = item.evento.horaFin.aTexto(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.evento.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${stringResource(nombreTipo(item.evento.tipo))} · ${item.nombreMateria}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Las clases semanales muestran además el día: "Cada lunes".
                if (item.evento.semanal) {
                    Text(
                        text = stringResource(R.string.cada_dia_semana, nombreDiaSemana(item.evento.fecha)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            CirculoBloque(tamano = 40.dp) {
                Icon(iconoTipo(item.evento.tipo), contentDescription = null, modifier = Modifier.size(22.dp))
            }
            MenuOpciones(
                listOf(
                    OpcionMenu(stringResource(R.string.editar), Icons.Filled.Edit, alTocar = onEditar),
                    OpcionMenu(stringResource(R.string.eliminar), Icons.Filled.Delete, destructiva = true, alTocar = onEliminar)
                ),
                descripcion = stringResource(R.string.opciones_de, item.evento.titulo)
            )
        }
    }
}
