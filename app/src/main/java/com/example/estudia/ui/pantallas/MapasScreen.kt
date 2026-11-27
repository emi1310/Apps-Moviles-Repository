package com.example.estudia.ui.pantallas

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.util.guardarImagenParaCompartir
import com.example.estudia.modelo.COLORES_MAPA
import com.example.estudia.modelo.MapaMental
import com.example.estudia.modelo.NodoMapa
import com.example.estudia.modelo.colorDe
import com.example.estudia.ui.componentes.CampoTexto
import com.example.estudia.ui.componentes.superficieVidrio
import com.example.estudia.ui.componentes.DialogoApp
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.EtiquetaCampo
import com.example.estudia.ui.componentes.MenuOpciones
import com.example.estudia.ui.componentes.OpcionMenu
import com.example.estudia.ui.componentes.PantallaDetalle
import com.example.estudia.ui.componentes.PastillaEstado
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
import com.example.estudia.ui.theme.LocalDegradado
import com.example.estudia.util.aTexto
import com.example.estudia.viewmodel.MapaViewModel
import com.example.estudia.viewmodel.MapasViewModel
import com.example.estudia.viewmodel.Mensaje
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

// Colores de las ramas (índices de NodoMapa.color). Cada rama que sale de
// la idea central toma uno por orden.
val PALETA_MAPA = listOf(BloqueVioleta, BloqueAzul, BloqueVerde, BloqueNaranja, Color(0xFFE84393), Color(0xFF00A8A8))

private fun colorIdea(mapa: MapaMental, id: Int): Color? = mapa.colorDe(id)?.let { PALETA_MAPA[it.mod(COLORES_MAPA)] }

// ====================================================================
// Lista de mapas
// ====================================================================

// Pantalla "Mapas mentales": los mapas del usuario, con una vista en
// miniatura. Tocar uno lo abre en el editor.
@Composable
fun MapasScreen(viewModel: MapasViewModel, onVolver: () -> Unit, onAbrir: (Int) -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RecolectarAvisos(viewModel.avisos)
    var creando by remember { mutableStateOf(false) }
    var renombrando by remember { mutableStateOf<MapaMental?>(null) }

    PantallaDetalle(
        titulo = stringResource(R.string.mapas_titulo),
        subtitulo = stringResource(R.string.mapas_subtitulo),
        onVolver = onVolver,
        textoAgregar = stringResource(R.string.mapas_agregar),
        iconoAgregar = Icons.Outlined.Add,
        onAgregar = { creando = true }
    ) {
        if (!estado.cargando && estado.mapas.isEmpty()) {
            item { EstadoVacio(Icons.Outlined.AccountTree, stringResource(R.string.mapas_vacio), stringResource(R.string.mapas_vacio_sugerencia)) }
        }
        items(estado.mapas, key = { it.id }) { mapa ->
            TarjetaMapa(
                mapa = mapa,
                colorMateria = estado.colores[mapa.materia.trim().lowercase()]?.let { Color(it) },
                onAbrir = { onAbrir(mapa.id) },
                onRenombrar = { renombrando = mapa },
                onEliminar = { viewModel.eliminar(mapa) }
            )
        }
    }

    if (creando) {
        DialogoDatosMapa(
            mapa = null,
            materias = estado.materias,
            colores = estado.colores,
            onConfirmar = { titulo, materia -> viewModel.crear(titulo, materia) { id -> onAbrir(id) }.also { if (it == null) creando = false } },
            onCancelar = { creando = false }
        )
    }
    renombrando?.let { mapa ->
        DialogoDatosMapa(
            mapa = mapa,
            materias = (estado.materias + listOf(mapa.materia).filter { it.isNotBlank() }).distinctBy { it.lowercase() },
            colores = estado.colores,
            onConfirmar = { titulo, materia -> viewModel.renombrar(mapa, titulo, materia).also { if (it == null) renombrando = null } },
            onCancelar = { renombrando = null }
        )
    }
}

