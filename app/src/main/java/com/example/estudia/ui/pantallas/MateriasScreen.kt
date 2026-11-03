package com.example.estudia.ui.pantallas

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Calificacion
import com.example.estudia.modelo.Tarea
import com.example.estudia.ui.componentes.Deslizable
import com.example.estudia.ui.componentes.DialogoCalificacion
import com.example.estudia.ui.componentes.Buscador
import com.example.estudia.ui.componentes.EncabezadoPantalla
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.DialogoConfirmacion
import com.example.estudia.ui.componentes.DialogoMateria
import com.example.estudia.ui.componentes.DialogoTarea
import com.example.estudia.ui.componentes.FilaTarea
import com.example.estudia.ui.componentes.LocalAvisos
import com.example.estudia.ui.componentes.OpcionMenu
import com.example.estudia.ui.componentes.RecolectarAvisos
import com.example.estudia.ui.componentes.SnackbarDeAvisos
import com.example.estudia.ui.componentes.TIPOS_ARCHIVO
import com.example.estudia.ui.componentes.TarjetaMateria
import com.example.estudia.ui.componentes.abrirArchivo
import com.example.estudia.viewmodel.MateriasViewModel
import java.time.LocalDate

// Pantalla "Mis Materias": materias activas o archivadas, con sus tareas y
// archivos, y un buscador de tareas. Todas las operaciones las hace
// MateriasViewModel; esta pantalla solo dibuja y avisa de los eventos.
@Composable
fun MateriasScreen(viewModel: MateriasViewModel) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RecolectarAvisos(viewModel.avisos)
    val context = LocalContext.current
    val avisos = LocalAvisos.current
    val textoSinApp = stringResource(R.string.sin_app_para_abrir)

    var crearMateria by rememberSaveable { mutableStateOf(false) }
    var materiaParaEditar by remember { mutableStateOf<Materia?>(null) }
    var materiaParaEliminar by remember { mutableStateOf<Materia?>(null) }
    // Tarea a crear (tarea == null) o editar, junto con su materia.
    var tareaEnDialogo by remember { mutableStateOf<Pair<Int, Tarea?>?>(null) }
    // Nota de examen a crear (calificación == null) o editar, junto con su materia.
    var calificacionEnDialogo by remember { mutableStateOf<Pair<Int, Calificacion?>?>(null) }
    // rememberSaveable: si Android recrea la pantalla mientras el usuario
    // está en el selector de archivos, no se pierde a qué materia adjuntarlo.
    var idMateriaParaAdjuntar by rememberSaveable { mutableStateOf<Int?>(null) }

    // Selector de archivos del sistema (Storage Access Framework): no
    // necesita permisos de almacenamiento, la app recibe acceso solo al
    // archivo elegido.
    val selectorArchivos = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val id = idMateriaParaAdjuntar
        if (uri != null && id != null) viewModel.adjuntarArchivo(id, uri)
        idMateriaParaAdjuntar = null
    }

    // contentWindowInsets en 0: el Scaffold de AppNavegacion ya dejó lugar
    // para las barras del sistema (si no, el espacio se duplica).
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarDeAvisos() },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { crearMateria = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.nueva_materia)) }
            )
        }
    ) { relleno ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(relleno),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp)
        ) {
            item {
                EncabezadoPantalla(
                    titulo = stringResource(R.string.mis_materias),
                    subtitulo = pluralStringResource(
                        R.plurals.materias_activas, estado.todas.count { !it.archivada }, estado.todas.count { !it.archivada }
                    )
                )
                Buscador(estado.busqueda, viewModel::cambiarBusqueda, stringResource(R.string.buscar_tareas))
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (estado.buscando) {
                // ---------- Resultados del buscador ----------
                if (estado.resultadosBusqueda.isEmpty()) {
                    item {
                        EstadoVacio(
                            Icons.Filled.SearchOff,
                            stringResource(R.string.sin_resultados),
                            stringResource(R.string.sin_resultados_sugerencia)
                        )
                    }
                }
                items(estado.resultadosBusqueda, key = { "t" + it.tarea.id }) { encontrada ->
                    Deslizable(
                        modifier = Modifier.animateItem(),
                        onCompletar = { viewModel.alternarTarea(encontrada.materiaId, encontrada.tarea) },
                        completada = encontrada.tarea.completada,
                        onEliminar = { viewModel.eliminarTarea(encontrada.materiaId, encontrada.tarea) },
                        forma = RoundedCornerShape(12.dp),
                        fondoContenido = MaterialTheme.colorScheme.background
                    ) {
                        FilaTarea(
                            tarea = encontrada.tarea,
                            hoy = LocalDate.now(),
                            subtitulo = encontrada.nombreMateria,
                            onAlternar = { viewModel.alternarTarea(encontrada.materiaId, encontrada.tarea) },
                            opciones = listOf(
                                OpcionMenu(stringResource(R.string.editar), Icons.Filled.Edit) {
                                    tareaEnDialogo = encontrada.materiaId to encontrada.tarea
                                },
                                OpcionMenu(stringResource(R.string.eliminar), Icons.Filled.Delete) {
                                    viewModel.eliminarTarea(encontrada.materiaId, encontrada.tarea)
                                }
                            )
                        )
                    }
                }
            } else {
                // ---------- Filtro Activas / Archivadas ----------
                item {
                    Row {
                        FilterChip(
                            selected = !estado.mostrarArchivadas,
                            onClick = { viewModel.mostrarArchivadas(false) },
                            label = { Text(stringResource(R.string.activas)) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = estado.mostrarArchivadas,
                            onClick = { viewModel.mostrarArchivadas(true) },
                            label = { Text(stringResource(R.string.archivadas)) }
                        )
                    }
                    if (estado.materias.isEmpty() && !estado.cargando) {
                        if (estado.mostrarArchivadas) {
                            EstadoVacio(
                                Icons.Filled.Archive,
                                stringResource(R.string.sin_materias_archivadas),
                                stringResource(R.string.sin_materias_archivadas_sugerencia)
                            )
                        } else {
                            EstadoVacio(
                                Icons.AutoMirrored.Filled.MenuBook,
                                stringResource(R.string.sin_materias_activas),
                                stringResource(R.string.sin_materias_activas_sugerencia)
                            )
                        }
                    }
                }
                items(estado.materias, key = { it.id }) { materia ->
                    TarjetaMateria(
                        modifier = Modifier.animateItem(),
                        materia = materia,
                        onAlternarTarea = { viewModel.alternarTarea(materia.id, it) },
                        onAgregarTarea = { tareaEnDialogo = materia.id to null },
                        onEditarTarea = { tareaEnDialogo = materia.id to it },
                        onEliminarTarea = { viewModel.eliminarTarea(materia.id, it) },
                        onAdjuntarArchivo = {
                            idMateriaParaAdjuntar = materia.id
                            selectorArchivos.launch(TIPOS_ARCHIVO)
                        },
                        onAbrirArchivo = { archivo ->
                            if (!abrirArchivo(context, viewModel.uriParaAbrir(archivo), archivo.tipoMime)) {
                                avisos.mostrarTexto(textoSinApp)
                            }
                        },
                        onEliminarArchivo = { viewModel.eliminarArchivo(materia.id, it) },
                        onAgregarCalificacion = { calificacionEnDialogo = materia.id to null },
                        onEditarCalificacion = { calificacionEnDialogo = materia.id to it },
                        onEliminarCalificacion = { viewModel.eliminarCalificacion(materia.id, it) },
                        onEditar = { materiaParaEditar = materia },
                        onArchivar = { viewModel.alternarArchivada(materia) },
                        onEliminar = { materiaParaEliminar = materia }
                    )
                }
            }
        }
    }

    if (crearMateria) {
        DialogoMateria(
            nombreInicial = "",
            nombreDisponible = { viewModel.nombreDisponible(it) },
            onConfirmar = {
                viewModel.agregarMateria(it)
                crearMateria = false
            },
            onCancelar = { crearMateria = false }
        )
    }

    materiaParaEditar?.let { materia ->
        DialogoMateria(
            nombreInicial = materia.nombre,
            nombreDisponible = { viewModel.nombreDisponible(it, exceptoId = materia.id) },
            onConfirmar = {
                viewModel.renombrarMateria(materia, it)
                materiaParaEditar = null
            },
            onCancelar = { materiaParaEditar = null }
        )
    }

    tareaEnDialogo?.let { (materiaId, tarea) ->
        DialogoTarea(
            tarea = tarea,
            onConfirmar = { titulo, fecha ->
                viewModel.guardarTarea(materiaId, tarea, titulo, fecha)
                tareaEnDialogo = null
            },
            onCancelar = { tareaEnDialogo = null }
        )
    }

    calificacionEnDialogo?.let { (materiaId, calificacion) ->
        DialogoCalificacion(
            calificacion = calificacion,
            onConfirmar = { titulo, nota, fecha ->
                val error = viewModel.guardarCalificacion(materiaId, calificacion, titulo, nota, fecha)
                if (error == null) calificacionEnDialogo = null
                error
            },
            onCancelar = { calificacionEnDialogo = null }
        )
    }

    materiaParaEliminar?.let { materia ->
        DialogoConfirmacion(
            mensaje = stringResource(R.string.confirmar_eliminar_materia, materia.nombre),
            textoConfirmar = stringResource(R.string.eliminar),
            onConfirmar = {
                viewModel.eliminarMateria(materia)
                materiaParaEliminar = null
            },
            onCancelar = { materiaParaEliminar = null }
        )
    }
}
