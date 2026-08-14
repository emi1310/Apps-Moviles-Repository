package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.Usuario
import com.example.estudia.ui.componentes.TarjetaNota
import com.example.estudia.ui.componentes.DialogoNuevaNota
import com.example.estudia.util.obtenerFechaHoyComoTexto


// Guarda una Nota junto con la Materia dueña, porque Nota no sabe por
// sí sola a qué materia pertenece, y necesitamos la Materia real (no
// solo su nombre) para poder modificar su lista de notas al tocar la estrella.
private data class NotaConMateria(val materia: Materia, val nota: Nota)

// Pantalla "Mis Notas": lista de notas del usuario, separadas en
// pestañas Todas / Recientes / Favoritas, con opción de marcar favorita.
@Composable
fun NotasScreen(usuario: Usuario) {
    var tabSeleccionada by remember { mutableStateOf(0) } // 0=Todas, 1=Recientes, 2=Favoritas
    var mostrarDialogoNuevaNota by remember { mutableStateOf(false) }

    val todasLasNotas: List<NotaConMateria> = usuario.materias.flatMap { materia ->
        materia.notas.map { nota -> NotaConMateria(materia, nota) }
    }

    // "Recientes" es una simplificación: como fechaCreacion es texto libre
    // (ej: "4 de junio de 2026"), no es un dato fácil de ordenar cronológicamente
    // sin cambiar cómo se guarda la fecha. Por ahora, "Recientes" muestra las
    // últimas notas agregadas a la lista (orden de creación en memoria),
    // no la fecha real más reciente. Queda marcado como posible mejora futura.
    val notasFiltradas: List<NotaConMateria> = when (tabSeleccionada) {
        1 -> todasLasNotas.reversed()
        2 -> todasLasNotas.filter { it.nota.esFavorita }
        else -> todasLasNotas
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Mis Notas", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = { mostrarDialogoNuevaNota = true }) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(selectedTabIndex = tabSeleccionada) {
            Tab(selected = tabSeleccionada == 0, onClick = { tabSeleccionada = 0 }, text = { Text("Todas") })
            Tab(selected = tabSeleccionada == 1, onClick = { tabSeleccionada = 1 }, text = { Text("Recientes") })
            Tab(selected = tabSeleccionada == 2, onClick = { tabSeleccionada = 2 }, text = { Text("Favoritas") })
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (notasFiltradas.isEmpty()) {
            Text(text = "No hay notas para mostrar", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(notasFiltradas) { notaConMateria ->
                    TarjetaNota(
                        titulo = notaConMateria.nota.titulo,
                        nombreMateria = notaConMateria.materia.nombre,
                        contenido = notaConMateria.nota.contenido,
                        fecha = notaConMateria.nota.fechaCreacion,
                        esFavorita = notaConMateria.nota.esFavorita,
                        alTocarEstrella = {
                            // Busca la posición exacta de esta nota dentro de
                            // la lista de su materia, para poder reemplazarla
                            // por una copia actualizada (necesario para que
                            // Compose detecte el cambio y redibuje la pantalla).
                            val indice = notaConMateria.materia.notas.indexOf(notaConMateria.nota)
                            val copia = notaConMateria.nota.copy()
                            if (copia.esFavorita) copia.quitarFavorita() else copia.marcarFavorita()
                            notaConMateria.materia.notas[indice] = copia


                        }
                    )
                }
            }
        }
    }
    if (mostrarDialogoNuevaNota) {
        DialogoNuevaNota(
            materiasDisponibles = usuario.materias.filter { !it.archivada },
            onConfirmar = { materiaElegida, titulo, contenido ->
                materiaElegida.notas.add(
                    Nota(
                        id = System.currentTimeMillis().toInt(),
                        titulo = titulo,
                        contenido = contenido,
                        fechaCreacion = obtenerFechaHoyComoTexto()
                    )
                )
                mostrarDialogoNuevaNota = false
            },
            onCancelar = { mostrarDialogoNuevaNota = false }
        )
    }
}