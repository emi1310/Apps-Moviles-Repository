package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Videocam
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.estudia.modelo.TipoEvento
import androidx.compose.material3.Icon
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
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

// Tarea pendiente en Inicio: checkbox; arriba el título (hasta 2 renglones)
// con la fecha límite a la derecha (en rojo si está vencida); abajo las
// etiquetas: materia, prioridad y pasos hechos. Las etiquetas van en un
// FlowRow: si no entran en un renglón pasan al siguiente en lugar de
// aplastarse (antes "Alta" y "1 de 3 pasos" quedaban letra por letra).
@Composable
fun TarjetaTareaPendiente(item: TareaConMateria, hoy: LocalDate, onAlternar: () -> Unit) {
    val tarea = item.tarea
    // El título usa todo el ancho; la fecha va con las demás etiquetas abajo.
    Row(modifier = Modifier.fillMaxWidth().padding(start = 2.dp, end = 12.dp, top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = tarea.completada, onCheckedChange = { onAlternar() })
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = tarea.titulo,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (tarea.completada) TextDecoration.LineThrough else null
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                tarea.fechaLimite?.let { fecha -> EtiquetaFecha(textoFecha(fecha, hoy), vencida = tarea.estaVencida(hoy)) }
                ChipMateria(item.nombreMateria, item.colorMateria)
                EtiquetaPrioridad(tarea.prioridad)
                if (tarea.subtareas.isNotEmpty()) {
                    // Pasos hechos, como pastilla gris con el ícono de lista.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Outlined.Checklist, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.subtareas_progreso, tarea.subtareasHechas, tarea.subtareas.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// Fecha dentro de una pastilla; roja si la tarea está vencida.
@Composable
fun EtiquetaFecha(texto: String, vencida: Boolean) {
    val color = if (vencida) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        softWrap = false,
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
            // Con temario: cuántos temas van estudiados; si no, la hora.
            Text(
                text = if (item.evento.temas.isNotEmpty())
                    stringResource(R.string.temario_progreso, item.evento.temasEstudiados, item.evento.temas.size)
                else item.evento.horaInicio.aTexto(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Evento del día elegido en el Calendario: tarjeta con la hora en una
// pastilla del color de la materia a la izquierda, el título, el tipo y la
// materia, el ícono del tipo y el menú para editar o eliminar. Los exámenes
// muestran además cuánto falta y el avance del temario, y al tocarlos se
// abre el temario (onAbrirTemario).
@Composable
fun TarjetaEventoCalendario(
    item: EventoConMateria,
    hoy: LocalDate,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onAbrirTemario: (() -> Unit)?,
    modifier: Modifier = Modifier,
    // Ese día es feriado: las clases no se dan.
    esFeriado: Boolean = false
) {
    val context = LocalContext.current
    val color = Color(item.colorMateria)
    val acento = colorAcentoTexto(color)
    val evento = item.evento
    val tocable = if (onAbrirTemario != null)
        Modifier.clickable(onClickLabel = stringResource(R.string.temario_ver), role = Role.Button, onClick = onAbrirTemario)
    else Modifier
    BloqueColor(color = color, modifier = modifier.padding(vertical = 4.dp)) {
      Column(modifier = tocable) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Horario: inicio en grande y fin debajo, en una pastilla del color.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(fondoSuave(color))
                    .padding(vertical = 10.dp)
            ) {
                Text(text = item.evento.horaInicio.aTexto(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = acento)
                Text(
                    text = item.evento.horaFin.aTexto(),
                    style = MaterialTheme.typography.bodySmall,
                    color = acento.copy(alpha = 0.8f)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Las clases semanales muestran además el día: "Cada lunes".
                if (item.evento.semanal) {
                    Text(
                        text = stringResource(R.string.cada_dia_semana, nombreDiaSemana(item.evento.fecha)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Presencial (con el aula, si se cargó) o virtual.
                if (evento.virtual || evento.lugar != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(
                            if (evento.virtual) Icons.Outlined.Videocam else Icons.Outlined.Place,
                            contentDescription = null,
                            tint = acento,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (evento.virtual) stringResource(R.string.evento_virtual) else evento.lugar.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = acento,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            CirculoBloque(tamano = 40.dp) {
                Icon(iconoTipo(item.evento.tipo), contentDescription = null, modifier = Modifier.size(22.dp))
            }
            MenuOpciones(
                listOfNotNull(
                    onAbrirTemario?.let { OpcionMenu(stringResource(R.string.temario_ver), Icons.Outlined.Checklist, alTocar = it) },
                    OpcionMenu(stringResource(R.string.editar), Icons.Outlined.Edit, alTocar = onEditar),
                    OpcionMenu(stringResource(R.string.eliminar), Icons.Outlined.Delete, destructiva = true, alTocar = onEliminar)
                ),
                descripcion = stringResource(R.string.opciones_de, item.evento.titulo)
            )
        }
        // Feriado: ese día la clase no se da (y no cuenta en la asistencia).
        if (esFeriado && evento.tipo == TipoEvento.CLASE) {
            Text(
                text = stringResource(R.string.feriado_sin_clase),
                style = MaterialTheme.typography.labelMedium,
                color = acento,
                modifier = Modifier
                    .padding(start = 12.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(fondoSuave(color))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        } else if (evento.virtual && evento.enlace != null) {
            // Clase virtual con link: botón para entrar.
            val sinApp = stringResource(R.string.evento_enlace_error)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(fondoSuave(color))
                    .clickable(role = Role.Button) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(evento.enlace)))
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, sinApp, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Outlined.Videocam, contentDescription = null, tint = acento, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.evento_unirse), style = MaterialTheme.typography.labelLarge, color = acento)
            }
        }
        // Examen: cuánto falta y cuántos temas del temario van estudiados.
        if (onAbrirTemario != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, end = 16.dp, bottom = 14.dp)
            ) {
                Text(
                    text = textoCuentaRegresiva(evento.fecha, hoy),
                    style = MaterialTheme.typography.labelMedium,
                    color = acento,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(fondoSuave(color))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                if (evento.temas.isEmpty()) {
                    Text(
                        text = stringResource(R.string.temario_agregar),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { evento.temasEstudiados.toFloat() / evento.temas.size },
                        color = color,
                        trackColor = color.copy(alpha = 0.16f),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                        modifier = Modifier.weight(1f).height(6.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.temario_progreso, evento.temasEstudiados, evento.temas.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
      }
    }
}
