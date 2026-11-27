package com.example.estudia.ui.pantallas

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
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
import com.example.estudia.modelo.CategoriaArchivo
import com.example.estudia.modelo.reglas
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
import com.example.estudia.ui.componentes.TarjetaAcceso
import com.example.estudia.ui.theme.BloqueNaranja
import com.example.estudia.ui.theme.BloqueAzul
import com.example.estudia.ui.theme.BloqueVioleta
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.WorkspacePremium
import com.example.estudia.ui.componentes.abrirArchivo
import com.example.estudia.viewmodel.MateriasViewModel
import java.time.LocalDate

// Pantalla "Mis Materias": materias activas o archivadas, con sus tareas y
// archivos; la lupa del dock busca entre las tareas. Todas las operaciones las hace
// MateriasViewModel; esta pantalla solo dibuja y avisa de los eventos.
@Composable
fun MateriasScreen(
    viewModel: MateriasViewModel,
    // true cuando se eligió crear desde el botón "+" del dock.
    crearAlAbrir: Boolean = false,
    onCreacionAbierta: () -> Unit = {},
    // Lo que se escribe en la lupa del dock: busca entre las tareas.
    busqueda: String = "",
    // Abre el repaso de las tarjetas de esa materia.
    onRepasar: (Int) -> Unit = {},
    // Quiz con las tarjetas y "¿Qué nota necesito?" de esa materia.
    onQuiz: (Int) -> Unit = {},
    onNotaNecesaria: (Int) -> Unit = {},
    // Accesos de arriba: el plan de estudios, las mesas de finales y la bibliografía.
    onAbrirCarrera: () -> Unit = {},
    onAbrirFinales: () -> Unit = {},
    onAbrirBibliografia: () -> Unit = {}
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(busqueda) { viewModel.cambiarBusqueda(busqueda) }
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
    // Categoría que va a tener lo que se adjunte (Teoría, TPs...).
    var categoriaParaAdjuntar by rememberSaveable { mutableStateOf(CategoriaArchivo.OTROS) }

    // Selector de archivos: solo se abre con el permiso de fotos y archivos
    // (ver selectorDeArchivos).
    val permisoArchivosPedido by viewModel.permisoArchivosPedido.collectAsStateWithLifecycle()
    val elegirArchivo = selectorDeArchivos(permisoArchivosPedido, viewModel::marcarPermisoArchivosPedido) { uri ->
        val id = idMateriaParaAdjuntar
        if (uri != null && id != null) viewModel.adjuntarArchivo(id, uri.toString(), categoriaParaAdjuntar)
        idMateriaParaAdjuntar = null
    }
    // Cámara: la foto se guarda como un archivo más de la materia.
    val sacarFoto = camaraDeFotos { uri ->
        idMateriaParaAdjuntar?.let { viewModel.adjuntarArchivo(it, uri, categoriaParaAdjuntar) }
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
                // Accesos a la carrera completa: el plan con sus correlativas y
                // las mesas de finales.
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 12.dp).height(IntrinsicSize.Min)) {
                    TarjetaAcceso(stringResource(R.string.carrera_titulo), stringResource(R.string.acceso_carrera), Icons.Outlined.School, BloqueVioleta, Modifier.weight(1f).fillMaxHeight(), alTocar = onAbrirCarrera)
                    TarjetaAcceso(stringResource(R.string.finales_titulo), stringResource(R.string.acceso_finales), Icons.Outlined.WorkspacePremium, BloqueNaranja, Modifier.weight(1f).fillMaxHeight(), alTocar = onAbrirFinales)
                }
                // La bibliografía, a lo ancho abajo (tres juntas no entran).
                TarjetaAcceso(
                    stringResource(R.string.biblio_titulo), stringResource(R.string.acceso_biblio), Icons.Outlined.LocalLibrary, BloqueAzul,
                    Modifier.fillMaxWidth().padding(bottom = 12.dp), apilada = false, alTocar = onAbrirBibliografia
                )
            }

            if (estado.buscando) {
                // ---------- Resultados del buscador ----------
                if (estado.resultadosBusqueda.isEmpty()) {
                    item {
                        EstadoVacio(
                            Icons.Outlined.SearchOff,
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
                        fondoContenido = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        FilaTarea(
                            tarea = encontrada.tarea,
                            hoy = LocalDate.now(),
                            subtitulo = encontrada.nombreMateria,
                            onAlternar = { viewModel.alternarTarea(encontrada.materiaId, encontrada.tarea) },
                            opciones = listOf(
                                OpcionMenu(stringResource(R.string.editar), Icons.Outlined.Edit) {
                                    // El buscador no trae las subtareas: se edita la tarea completa.
                                    val completa = estado.todas.flatMap { it.tareas }.firstOrNull { it.id == encontrada.tarea.id }
                                    tareaEnDialogo = encontrada.materiaId to (completa ?: encontrada.tarea)
                                },
                                OpcionMenu(stringResource(R.string.eliminar), Icons.Outlined.Delete, destructiva = true) {
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
                                Icons.Outlined.Archive,
                                stringResource(R.string.sin_materias_archivadas),
                                stringResource(R.string.sin_materias_archivadas_sugerencia)
                            )
                        } else {
                            EstadoVacio(
                                Icons.AutoMirrored.Outlined.MenuBook,
                                stringResource(R.string.sin_materias_activas),
                                stringResource(R.string.sin_materias_activas_sugerencia)
                            )
                        }
                    }
                }
                items(estado.materias, key = { it.id }) { materia ->
                    // Deslizar a la derecha archiva (o desarchiva) la materia.
                    Deslizable(
                        modifier = Modifier.animateItem(),
                        onArchivar = { viewModel.alternarArchivada(materia) },
                        archivada = materia.archivada,
                        forma = MaterialTheme.shapes.large,
                        margenVertical = 6.dp
                    ) {
                    TarjetaMateria(
                        modifier = Modifier,
                        materia = materia,
                        onAlternarTarea = { viewModel.alternarTarea(materia.id, it) },
                        onAlternarSubtarea = viewModel::alternarSubtarea,
                        onAgregarTarea = { tareaEnDialogo = materia.id to null },
                        onEditarTarea = { tareaEnDialogo = materia.id to it },
                        onEliminarTarea = { viewModel.eliminarTarea(materia.id, it) },
                        onAdjuntarArchivo = { categoria ->
                            idMateriaParaAdjuntar = materia.id
                            categoriaParaAdjuntar = categoria
                            elegirArchivo()
                        },
                        onSacarFoto = { categoria ->
                            idMateriaParaAdjuntar = materia.id
                            categoriaParaAdjuntar = categoria
                            sacarFoto()
                        },
                        onAbrirArchivo = { archivo ->
                            if (!abrirArchivo(context, Uri.parse(viewModel.uriParaAbrir(archivo)), archivo.tipoMime)) {
                                avisos.mostrarTexto(textoSinApp)
                            }
                        },
                        onEliminarArchivo = { viewModel.eliminarArchivo(materia.id, it) },
                        onCambiarCategoria = { archivo, categoria -> viewModel.cambiarCategoriaArchivo(materia.id, archivo, categoria) },
                        onAgregarCalificacion = { calificacionEnDialogo = materia.id to null },
                        onEditarCalificacion = { calificacionEnDialogo = materia.id to it },
                        onEliminarCalificacion = { viewModel.eliminarCalificacion(materia.id, it) },
                        onAgregarTarjeta = { tarjetaEnDialogo = materia.id to null },
                        onEditarTarjeta = { tarjetaEnDialogo = materia.id to it },
                        onEliminarTarjeta = { viewModel.eliminarTarjeta(materia.id, it) },
                        onRepasar = { onRepasar(materia.id) },
                        onQuiz = { onQuiz(materia.id) },
                        onNotaNecesaria = { onNotaNecesaria(materia.id) },
                        onEditar = { materiaParaEditar = materia },
                        onArchivar = { viewModel.alternarArchivada(materia) },
                        onEliminar = { materiaParaEliminar = materia }
                    )
                    }
                }
            }
        }
    }

    if (crearMateria) {
        DialogoMateria(
            nombreInicial = "",
            nombreDisponible = { viewModel.nombreDisponible(it) },
            onConfirmar = { nombre, reglas ->
                viewModel.agregarMateria(nombre, reglas)
                crearMateria = false
            },
            onCancelar = { crearMateria = false }
        )
    }

    materiaParaEditar?.let { materia ->
        DialogoMateria(
            nombreInicial = materia.nombre,
            nombreDisponible = { viewModel.nombreDisponible(it, exceptoId = materia.id) },
            reglasIniciales = materia.reglas,
            onConfirmar = { nombre, reglas ->
                viewModel.editarMateria(materia, nombre, reglas)
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
