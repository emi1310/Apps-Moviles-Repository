package com.example.estudia.ui.pantallas

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.modelo.Fuente
import com.example.estudia.modelo.TipoFuente
import com.example.estudia.modelo.TrozoCita
import com.example.estudia.modelo.citaApa
import com.example.estudia.modelo.citaEnTexto
import com.example.estudia.modelo.textoPlano
import com.example.estudia.ui.componentes.BloqueOpcion
import com.example.estudia.ui.componentes.BotonSuave
import com.example.estudia.ui.componentes.CampoTexto
import com.example.estudia.ui.componentes.DialogoApp
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.EtiquetaCampo
import com.example.estudia.ui.componentes.IconoDegradado
import com.example.estudia.ui.componentes.MenuOpciones
import com.example.estudia.ui.componentes.OpcionMenu
import com.example.estudia.ui.componentes.PantallaDetalle
import com.example.estudia.ui.componentes.PastillaFiltro
import com.example.estudia.ui.componentes.RecolectarAvisos
import com.example.estudia.ui.componentes.SugerenciasMateria
import com.example.estudia.ui.componentes.TextoError
import com.example.estudia.ui.componentes.bordeVidrio
import com.example.estudia.ui.componentes.rellenoVidrio
import com.example.estudia.ui.componentes.texto
import com.example.estudia.ui.theme.BloqueAzul
import com.example.estudia.ui.theme.BloqueNaranja
import com.example.estudia.ui.theme.BloqueVerde
import com.example.estudia.ui.theme.BloqueVioleta
import com.example.estudia.viewmodel.BibliografiaViewModel
import com.example.estudia.viewmodel.Mensaje

// Pantalla "Bibliografía": los libros, artículos, páginas web y apuntes de
// cada materia, con la cita en normas APA lista para copiar (en la lista de
// referencias o dentro del texto de una nota o un trabajo).
@Composable
fun BibliografiaScreen(viewModel: BibliografiaViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RecolectarAvisos(viewModel.avisos)
    val contexto = LocalContext.current
    val textoCopiada = stringResource(R.string.biblio_copiada)
    var editando by remember { mutableStateOf<Fuente?>(null) }   // id 0 = nueva

    fun copiar(texto: String) = copiarAlPortapapeles(contexto, texto, textoCopiada)

    PantallaDetalle(
        titulo = stringResource(R.string.biblio_titulo),
        subtitulo = stringResource(R.string.biblio_subtitulo),
        onVolver = onVolver,
        textoAgregar = stringResource(R.string.biblio_agregar),
        iconoAgregar = Icons.Outlined.Add,
        onAgregar = { editando = Fuente(0, estado.filtro ?: "", TipoFuente.LIBRO, "", null, "") }
    ) {
        if (!estado.cargando && estado.total == 0) {
            item { EstadoVacio(Icons.Outlined.LocalLibrary, stringResource(R.string.biblio_vacio), stringResource(R.string.biblio_vacio_sugerencia)) }
        }
        // Filtro por materia (solo si hay más de una con fuentes).
        if (estado.materiasConFuentes.size > 1) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    PastillaFiltro(stringResource(R.string.biblio_todas), elegida = estado.filtro == null) { viewModel.filtrar(null) }
                    estado.materiasConFuentes.forEach { materia ->
                        PastillaFiltro(materia, elegida = estado.filtro.equals(materia, ignoreCase = true)) { viewModel.filtrar(materia) }
                    }
                }
            }
        }
        items(estado.fuentes, key = { it.id }) { fuente ->
            TarjetaFuente(
                fuente = fuente,
                color = estado.colores[fuente.materia.lowercase()]?.let { Color(it) },
                onCopiar = { copiar(citaApa(fuente).textoPlano()) },
                onCopiarEnTexto = { copiar(citaEnTexto(fuente)) },
                onEditar = { editando = fuente },
                onEliminar = { viewModel.eliminar(fuente) }
            )
        }
    }

    editando?.let { fuente ->
        DialogoFuente(
            fuente = fuente,
            materias = (estado.materiasParaElegir + estado.materiasConFuentes).distinctBy { it.lowercase() },
            colores = estado.colores,
            onConfirmar = { viewModel.guardar(it).also { error -> if (error == null) editando = null } },
            onCancelar = { editando = null }
        )
    }
}

