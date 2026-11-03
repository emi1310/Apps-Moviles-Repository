package com.example.estudia.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.modelo.SesionEstudio
import com.example.estudia.ui.componentes.Deslizable
import com.example.estudia.ui.componentes.GraficoBarrasProgreso
import com.example.estudia.ui.componentes.GraficoTortaHoras
import com.example.estudia.ui.componentes.progresoAnimado
import com.example.estudia.ui.componentes.TarjetaEventoResumen
import com.example.estudia.ui.componentes.TarjetaSeccion
import com.example.estudia.ui.componentes.TarjetaTareaPendiente
import com.example.estudia.ui.theme.AmarilloFavorita
import com.example.estudia.ui.theme.LocalDegradado
import com.example.estudia.ui.theme.NaranjaRacha
import com.example.estudia.viewmodel.InicioUiState
import com.example.estudia.viewmodel.InicioViewModel
import kotlinx.coroutines.delay

// Pantalla "Inicio": resumen de todas las materias — tareas pendientes,
// próximos exámenes y entregas, una nota destacada y las horas de estudio.
// Solo dibuja el UiState: los cálculos los hace InicioViewModel.
@Composable
fun InicioScreen(viewModel: InicioViewModel, onAbrirTemporizador: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val sesionEstudio by viewModel.sesionEstudio.collectAsStateWithLifecycle()

    if (estado.cargando) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EncabezadoInicio(estado, sesionEstudio, onAbrirTemporizador)

        // ---------- Tareas pendientes ----------
        TarjetaSeccion(
            titulo = stringResource(R.string.tareas_pendientes_titulo),
            icono = Icons.Filled.TaskAlt,
            color = MaterialTheme.colorScheme.primary,
            detalle = estado.tareasPendientes.size.toString()
        ) {
            if (estado.tareasPendientes.isEmpty()) {
                TextoSecundario(stringResource(R.string.sin_tareas_pendientes_feliz))
            }
            estado.tareasPendientes.take(5).forEach { item ->
                // Deslizar hacia la derecha la completa (y sale de la lista).
                key(item.tarea.id) {
                    Deslizable(
                        onCompletar = { viewModel.alternarTarea(item) },
                        forma = RoundedCornerShape(12.dp),
                        fondoContenido = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        TarjetaTareaPendiente(item, estado.hoy, onAlternar = { viewModel.alternarTarea(item) })
                    }
                }
            }
        }

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

// Encabezado con degradado: saludo, fecha de hoy y tres casillas: la meta
// semanal, la racha de días estudiando y el acceso al temporizador.
@Composable
private fun EncabezadoInicio(estado: InicioUiState, sesionEstudio: SesionEstudio?, onAbrirTemporizador: () -> Unit) {
    val dias = stringArrayResource(R.array.dias_semana_largo)
    val meses = stringArrayResource(R.array.meses)
    val hoy = estado.hoy
    val fecha = stringResource(
        R.string.fecha_larga,
        dias[hoy.dayOfWeek.value - 1],
        hoy.dayOfMonth,
        meses[hoy.monthValue - 1].lowercase()
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(LocalDegradado.current)
            .padding(20.dp)
    ) {
        Text(text = fecha, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
        Text(
            text = stringResource(R.string.saludo, estado.nombre),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Text(
            text = stringResource(R.string.saludo_subtitulo),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.height(18.dp))
        // IntrinsicSize.Min: las tres casillas quedan de la misma altura.
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(IntrinsicSize.Min)
        ) {
            CasillaMeta(estado, Modifier.weight(1f))
            CasillaRacha(estado, Modifier.weight(1f))
            CasillaTemporizador(sesionEstudio, onAbrirTemporizador, Modifier.weight(1f))
        }
    }
}

// Horas estudiadas esta semana. Con meta: "3/10h" y una barrita de avance.
@Composable
private fun CasillaMeta(estado: InicioUiState, modifier: Modifier) {
    val meta = estado.metaHorasSemanales?.takeIf { it > 0 }
    CasillaEncabezado(
        etiqueta = stringResource(if (meta != null) R.string.inicio_meta_semanal else R.string.inicio_esta_semana),
        modifier = modifier
    ) {
        Text(
            text = if (meta != null) stringResource(R.string.inicio_horas_de_meta, estado.horasEstaSemana, meta)
            else stringResource(R.string.horas_corto, estado.horasEstaSemana),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        val progreso = estado.progresoMeta
        if (progreso != null) {
            val avance = progresoAnimado(progreso)
            LinearProgressIndicator(
                progress = { avance },
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
                // Sin el hueco entre lo cumplido y lo que falta (Material 3 lo
                // agrega por defecto): en una barra tan chica parecen dos.
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .width(56.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

// Días seguidos estudiando. El fueguito se enciende si hoy ya estudió; si
// no, queda apagado (la racha sigue, pero hay que estudiar hoy para no cortarla).
@Composable
private fun CasillaRacha(estado: InicioUiState, modifier: Modifier) {
    CasillaEncabezado(
        etiqueta = pluralStringResource(R.plurals.inicio_dias_racha, estado.rachaDias),
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = if (estado.estudioHoy) NaranjaRacha else Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(text = estado.rachaDias.toString(), style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
    }
}

// Acceso al temporizador. Si hay una sesión en curso, abajo dice cuánto
// falta (se actualiza cada segundo) o que está en pausa.
@Composable
private fun CasillaTemporizador(sesion: SesionEstudio?, onAbrir: () -> Unit, modifier: Modifier) {
    val ahora by produceState(System.currentTimeMillis(), sesion) {
        while (sesion != null && !sesion.enPausa) {
            value = System.currentTimeMillis()
            delay(1_000)
        }
    }
    CasillaEncabezado(
        etiqueta = when {
            sesion == null -> stringResource(R.string.inicio_temporizador)
            sesion.enPausa -> stringResource(R.string.temporizador_en_pausa)
            else -> stringResource(R.string.inicio_temporizador_faltan, sesion.minutosRestantes(ahora))
        },
        modifier = modifier,
        onClick = onAbrir,
        descripcionClick = stringResource(R.string.temporizador_titulo)
    ) {
        Icon(Icons.Filled.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

// Casilla translúcida del encabezado: un dato arriba y su etiqueta abajo.
// Se lee como un solo elemento con TalkBack (mergeDescendants).
@Composable
private fun CasillaEncabezado(
    etiqueta: String,
    modifier: Modifier,
    onClick: (() -> Unit)? = null,
    descripcionClick: String? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .then(
                if (onClick != null) Modifier.clickable(onClickLabel = descripcionClick, role = Role.Button, onClick = onClick)
                else Modifier.semantics(mergeDescendants = true) {}
            )
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        contenido()
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TextoSecundario(texto: String) {
    Text(text = texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
