package com.example.estudia.ui.componentes

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.Calificacion
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Prioridad
import com.example.estudia.modelo.Subtarea
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.Tarjeta
import java.time.LocalDate

// Bloque de una materia: una tarjeta neutra con el nombre, las tareas
// pendientes, la inicial en un círculo, el avance y el promedio. El color de
// la materia va en los detalles (círculo, porcentaje y barra). Al tocarla se
// expande (con animación) y aparecen tres bloques: tareas, notas de los
// exámenes y archivos.
@Composable
fun TarjetaMateria(
    materia: Materia,
    onAlternarTarea: (Tarea) -> Unit,
    onAlternarSubtarea: (Tarea, Subtarea) -> Unit,
    onAgregarTarea: () -> Unit,
    onEditarTarea: (Tarea) -> Unit,
    onEliminarTarea: (Tarea) -> Unit,
    onAdjuntarArchivo: () -> Unit,
    onSacarFoto: () -> Unit,
    onAbrirArchivo: (Archivo) -> Unit,
    onEliminarArchivo: (Archivo) -> Unit,
    onAgregarCalificacion: () -> Unit,
    onEditarCalificacion: (Calificacion) -> Unit,
    onEliminarCalificacion: (Calificacion) -> Unit,
    onAgregarTarjeta: () -> Unit,
    onEditarTarjeta: (Tarjeta) -> Unit,
    onEliminarTarjeta: (Tarjeta) -> Unit,
    onRepasar: () -> Unit,
    onEditar: () -> Unit,
    onArchivar: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandida by rememberSaveable { mutableStateOf(false) }
    val color = Color(materia.color)
    val pendientes = materia.tareas.count { !it.completada }
    val progreso = materia.calcularProgreso()
    // La barra se llena de a poco al abrir la pantalla y al marcar tareas.
    val avance = progresoAnimado(progreso / 100f)
    val hoy = LocalDate.now()

    val acento = colorAcentoTexto(color)
    BloqueColor(color = color, modifier = modifier.padding(vertical = 6.dp).animateContentSize()) {
        // ---------- Cabecera del bloque ----------
        run {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = stringResource(if (expandida) R.string.contraer else R.string.expandir),
                        role = Role.Button
                    ) { expandida = !expandida }
                    .padding(start = 20.dp, top = 18.dp, end = 8.dp, bottom = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f).padding(top = 2.dp)) {
                        Text(
                            text = materia.nombre,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (pendientes > 0) pluralStringResource(R.plurals.tareas_pendientes, pendientes, pendientes)
                            else stringResource(R.string.sin_tareas_pendientes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Inicial en un círculo del color de la materia.
                    CirculoBloque(tamano = 48.dp) {
                        Text(text = materia.nombre.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    MenuOpciones(
                        listOf(
                            OpcionMenu(stringResource(R.string.editar), Icons.Filled.Edit, alTocar = onEditar),
                            OpcionMenu(
                                stringResource(if (materia.archivada) R.string.desarchivar else R.string.archivar),
                                if (materia.archivada) Icons.Filled.Unarchive else Icons.Filled.Archive,
                                alTocar = onArchivar
                            ),
                            OpcionMenu(stringResource(R.string.eliminar), Icons.Filled.Delete, destructiva = true, alTocar = onEliminar)
                        ),
                        descripcion = stringResource(R.string.opciones_de, materia.nombre)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(end = 12.dp)) {
                    Text(text = "$progreso%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = acento)
                    Spacer(modifier = Modifier.weight(1f))
                    // Promedio de las notas de los exámenes (verde si aprueba).
                    materia.promedio()?.let {
                        EtiquetaNota(it, prefijo = stringResource(R.string.promedio_corto))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        imageVector = if (expandida) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { avance },
                    color = color,
                    trackColor = color.copy(alpha = 0.16f),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().padding(end = 12.dp).height(8.dp)
                )
            }
        }

        // ---------- Bloques de detalle ----------
        if (expandida) {
            Column(
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BloqueDetalle(Icons.Filled.TaskAlt, stringResource(R.string.tareas_pendientes_titulo), color) {
                    if (materia.tareas.isEmpty()) TextoVacio(stringResource(R.string.sin_tareas))
                    materia.tareas.forEach { tarea ->
                        // key: si la tarea se elimina y se recupera con "Deshacer",
                        // vuelve con su deslizamiento en cero.
                        key(tarea.id) {
                            Deslizable(
                                onCompletar = { onAlternarTarea(tarea) },
                                completada = tarea.completada,
                                onEliminar = { onEliminarTarea(tarea) },
                                forma = RoundedCornerShape(16.dp),
                                fondoContenido = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                FilaTarea(
                                    tarea = tarea,
                                    hoy = hoy,
                                    onAlternar = { onAlternarTarea(tarea) },
                                    onAlternarSubtarea = { onAlternarSubtarea(tarea, it) },
                                    opciones = listOf(
                                        OpcionMenu(stringResource(R.string.editar), Icons.Filled.Edit) { onEditarTarea(tarea) },
                                        OpcionMenu(stringResource(R.string.eliminar), Icons.Filled.Delete, destructiva = true) { onEliminarTarea(tarea) }
                                    )
                                )
                            }
                        }
                    }
                    BotonBloque(stringResource(R.string.agregar_tarea), Icons.Filled.Add, color, onAgregarTarea)
                }

                BloqueDetalle(Icons.Filled.Grade, stringResource(R.string.notas_examenes), color) {
                    if (materia.calificaciones.isEmpty()) TextoVacio(stringResource(R.string.sin_calificaciones))
                    materia.calificaciones.forEach { calificacion ->
                        key(calificacion.id) {
                            Deslizable(
                                onEliminar = { onEliminarCalificacion(calificacion) },
                                forma = RoundedCornerShape(16.dp),
                                fondoContenido = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                FilaCalificacion(
                                    calificacion = calificacion,
                                    onEditar = { onEditarCalificacion(calificacion) },
                                    onEliminar = { onEliminarCalificacion(calificacion) }
                                )
                            }
                        }
                    }
                    BotonBloque(stringResource(R.string.agregar_calificacion), Icons.Filled.Add, color, onAgregarCalificacion)
                }

                // ---------- Tarjetas de estudio ----------
                val paraHoy = materia.tarjetasParaHoy(hoy).size
                BloqueDetalle(Icons.Filled.Style, stringResource(R.string.tarjetas_titulo), color) {
                    if (materia.tarjetas.isEmpty()) TextoVacio(stringResource(R.string.tarjetas_vacio))
                    materia.tarjetas.forEach { tarjeta ->
                        key(tarjeta.id) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(start = 14.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                                    Text(tarjeta.pregunta, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        stringResource(R.string.tarjeta_caja, tarjeta.caja),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                MenuOpciones(
                                    listOf(
                                        OpcionMenu(stringResource(R.string.editar), Icons.Filled.Edit) { onEditarTarjeta(tarjeta) },
                                        OpcionMenu(stringResource(R.string.eliminar), Icons.Filled.Delete, destructiva = true) { onEliminarTarjeta(tarjeta) }
                                    ),
                                    descripcion = stringResource(R.string.opciones_de, tarjeta.pregunta)
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonBloque(stringResource(R.string.tarjeta_agregar), Icons.Filled.Add, color, onAgregarTarjeta, Modifier.weight(1f))
                        if (paraHoy > 0) {
                            BotonBloque(
                                "${stringResource(R.string.tarjetas_repasar)} ($paraHoy)",
                                Icons.Filled.PlayArrow, color, onRepasar, Modifier.weight(1f)
                            )
                        }
                    }
                }

                BloqueDetalle(Icons.Filled.Folder, stringResource(R.string.archivos_cantidad, materia.archivos.size), color) {
                    if (materia.archivos.isEmpty()) TextoVacio(stringResource(R.string.sin_archivos))
                    ListaArchivos(materia.archivos, color, onAbrirArchivo, onEliminarArchivo)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonBloque(stringResource(R.string.adjuntar_archivo), Icons.Filled.AttachFile, color, onAdjuntarArchivo, Modifier.weight(1f))
                        BotonBloque(stringResource(R.string.foto_sacar), Icons.Filled.PhotoCamera, color, onSacarFoto, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// Bloque dentro de la materia expandida (con el fondo de la pantalla, para
// que se distinga de la tarjeta): un título con su ícono en un círculo del
// color de la materia y el contenido debajo.
@Composable
private fun BloqueDetalle(icono: ImageVector, titulo: String, color: Color, contenido: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 2.dp)) {
                IconoCircular(icono, colorAcentoTexto(color), tamano = 32.dp, fondo = color.copy(alpha = 0.16f))
                Spacer(modifier = Modifier.width(10.dp))
                Text(titulo, style = MaterialTheme.typography.titleSmall)
            }
            contenido()
        }
    }
}

// Acción de un bloque ("Agregar tarea", "Adjuntar archivo"): un bloque
// tenue del color de la materia, de todo el ancho (o del que le den).
@Composable
private fun BotonBloque(
    texto: String,
    icono: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    // El texto usa el color de la materia ajustado para que se lea bien.
    val colorTexto = colorAcentoTexto(color)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.14f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = colorTexto, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.labelLarge, color = colorTexto)
    }
}

@Composable
private fun TextoVacio(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp)
    )
}

// Una tarea con checkbox, título (tachado si está hecha), prioridad, fecha
// límite (en rojo si está vencida) y su menú de opciones. Debajo, sus pasos
// (subtareas): si llega onAlternarSubtarea se pueden marcar. La usa también
// el buscador.
@Composable
fun FilaTarea(
    tarea: Tarea,
    hoy: LocalDate,
    onAlternar: () -> Unit,
    opciones: List<OpcionMenu>,
    subtitulo: String? = null,
    onAlternarSubtarea: ((Subtarea) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onAlternar),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = tarea.completada, onCheckedChange = { onAlternar() })
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tarea.titulo,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (tarea.completada) TextDecoration.LineThrough else null,
                    color = if (tarea.completada) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (subtitulo != null) {
                    Text(text = subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val conAviso = !tarea.completada && tarea.avisoDiasAntes != null && tarea.fechaLimite != null
                if (!tarea.completada && tarea.prioridad != Prioridad.MEDIA || tarea.subtareas.isNotEmpty() || conAviso) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (!tarea.completada) EtiquetaPrioridad(tarea.prioridad)
                        if (conAviso) {
                            Icon(
                                Icons.Filled.NotificationsActive,
                                contentDescription = stringResource(R.string.aviso_con_recordatorio),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (tarea.subtareas.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.subtareas_progreso, tarea.subtareasHechas, tarea.subtareas.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            val fecha = tarea.fechaLimite
            if (fecha != null) EtiquetaFecha(textoFecha(fecha, hoy), vencida = tarea.estaVencida(hoy))
            MenuOpciones(opciones, descripcion = stringResource(R.string.opciones_de, tarea.titulo))
        }
        // Los pasos, con sangría, debajo del título.
        if (onAlternarSubtarea != null && !tarea.completada) {
            tarea.subtareas.forEach { paso ->
                FilaSubtarea(paso) { onAlternarSubtarea(paso) }
            }
        }
    }
}

// Un paso de una tarea: casilla chica y el texto (tachado si está hecho).
@Composable
private fun FilaSubtarea(subtarea: Subtarea, onAlternar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .toggleable(value = subtarea.completada, role = Role.Checkbox, onValueChange = { onAlternar() })
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (subtarea.completada) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (subtarea.completada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = subtarea.titulo,
            style = MaterialTheme.typography.bodySmall,
            textDecoration = if (subtarea.completada) TextDecoration.LineThrough else null,
            color = if (subtarea.completada) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}
