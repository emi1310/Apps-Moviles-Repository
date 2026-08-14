package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.Usuario
import com.example.estudia.ui.componentes.TarjetaEventoResumen
import com.example.estudia.ui.componentes.TarjetaTareaPendiente

// Pantalla "Inicio": junta información de todas las materias del usuario
// en un solo resumen — tareas pendientes, próximos eventos, una nota
// destacada y las horas de estudio semanales.
@Composable
fun InicioScreen(usuario: Usuario) {

    // Junta las tareas pendientes de TODAS las materias en una sola lista,
    // guardando también el nombre y color de la materia a la que pertenecen
    // (porque Tarea por sí sola no sabe de qué materia es).
    val tareasPendientes = usuario.materias.flatMap { materia ->
        materia.tareas
            .filter { !it.completada }
            .map { tarea -> Triple(tarea.titulo, materia.nombre, materia.color to tarea.fechaLimite) }
    }

    // Junta los eventos de tipo EXAMEN o ENTREGA de todas las materias
    // (dejamos afuera las CLASE y BLOQUE_ESTUDIO para no saturar esta sección).
    val proximosEventos = usuario.materias.flatMap { materia ->
        materia.eventos.filter { it.tipo == TipoEvento.EXAMEN || it.tipo == TipoEvento.ENTREGA }
    }

    // Busca la primera nota marcada como favorita, de cualquier materia.
    val notaDestacada = usuario.materias.flatMap { it.notas }.firstOrNull { it.esFavorita }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "¡Hola, ${usuario.nombre.substringBefore(" ")}!", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Vamos por un día productivo", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // ---------- Tareas pendientes ----------
        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Tareas pendientes", style = MaterialTheme.typography.titleMedium)
                    Text(text = "${tareasPendientes.size} pendientes", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (tareasPendientes.isEmpty()) {
                    Text(text = "No tenés tareas pendientes", style = MaterialTheme.typography.bodySmall)
                } else {
                    tareasPendientes.take(4).forEach { (titulo, nombreMateria, colorYFecha) ->
                        TarjetaTareaPendiente(
                            titulo = titulo,
                            nombreMateria = nombreMateria,
                            fechaLimite = colorYFecha.second,
                            colorMateria = colorYFecha.first
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------- Próximos eventos ----------
        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "Próximos eventos", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (proximosEventos.isEmpty()) {
                    Text(text = "No hay eventos próximos", style = MaterialTheme.typography.bodySmall)
                } else {
                    proximosEventos.take(3).forEach { evento ->
                        TarjetaEventoResumen(titulo = evento.titulo, fecha = evento.fecha)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------- Nota destacada ----------
        if (notaDestacada != null) {
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Nota destacada", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = notaDestacada.titulo, style = MaterialTheme.typography.bodyLarge)
                    Text(text = notaDestacada.contenido, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ---------- Horas de estudio ----------
        Text(
            text = "Horas de estudio esta semana: ${usuario.calcularHorasEstudioSemanal()}h",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}