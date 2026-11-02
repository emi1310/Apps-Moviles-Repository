package com.example.estudia.ui.pantallas

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.ui.componentes.Buscador
import com.example.estudia.ui.componentes.EncabezadoPantalla
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.DialogoNota
import com.example.estudia.ui.componentes.LocalAvisos
import com.example.estudia.ui.componentes.RecolectarAvisos
import com.example.estudia.ui.componentes.TIPOS_ARCHIVO
import com.example.estudia.ui.componentes.TarjetaNota
import com.example.estudia.ui.componentes.abrirArchivo
import com.example.estudia.viewmodel.FiltroNotas
import com.example.estudia.viewmodel.NotaConMateria
import com.example.estudia.viewmodel.NotasViewModel

// Pantalla "Mis Notas": buscador, filtros Todas / Recientes / Favoritas y
// la lista de notas, cada una con sus archivos adjuntos.
@Composable
fun NotasScreen(viewModel: NotasViewModel) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RecolectarAvisos(viewModel.avisos)
    val context = LocalContext.current
    val avisos = LocalAvisos.current
    val textoSinApp = stringResource(R.string.sin_app_para_abrir)

    var crearNota by rememberSaveable { mutableStateOf(false) }
    var notaParaEditar by remember { mutableStateOf<NotaConMateria?>(null) }
    var idNotaParaAdjuntar by rememberSaveable { mutableStateOf<Int?>(null) }

    // Selector de archivos del sistema: no necesita permisos.
    val selectorArchivos = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val nota = estado.notas.firstOrNull { it.nota.id == idNotaParaAdjuntar }
        if (uri != null && nota != null) viewModel.adjuntarArchivo(nota, uri)
        idNotaParaAdjuntar = null
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { crearNota = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.nueva_nota)) }
            )
        }
    ) { relleno ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(relleno),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp)
        ) {
            item {
                EncabezadoPantalla(titulo = stringResource(R.string.mis_notas), subtitulo = stringResource(R.string.notas_subtitulo))
                Buscador(estado.busqueda, viewModel::cambiarBusqueda, stringResource(R.string.buscar_notas))
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    listOf(
                        FiltroNotas.TODAS to R.string.todas,
                        FiltroNotas.RECIENTES to R.string.recientes,
                        FiltroNotas.FAVORITAS to R.string.favoritas
                    ).forEach { (filtro, texto) ->
                        FilterChip(
                            selected = estado.filtro == filtro,
                            onClick = { viewModel.cambiarFiltro(filtro) },
                            label = { Text(stringResource(texto)) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                if (estado.notas.isEmpty() && !estado.cargando) {
                    if (estado.busqueda.isNotBlank()) {
                        EstadoVacio(Icons.Filled.SearchOff, stringResource(R.string.sin_resultados), stringResource(R.string.sin_resultados_sugerencia))
                    } else {
                        EstadoVacio(Icons.AutoMirrored.Filled.Notes, stringResource(R.string.sin_notas), stringResource(R.string.sin_notas_sugerencia))
                    }
                }
            }

            items(estado.notas, key = { it.nota.id }) { item ->
                TarjetaNota(
                    nota = item.nota,
                    nombreMateria = item.nombreMateria,
                    colorMateria = item.colorMateria,
                    alTocarEstrella = { viewModel.alternarFavorita(item) },
                    alAdjuntar = {
                        idNotaParaAdjuntar = item.nota.id
                        selectorArchivos.launch(TIPOS_ARCHIVO)
                    },
                    alEditar = { notaParaEditar = item },
                    alEliminar = { viewModel.eliminarNota(item) },
                    alAbrirArchivo = { archivo ->
                        if (!abrirArchivo(context, viewModel.uriParaAbrir(archivo), archivo.tipoMime)) {
                            avisos.mostrarTexto(textoSinApp)
                        }
                    },
                    alEliminarArchivo = { viewModel.eliminarArchivo(item, it) }
                )
            }
        }
    }

    if (crearNota) {
        DialogoNota(
            nota = null,
            materias = estado.materiasActivas,
            onConfirmar = { materiaId, titulo, contenido ->
                if (materiaId != null) viewModel.agregarNota(materiaId, titulo, contenido)
                crearNota = false
            },
            onCancelar = { crearNota = false }
        )
    }

    notaParaEditar?.let { item ->
        DialogoNota(
            nota = item.nota,
            materias = estado.materiasActivas,
            onConfirmar = { _, titulo, contenido ->
                viewModel.editarNota(item, titulo, contenido)
                notaParaEditar = null
            },
            onCancelar = { notaParaEditar = null }
        )
    }
}
