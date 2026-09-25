package com.example.estudia.ui.pantallas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Tarea
import com.example.estudia.modelo.Usuario
import com.example.estudia.ui.componentes.DialogoConfirmacion
import com.example.estudia.ui.componentes.DialogoNuevaMateria
import com.example.estudia.ui.componentes.DialogoNuevaTarea
import com.example.estudia.ui.componentes.TarjetaMateria

@Composable
fun MateriasScreen(
    usuario: Usuario,
    onAgregarMateria: (String) -> Unit,
    onAgregarTarea: (Materia, String, String) -> Unit,
    onAlternarTarea: (Materia, Tarea) -> Unit,
    onEditarMateria: (Materia, String) -> Unit,
    onArchivarMateria: (Materia) -> Unit,
    onEliminarMateria: (Materia) -> Unit
) {
    var tabSeleccionada by remember { mutableStateOf(0) }
    var mostrarDialogo by remember { mutableStateOf(false) }
    var materiaParaNuevaTarea by remember { mutableStateOf<Materia?>(null) }
    var materiaParaEditar by remember { mutableStateOf<Materia?>(null) }
    var materiaParaEliminar by remember { mutableStateOf<Materia?>(null) }

    val materiasFiltradas: List<Materia> = usuario.materias.filter {
        it.archivada == (tabSeleccionada == 1)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(text = "Mis Materias", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { tabSeleccionada = 0 }) {
                    Text(if (tabSeleccionada == 0) "✓ Activas" else "Activas")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { tabSeleccionada = 1 }) {
                    Text(if (tabSeleccionada == 1) "✓ Archivadas" else "Archivadas")
                }
            }

            if (materiasFiltradas.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (tabSeleccionada == 1) "No hay materias archivadas" else "No hay materias activas",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(materiasFiltradas, key = { it.id }) { materia ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { alto -> alto / 4 })
                    ) {
                        TarjetaMateria(
                            materia = materia,
                            onAlternarTarea = { tarea -> onAlternarTarea(materia, tarea) },
                            onAgregarTarea = { materiaParaNuevaTarea = materia },
                            onEditar = { materiaParaEditar = materia },
                            onArchivar = { onArchivarMateria(materia) },
                            onEliminar = { materiaParaEliminar = materia }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { mostrarDialogo = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
    }

    if (mostrarDialogo) {
        DialogoNuevaMateria(
            nombreYaExiste = { nombreNuevo ->
                usuario.materias.any { materia ->
                    materia.nombre.equals(nombreNuevo, ignoreCase = true)
                }
            },
            onConfirmar = { nombreNuevo ->
                onAgregarMateria(nombreNuevo)
                mostrarDialogo = false
            },
            onCancelar = { mostrarDialogo = false }
        )
    }

    val materiaTarea = materiaParaNuevaTarea
    if (materiaTarea != null) {
        DialogoNuevaTarea(
            onConfirmar = { titulo, fechaLimite ->
                onAgregarTarea(materiaTarea, titulo, fechaLimite)
                materiaParaNuevaTarea = null
            },
            onCancelar = { materiaParaNuevaTarea = null }
        )
    }

    val materiaEditar = materiaParaEditar
    if (materiaEditar != null) {
        DialogoNuevaMateria(
            nombreYaExiste = { nombreNuevo ->
                usuario.materias.any { materia ->
                    materia.id != materiaEditar.id && materia.nombre.equals(nombreNuevo, ignoreCase = true)
                }
            },
            onConfirmar = { nombreNuevo ->
                onEditarMateria(materiaEditar, nombreNuevo)
                materiaParaEditar = null
            },
            onCancelar = { materiaParaEditar = null },
            nombreInicial = materiaEditar.nombre,
            titulo = "Editar materia",
            textoConfirmar = "Guardar"
        )
    }

    val materiaEliminar = materiaParaEliminar
    if (materiaEliminar != null) {
        DialogoConfirmacion(
            mensaje = "¿Eliminar \"${materiaEliminar.nombre}\"? Se van a borrar también sus tareas, eventos y notas.",
            textoConfirmar = "Eliminar",
            onConfirmar = {
                onEliminarMateria(materiaEliminar)
                materiaParaEliminar = null
            },
            onCancelar = { materiaParaEliminar = null }
        )
    }
}
