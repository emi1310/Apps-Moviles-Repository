package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Tarea
import java.time.LocalDate

// Tarjeta que muestra el resumen de una materia: nombre, tareas pendientes
// y progreso. Al tocarla se expande y muestra las tareas (con checkbox y
// menú para editarlas o eliminarlas) y los archivos adjuntos.
@Composable
fun TarjetaMateria(
    materia: Materia,
    onAlternarTarea: (Tarea) -> Unit,
    onAgregarTarea: () -> Unit,
    onEditarTarea: (Tarea) -> Unit,
    onEliminarTarea: (Tarea) -> Unit,
    onAdjuntarArchivo: () -> Unit,
    onAbrirArchivo: (Archivo) -> Unit,
    onEliminarArchivo: (Archivo) -> Unit,
    onEditar: () -> Unit,
    onArchivar: () -> Unit,
    onEliminar: () -> Unit
) {
    var expandida by rememberSaveable { mutableStateOf(false) }
    val color = Color(materia.color)
    val pendientes = materia.tareas.count { !it.completada }
    val hoy = LocalDate.now()

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expandida = !expandida },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = materia.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (pendientes > 0) pluralStringResource(R.plurals.tareas_pendientes, pendientes, pendientes)
                        else stringResource(R.string.sin_tareas_pendientes),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { materia.calcularProgreso() / 100f },
                        color = color,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.progreso, materia.calcularProgreso()),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                MenuOpciones(
                    listOf(
                        OpcionMenu(stringResource(R.string.editar), Icons.Filled.Edit, onEditar),
                        OpcionMenu(
                            stringResource(if (materia.archivada) R.string.desarchivar else R.string.archivar),
                            if (materia.archivada) Icons.Filled.Unarchive else Icons.Filled.Archive,
                            onArchivar
                        ),
                        OpcionMenu(stringResource(R.string.eliminar), Icons.Filled.Delete, onEliminar)
                    ),
                    descripcion = stringResource(R.string.opciones_de, materia.nombre)
                )
                Icon(
                    imageVector = if (expandida) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(if (expandida) R.string.contraer else R.string.expandir)
                )
            }

            if (expandida) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))

                if (materia.tareas.isEmpty()) {
                    Text(text = stringResource(R.string.sin_tareas), style = MaterialTheme.typography.bodySmall)
                }
                materia.tareas.forEach { tarea ->
                    FilaTarea(
                        tarea = tarea,
                        hoy = hoy,
                        onAlternar = { onAlternarTarea(tarea) },
                        opciones = listOf(
                            OpcionMenu(stringResource(R.string.editar), Icons.Filled.Edit) { onEditarTarea(tarea) },
                            OpcionMenu(stringResource(R.string.eliminar), Icons.Filled.Delete) { onEliminarTarea(tarea) }
                        )
                    )
                }
                TextButton(onClick = onAgregarTarea) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.agregar_tarea))
                }

                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.archivos_cantidad, materia.archivos.size),
                    style = MaterialTheme.typography.titleSmall
                )
                if (materia.archivos.isEmpty()) {
                    Text(text = stringResource(R.string.sin_archivos), style = MaterialTheme.typography.bodySmall)
                }
                ListaArchivos(materia.archivos, color, onAbrirArchivo, onEliminarArchivo)
                TextButton(onClick = onAdjuntarArchivo) {
                    Icon(Icons.Filled.AttachFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.adjuntar_archivo))
                }
            }
        }
    }
}

// Una tarea con checkbox, título, fecha límite (en rojo si está vencida)
// y su menú de opciones. La usa también el buscador de tareas.
@Composable
fun FilaTarea(
    tarea: Tarea,
    hoy: LocalDate,
    onAlternar: () -> Unit,
    opciones: List<OpcionMenu>,
    subtitulo: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAlternar),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = tarea.completada, onCheckedChange = { onAlternar() })
        Column(modifier = Modifier.weight(1f)) {
            Text(text = tarea.titulo, style = MaterialTheme.typography.bodyMedium)
            if (subtitulo != null) Text(text = subtitulo, style = MaterialTheme.typography.bodySmall)
        }
        val fecha = tarea.fechaLimite
        if (fecha != null) {
            Text(
                text = textoFecha(fecha, hoy),
                style = MaterialTheme.typography.bodySmall,
                color = if (tarea.estaVencida(hoy)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MenuOpciones(opciones, descripcion = stringResource(R.string.opciones_de, tarea.titulo))
    }
}
