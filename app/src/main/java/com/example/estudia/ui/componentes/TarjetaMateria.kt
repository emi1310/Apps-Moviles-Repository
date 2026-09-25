package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Tarea

@Composable
fun TarjetaMateria(
    materia: Materia,
    onAlternarTarea: (Tarea) -> Unit,
    onAgregarTarea: () -> Unit,
    onEditar: () -> Unit,
    onArchivar: () -> Unit,
    onEliminar: () -> Unit
) {
    var expandida by remember { mutableStateOf(false) }
    var menuAbierto by remember { mutableStateOf(false) }

    var pendientes = 0
    for (tarea in materia.tareas) {
        if (!tarea.completada) {
            pendientes++
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandida = !expandida },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(materia.color)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = materia.nombre, style = MaterialTheme.typography.titleMedium)

                    if (pendientes > 0) {
                        Text(text = "$pendientes tareas pendientes", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(text = "No hay tareas pendientes", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(text = "Progreso: ${materia.calcularProgreso()}%")
                }

                Box {
                    IconButton(onClick = { menuAbierto = true }) {
                        Text("⋮", style = MaterialTheme.typography.titleLarge)
                    }
                    DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            onClick = {
                                menuAbierto = false
                                onEditar()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (materia.archivada) "Desarchivar" else "Archivar") },
                            onClick = {
                                menuAbierto = false
                                onArchivar()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            onClick = {
                                menuAbierto = false
                                onEliminar()
                            }
                        )
                    }
                }

                Text(if (expandida) "▲" else "▼", style = MaterialTheme.typography.bodyMedium)
            }

            if (expandida) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(6.dp))

                if (materia.tareas.isEmpty()) {
                    Text(text = "Todavía no hay tareas cargadas", style = MaterialTheme.typography.bodySmall)
                } else {
                    for (tarea in materia.tareas) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAlternarTarea(tarea) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = tarea.completada, onCheckedChange = { onAlternarTarea(tarea) })
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = tarea.titulo, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(text = tarea.fechaLimite, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(onClick = onAgregarTarea, modifier = Modifier.fillMaxWidth()) {
                    Text("+ Agregar tarea")
                }
            }
        }
    }
}
