package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.ui.componentes.GraficoBarrasProgreso
import com.example.estudia.ui.componentes.GraficoTortaHoras
import com.example.estudia.ui.componentes.TarjetaEventoResumen
import com.example.estudia.ui.componentes.TarjetaTareaPendiente
import com.example.estudia.viewmodel.InicioViewModel

// Pantalla "Inicio": resumen de todas las materias — tareas pendientes,
// próximos exámenes y entregas, una nota destacada y las horas de estudio.
// Solo dibuja el UiState: los cálculos los hace InicioViewModel.
@Composable
fun InicioScreen(viewModel: InicioViewModel) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    if (estado.cargando) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = stringResource(R.string.saludo, estado.nombre), style = MaterialTheme.typography.headlineSmall)
        Text(text = stringResource(R.string.saludo_subtitulo), style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // ---------- Tareas pendientes ----------
        Seccion(
            titulo = stringResource(R.string.tareas_pendientes_titulo),
            detalle = pluralStringResource(R.plurals.tareas_pendientes, estado.tareasPendientes.size, estado.tareasPendientes.size)
        ) {
            if (estado.tareasPendientes.isEmpty()) {
                Text(text = stringResource(R.string.sin_tareas_pendientes), style = MaterialTheme.typography.bodySmall)
            }
            estado.tareasPendientes.take(5).forEach { item ->
                TarjetaTareaPendiente(item, estado.hoy, onAlternar = { viewModel.alternarTarea(item) })
            }
        }

        // ---------- Próximos exámenes y entregas ----------
        Seccion(titulo = stringResource(R.string.proximos_eventos)) {
            if (estado.proximosEventos.isEmpty()) {
                Text(text = stringResource(R.string.sin_eventos_proximos), style = MaterialTheme.typography.bodySmall)
            }
            estado.proximosEventos.take(3).forEach { TarjetaEventoResumen(it, estado.hoy) }
        }

        // ---------- Nota destacada ----------
        val nota = estado.notaDestacada
        if (nota != null) {
            Seccion(titulo = stringResource(R.string.nota_destacada)) {
                Text(text = nota.titulo, style = MaterialTheme.typography.bodyLarge)
                Text(text = nota.contenido, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
        }

        if (estado.materiasActivas.isNotEmpty()) {
            Seccion(titulo = stringResource(R.string.progreso_por_materia)) {
                GraficoBarrasProgreso(materias = estado.materiasActivas)
            }
            Seccion(titulo = stringResource(R.string.horas_por_materia)) {
                GraficoTortaHoras(datos = estado.minutosPorMateria)
            }
        }

        Text(
            text = stringResource(R.string.horas_planificadas, estado.horasEstudio),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// Tarjeta con título (y un detalle opcional a la derecha) para cada sección.
@Composable
private fun Seccion(titulo: String, detalle: String? = null, contenido: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = titulo, style = MaterialTheme.typography.titleMedium)
                if (detalle != null) Text(text = detalle, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            contenido()
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}
