package com.example.estudia.ui.pantallas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.ui.componentes.Deslizable
import com.example.estudia.ui.componentes.GraficoBarrasProgreso
import com.example.estudia.ui.componentes.GraficoTortaHoras
import com.example.estudia.ui.componentes.LocalEspacioDock
import com.example.estudia.ui.componentes.progresoAnimado
import com.example.estudia.ui.componentes.TarjetaEventoResumen
import com.example.estudia.ui.componentes.TarjetaSeccion
import com.example.estudia.ui.componentes.TarjetaTareaPendiente
import com.example.estudia.ui.theme.AmarilloFavorita
import com.example.estudia.ui.theme.DegradadoFin
import com.example.estudia.ui.theme.DegradadoInicio
import com.example.estudia.ui.theme.LocalDegradado
import com.example.estudia.ui.theme.NaranjaRacha
import com.example.estudia.viewmodel.InicioUiState
import com.example.estudia.viewmodel.InicioViewModel
import com.example.estudia.viewmodel.TareaConMateria

// Pantalla "Inicio": arriba el saludo con la racha y el avatar (abre el
// Perfil); después las tareas pendientes, que son lo principal, y debajo
// los próximos exámenes, una nota destacada y las horas de estudio.
// Solo dibuja el UiState: los cálculos los hace InicioViewModel.
@Composable
fun InicioScreen(viewModel: InicioViewModel, onAbrirPerfil: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    if (estado.cargando) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = LocalEspacioDock.current + 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EncabezadoInicio(estado, onAbrirPerfil)

        TarjetaTareas(estado, onAlternar = viewModel::alternarTarea)

        // ---------- Próximos exámenes y entregas ----------
        TarjetaSeccion(
            titulo = stringResource(R.string.proximos_eventos),
            icono = Icons.Filled.Event,
            color = MaterialTheme.colorScheme.tertiary
        ) {
            if (estado.proximosEventos.isEmpty()) {
                TextoSecundario(stringResource(R.string.sin_eventos_proximos))
            }
            estado.proximosEventos.take(3).forEach { TarjetaEventoResumen(it, estado.hoy) }
        }

        // ---------- Nota destacada ----------
        val nota = estado.notaDestacada
        if (nota != null) {
            TarjetaSeccion(
                titulo = stringResource(R.string.nota_destacada),
                icono = Icons.Filled.Star,
                color = AmarilloFavorita
            ) {
                Text(text = nota.titulo, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = nota.contenido,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
        }

        if (estado.materiasActivas.isNotEmpty()) {
            TarjetaSeccion(
                titulo = stringResource(R.string.progreso_por_materia),
                icono = Icons.Filled.BarChart,
                color = MaterialTheme.colorScheme.secondary
            ) {
                GraficoBarrasProgreso(materias = estado.materiasActivas)
            }
            TarjetaSeccion(
                titulo = stringResource(R.string.horas_por_materia),
                icono = Icons.Filled.DonutLarge,
                color = MaterialTheme.colorScheme.primary,
                detalle = stringResource(R.string.horas_corto, estado.horasEstudio)
            ) {
                GraficoTortaHoras(datos = estado.minutosPorMateria)
            }
        }
    }
}

// Encabezado: fecha y saludo a la izquierda; a la derecha la racha (un
// fueguito con los días seguidos) y el avatar, que abre el Perfil.
@Composable
private fun EncabezadoInicio(estado: InicioUiState, onAbrirPerfil: () -> Unit) {
    val dias = stringArrayResource(R.array.dias_semana_largo)
    val meses = stringArrayResource(R.array.meses)
    val hoy = estado.hoy
    val fecha = stringResource(
        R.string.fecha_larga,
        dias[hoy.dayOfWeek.value - 1],
        hoy.dayOfMonth,
        meses[hoy.monthValue - 1].lowercase()
    )
    val descripcionRacha = pluralStringResource(R.plurals.inicio_racha_descripcion, estado.rachaDias, estado.rachaDias)

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = fecha, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = stringResource(R.string.saludo, estado.nombre), style = MaterialTheme.typography.headlineMedium)
        }
        // Racha: el fueguito se enciende si hoy ya estudió; si no, queda gris
        // (la racha sigue, pero hay que estudiar hoy para no cortarla).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .semantics(mergeDescendants = true) { contentDescription = descripcionRacha }
                .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Icon(
                Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = if (estado.estudioHoy) NaranjaRacha else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = estado.rachaDias.toString(), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(LocalDegradado.current)
                .clickable(onClickLabel = stringResource(R.string.inicio_ir_perfil), role = Role.Button, onClick = onAbrirPerfil),
            contentAlignment = Alignment.Center
        ) {
            Text(text = estado.nombre.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
    }
}

// Tarjeta principal: las tareas. Arriba los números (completadas, para hoy y
// vencidas) con un anillo del avance; debajo, las tareas pendientes.
@Composable
private fun TarjetaTareas(estado: InicioUiState, onAlternar: (TareaConMateria) -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = stringResource(R.string.inicio_tus_tareas), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.inicio_tus_tareas_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f)) {
                    DatoTareas(
                        Icons.Filled.CheckCircle, MaterialTheme.colorScheme.secondary, stringResource(R.string.inicio_completadas),
                        stringResource(R.string.inicio_completadas_valor, estado.tareasCompletadas, estado.totalTareas)
                    )
                    DatoTareas(Icons.Filled.Today, MaterialTheme.colorScheme.primary, stringResource(R.string.inicio_para_hoy), estado.tareasParaHoy.toString())
                    DatoTareas(Icons.Filled.Warning, MaterialTheme.colorScheme.error, stringResource(R.string.inicio_vencidas), estado.tareasVencidas.toString())
                }
                AnilloTareas(estado.progresoTareas, estado.tareasPendientes.size)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))
            if (estado.tareasPendientes.isEmpty()) {
                TextoSecundario(stringResource(R.string.sin_tareas_pendientes_feliz))
            }
            estado.tareasPendientes.take(5).forEach { item ->
                // Deslizar hacia la derecha la completa (y sale de la lista).
                key(item.tarea.id) {
                    Deslizable(
                        onCompletar = { onAlternar(item) },
                        forma = RoundedCornerShape(12.dp),
                        fondoContenido = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        TarjetaTareaPendiente(item, estado.hoy, onAlternar = { onAlternar(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DatoTareas(icono: ImageVector, color: Color, etiqueta: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = valor, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// Anillo de las tareas: una pista gris y lo completado en degradado
// violeta, con la cantidad de pendientes en el centro.
@Composable
private fun AnilloTareas(progreso: Float, pendientes: Int) {
    val avance = progresoAnimado(progreso.coerceIn(0f, 1f))
    val pista = MaterialTheme.colorScheme.surfaceVariant
    val violeta = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.size(148.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grosor = 14.dp.toPx()
            val trazo = Stroke(width = grosor, cap = StrokeCap.Round)
            val tamano = Size(size.width - grosor, size.height - grosor)
            val esquina = Offset(grosor / 2, grosor / 2)
            drawArc(pista, 0f, 360f, false, esquina, tamano, style = trazo)
            if (avance > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(DegradadoFin, violeta, DegradadoInicio, DegradadoFin)),
                    startAngle = -90f,
                    sweepAngle = 360f * avance,
                    useCenter = false,
                    topLeft = esquina,
                    size = tamano,
                    style = trazo
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = pendientes.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = pluralStringResource(R.plurals.inicio_pendientes, pendientes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TextoSecundario(texto: String) {
    Text(text = texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
