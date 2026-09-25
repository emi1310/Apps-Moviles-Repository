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
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.Usuario
import com.example.estudia.ui.componentes.DialogoConfirmacion
import com.example.estudia.ui.componentes.DialogoEditarNota
import com.example.estudia.ui.componentes.DialogoNuevaNota
import com.example.estudia.ui.componentes.TarjetaNota
import com.example.estudia.util.parsearFechaAMilisegundos

private data class NotaConMateria(val materia: Materia, val nota: Nota)

@Composable
fun NotasScreen(
    usuario: Usuario,
    onAgregarNota: (Materia, String, String) -> Unit,
    onAlternarFavorita: (Materia, Nota) -> Unit,
    onEditarNota: (Materia, Nota, String, String) -> Unit,
    onEliminarNota: (Materia, Nota) -> Unit
) {
    var tabSeleccionada by remember { mutableStateOf(0) }
    var mostrarDialogoNuevaNota by remember { mutableStateOf(false) }
    var notaParaEditar by remember { mutableStateOf<NotaConMateria?>(null) }
    var notaParaEliminar by remember { mutableStateOf<NotaConMateria?>(null) }

    val todasLasNotas: List<NotaConMateria> = usuario.materias.flatMap { materia ->
        materia.notas.map { nota -> NotaConMateria(materia, nota) }
    }

    val notasFiltradas: List<NotaConMateria> = when (tabSeleccionada) {
        1 -> todasLasNotas.sortedByDescending { parsearFechaAMilisegundos(it.nota.fechaCreacion) }
        2 -> todasLasNotas.filter { it.nota.esFavorita }
        else -> todasLasNotas
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(text = "Mis Notas", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { tabSeleccionada = 0 }) { Text(if (tabSeleccionada == 0) "✓ Todas" else "Todas") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { tabSeleccionada = 1 }) { Text(if (tabSeleccionada == 1) "✓ Recientes" else "Recientes") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { tabSeleccionada = 2 }) { Text(if (tabSeleccionada == 2) "✓ Favoritas" else "Favoritas") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (notasFiltradas.isEmpty()) {
                Text(text = "No hay notas para mostrar", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(notasFiltradas, key = { it.nota.id * 1000 + it.materia.id }) { notaConMateria ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { visible = true }

                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { alto -> alto / 4 })
                        ) {
                            TarjetaNota(
                                titulo = notaConMateria.nota.titulo,
                                nombreMateria = notaConMateria.materia.nombre,
                                contenido = notaConMateria.nota.contenido,
                                fecha = notaConMateria.nota.fechaCreacion,
                                esFavorita = notaConMateria.nota.esFavorita,
                                alTocarEstrella = {
                                    onAlternarFavorita(notaConMateria.materia, notaConMateria.nota)
                                },
                                alEditar = {
                                    notaParaEditar = notaConMateria
                                },
                                alTocarEliminar = {
                                    notaParaEliminar = notaConMateria
                                }
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { mostrarDialogoNuevaNota = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
    }

    if (mostrarDialogoNuevaNota) {
        DialogoNuevaNota(
            materiasDisponibles = usuario.materias.filter { !it.archivada },
            onConfirmar = { materiaElegida, titulo, contenido ->
                onAgregarNota(materiaElegida, titulo, contenido)
                mostrarDialogoNuevaNota = false
            },
            onCancelar = { mostrarDialogoNuevaNota = false }
        )
    }

    val paraEditar = notaParaEditar
    if (paraEditar != null) {
        DialogoEditarNota(
            tituloInicial = paraEditar.nota.titulo,
            contenidoInicial = paraEditar.nota.contenido,
            onConfirmar = { titulo, contenido ->
                onEditarNota(paraEditar.materia, paraEditar.nota, titulo, contenido)
                notaParaEditar = null
            },
            onCancelar = { notaParaEditar = null }
        )
    }

    val paraEliminar = notaParaEliminar
    if (paraEliminar != null) {
        DialogoConfirmacion(
            mensaje = "¿Eliminar la nota \"${paraEliminar.nota.titulo}\"?",
            textoConfirmar = "Eliminar",
            onConfirmar = {
                onEliminarNota(paraEliminar.materia, paraEliminar.nota)
                notaParaEliminar = null
            },
            onCancelar = { notaParaEliminar = null }
        )
    }
}
