package com.example.estudia.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.ui.componentes.colorAcentoTexto
import com.example.estudia.ui.componentes.superficieVidrio
import com.example.estudia.ui.componentes.fondoSuave
import com.example.estudia.ui.componentes.BloqueColor
import com.example.estudia.ui.componentes.CirculoBloque
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.textoHoras
import com.example.estudia.ui.theme.LocalDegradado
import com.example.estudia.util.aTexto
import com.example.estudia.viewmodel.ClaseHorario
import com.example.estudia.viewmodel.DiaHorario
import com.example.estudia.viewmodel.HorarioViewModel

// Pantalla "Horario semanal": las clases de cada día, de lunes a viernes
// (y sábado o domingo si hay). Se abre desde el Calendario. Solo dibuja:
// arma la grilla HorarioViewModel.
@Composable
fun HorarioScreen(viewModel: HorarioViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val nombresDias = stringArrayResource(R.array.dias_semana_largo)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.volver))
            }
            Column {
                Text(text = stringResource(R.string.horario_titulo), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(R.string.horario_subtitulo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!estado.cargando && estado.totalClases == 0) {
            EstadoVacio(Icons.Outlined.EventBusy, stringResource(R.string.horario_vacio), stringResource(R.string.horario_vacio_sugerencia))
            return@Column
        }

        // Resumen: cuántas clases y cuántas horas de cursada por semana.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(LocalDegradado.current)
                .padding(18.dp)
        ) {
            CirculoBloque(tamano = 44.dp) {
                Icon(Icons.Outlined.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = pluralStringResource(R.plurals.horario_clases, estado.totalClases, estado.totalClases),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.horario_por_semana, textoHoras(estado.minutosPorSemana)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        estado.dias.forEach { dia ->
            DiaDelHorario(dia, nombresDias[dia.dia.value - 1], esHoy = dia.dia == estado.hoy.dayOfWeek)
        }
    }
}

// Un día: su nombre (con "Hoy" si corresponde) y sus clases, o "Sin clases".
@Composable
private fun DiaDelHorario(dia: DiaHorario, nombre: String, esHoy: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
            Text(
                text = nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            if (esHoy) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.hoy),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }
        if (dia.clases.isEmpty()) {
            Text(
                text = stringResource(R.string.horario_sin_clases_dia),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .superficieVidrio(RoundedCornerShape(18.dp), borde = false)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
        dia.clases.forEach { BloqueClase(it) }
    }
}

// Una clase: bloque del color de la materia con el horario a la izquierda.
@Composable
private fun BloqueClase(clase: ClaseHorario) {
    val color = Color(clase.colorMateria)
    val acento = colorAcentoTexto(color)
    BloqueColor(color = color, modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(fondoSuave(color))
                    .padding(vertical = 10.dp)
            ) {
                Text(text = clase.inicio.aTexto(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = acento)
                Text(text = clase.fin.aTexto(), style = MaterialTheme.typography.bodySmall, color = acento.copy(alpha = 0.8f))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clase.nombreMateria,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = clase.titulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
