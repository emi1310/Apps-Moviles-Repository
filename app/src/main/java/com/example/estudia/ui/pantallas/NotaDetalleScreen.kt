package com.example.estudia.ui.pantallas

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.ui.componentes.DialogoNota
import com.example.estudia.ui.componentes.DialogoColorNota
import com.example.estudia.ui.componentes.DialogoHistorialNota
import com.example.estudia.ui.componentes.PastillaEtiqueta
import com.example.estudia.ui.componentes.TextoNotaConCasillas
import com.example.estudia.ui.componentes.grabadoraDeAudio
import com.example.estudia.modelo.etiquetas
import com.example.estudia.modelo.progresoCasillas
import com.example.estudia.ui.componentes.ListaArchivos
import com.example.estudia.ui.componentes.LocalAvisos
import com.example.estudia.ui.componentes.MenuOpciones
import com.example.estudia.ui.componentes.OpcionMenu
import com.example.estudia.ui.componentes.abrirArchivo
import com.example.estudia.ui.componentes.camaraDeFotos
import com.example.estudia.ui.componentes.colorAcentoTexto
import com.example.estudia.ui.componentes.elementoCompartido
import com.example.estudia.ui.componentes.fondoSuave
import com.example.estudia.ui.componentes.limitesCompartidos
import com.example.estudia.ui.componentes.selectorDeArchivos
import com.example.estudia.ui.componentes.superficieVidrio
import com.example.estudia.ui.theme.AmarilloFavorita
import com.example.estudia.ui.theme.LocalDegradado
import com.example.estudia.util.aTexto
import com.example.estudia.viewmodel.NotasViewModel