@Composable
private fun TarjetaMapa(mapa: MapaMental, colorMateria: Color?, onAbrir: () -> Unit, onRenombrar: () -> Unit, onEliminar: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().superficieVidrio(MaterialTheme.shapes.large, MaterialTheme.colorScheme.surfaceContainerHighest).clickable(role = Role.Button, onClick = onAbrir)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            MiniaturaMapa(mapa, Modifier.size(width = 104.dp, height = 84.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(mapa.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    pluralStringResource(R.plurals.mapas_ideas, mapa.cantidadIdeas, mapa.cantidadIdeas) + " · " + mapa.modificado.aTexto(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (mapa.materia.isNotBlank()) PastillaEstado(mapa.materia, colorMateria ?: MaterialTheme.colorScheme.primary)
            }
            MenuOpciones(
                listOf(
                    OpcionMenu(stringResource(R.string.mapas_renombrar), Icons.Outlined.Edit, alTocar = onRenombrar),
                    OpcionMenu(stringResource(R.string.eliminar), Icons.Outlined.Delete, destructiva = true, alTocar = onEliminar)
                ),
                descripcion = stringResource(R.string.opciones_de, mapa.titulo)
            )
        }
    }
}

// El mapa en chiquito: las ramas como líneas y las ideas como puntos.
@Composable
private fun MiniaturaMapa(mapa: MapaMental, modifier: Modifier) {
    val violeta = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(rellenoVidrio())
    ) {
        val minX = mapa.nodos.minOf { it.x } - 40f
        val maxX = mapa.nodos.maxOf { it.x } + 40f
        val minY = mapa.nodos.minOf { it.y } - 30f
        val maxY = mapa.nodos.maxOf { it.y } + 30f
        val escala = min(size.width / (maxX - minX), size.height / (maxY - minY))
        val dx = (size.width - (maxX - minX) * escala) / 2
        val dy = (size.height - (maxY - minY) * escala) / 2
        fun punto(n: NodoMapa) = Offset(dx + (n.x - minX) * escala, dy + (n.y - minY) * escala)
        mapa.nodos.forEach { n ->
            val padre = mapa.nodos.firstOrNull { it.id == n.padreId } ?: return@forEach
            drawLine((colorIdea(mapa, n.id) ?: violeta).copy(alpha = 0.6f), punto(padre), punto(n), strokeWidth = 2.dp.toPx())
        }
        mapa.nodos.forEach { n ->
            val raiz = n.padreId == null
            drawCircle(colorIdea(mapa, n.id) ?: violeta, radius = (if (raiz) 6 else 4).dp.toPx(), center = punto(n))
        }
    }
}

