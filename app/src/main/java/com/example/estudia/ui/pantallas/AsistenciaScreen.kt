package com.example.estudia.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import com.example.estudia.R
import com.example.estudia.ui.componentes.BloqueColor
import com.example.estudia.ui.componentes.superficieVidrio
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.PantallaDetalle
import com.example.estudia.ui.componentes.TituloLista
import com.example.estudia.ui.componentes.colorAcentoTexto
import com.example.estudia.ui.componentes.progresoAnimado
import com.example.estudia.ui.componentes.textoFecha
import com.example.estudia.ui.theme.BloqueNaranja
import com.example.estudia.ui.theme.BloqueVerde
import com.example.estudia.util.aTexto
import com.example.estudia.viewmodel.AsistenciaMateria
import com.example.estudia.viewmodel.AsistenciaViewModel
import com.example.estudia.viewmodel.ClaseParaMarcar

private val RojoAusente = Color(0xFFE74C3C)

// Pantalla "Asistencia": el % de cada materia con cuántas faltas le quedan
// y, abajo, las clases de las últimas dos semanas para marcar presente o
// ausente. Se abre desde el Calendario.
@Composable
fun AsistenciaScreen(viewModel: AsistenciaViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    PantallaDetalle(
        titulo = stringResource(R.string.asistencia_titulo),
        subtitulo = stringResource(R.string.asistencia_subtitulo),
        onVolver = onVolver
    ) {
        if (!estado.cargando && estado.materias.isEmpty()) {
            item {
                EstadoVacio(Icons.Outlined.EventBusy, stringResource(R.string.asistencia_vacio), stringResource(R.string.asistencia_vacio_sugerencia))
            }
            return@PantallaDetalle
        }
        items(estado.materias, key = { "m${it.materiaId}" }) { TarjetaAsistencia(it) }

        item {
            TituloLista(
                stringResource(R.string.asistencia_marcar),
                if (estado.sinMarcar > 0) pluralStringResource(R.plurals.asistencia_sin_marcar, estado.sinMarcar, estado.sinMarcar) else null
            )
        }
        if (estado.clases.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.asistencia_sin_clases_recientes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        items(estado.clases, key = { "c${it.eventoId}_${it.fecha}" }) { clase ->
            FilaClase(clase, textoFecha(clase.fecha, estado.hoy), onMarcar = { viewModel.marcar(clase, it) })
        }
    }
}

// % de asistencia de una materia con una barra que marca el mínimo.
@Composable
private fun TarjetaAsistencia(item: AsistenciaMateria) {
    val color = Color(item.color)
    val resumen = item.resumen
    val porcentaje = resumen.porcentaje
    val colorEstado = when {
        porcentaje == null -> MaterialTheme.colorScheme.onSurfaceVariant
        resumen.debajoDelMinimo -> RojoAusente
        resumen.enRiesgo -> BloqueNaranja
        else -> BloqueVerde
    }
    BloqueColor(color = color, modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = porcentaje?.let { "$it%" } ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorAcentoTexto(color)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Barra del % con una rayita en el mínimo que pide la materia.
            val avance = progresoAnimado((porcentaje ?: 0) / 100f)
            Box(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                LinearProgressIndicator(
                    progress = { avance },
                    color = colorEstado,
                    trackColor = color.copy(alpha = 0.18f),
                    drawStopIndicator = {},
                    gapSize = 0.dp,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(resumen.minimo / 100f)
                        .wrapContentWidth(Alignment.End)
                        .width(2.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    porcentaje == null -> stringResource(R.string.asistencia_sin_registros)
                    resumen.minimo <= 0 -> stringResource(R.string.asistencia_sin_minimo)
                    resumen.debajoDelMinimo -> stringResource(R.string.asistencia_debajo, resumen.minimo)
                    resumen.faltasDisponibles == 0 -> stringResource(R.string.asistencia_limite)
                    else -> pluralStringResource(R.plurals.asistencia_faltas_disponibles, resumen.faltasDisponibles, resumen.faltasDisponibles)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colorEstado
            )
            Text(
                text = buildString {
                    append(stringResource(R.string.asistencia_conteo, resumen.presentes, resumen.ausentes))
                    append(" · ").append(stringResource(R.string.asistencia_pide, resumen.minimo))
                    resumen.clasesTotales?.let { append(" · ").append(stringResource(R.string.asistencia_total, it)) }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Una clase con sus dos botones: presente (verde) y ausente (rojo).
@Composable
private fun FilaClase(clase: ClaseParaMarcar, fecha: String, onMarcar: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(RoundedCornerShape(22.dp), borde = false)
            .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(clase.colorMateria)))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(clase.nombreMateria, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "$fecha · ${clase.hora.aTexto()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        BotonMarca(Icons.Outlined.Check, stringResource(R.string.asistencia_presente), BloqueVerde, clase.presente == true) { onMarcar(true) }
        Spacer(modifier = Modifier.width(6.dp))
        BotonMarca(Icons.Outlined.Close, stringResource(R.string.asistencia_ausente), RojoAusente, clase.presente == false) { onMarcar(false) }
    }
}

@Composable
private fun BotonMarca(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    descripcion: String,
    color: Color,
    elegido: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (elegido) color else color.copy(alpha = 0.12f))
            .clickable(role = Role.RadioButton, onClickLabel = descripcion, onClick = onClick)
            .semantics { selected = elegido }
    ) {
        Icon(icono, contentDescription = descripcion, tint = if (elegido) Color.White else color)
    }
}