// Una nota abierta: la materia, el título, cuándo se creó, cuánto texto
// tiene, el texto completo (se puede seleccionar y copiar) y los adjuntos.
// Arriba están fijar, la estrella y el menú (adjuntar, foto, grabar audio,
// color, historial, PDF, copiar, compartir, eliminar). Las casillas del texto
// se marcan tocándolas y las etiquetas llevan a la lista filtrada. Abajo
// está el botón Editar. Usa el mismo ViewModel que la lista de
// notas, así cualquier cambio se ve en los dos lados.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotaDetalleScreen(notaId: Int, viewModel: NotasViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val item = estado.notas.firstOrNull { it.nota.id == notaId }
    // Si la nota ya no está (se borró), se vuelve a la lista.
    LaunchedEffect(item == null, estado.cargando) {
        if (item == null && !estado.cargando) onVolver()
    }
    CompartirPdfDeNotas(viewModel)
    item ?: return

    val context = LocalContext.current
    val avisos = LocalAvisos.current
    val textoSinApp = stringResource(R.string.sin_app_para_abrir)
    val textoCopiado = stringResource(R.string.nota_texto_copiado)
    val tituloCompartir = stringResource(R.string.nota_compartir_texto)
    var editando by rememberSaveable { mutableStateOf(false) }
    var eligiendoColor by rememberSaveable { mutableStateOf(false) }
    var viendoHistorial by rememberSaveable { mutableStateOf(false) }
    val grabar = grabadoraDeAudio { uri, cuando, marcas -> viewModel.guardarGrabacion(item, uri, cuando, marcas) }

    val permisoArchivosPedido by viewModel.permisoArchivosPedido.collectAsStateWithLifecycle()
    val elegirArchivo = selectorDeArchivos(permisoArchivosPedido, viewModel::marcarPermisoArchivosPedido) { uri ->
        if (uri != null) viewModel.adjuntarArchivo(item, uri.toString())
    }
    val sacarFoto = camaraDeFotos { uri -> viewModel.adjuntarArchivo(item, uri) }

    val nota = item.nota
    val color = Color(nota.color ?: item.colorMateria)
    val etiquetas = remember(nota.titulo, nota.contenido) { nota.etiquetas }
    val (hechas, casillas) = remember(nota.contenido) { progresoCasillas(nota.contenido) }
    val acento = colorAcentoTexto(color)
    val palabras = remember(nota.contenido) { nota.contenido.split(Regex("\\s+")).count { it.isNotBlank() } }
    val minutosLectura = ((palabras + 199) / 200).coerceAtLeast(1)
    val textoCompleto = listOf(nota.titulo, nota.contenido).filter { it.isNotBlank() }.joinToString("\n\n")

    Box(
        modifier = Modifier
            .fillMaxSize()
            // La tarjeta de la lista se agranda hasta ser esta pantalla.
            .limitesCompartidos("nota-${nota.id}")
            .superficieVidrio(RectangleShape, borde = false, hinchar = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp)
        ) {
            // Barra de arriba: volver, favorita y menú.
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.volver))
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.alternarFijada(item) }) {
                    Icon(
                        if (nota.fijada) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = stringResource(if (nota.fijada) R.string.nota_desfijar else R.string.nota_fijar),
                        tint = if (nota.fijada) acento else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.alternarFavorita(item) }) {
                    Icon(
                        if (nota.esFavorita) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = stringResource(if (nota.esFavorita) R.string.quitar_favorita else R.string.marcar_favorita),
                        tint = if (nota.esFavorita) AmarilloFavorita else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MenuOpciones(
                    listOf(
                        OpcionMenu(stringResource(R.string.nota_copiar_texto), Icons.Outlined.ContentCopy) {
                            copiarAlPortapapeles(context, nota.titulo, textoCompleto)
                            avisos.mostrarTexto(textoCopiado)
                        },
                        OpcionMenu(stringResource(R.string.nota_compartir_texto), Icons.Outlined.Share) {
                            val enviar = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, textoCompleto)
                            try {
                                context.startActivity(Intent.createChooser(enviar, tituloCompartir))
                            } catch (e: ActivityNotFoundException) {
                                avisos.mostrarTexto(textoSinApp)
                            }
                        },
                        OpcionMenu(stringResource(R.string.adjuntar_archivo), Icons.Outlined.AttachFile, alTocar = elegirArchivo),
                        OpcionMenu(stringResource(R.string.foto_sacar), Icons.Outlined.PhotoCamera, alTocar = sacarFoto),
                        OpcionMenu(stringResource(R.string.grabar_audio), Icons.Outlined.Mic, alTocar = grabar),
                        OpcionMenu(stringResource(R.string.nota_color), Icons.Outlined.Palette) { eligiendoColor = true },
                        OpcionMenu(stringResource(R.string.nota_historial), Icons.Outlined.History) { viendoHistorial = true },
                        OpcionMenu(stringResource(R.string.exportar_pdf), Icons.Outlined.PictureAsPdf) { viewModel.exportarPdf(item) },
                        OpcionMenu(stringResource(R.string.eliminar), Icons.Outlined.Delete, destructiva = true) {
                            // Primero se vuelve: el aviso con "Deshacer" sale en la lista.
                            onVolver()
                            viewModel.eliminarNota(item)
                        }
                    ),
                    descripcion = stringResource(R.string.opciones_de, nota.titulo)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            // La materia, en su color.
            Text(
                text = item.nombreMateria,
                style = MaterialTheme.typography.labelLarge,
                color = acento,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(fondoSuave(color))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = nota.titulo,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .elementoCompartido("titulo-nota-${nota.id}")
                    .semantics { heading() }
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Datos: creada, largo, lectura y adjuntos.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Dato(Icons.Outlined.CalendarToday, nota.fechaCreacion.aTexto(), acento, color)
                Dato(Icons.AutoMirrored.Outlined.Notes, pluralStringResource(R.plurals.nota_palabras, palabras, palabras), acento, color)
                if (palabras > 0) Dato(Icons.Outlined.Schedule, stringResource(R.string.nota_minutos_lectura, minutosLectura), acento, color)
                if (nota.archivos.isNotEmpty()) Dato(Icons.Outlined.AttachFile, pluralStringResource(R.plurals.nota_adjuntos, nota.archivos.size, nota.archivos.size), acento, color)
                if (casillas > 0) Dato(Icons.Outlined.CheckBox, stringResource(R.string.nota_casillas, hechas, casillas), acento, color)
            }
            // Etiquetas: tocar una vuelve a la lista mostrando solo esas notas.
            if (etiquetas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    etiquetas.forEach { etiqueta ->
                        PastillaEtiqueta(etiqueta, color) {
                            viewModel.filtrarPorEtiqueta(etiqueta)
                            onVolver()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            // El texto completo, en un bloque de vidrio. Se puede seleccionar.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .superficieVidrio(MaterialTheme.shapes.large, hinchar = false)
                    .padding(20.dp)
            ) {
                if (nota.contenido.isBlank()) {
                    Text(
                        stringResource(R.string.nota_sin_texto),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Las casillas ("- [ ]") se marcan tocándolas.
                    TextoNotaConCasillas(nota.contenido, acento) { linea -> viewModel.alternarCasilla(item, linea) }
                }
            }

            if (nota.archivos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    stringResource(R.string.nota_adjuntos_titulo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp).semantics { heading() }
                )
                ListaArchivos(
                    nota.archivos,
                    color,
                    onAbrir = { archivo ->
                        if (!abrirArchivo(context, Uri.parse(viewModel.uriParaAbrir(archivo)), archivo.tipoMime)) {
                            avisos.mostrarTexto(textoSinApp)
                        }
                    },
                    onEliminar = { viewModel.eliminarArchivo(item, it) },
                    onPasarATexto = { viewModel.pasarATexto(item, it) }
                )
            }
        }

        // Editar: pastilla con el degradado de la app, abajo a la derecha.
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp)
                .clip(RoundedCornerShape(50))
                .background(LocalDegradado.current)
                .clickable(role = Role.Button) { editando = true }
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.editar), color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }

    if (eligiendoColor) {
        DialogoColorNota(
            actual = nota.color,
            colorMateria = item.colorMateria,
            onElegir = {
                viewModel.cambiarColor(item, it)
                eligiendoColor = false
            },
            onCancelar = { eligiendoColor = false }
        )
    }

    if (viendoHistorial) {
        val versiones by remember(nota.id) { viewModel.versiones(nota.id) }.collectAsStateWithLifecycle(emptyList())
        DialogoHistorialNota(
            versiones = versiones,
            onRestaurar = {
                viewModel.restaurarVersion(item, it)
                viendoHistorial = false
            },
            onCerrar = { viendoHistorial = false }
        )
    }

    if (editando) {
        DialogoNota(
            nota = nota,
            materias = estado.materiasActivas,
            fuentes = estado.fuentes,
            materiaDeLaNota = item.nombreMateria,
            onConfirmar = { _, titulo, contenido ->
                viewModel.editarNota(item, titulo, contenido)
                editando = false
            },
            onCancelar = { editando = false }
        )
    }
}

