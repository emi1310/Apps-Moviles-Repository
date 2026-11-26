package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.ui.componentes.Buscador
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.FilaTarjeta
import com.example.estudia.ui.componentes.IconoCircular
import com.example.estudia.ui.componentes.PantallaDetalle
import com.example.estudia.ui.componentes.TituloLista
import com.example.estudia.ui.componentes.textoFecha
import com.example.estudia.viewmodel.BusquedaViewModel
import com.example.estudia.viewmodel.ResultadoBusqueda
import com.example.estudia.viewmodel.TipoResultado

// Búsqueda global: un solo buscador para materias, tareas, notas, eventos,
// tarjetas, el plan de estudios y los finales. Tocar un resultado lleva a
// la sección donde está. Se abre con la lupa de Inicio.
@Composable
fun BusquedaScreen(viewModel: BusquedaViewModel, onVolver: () -> Unit, onAbrir: (TipoResultado) -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val foco = remember { FocusRequester() }
    // El teclado se abre solo al entrar.
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }

    PantallaDetalle(titulo = stringResource(R.string.busqueda_titulo), subtitulo = stringResource(R.string.busqueda_subtitulo), onVolver = onVolver) {
        item {
            Buscador(
                texto = estado.texto,
                onCambiar = viewModel::cambiarTexto,
                sugerencia = stringResource(R.string.busqueda_sugerencia),
                modifier = Modifier.fillMaxWidth().focusRequester(foco)
            )
        }
        when {
            !estado.buscando -> item {
                EstadoVacio(Icons.Outlined.Search, stringResource(R.string.busqueda_vacio), stringResource(R.string.busqueda_vacio_sugerencia))
            }
            estado.resultados.isEmpty() -> item {
                EstadoVacio(Icons.Outlined.Search, stringResource(R.string.busqueda_sin_resultados), stringResource(R.string.busqueda_sin_resultados_sugerencia, estado.texto.trim()))
            }
            else -> itemsIndexed(estado.resultados, key = { i, r -> "$i${r.tipo}${r.titulo}" }) { i, resultado ->
                Column {
                // Título del grupo cuando cambia el tipo.
                if (i == 0 || estado.resultados[i - 1].tipo != resultado.tipo) {
                    TituloLista(stringResource(tituloTipo(resultado.tipo)))
                    Spacer(modifier = Modifier.height(8.dp))
                }
                FilaResultado(resultado) { onAbrir(resultado.tipo) }
                }
            }
        }
    }
}

@Composable
private fun FilaResultado(resultado: ResultadoBusqueda, onClick: () -> Unit) {
    val color = resultado.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    FilaTarjeta(onClick = onClick, modifier = Modifier.semantics(mergeDescendants = true) {}) {
        IconoCircular(iconoTipo(resultado.tipo), color)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(resultado.titulo, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            resultado.detalle?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            val partes = listOfNotNull(resultado.nombreMateria, resultado.fecha?.let { textoFecha(it) })
            if (partes.isNotEmpty()) {
                Text(
                    partes.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun tituloTipo(tipo: TipoResultado): Int = when (tipo) {
    TipoResultado.MATERIA -> R.string.busqueda_materias
    TipoResultado.TAREA -> R.string.busqueda_tareas
    TipoResultado.NOTA -> R.string.busqueda_notas
    TipoResultado.EVENTO -> R.string.busqueda_eventos
    TipoResultado.TARJETA -> R.string.busqueda_tarjetas
    TipoResultado.CARRERA -> R.string.busqueda_carrera
    TipoResultado.FINAL -> R.string.busqueda_finales
    TipoResultado.FUENTE -> R.string.busqueda_bibliografia
    TipoResultado.MAPA -> R.string.busqueda_mapas
}

private fun iconoTipo(tipo: TipoResultado): ImageVector = when (tipo) {
    TipoResultado.MATERIA -> Icons.Outlined.Book
    TipoResultado.TAREA -> Icons.Filled.CheckCircle
    TipoResultado.NOTA -> Icons.AutoMirrored.Outlined.StickyNote2
    TipoResultado.EVENTO -> Icons.Outlined.Event
    TipoResultado.TARJETA -> Icons.Outlined.Style
    TipoResultado.CARRERA -> Icons.Outlined.School
    TipoResultado.FINAL -> Icons.Outlined.WorkspacePremium
    TipoResultado.FUENTE -> Icons.Outlined.LocalLibrary
    TipoResultado.MAPA -> Icons.Outlined.AccountTree
}