@Composable
private fun DialogoDatosMapa(
    mapa: MapaMental?,
    materias: List<String>,
    colores: Map<String, Long>,
    onConfirmar: (String, String) -> Mensaje?,
    onCancelar: () -> Unit
) {
    var titulo by remember { mutableStateOf(mapa?.titulo ?: "") }
    var materia by remember { mutableStateOf(mapa?.materia ?: "") }
    var error by remember { mutableStateOf<Mensaje?>(null) }
    DialogoApp(
        titulo = stringResource(if (mapa == null) R.string.mapas_nuevo else R.string.mapas_renombrar),
        icono = Icons.Outlined.AccountTree,
        textoConfirmar = stringResource(if (mapa == null) R.string.crear else R.string.guardar),
        onConfirmar = { error = onConfirmar(titulo, materia) },
        onCancelar = onCancelar
    ) {
        CampoTexto(
            value = titulo,
            onValueChange = { titulo = it; error = null },
            label = { Text(stringResource(R.string.mapas_tema_central)) },
            placeholder = { Text(stringResource(R.string.mapas_tema_ejemplo)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (materias.isNotEmpty()) {
            EtiquetaCampo(stringResource(R.string.biblio_materia))
            SugerenciasMateria(materias, colores, elegida = materia) { elegida ->
                materia = if (materia.equals(elegida, ignoreCase = true)) "" else elegida
            }
        }
        TextoError(error?.let { stringResource(it.texto()) })
    }
}

// ====================================================================
// Editor
// ====================================================================

// Editor del mapa mental: un lienzo que se mueve con un dedo y se acerca o
// aleja con dos. Tocar una idea la elige (abajo aparecen sus acciones) y
// arrastrarla la mueve con todo lo que sale de ella.
@Composable
fun MapaEditorScreen(viewModel: MapaViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RecolectarAvisos(viewModel.avisos)
    val mapa = estado.mapa
    val contexto = LocalContext.current
    val densidad = LocalDensity.current.density
    val alcance = rememberCoroutineScope()
    val fondo = MaterialTheme.colorScheme.background
    val capa = rememberGraphicsLayer()
    val tituloCompartir = stringResource(R.string.mapas_compartir)

    var zoom by remember { mutableFloatStateOf(1f) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }   // px, desde el centro
    var tamano by remember { mutableStateOf(IntSize.Zero) }
    var agregandoA by remember { mutableStateOf<Int?>(null) }
    var editando by remember { mutableStateOf<NodoMapa?>(null) }
    var eligiendoColor by remember { mutableStateOf<NodoMapa?>(null) }

    // Acomoda el zoom para que entre todo el mapa.
    fun ajustar() {
        val m = mapa ?: return
        if (tamano.width == 0) return
        val minX = m.nodos.minOf { it.x } - 110f
        val maxX = m.nodos.maxOf { it.x } + 110f
        val minY = m.nodos.minOf { it.y } - 60f
        val maxY = m.nodos.maxOf { it.y } + 60f
        zoom = min(tamano.width / ((maxX - minX) * densidad), tamano.height / ((maxY - minY) * densidad)).coerceIn(ZOOM_MINIMO, 1.4f)
        desplazamiento = Offset(-(minX + maxX) / 2 * densidad * zoom, -(minY + maxY) / 2 * densidad * zoom)
    }

    // Al abrir, que se vea todo el mapa.
    var ajustado by remember { mutableStateOf(false) }
    LaunchedEffect(mapa != null, tamano) {
        if (mapa != null && tamano.width > 0 && !ajustado) {
            ajustar()
            ajustado = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        // Barra de arriba: volver, el título y las acciones.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.volver))
            }
            Text(
                mapa?.titulo ?: "",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { ajustar() }) {
                Icon(Icons.Outlined.CenterFocusStrong, contentDescription = stringResource(R.string.mapas_ajustar))
            }
            IconButton(onClick = {
                val m = mapa ?: return@IconButton
                ajustar()
                alcance.launch {
                    // Se espera un cuadro para que la imagen salga ya ajustada.
                    withFrameNanos { }
                    withFrameNanos { }
                    val imagen = capa.toImageBitmap().asAndroidBitmap()
                    compartirImagen(contexto, imagen, m.titulo, tituloCompartir)
                }
            }) {
                Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.mapas_compartir))
            }
        }

        if (mapa == null) {
            if (!estado.cargando) EstadoVacio(Icons.Outlined.AccountTree, stringResource(R.string.mapas_no_existe), "")
            return@Column
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // El lienzo. Lo que se dibuja se graba en "capa" para poder
            // compartirlo como imagen (con el fondo de la app).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .onSizeChanged { tamano = it }
                    .drawWithContent {
                        capa.record {
                            drawRect(fondo)
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(capa)
                    }
                    .pointerInput(Unit) { detectTapGestures { viewModel.seleccionar(null) } }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroide, mover, escala, _ ->
                            val nuevo = (zoom * escala).coerceIn(ZOOM_MINIMO, ZOOM_MAXIMO)
                            val centro = Offset(size.width / 2f, size.height / 2f)
                            // El punto bajo los dedos queda en el mismo lugar.
                            val relativo = centroide - centro
                            desplazamiento = relativo - (relativo - desplazamiento) * (nuevo / zoom) + mover
                            zoom = nuevo
                        }
                    }
            ) {
                LienzoMapa(
                    mapa = mapa,
                    seleccionada = estado.seleccionada,
                    zoom = zoom,
                    desplazamiento = desplazamiento,
                    onTocar = { viewModel.seleccionar(it) },
                    onEditar = { id -> editando = mapa.nodos.first { it.id == id } },
                    onMover = { id, dx, dy -> viewModel.mover(id, dx / densidad, dy / densidad) },
                    onSoltar = viewModel::terminarArrastre
                )
            }

            // Abajo: las acciones de la idea elegida, o una ayuda.
            BarraAcciones(
                mapa = mapa,
                seleccionada = estado.seleccionada,
                onAgregar = { agregandoA = it },
                onEditar = { id -> editando = mapa.nodos.first { it.id == id } },
                onColor = { id -> eligiendoColor = mapa.nodos.first { it.id == id } },
                onEliminar = viewModel::eliminar,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)
            )
        }
    }

    agregandoA?.let { padreId ->
        DialogoTextoIdea(
            titulo = stringResource(R.string.mapas_nueva_idea),
            textoInicial = "",
            onConfirmar = { texto ->
                viewModel.agregarIdea(padreId, texto)
                agregandoA = null
            },
            onCancelar = { agregandoA = null }
        )
    }
    editando?.let { nodo ->
        DialogoTextoIdea(
            titulo = stringResource(R.string.mapas_editar_idea),
            textoInicial = nodo.texto,
            onConfirmar = { texto ->
                viewModel.editarTexto(nodo.id, texto)
                editando = null
            },
            onCancelar = { editando = null }
        )
    }
    eligiendoColor?.let { nodo ->
        DialogoColorIdea(
            actual = nodo.color,
            onElegir = { color ->
                viewModel.cambiarColor(nodo.id, color)
                eligiendoColor = null
            },
            onCancelar = { eligiendoColor = null }
        )
    }
}