// Una pastilla con un dato de la nota (fecha, palabras...).
@Composable
private fun Dato(icono: ImageVector, texto: String, acento: Color, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(fondoSuave(color))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = acento, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.labelMedium, color = acento, maxLines = 1)
    }
}

private fun copiarAlPortapapeles(context: Context, etiqueta: String, texto: String) {
    val portapapeles = context.getSystemService(ClipboardManager::class.java) ?: return
    portapapeles.setPrimaryClip(ClipData.newPlainText(etiqueta, texto))
}

// Cuando el PDF de una nota está listo se abre "Compartir" (WhatsApp, mail,
// Drive...). Lo usan la lista de notas y la nota abierta.
@Composable
fun CompartirPdfDeNotas(viewModel: NotasViewModel) {
    val context = LocalContext.current
    val avisos = LocalAvisos.current
    val textoSinApp = stringResource(R.string.sin_app_para_abrir)
    val pdf by viewModel.pdfParaCompartir.collectAsStateWithLifecycle()
    val tituloCompartir = stringResource(R.string.exportar_pdf_compartir)
    LaunchedEffect(pdf) {
        val uri = pdf ?: return@LaunchedEffect
        val enviar = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(enviar, tituloCompartir))
        } catch (e: ActivityNotFoundException) {
            avisos.mostrarTexto(textoSinApp)
        }
        viewModel.pdfCompartido()
    }
}