// Copia el texto. Desde Android 13 el sistema ya muestra que se copió; antes,
// un aviso corto.
private fun copiarAlPortapapeles(contexto: Context, texto: String, aviso: String) {
    contexto.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("cita", texto))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(contexto, aviso, Toast.LENGTH_SHORT).show()
}

private fun iconoFuente(tipo: TipoFuente): ImageVector = when (tipo) {
    TipoFuente.LIBRO -> Icons.AutoMirrored.Outlined.MenuBook
    TipoFuente.ARTICULO -> Icons.AutoMirrored.Outlined.Article
    TipoFuente.WEB -> Icons.Outlined.Language
    TipoFuente.APUNTE -> Icons.Outlined.Description
}

private fun colorFuente(tipo: TipoFuente): Color = when (tipo) {
    TipoFuente.LIBRO -> BloqueVioleta
    TipoFuente.ARTICULO -> BloqueAzul
    TipoFuente.WEB -> BloqueVerde
    TipoFuente.APUNTE -> BloqueNaranja
}

@Composable
private fun nombreTipo(tipo: TipoFuente): String = stringResource(
    when (tipo) {
        TipoFuente.LIBRO -> R.string.biblio_tipo_libro
        TipoFuente.ARTICULO -> R.string.biblio_tipo_articulo
        TipoFuente.WEB -> R.string.biblio_tipo_web
        TipoFuente.APUNTE -> R.string.biblio_tipo_apunte
    }
)