private const val ZOOM_MINIMO = 0.3f
private const val ZOOM_MAXIMO = 2.5f

// Las ramas (curvas) y las ideas, ubicadas según el zoom y el desplazamiento.
// Cada idea se dibuja en su tamaño y se agranda o achica con graphicsLayer,
// así el texto se ve nítido y los toques caen donde corresponde.
@Composable
private fun LienzoMapa(
    mapa: MapaMental,
    seleccionada: Int?,
    zoom: Float,
    desplazamiento: Offset,
    onTocar: (Int) -> Unit,
    onEditar: (Int) -> Unit,
    onMover: (Int, Float, Float) -> Unit,
    onSoltar: () -> Unit
) {
    val violeta = MaterialTheme.colorScheme.primary
    val densidad = LocalDensity.current.density
    fun enPantalla(n: NodoMapa, centro: Offset) = centro + desplazamiento + Offset(n.x * densidad * zoom, n.y * densidad * zoom)

    // Ramas: curvas suaves de la idea madre a cada idea.
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centro = Offset(size.width / 2, size.height / 2)
        mapa.nodos.forEach { n ->
            val padre = mapa.nodos.firstOrNull { it.id == n.padreId } ?: return@forEach
            val desde = enPantalla(padre, centro)
            val hasta = enPantalla(n, centro)
            val medio = (desde.x + hasta.x) / 2
            val camino = Path().apply {
                moveTo(desde.x, desde.y)
                cubicTo(medio, desde.y, medio, hasta.y, hasta.x, hasta.y)
            }
            val grosor = (if (padre.padreId == null) 4.dp else 2.5.dp).toPx() * zoom
            drawPath(camino, (colorIdea(mapa, n.id) ?: violeta).copy(alpha = 0.75f), style = Stroke(width = grosor.coerceAtLeast(1.5f)))
        }
    }

    Layout(
        content = {
            mapa.nodos.forEach { n ->
                key(n.id) {
                    IdeaMapa(
                        nodo = n,
                        color = colorIdea(mapa, n.id),
                        elegida = n.id == seleccionada,
                        modifier = Modifier
                            .graphicsLayer { scaleX = zoom; scaleY = zoom }
                            .pointerInput(n.id) {
                                detectTapGestures(onTap = { onTocar(n.id) }, onDoubleTap = { onEditar(n.id) })
                            }
                            .pointerInput(n.id) {
                                detectDragGestures(
                                    onDragStart = { onTocar(n.id) },
                                    onDragEnd = onSoltar,
                                    onDragCancel = onSoltar
                                ) { cambio, arrastre ->
                                    cambio.consume()
                                    onMover(n.id, arrastre.x, arrastre.y)
                                }
                            }
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { medibles, restricciones ->
        val sueltas = Constraints(maxWidth = (200 * densidad).toInt())
        val medidas = medibles.map { it.measure(sueltas) }
        layout(restricciones.maxWidth, restricciones.maxHeight) {
            val centro = Offset(restricciones.maxWidth / 2f, restricciones.maxHeight / 2f)
            medidas.forEachIndexed { i, medida ->
                val p = enPantalla(mapa.nodos[i], centro)
                medida.place((p.x - medida.width / 2f).toInt(), (p.y - medida.height / 2f).toInt())
            }
        }
    }
}

// Una idea: la central con el degradado violeta; las demás en vidrio con
// el color de su rama. La elegida lleva un borde más marcado.
@Composable
private fun IdeaMapa(nodo: NodoMapa, color: Color?, elegida: Boolean, modifier: Modifier) {
    val forma = RoundedCornerShape(if (nodo.padreId == null) 22.dp else 16.dp)
    val central = nodo.padreId == null
    val acento = color ?: MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .clip(forma)
            .then(
                if (central && color == null) Modifier.background(LocalDegradado.current)
                else if (central) Modifier.background(acento)
                else Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)).background(acento.copy(alpha = 0.16f))
            )
            .border(
                if (elegida) 2.5.dp else 1.dp,
                if (elegida) (if (central) Color.White else acento) else acento.copy(alpha = if (central) 0f else 0.55f),
                forma
            )
            .padding(horizontal = if (central) 18.dp else 14.dp, vertical = if (central) 14.dp else 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            nodo.texto,
            style = if (central) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (central) FontWeight.Bold else FontWeight.Medium,
            color = if (central) Color.White else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BarraAcciones(
    mapa: MapaMental,
    seleccionada: Int?,
    onAgregar: (Int) -> Unit,
    onEditar: (Int) -> Unit,
    onColor: (Int) -> Unit,
    onEliminar: (Int) -> Unit,
    modifier: Modifier
) {
    val forma = RoundedCornerShape(28.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .shadow(12.dp, forma)
            .clip(forma)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f))
            .border(bordeVidrio(), forma)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        if (seleccionada == null) {
            Text(
                stringResource(R.string.mapas_ayuda_tocar),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false).padding(horizontal = 12.dp)
            )
            AccionMapa(Icons.Outlined.Add, stringResource(R.string.mapas_nueva_idea)) { onAgregar(mapa.raiz.id) }
        } else {
            AccionMapa(Icons.Outlined.Add, stringResource(R.string.mapas_nueva_idea)) { onAgregar(seleccionada) }
            AccionMapa(Icons.Outlined.Edit, stringResource(R.string.editar)) { onEditar(seleccionada) }
            AccionMapa(Icons.Outlined.Palette, stringResource(R.string.mapas_color)) { onColor(seleccionada) }
            if (seleccionada != mapa.raiz.id) {
                AccionMapa(Icons.Outlined.Delete, stringResource(R.string.eliminar), destructiva = true) { onEliminar(seleccionada) }
            }
        }
    }
}

@Composable
private fun AccionMapa(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String, destructiva: Boolean = false, alTocar: () -> Unit) {
    val color = if (destructiva) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = alTocar)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(texto, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}

@Composable
private fun DialogoTextoIdea(titulo: String, textoInicial: String, onConfirmar: (String) -> Unit, onCancelar: () -> Unit) {
    var texto by remember { mutableStateOf(textoInicial) }
    var error by remember { mutableStateOf(false) }
    DialogoApp(
        titulo = titulo,
        icono = Icons.Outlined.AccountTree,
        textoConfirmar = stringResource(R.string.guardar),
        onConfirmar = { if (texto.isBlank()) error = true else onConfirmar(texto) },
        onCancelar = onCancelar
    ) {
        CampoTexto(
            value = texto,
            onValueChange = { texto = it; error = false },
            label = { Text(stringResource(R.string.mapas_texto_idea)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        TextoError(if (error) stringResource(R.string.error_completar_campos) else null)
    }
}

// Colores para una idea: los de la paleta o "Automático" (el de su rama).
@Composable
private fun DialogoColorIdea(actual: Int?, onElegir: (Int?) -> Unit, onCancelar: () -> Unit) {
    DialogoApp(
        titulo = stringResource(R.string.mapas_color),
        icono = Icons.Outlined.Palette,
        textoConfirmar = null,
        onConfirmar = {},
        onCancelar = onCancelar
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PALETA_MAPA.forEachIndexed { i, color ->
                val descripcion = stringResource(R.string.mapas_color_numero, i + 1)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(color)
                        .clickable(role = Role.RadioButton) { onElegir(i) }
                        .semantics { contentDescription = descripcion }
                ) {
                    if (actual == i) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White)
                }
            }
        }
        TextButton(onClick = { onElegir(null) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.mapas_color_automatico))
        }
    }
}

// Guarda la imagen (util/Imagenes.kt) y abre "Compartir" (WhatsApp, Drive...).
private suspend fun compartirImagen(contexto: Context, imagen: Bitmap, titulo: String, tituloCompartir: String) {
    val uri = guardarImagenParaCompartir(contexto, imagen, titulo)
    val enviar = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    contexto.startActivity(Intent.createChooser(enviar, tituloCompartir))
}
