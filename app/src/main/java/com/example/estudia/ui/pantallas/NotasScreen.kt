package com.example.estudia.ui.pantallas

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.ui.componentes.LocalEspacioDock
import com.example.estudia.ui.componentes.selectorDeArchivos
import com.example.estudia.ui.componentes.camaraDeFotos
import com.example.estudia.ui.componentes.Deslizable
import com.example.estudia.ui.componentes.EncabezadoPantalla
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.DialogoNota
import com.example.estudia.ui.componentes.LocalAvisos
import com.example.estudia.ui.componentes.PastillaFiltro
import com.example.estudia.ui.componentes.RecolectarAvisos
import com.example.estudia.ui.componentes.TarjetaNota
import com.example.estudia.ui.componentes.abrirArchivo
import com.example.estudia.viewmodel.FiltroNotas
import com.example.estudia.viewmodel.NotaConMateria
import com.example.estudia.viewmodel.NotasViewModel

// Pantalla "Mis Notas": buscador, filtros Todas / Recientes / Favoritas y
// la lista de notas, cada una con sus archivos adjuntos.
@Composable
fun NotasScreen(
    viewModel: NotasViewModel,
    // true cuando se eligió crear desde el botón "+" del dock.
    crearAlAbrir: Boolean = false,
    onCreacionAbierta: () -> Unit = {},
    // Lo que se escribe en la lupa del dock: busca entre las notas.
    busqueda: String = ""
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(busqueda) { viewModel.cambiarBusqueda(busqueda) }
    RecolectarAvisos(viewModel.avisos)
    val context = LocalContext.current
    val avisos = LocalAvisos.current
    val textoSinApp = stringResource(R.string.sin_app_para_abrir)

    // Cuando el PDF está listo se abre "Compartir" (WhatsApp, mail, Drive...).
    val pdf by viewModel.pdfParaCompartir.collectAsStateWithLifecycle()
    val tituloCompartir = stringResource(R.string.exportar_pdf_compartir)
    LaunchedEffect(pdf) {
        val uri = pdf ?: return@LaunchedEffect
        val enviar = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, Uri.parse(uri))
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(android.content.Intent.createChooser(enviar, tituloCompartir))
        } catch (e: android.content.ActivityNotFoundException) {
            avisos.mostrarTexto(textoSinApp)
        }
        viewModel.pdfCompartido()
    }

    var crearNota by rememberSaveable { mutableStateOf(false) }
    var notaParaEditar by remember { mutableStateOf<NotaConMateria?>(null) }
    var idNotaParaAdjuntar by rememberSaveable { mutableStateOf<Int?>(null) }

    // Selector de archivos: solo se abre con el permiso de fotos y archivos
    // (ver selectorDeArchivos).
    val permisoArchivosPedido by viewModel.permisoArchivosPedido.collectAsStateWithLifecycle()
    val elegirArchivo = selectorDeArchivos(permisoArchivosPedido, viewModel::marcarPermisoArchivosPedido) { uri ->
        val nota = estado.notas.firstOrNull { it.nota.id == idNotaParaAdjuntar }
        if (uri != null && nota != null) viewModel.adjuntarArchivo(nota, uri.toString())
        idNotaParaAdjuntar = null
    }
    // Cámara: por ejemplo, una foto del pizarrón que queda adjunta a la nota.
    val sacarFoto = camaraDeFotos { uri ->
        estado.notas.firstOrNull { it.nota.id == idNotaParaAdjuntar }?.let { viewModel.adjuntarArchivo(it, uri) }
        idNotaParaAdjuntar = null
    }

    LaunchedEffect(crearAlAbrir) {
        if (crearAlAbrir) {
            crearNota = true
            onCreacionAbierta()
        }
    }

    // La lista pasa por detrás del dock flotante: abajo deja su espacio.
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = LocalEspacioDock.current + 16.dp)
        ) {
            item {
                EncabezadoPantalla(titulo = stringResource(R.string.mis_notas), subtitulo = stringResource(R.string.notas_subtitulo))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                    listOf(
                        FiltroNotas.TODAS to R.string.todas,
                        FiltroNotas.RECIENTES to R.string.recientes,
                        FiltroNotas.FAVORITAS to R.string.favoritas
                    ).forEach { (filtro, texto) ->
                        PastillaFiltro(stringResource(texto), elegida = estado.filtro == filtro) {
                            viewModel.cambiarFiltro(filtro)
                        }
                    }
                }
                if (estado.notas.isEmpty() && !estado.cargando) {
                    if (estado.busqueda.isNotBlank()) {
                        EstadoVacio(Icons.Outlined.SearchOff, stringResource(R.string.sin_resultados), stringResource(R.string.sin_resultados_sugerencia))
                    } else {
                        EstadoVacio(Icons.Outlined.StickyNote2, stringResource(R.string.sin_notas), stringResource(R.string.sin_notas_sugerencia))
                    }
                }
            }

            items(estado.notas, key = { it.nota.id }) { item ->
                // Deslizar hacia la izquierda elimina la nota (con "Deshacer").
                Deslizable(
                    modifier = Modifier.animateItem(),
                    onEliminar = { viewModel.eliminarNota(item) },
                    forma = MaterialTheme.shapes.large,
                    margenVertical = 6.dp
                ) {
                    TarjetaNota(
                        nota = item.nota,
                        nombreMateria = item.nombreMateria,
                        colorMateria = item.colorMateria,
                        alTocarEstrella = { viewModel.alternarFavorita(item) },
                        alAdjuntar = {
                            idNotaParaAdjuntar = item.nota.id
                            elegirArchivo()
                        },
                        alSacarFoto = {
                            idNotaParaAdjuntar = item.nota.id
                            sacarFoto()
                        },
                        alEditar = { notaParaEditar = item },
                        alEliminar = { viewModel.eliminarNota(item) },
                        alAbrirArchivo = { archivo ->
                            if (!abrirArchivo(context, Uri.parse(viewModel.uriParaAbrir(archivo)), archivo.tipoMime)) {
                                avisos.mostrarTexto(textoSinApp)
                            }
                        },
                        alEliminarArchivo = { viewModel.eliminarArchivo(item, it) },
                        alPasarATexto = { viewModel.pasarATexto(item, it) },
                        alExportarPdf = { viewModel.exportarPdf(item) }
                    )
                }
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