// La cita con el título (o la revista) en cursiva, como pide APA.
private fun citaConEstilo(trozos: List<TrozoCita>): AnnotatedString = buildAnnotatedString {
    trozos.forEach { trozo ->
        if (trozo.cursiva) withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(trozo.texto) } else append(trozo.texto)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaFuente(
    fuente: Fuente,
    color: Color?,
    onCopiar: () -> Unit,
    onCopiarEnTexto: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val contexto = LocalContext.current
    val sinApp = stringResource(R.string.evento_enlace_error)
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = bordeVidrio(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                IconoDegradado(iconoFuente(fuente.tipo), colorFuente(fuente.tipo), tamano = 38.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(fuente.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val autores = fuente.listaAutores.joinToString(", ").ifEmpty { nombreTipo(fuente.tipo) }
                    Text(
                        "$autores · ${fuente.anio ?: stringResource(R.string.biblio_sin_anio)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                MenuOpciones(
                    listOf(
                        OpcionMenu(stringResource(R.string.biblio_copiar_en_texto), Icons.Outlined.FormatQuote, alTocar = onCopiarEnTexto),
                        OpcionMenu(stringResource(R.string.editar), Icons.Outlined.Edit, alTocar = onEditar),
                        OpcionMenu(stringResource(R.string.eliminar), Icons.Outlined.Delete, destructiva = true, alTocar = onEliminar)
                    ),
                    descripcion = stringResource(R.string.opciones_de, fuente.titulo)
                )
            }
            Column(modifier = Modifier.padding(end = 12.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (fuente.materia.isNotEmpty()) {
                        com.example.estudia.ui.componentes.PastillaEstado(fuente.materia, color ?: MaterialTheme.colorScheme.primary)
                    }
                    com.example.estudia.ui.componentes.PastillaEstado(nombreTipo(fuente.tipo), colorFuente(fuente.tipo))
                }
                // La cita en normas APA, en un bloque apenas marcado.
                Surface(shape = MaterialTheme.shapes.medium, color = rellenoVidrio(), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        citaConEstilo(citaApa(fuente)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
                if (fuente.comentario.isNotEmpty()) {
                    Text(fuente.comentario, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonSuave(stringResource(R.string.biblio_copiar), Icons.Outlined.ContentCopy, MaterialTheme.colorScheme.primary, onCopiar, Modifier.weight(1f))
                    if (fuente.enlace.isNotEmpty()) {
                        BotonSuave(
                            stringResource(R.string.biblio_abrir),
                            Icons.Outlined.OpenInNew,
                            BloqueVerde,
                            {
                                val uri = fuente.enlace.let { if (it.startsWith("http")) it else "https://$it" }
                                try {
                                    contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(contexto, sinApp, Toast.LENGTH_SHORT).show()
                                }
                            },
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoFuente(
    fuente: Fuente,
    materias: List<String>,
    colores: Map<String, Long>,
    onConfirmar: (Fuente) -> Mensaje?,
    onCancelar: () -> Unit
) {
    var tipo by remember { mutableStateOf(fuente.tipo) }
    var materia by remember { mutableStateOf(fuente.materia) }
    var autores by remember { mutableStateOf(fuente.autores) }
    var anio by remember { mutableStateOf(fuente.anio?.toString() ?: "") }
    var titulo by remember { mutableStateOf(fuente.titulo) }
    var origen by remember { mutableStateOf(fuente.origen) }
    var paginas by remember { mutableStateOf(fuente.paginas) }
    var enlace by remember { mutableStateOf(fuente.enlace) }
    var comentario by remember { mutableStateOf(fuente.comentario) }
    var error by remember { mutableStateOf<Mensaje?>(null) }
    val anioValido = anio.trim().toIntOrNull()
    val armada = fuente.copy(
        tipo = tipo, materia = materia, autores = autores, anio = anioValido, titulo = titulo,
        origen = origen, enlace = enlace, paginas = if (tipo == TipoFuente.ARTICULO) paginas else "", comentario = comentario
    )

    DialogoApp(
        titulo = stringResource(if (fuente.id == 0) R.string.biblio_nueva else R.string.biblio_editar),
        icono = Icons.Outlined.LocalLibrary,
        textoConfirmar = stringResource(if (fuente.id == 0) R.string.agregar else R.string.guardar),
        onConfirmar = {
            error = if (anio.isNotBlank() && anioValido == null) Mensaje.ERROR_ANIO_INVALIDO else onConfirmar(armada)
        },
        onCancelar = onCancelar
    ) {
        // Tipo de fuente: dos filas de dos.
        TipoFuente.entries.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                fila.forEach { opcion ->
                    BloqueOpcion(iconoFuente(opcion), nombreTipo(opcion), elegido = tipo == opcion, modifier = Modifier.weight(1f)) { tipo = opcion }
                }
            }
        }
        CampoTexto(
            value = titulo,
            onValueChange = { titulo = it; error = null },
            label = { Text(stringResource(R.string.biblio_campo_titulo)) },
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            value = autores,
            onValueChange = { autores = it; error = null },
            label = { Text(stringResource(R.string.biblio_autores)) },
            supportingText = { Text(stringResource(R.string.biblio_autores_ayuda)) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CampoTexto(
                value = anio,
                onValueChange = { anio = it.filter(Char::isDigit).take(4); error = null },
                label = { Text(stringResource(R.string.biblio_anio)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(0.38f)
            )
            CampoTexto(
                value = origen,
                onValueChange = { origen = it },
                label = {
                    Text(
                        stringResource(
                            when (tipo) {
                                TipoFuente.LIBRO -> R.string.biblio_editorial
                                TipoFuente.ARTICULO -> R.string.biblio_revista
                                TipoFuente.WEB -> R.string.biblio_sitio
                                TipoFuente.APUNTE -> R.string.biblio_catedra
                            }
                        )
                    )
                },
                singleLine = true,
                modifier = Modifier.weight(0.62f)
            )
        }
        if (tipo == TipoFuente.ARTICULO) {
            CampoTexto(
                value = paginas,
                onValueChange = { paginas = it },
                label = { Text(stringResource(R.string.biblio_paginas)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        CampoTexto(
            value = enlace,
            onValueChange = { enlace = it },
            label = { Text(stringResource(R.string.biblio_enlace)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        EtiquetaCampo(stringResource(R.string.biblio_materia))
        SugerenciasMateria(materias, colores, elegida = materia) { elegida ->
            // Tocar la elegida la saca (queda en la bibliografía general).
            materia = if (materia.equals(elegida, ignoreCase = true)) "" else elegida
        }
        CampoTexto(
            value = comentario,
            onValueChange = { comentario = it },
            label = { Text(stringResource(R.string.biblio_comentario)) },
            placeholder = { Text(stringResource(R.string.biblio_comentario_ayuda)) },
            modifier = Modifier.fillMaxWidth()
        )
        // Cómo va a quedar la cita, mientras se escribe.
        if (titulo.isNotBlank()) {
            EtiquetaCampo(stringResource(R.string.biblio_vista_previa))
            Text(
                citaConEstilo(citaApa(armada)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        TextoError(error?.let { stringResource(it.texto()) })
    }
}
