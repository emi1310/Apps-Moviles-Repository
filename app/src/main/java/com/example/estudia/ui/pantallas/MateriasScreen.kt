package com.example.estudia.ui.pantallas

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
import com.example.estudia.modelo.Tarjeta
import com.example.estudia.ui.componentes.DialogoTarjeta
import com.example.estudia.ui.componentes.LocalEspacioDock
import com.example.estudia.ui.componentes.selectorDeArchivos
import com.example.estudia.ui.componentes.camaraDeFotos
import com.example.estudia.ui.componentes.texto
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
import com.example.estudia.ui.componentes.PastillaFiltro
import com.example.estudia.ui.componentes.RecolectarAvisos
import com.example.estudia.ui.componentes.TarjetaMateria
import com.example.estudia.ui.componentes.abrirArchivo
import com.example.estudia.viewmodel.MateriasViewModel
import java.time.LocalDate

// Pantalla "Mis Materias": materias activas o archivadas, con sus tareas y
// archivos, y un buscador de tareas. Todas las operaciones las hace
// MateriasViewModel; esta pantalla solo dibuja y avisa de los eventos.
@Composable
fun MateriasScreen(
    viewModel: MateriasViewModel,
    // true cuando se eligió crear desde el botón "+" del dock.
    crearAlAbrir: Boolean = false,
    onCreacionAbierta: () -> Unit = {},
    // Abre el repaso de las tarjetas de esa materia.
    onRepasar: (Int) -> Unit = {}
) {
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
    // Tarjeta de estudio a crear (tarjeta == null) o editar, junto con su materia.
    var tarjetaEnDialogo by remember { mutableStateOf<Pair<Int, Tarjeta?>?>(null) }
    // rememberSaveable: si Android recrea la pantalla mientras el usuario
    // está en el selector de archivos, no se pierde a qué materia adjuntarlo.
    var idMateriaParaAdjuntar by rememberSaveable { mutableStateOf<Int?>(null) }

    // Selector de archivos: solo se abre con el permiso de fotos y archivos
    // (ver selectorDeArchivos).
    val permisoArchivosPedido by viewModel.permisoArchivosPedido.collectAsStateWithLifecycle()
    val elegirArchivo = selectorDeArchivos(permisoArchivosPedido, viewModel::marcarPermisoArchivosPedido) { uri ->
        val id = idMateriaParaAdjuntar
        if (uri != null && id != null) viewModel.adjuntarArchivo(id, uri.toString())
        idMateriaParaAdjuntar = null
    }
    // Cámara: la foto se guarda como un archivo más de la materia.
    val sacarFoto = camaraDeFotos { uri ->
        idMateriaParaAdjuntar?.let { viewModel.adjuntarArchivo(it, uri) }
        idMateriaParaAdjuntar = null
    }

    LaunchedEffect(crearAlAbrir) {
        if (crearAlAbrir) {
            crearMateria = true
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
                                    // El buscador no trae las subtareas: se edita la tarea completa.
                                    val completa = estado.todas.flatMap { it.tareas }.firstOrNull { it.id == encontrada.tarea.id }
                                    tareaEnDialogo = encontrada.materiaId to (completa ?: encontrada.tarea)
                                },
                                OpcionMenu(stringResource(R.string.eliminar), Icons.Filled.Delete, destructiva = true) {
                                    viewModel.eliminarTarea(encontrada.materiaId, encontrada.tarea)
                                }
                            )
                        )
                    }
                }
            } else {
                // ---------- Filtro Activas / Archivadas ----------
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                        PastillaFiltro(stringResource(R.string.activas), elegida = !estado.mostrarArchivadas) {
                            viewModel.mostrarArchivadas(false)
                        }
                        PastillaFiltro(stringResource(R.string.archivadas), elegida = estado.mostrarArchivadas) {
                            viewModel.mostrarArchivadas(true)
                        }
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
                        onAlternarSubtarea = viewModel::alternarSubtarea,
                        onAgregarTarea = { tareaEnDialogo = materia.id to null },
                        onEditarTarea = { tareaEnDialogo = materia.id to it },
                        onEliminarTarea = { viewModel.eliminarTarea(materia.id, it) },
                        onAdjuntarArchivo = {
                            idMateriaParaAdjuntar = materia.id
                            elegirArchivo()
                        },
                        onSacarFoto = {
                            idMateriaParaAdjuntar = materia.id
                            sacarFoto()
                        },
                        onAbrirArchivo = { archivo ->
                            if (!abrirArchivo(context, Uri.parse(viewModel.uriParaAbrir(archivo)), archivo.tipoMime)) {
                                avisos.mostrarTexto(textoSinApp)
                            }
                        },
                        onEliminarArchivo = { viewModel.eliminarArchivo(materia.id, it) },
                        onAgregarCalificacion = { calificacionEnDialogo = materia.id to null },
                        onEditarCalificacion = { calificacionEnDialogo = materia.id to it },
                        onEliminarCalificacion = { viewModel.eliminarCalificacion(materia.id, it) },
                        onAgregarTarjeta = { tarjetaEnDialogo = materia.id to null },
                        onEditarTarjeta = { tarjetaEnDialogo = materia.id to it },
                        onEliminarTarjeta = { viewModel.eliminarTarjeta(materia.id, it) },
                        onRepasar = { onRepasar(materia.id) },
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
            onConfirmar = { titulo, fecha, prioridad, subtareas, aviso ->
                viewModel.guardarTarea(materiaId, tarea, titulo, fecha, prioridad, subtareas, aviso)
                tareaEnDialogo = null
            },
            onCancelar = { tareaEnDialogo = null }
        )
    }

    tarjetaEnDialogo?.let { (materiaId, tarjeta) ->
        DialogoTarjeta(
            tarjeta = tarjeta,
            onConfirmar = { pregunta, respuesta ->
                val guardada = viewModel.guardarTarjeta(materiaId, tarjeta, pregunta, respuesta)
                if (guardada) tarjetaEnDialogo = null
                guardada
            },
            onCancelar = { tarjetaEnDialogo = null }
        )
    }

    calificacionEnDialogo?.let { (materiaId, calificacion) ->
        DialogoCalificacion(
            calificacion = calificacion,
            onConfirmar = { titulo, nota, fecha ->
                val error = viewModel.guardarCalificacion(materiaId, calificacion, titulo, nota, fecha)
                if (error == null) calificacionEnDialogo = null
                error?.texto()
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
