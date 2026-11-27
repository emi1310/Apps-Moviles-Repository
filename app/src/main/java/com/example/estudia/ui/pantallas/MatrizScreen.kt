package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.LowPriority
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.modelo.Prioridad
import com.example.estudia.ui.componentes.BloqueColor
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.FilaTarea
import com.example.estudia.ui.componentes.OpcionMenu
import com.example.estudia.ui.componentes.PantallaDetalle
import com.example.estudia.ui.componentes.colorAcentoTexto
import com.example.estudia.viewmodel.Cuadrante
import com.example.estudia.viewmodel.MatrizViewModel
import com.example.estudia.viewmodel.TareaConMateria

private val RojoUrgente = Color(0xFFE74C3C)

// Pantalla "Prioridades": las tareas pendientes repartidas en cuatro
// bloques según si son urgentes (vencen en 2 días o menos) e importantes
// (prioridad alta). Tocar una la completa; el menú la pasa a importante o
// no importante. Se abre desde Materias.
@Composable
fun MatrizScreen(viewModel: MatrizViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    PantallaDetalle(
        titulo = stringResource(R.string.matriz_titulo),
        subtitulo = stringResource(R.string.matriz_subtitulo),
        onVolver = onVolver
    ) {
        if (!estado.cargando && estado.total == 0) {
            item { EstadoVacio(Icons.Outlined.TaskAlt, stringResource(R.string.matriz_vacia), stringResource(R.string.matriz_vacia_sugerencia)) }
            return@PantallaDetalle
        }
        Cuadrante.entries.forEach { cuadrante ->
            item(key = cuadrante.name) {
                BloqueCuadrante(
                    cuadrante = cuadrante,
                    tareas = estado.cuadrantes[cuadrante].orEmpty(),
                    hoy = estado.hoy,
                    onCompletar = viewModel::completar,
                    onImportante = viewModel::alternarImportante
                )
            }
        }
    }
}

private data class DatosCuadrante(val titulo: Int, val detalle: Int, val icono: ImageVector, val color: Color)

@Composable
private fun datosDe(cuadrante: Cuadrante): DatosCuadrante = when (cuadrante) {
    Cuadrante.HACER_YA -> DatosCuadrante(R.string.matriz_hacer_ya, R.string.matriz_hacer_ya_detalle, Icons.Outlined.Bolt, RojoUrgente)
    Cuadrante.PLANIFICAR -> DatosCuadrante(R.string.matriz_planificar, R.string.matriz_planificar_detalle, Icons.Outlined.CalendarMonth, MaterialTheme.colorScheme.primary)
    Cuadrante.SACAR_RAPIDO -> DatosCuadrante(R.string.matriz_rapido, R.string.matriz_rapido_detalle, Icons.Outlined.HourglassBottom, Color(0xFFFDA43C))
    Cuadrante.DESPUES -> DatosCuadrante(R.string.matriz_despues, R.string.matriz_despues_detalle, Icons.Outlined.LowPriority, Color(0xFF00B894))
}

@Composable
private fun BloqueCuadrante(
    cuadrante: Cuadrante,
    tareas: List<TareaConMateria>,
    hoy: java.time.LocalDate,
    onCompletar: (TareaConMateria) -> Unit,
    onImportante: (TareaConMateria) -> Unit
) {
    val datos = datosDe(cuadrante)
    val acento = colorAcentoTexto(datos.color)
    BloqueColor(color = datos.color, modifier = Modifier.padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(datos.icono, contentDescription = null, tint = acento, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    stringResource(datos.titulo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).semantics { heading() }
                )
                Text(tareas.size.toString(), style = MaterialTheme.typography.titleMedium, color = acento, modifier = Modifier.padding(end = 12.dp))
            }
            Text(
                stringResource(datos.detalle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
            )
            if (tareas.isEmpty()) {
                Text(
                    stringResource(R.string.matriz_cuadrante_vacio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                tareas.forEach { item ->
                    val importante = item.tarea.prioridad == Prioridad.ALTA
                    FilaTarea(
                        tarea = item.tarea,
                        hoy = hoy,
                        subtitulo = item.nombreMateria,
                        onAlternar = { onCompletar(item) },
                        opciones = listOf(
                            OpcionMenu(
                                stringResource(if (importante) R.string.matriz_quitar_importante else R.string.matriz_marcar_importante),
                                Icons.Outlined.Flag
                            ) { onImportante(item) }
                        )
                    )
                }
            }
        }
    }
}
