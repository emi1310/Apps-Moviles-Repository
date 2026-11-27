package com.example.estudia.ui.pantallas

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.estudia.modelo.ExamenProximo
import com.example.estudia.modelo.MAXIMO_NOTAS_SIMULACRO
import com.example.estudia.modelo.SesionPlan
import com.example.estudia.ui.componentes.camaraDeFotos
import com.example.estudia.viewmodel.PlanPropuesto
import com.example.estudia.viewmodel.ClaseDelHorario
import com.example.estudia.viewmodel.PdfDisponible
import com.example.estudia.modelo.AccionPropuesta
import com.example.estudia.modelo.EstadoAccion
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.ConversacionGuardada
import com.example.estudia.modelo.ResultadoSimulacro
import com.example.estudia.modelo.EvaluacionSimulacro
import com.example.estudia.modelo.SesionAtrasada
import com.example.estudia.modelo.MAXIMO_FOTOS_APUNTE
import com.example.estudia.modelo.textoPlano
import com.example.estudia.ui.componentes.TextoConFormato
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.ui.res.stringArrayResource
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.modelo.AccionIA
import com.example.estudia.modelo.ErrorIA
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.MensajeIA
import com.example.estudia.modelo.TarjetaSugerida
import com.example.estudia.ui.componentes.CampoTexto
import com.example.estudia.ui.componentes.superficieVidrio
import com.example.estudia.ui.componentes.abrirGemini
import com.example.estudia.ui.componentes.TecladoAjustable
import com.example.estudia.ui.componentes.DialogoApp
import com.example.estudia.ui.componentes.EncabezadoPantalla
import com.example.estudia.ui.componentes.EtiquetaCampo
import com.example.estudia.ui.componentes.IconoDegradado
import com.example.estudia.ui.componentes.LocalEspacioDock
import com.example.estudia.ui.componentes.RecolectarAvisos
import com.example.estudia.ui.componentes.SelectorMateria
import com.example.estudia.ui.componentes.TarjetaAcceso
import com.example.estudia.ui.componentes.bordeVidrio
import com.example.estudia.ui.componentes.rellenoVidrio
import com.example.estudia.ui.theme.BloqueAzul
import com.example.estudia.ui.theme.BloqueNaranja
import com.example.estudia.ui.theme.BloqueVerde
import com.example.estudia.ui.theme.BloqueVioleta
import com.example.estudia.ui.theme.LocalDegradado
import com.example.estudia.viewmodel.AsistenteViewModel
import com.example.estudia.viewmodel.NotaConMateria
import com.example.estudia.viewmodel.TarjetasPropuestas

// Pantalla "Asistente IA" (quinta sección del dock): una charla con Gemini
// para estudiar con tu propio material. Acciones rápidas: resumir una
// nota, explicar algo paso a paso, preguntas para autoevaluarte y crear
// tarjetas de repaso. La IA ayuda a entender, no hace el trabajo por vos.
@Composable
fun AsistenteScreen(
    viewModel: AsistenteViewModel,
    onAbrirMapa: (Int) -> Unit = {},
    // true cuando se llega desde la guía de primera vez ("Desde una foto").
    abrirHorario: Boolean = false,
    onHorarioAbierto: () -> Unit = {}
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RecolectarAvisos(viewModel.avisos)
    val lista = rememberLazyListState()
    var texto by rememberSaveable { mutableStateOf("") }
    var eligiendoNota by remember { mutableStateOf<AccionIA?>(null) }
    var explicando by remember { mutableStateOf(false) }
    var guardandoRespuesta by remember { mutableStateOf<String?>(null) }
    var conectando by remember { mutableStateOf(false) }
    var eligiendoExamen by remember { mutableStateOf(false) }
    var eligiendoFoto by remember { mutableStateOf(false) }
    // Para qué es la foto: un apunte (FOTO) o el horario de cursada (HORARIO).
    // rememberSaveable: la cámara puede cerrar la app mientras está abierta.
    var fotoPara by rememberSaveable { mutableStateOf(AccionIA.FOTO) }
    var eligiendoSimulacro by remember { mutableStateOf(false) }
    var viendoHistorial by remember { mutableStateOf(false) }
    var eligiendoPdf by remember { mutableStateOf(false) }
    var revisandoPlan by remember { mutableStateOf(false) }
    // Las hojas del apunte que se van juntando (fotos de la cámara o de la galería).
    var hojas by rememberSaveable { mutableStateOf(listOf<String>()) }

    val contexto = LocalContext.current
    val textoResumir = stringResource(R.string.ia_resumir)
    val textoPreguntas = stringResource(R.string.ia_preguntas)
    val textoFoto = stringResource(R.string.ia_foto_etiqueta)
    val textoPdf = stringResource(R.string.ia_pdf_etiqueta)
    val prefijoPlan = stringResource(R.string.ia_plan_prefijo)
    fun accion(a: AccionIA) {
        when (a) {
            AccionIA.EXPLICAR -> explicando = true
            AccionIA.PLAN -> eligiendoExamen = true
            AccionIA.FOTO, AccionIA.HORARIO -> { fotoPara = a; hojas = emptyList(); eligiendoFoto = true }
            AccionIA.SIMULACRO -> eligiendoSimulacro = true
            AccionIA.PDF -> eligiendoPdf = true
            else -> eligiendoNota = a
        }
    }

    // Fotos: el horario se lee apenas llega; las hojas de un apunte se juntan
    // hasta tocar "Pasar a texto".
    fun usarFoto(uri: String) {
        if (fotoPara == AccionIA.HORARIO) viewModel.leerHorario(uri)
        else {
            hojas = (hojas + uri).take(MAXIMO_FOTOS_APUNTE)
            eligiendoFoto = true
        }
    }
    val sacarFoto = camaraDeFotos { uri -> usarFoto(uri) }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) usarFoto(uri.toString())
    }
    val galeriaVarias = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAXIMO_FOTOS_APUNTE)) { uris ->
        if (uris.isNotEmpty()) {
            hojas = (hojas + uris.map { it.toString() }).take(MAXIMO_FOTOS_APUNTE)
        }
        eligiendoFoto = true
    }
    // Un PDF del teléfono (los de las materias se eligen de la lista).
    val pdfDelTelefono = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val nombre = nombreDeArchivo(contexto, uri) ?: "documento.pdf"
            viewModel.preguntarPdf(uri.toString(), nombre, null, "$textoPdf: $nombre")
        }
    }
    LaunchedEffect(abrirHorario) {
        if (abrirHorario) {
            accion(AccionIA.HORARIO)
            onHorarioAbierto()
        }
    }

    // Un mapa recién creado desde una nota se abre en el editor.
    LaunchedEffect(estado.mapaCreado) {
        estado.mapaCreado?.let { id ->
            viewModel.mapaAbierto()
            onAbrirMapa(id)
        }
    }

    // Siempre se ve lo último de la charla (también mientras la respuesta se escribe).
    val cantidad = estado.conversacion.size + (if (estado.pensando) 1 else 0) + (if (estado.error != null) 1 else 0)
    val largoUltima = (estado.conversacion.lastOrNull()?.texto?.length ?: 0) / 80
    LaunchedEffect(cantidad, largoUltima) { if (cantidad > 0) lista.animateScrollToItem(lista.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1) }

    // Con el teclado abierto, la barra de escribir va arriba del teclado;
    // si no, arriba del dock. (La ventana no se desplaza: lo acomoda la pantalla.)
    TecladoAjustable()
    val teclado = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val abajo = maxOf(teclado, LocalEspacioDock.current) + 8.dp

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lista,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                EncabezadoPantalla(stringResource(R.string.ia_titulo), stringResource(R.string.ia_subtitulo)) {
                    // Charlas guardadas y simulacros.
                    IconButton(onClick = { viendoHistorial = true }, enabled = !estado.pensando) {
                        Icon(Icons.Outlined.History, contentDescription = stringResource(R.string.ia_historial))
                    }
                    if (estado.conversacion.isNotEmpty()) {
                        IconButton(onClick = viewModel::nuevaConversacion, enabled = !estado.pensando) {
                            Icon(Icons.Outlined.RestartAlt, contentDescription = stringResource(R.string.ia_nueva_charla))
                        }
                    }
                    // Conectar (o cambiar) la IA de este usuario: la llave se
                    // pinta de violeta cuando ya conectó la suya.
                    IconButton(onClick = { conectando = true }) {
                        Icon(
                            Icons.Outlined.Key,
                            contentDescription = stringResource(R.string.ia_conectar_titulo),
                            tint = if (estado.clavePropia) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (!estado.configurado) item { AvisoSinClave(onConectar = { conectando = true }) }
            // Sesiones del plan que ya pasaron: ¿se hicieron?
            if (estado.sesionesAtrasadas.isNotEmpty() && !estado.pensando) {
                item { TarjetaPlanAtrasado(estado.sesionesAtrasadas.size, onRevisar = { revisandoPlan = true }) }
            }
            if (estado.conversacion.isEmpty()) {
                item { Bienvenida(onAccion = ::accion) }
            }
            itemsIndexed(estado.conversacion) { i, mensaje ->
                // La burbuja de la respuesta aparece cuando llega lo primero.
                if (mensaje.deUsuario || mensaje.texto.isNotEmpty() || mensaje.accion != null) {
                    Burbuja(
                        mensaje,
                        escribiendo = estado.pensando && i == estado.conversacion.lastIndex,
                        onGuardar = { guardandoRespuesta = mensaje.texto },
                        onConfirmar = { viewModel.confirmarAccion(i) },
                        onDescartar = { viewModel.descartarAccion(i) }
                    )
                }
            }
            if (estado.pensando && !estado.escribiendo) item { Pensando() }
            estado.error?.let { error -> item { TarjetaError(error, onReintentar = viewModel::reintentar) } }
        }

        // Simulacro en curso: terminarlo para ver la nota y guardar lo que costó.
        if (estado.simulacroEnCurso) {
            BarraSimulacro(habilitado = !estado.pensando, onTerminar = viewModel::terminarSimulacro)
        }
        // Acciones rápidas (cuando ya hay charla, arriba de donde se escribe).
        if (estado.conversacion.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                accionesIA().forEach { (a, nombre, icono) -> PastillaAccion(nombre, icono, enabled = !estado.pensando) { accion(a) } }
            }
        }
        BarraEscribir(
            texto = texto,
            onCambiar = { texto = it },
            habilitado = !estado.pensando,
            respondiendo = estado.escribiendo,
            onDetener = viewModel::detener,
            onEnviar = {
                viewModel.enviar(texto)
                texto = ""
            },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = abajo)
        )
    }

    eligiendoNota?.let { accionElegida ->
        DialogoElegirNota(
            notas = estado.notas,
            onElegir = { nota ->
                when (accionElegida) {
                    AccionIA.RESUMIR -> viewModel.resumir(nota, "$textoResumir: ${nota.nota.titulo}")
                    AccionIA.PREGUNTAS -> viewModel.preguntas(nota, "$textoPreguntas: ${nota.nota.titulo}")
                    AccionIA.MAPA -> viewModel.crearMapa(nota)
                    else -> viewModel.pedirTarjetas(nota)
                }
                eligiendoNota = null
            },
            onCancelar = { eligiendoNota = null }
        )
    }
    if (explicando) {
        DialogoExplicar(
            onConfirmar = { tema -> viewModel.explicar(tema); explicando = false },
            onCancelar = { explicando = false }
        )
    }
    estado.tarjetasPropuestas?.let { propuestas ->
        DialogoTarjetas(propuestas, onGuardar = viewModel::guardarTarjetas, onCancelar = viewModel::descartarTarjetas)
    }
    if (eligiendoExamen) {
        DialogoElegirExamen(
            examenes = estado.examenes,
            onElegir = { viewModel.pedirPlan(it); eligiendoExamen = false },
            onCancelar = { eligiendoExamen = false }
        )
    }
    estado.planPropuesto?.let { plan ->
        DialogoPlan(plan, onGuardar = { viewModel.guardarPlan(it, prefijoPlan) }, onCancelar = viewModel::descartarPlan)
    }
    estado.horarioPropuesto?.let { clases ->
        DialogoHorario(clases, onGuardar = viewModel::guardarHorario, onCancelar = viewModel::descartarHorario)
    }
    if (eligiendoFoto) {
        val horario = fotoPara == AccionIA.HORARIO
        DialogoFoto(
            horario = horario,
            hojas = hojas.size,
            onCamara = { eligiendoFoto = false; sacarFoto() },
            onGaleria = {
                eligiendoFoto = false
                val soloImagenes = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                if (horario) galeria.launch(soloImagenes) else galeriaVarias.launch(soloImagenes)
            },
            onListo = {
                eligiendoFoto = false
                viewModel.leerApunte(hojas, if (hojas.size > 1) "$textoFoto (${hojas.size})" else textoFoto)
                hojas = emptyList()
            },
            onCancelar = { eligiendoFoto = false; hojas = emptyList() }
        )
    }
    if (eligiendoPdf) {
        DialogoPdf(
            pdfs = estado.pdfs,
            onElegir = { pdf ->
                eligiendoPdf = false
                viewModel.preguntarPdf(pdf.uri, pdf.archivo.nombre, pdf.nombreMateria, "$textoPdf: ${pdf.archivo.nombre}")
            },
            onDelTelefono = {
                eligiendoPdf = false
                pdfDelTelefono.launch(arrayOf("application/pdf"))
            },
            onCancelar = { eligiendoPdf = false }
        )
    }
    if (viendoHistorial) {
        DialogoHistorial(
            conversaciones = estado.historial,
            simulacros = estado.simulacros,
            actual = estado.conversacionId,
            onAbrir = { viewModel.abrirConversacion(it); viendoHistorial = false },
            onBorrar = { viewModel.borrarConversacion(it.id) },
            onCerrar = { viendoHistorial = false }
        )
    }
    estado.resultadoSimulacro?.let { resultado ->
        DialogoResultadoSimulacro(resultado, onCerrar = viewModel::cerrarResultadoSimulacro)
    }
    if (revisandoPlan && estado.sesionesAtrasadas.isNotEmpty()) {
        DialogoReacomodar(
            sesiones = estado.sesionesAtrasadas,
            onReacomodar = { noHechas -> viewModel.reacomodarPlan(noHechas); revisandoPlan = false },
            onCancelar = { revisandoPlan = false }
        )
    }
    if (eligiendoSimulacro) {
        val textoSimulacro = stringResource(R.string.ia_simulacro)
        DialogoSimulacro(
            notas = estado.notas,
            onEmpezar = { elegidas ->
                viewModel.simulacro(elegidas, "$textoSimulacro: ${elegidas.joinToString(", ") { it.nota.titulo }}")
                eligiendoSimulacro = false
            },
            onCancelar = { eligiendoSimulacro = false }
        )
    }
    if (conectando) {
        DialogoConectarIA(
            clavePropia = estado.clavePropia,
            probando = estado.probandoClave,
            error = estado.errorClave,
            onConectar = { clave -> viewModel.conectarClave(clave) { conectando = false } },
            onCambiar = viewModel::limpiarErrorClave,
            onQuitar = {
                viewModel.quitarClave()
                conectando = false
            },
            onCancelar = {
                viewModel.limpiarErrorClave()
                conectando = false
            }
        )
    }
    guardandoRespuesta?.let { respuesta ->
        DialogoGuardarNota(
            materias = estado.materias,
            onGuardar = { materia, titulo ->
                viewModel.guardarComoNota(materia.id, titulo, respuesta)
                guardandoRespuesta = null
            },
            onCancelar = { guardandoRespuesta = null }
        )
    }
}

@Composable
private fun accionesIA(): List<Triple<AccionIA, String, ImageVector>> = listOf(
    Triple(AccionIA.RESUMIR, stringResource(R.string.ia_resumir), Icons.Outlined.Summarize),
    Triple(AccionIA.EXPLICAR, stringResource(R.string.ia_explicar), Icons.Outlined.Lightbulb),
    Triple(AccionIA.PREGUNTAS, stringResource(R.string.ia_preguntas), Icons.Outlined.Quiz),
    Triple(AccionIA.TARJETAS, stringResource(R.string.ia_tarjetas), Icons.Outlined.Style),
    Triple(AccionIA.PLAN, stringResource(R.string.ia_plan), Icons.Outlined.EventNote),
    Triple(AccionIA.MAPA, stringResource(R.string.ia_mapa), Icons.Outlined.AccountTree),
    Triple(AccionIA.FOTO, stringResource(R.string.ia_foto), Icons.Outlined.DocumentScanner),
    Triple(AccionIA.SIMULACRO, stringResource(R.string.ia_simulacro), Icons.Outlined.Assignment),
    Triple(AccionIA.HORARIO, stringResource(R.string.ia_horario), Icons.Outlined.CalendarViewWeek),
    Triple(AccionIA.PDF, stringResource(R.string.ia_pdf), Icons.Outlined.PictureAsPdf)
)

// Primera vez: las acciones como tarjetas y el aviso de privacidad.
@Composable
private fun Bienvenida(onAccion: (AccionIA) -> Unit) {
    val colores = listOf(BloqueVioleta, BloqueNaranja, BloqueAzul, BloqueVerde, BloqueNaranja, BloqueVioleta, BloqueVerde, BloqueAzul, BloqueVioleta, BloqueNaranja)
    val detalles = listOf(
        R.string.ia_resumir_detalle, R.string.ia_explicar_detalle, R.string.ia_preguntas_detalle, R.string.ia_tarjetas_detalle,
        R.string.ia_plan_detalle, R.string.ia_mapa_detalle, R.string.ia_foto_detalle, R.string.ia_simulacro_detalle,
        R.string.ia_horario_detalle, R.string.ia_pdf_detalle
    )
    val acciones = accionesIA()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        acciones.chunked(2).forEachIndexed { fila, par ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                par.forEachIndexed { columna, (accion, nombre, icono) ->
                    val i = fila * 2 + columna
                    TarjetaAcceso(nombre, stringResource(detalles[i]), icono, colores[i], Modifier.weight(1f).fillMaxHeight(), alTocar = { onAccion(accion) })
                }
            }
        }
        Text(
            stringResource(R.string.ia_privacidad),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
    }
}

// Sin clave: invita a conectar la IA propia (gratis, con la cuenta de Google).
@Composable
private fun AvisoSinClave(onConectar: () -> Unit) {
    val forma = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(forma)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconoDegradado(Icons.Outlined.Key, BloqueVioleta, tamano = 36.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(R.string.ia_sin_clave), style = MaterialTheme.typography.titleSmall)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.ia_sin_clave_detalle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(LocalDegradado.current)
                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onConectar)
                .padding(vertical = 12.dp)
        ) {
            Text(stringResource(R.string.ia_conectar), color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
        // Sin conectar nada: abrir la app de Gemini (lo mismo que el + del dock).
        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.ia_sin_clave_gemini), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        val contexto = LocalContext.current
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                .clickable(role = androidx.compose.ui.semantics.Role.Button) { abrirGemini(contexto) }
                .padding(vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.example.estudia.ui.componentes.IconoGemini(18.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.abrir_gemini), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// Conectar la IA propia: los pasos para sacar la clave gratis en Google AI
// Studio, el campo para pegarla y "Probar y conectar" (hace un pedido
// mínimo: si la clave no anda, lo dice acá mismo). Si ya hay una conectada,
// se puede cambiar o desconectar.
@Composable
private fun DialogoConectarIA(
    clavePropia: Boolean,
    probando: Boolean,
    error: ErrorIA?,
    onConectar: (String) -> Unit,
    onCambiar: () -> Unit,
    onQuitar: () -> Unit,
    onCancelar: () -> Unit
) {
    val contexto = LocalContext.current
    val sinNavegador = stringResource(R.string.evento_enlace_error)
    var clave by remember { mutableStateOf("") }
    var verClave by remember { mutableStateOf(false) }
    DialogoApp(
        titulo = stringResource(R.string.ia_conectar_titulo),
        icono = Icons.Outlined.Key,
        textoConfirmar = stringResource(if (probando) R.string.ia_probando else R.string.ia_probar_conectar),
        onConfirmar = { if (!probando && clave.isNotBlank()) onConectar(clave) },
        onCancelar = onCancelar
    ) {
        if (clavePropia) {
            Text(stringResource(R.string.ia_ya_conectada), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Text(stringResource(R.string.ia_pasos_clave), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(
            onClick = {
                try {
                    contexto.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(URL_CLAVE_GEMINI)))
                } catch (e: android.content.ActivityNotFoundException) {
                    Toast.makeText(contexto, sinNavegador, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.ia_abrir_ai_studio))
        }
        CampoTexto(
            value = clave,
            onValueChange = { clave = it.trim(); onCambiar() },
            label = { Text(stringResource(R.string.ia_pegar_clave)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
            visualTransformation = if (verClave) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { verClave = !verClave }) {
                    Text(stringResource(if (verClave) R.string.ia_ocultar else R.string.ia_ver))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (probando) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        error?.let {
            Text(
                stringResource(
                    when (it) {
                        ErrorIA.CLAVE_INVALIDA, ErrorIA.SIN_CLAVE -> R.string.ia_clave_no_anda
                        ErrorIA.SIN_INTERNET -> R.string.ia_error_internet
                        else -> R.string.ia_error_otro
                    }
                ),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(stringResource(R.string.ia_clave_privacidad), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (clavePropia) {
            TextButton(onClick = onQuitar, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ia_desconectar), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private const val URL_CLAVE_GEMINI = "https://aistudio.google.com/apikey"

// Un mensaje: los tuyos a la derecha con el degradado violeta; los de la
// IA a la izquierda, en vidrio, con "Copiar" y "Guardar como nota".
@Composable
private fun Burbuja(
    mensaje: MensajeIA,
    escribiendo: Boolean,
    onGuardar: () -> Unit,
    onConfirmar: () -> Unit,
    onDescartar: () -> Unit
) {
    val contexto = LocalContext.current
    val copiada = stringResource(R.string.ia_copiada)
    if (mensaje.deUsuario) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .padding(start = 48.dp)
                    .clip(RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp))
                    .background(LocalDegradado.current)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(mensaje.mostrado, color = Color.White, style = MaterialTheme.typography.bodyLarge, maxLines = 8, overflow = TextOverflow.Ellipsis)
                // Las fotos o el PDF que van con el mensaje.
                if (mensaje.adjuntos.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(
                            if (mensaje.adjuntos.first().tipoMime == "application/pdf") Icons.Outlined.PictureAsPdf else Icons.Outlined.Image,
                            contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            pluralStringResource(R.plurals.ia_adjuntos, mensaje.adjuntos.size, mensaje.adjuntos.size),
                            style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    } else {
        val forma = RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)
        Column(
            modifier = Modifier
                .padding(end = 24.dp)
                .superficieVidrio(forma)
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.ia_firma), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (mensaje.texto.isNotEmpty()) {
                SelectionContainer {
                    TextoConFormato(mensaje.texto, MaterialTheme.typography.bodyLarge, Modifier.padding(end = 8.dp), escribiendo = escribiendo)
                }
            }
            // Las notas del estudiante que se usaron para responder.
            if (mensaje.fuentes.isNotEmpty() && !escribiendo && mensaje.texto.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, end = 8.dp)) {
                    Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.ia_fuentes, mensaje.fuentes.joinToString(", ")),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            mensaje.accion?.let { accion ->
                TarjetaAccion(accion, mensaje.estadoAccion, onConfirmar, onDescartar, Modifier.padding(top = 8.dp, end = 8.dp, bottom = 8.dp))
            }
            if (mensaje.texto.isNotEmpty() && !escribiendo) {
                Row {
                    TextButton(onClick = {
                        contexto.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("respuesta", textoPlano(mensaje.texto)))
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(contexto, copiada, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.ia_copiar))
                    }
                    TextButton(onClick = onGuardar) {
                        Icon(Icons.Outlined.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.ia_guardar_nota))
                    }
                }
            }
        }
    }
}

// Lo que propone la IA (crear una tarea, agendar algo, marcar un tema), con
// Confirmar y Descartar. Después muestra cómo quedó.
@Composable
private fun TarjetaAccion(accion: AccionPropuesta, estado: EstadoAccion, onConfirmar: () -> Unit, onDescartar: () -> Unit, modifier: Modifier) {
    val formato = remember { DateTimeFormatter.ofPattern("EEE d/M", Locale.forLanguageTag("es-AR")) }
    val tipos = mapOf(
        TipoEvento.EXAMEN to stringResource(R.string.ia_accion_examen),
        TipoEvento.ENTREGA to stringResource(R.string.ia_accion_entrega),
        TipoEvento.CLASE to stringResource(R.string.ia_accion_clase),
        TipoEvento.BLOQUE_ESTUDIO to stringResource(R.string.ia_accion_bloque)
    )
    val (icono, titulo, detalle) = when (accion) {
        is AccionPropuesta.CrearTarea -> Triple(
            Icons.Outlined.TaskAlt,
            stringResource(R.string.ia_accion_tarea, accion.titulo),
            accion.materia + (accion.fechaLimite?.let { " · " + stringResource(R.string.ia_accion_vence, it.format(formato)) } ?: "")
        )
        is AccionPropuesta.CrearEvento -> Triple(
            Icons.Outlined.Event,
            "${tipos[accion.tipo].orEmpty()}: ${accion.titulo}",
            "${accion.materia} · ${accion.fecha.format(formato)}, ${accion.inicio} a ${accion.fin}"
        )
        is AccionPropuesta.MarcarTema -> Triple(
            Icons.Outlined.CheckCircle,
            stringResource(R.string.ia_accion_tema, accion.tema),
            "${accion.materia} · ${accion.examen}"
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        when (estado) {
            EstadoAccion.PENDIENTE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onConfirmar) { Text(stringResource(R.string.ia_accion_confirmar)) }
                TextButton(onClick = onDescartar) { Text(stringResource(R.string.ia_accion_descartar)) }
            }
            else -> Text(
                stringResource(
                    when (estado) {
                        EstadoAccion.HECHA -> R.string.ia_accion_hecha
                        EstadoAccion.DESCARTADA -> R.string.ia_accion_descartada
                        else -> R.string.ia_accion_fallo
                    }
                ),
                style = MaterialTheme.typography.labelMedium,
                color = if (estado == EstadoAccion.FALLO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun Pensando() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(rellenoVidrio())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(stringResource(R.string.ia_pensando), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TarjetaError(error: ErrorIA, onReintentar: () -> Unit) {
    val texto = stringResource(
        when (error) {
            ErrorIA.SIN_CLAVE -> R.string.ia_error_sin_clave
            ErrorIA.CLAVE_INVALIDA -> R.string.ia_error_clave
            ErrorIA.SIN_INTERNET -> R.string.ia_error_internet
            ErrorIA.LIMITE -> R.string.ia_error_limite
            ErrorIA.BLOQUEADO -> R.string.ia_error_bloqueado
            ErrorIA.ARCHIVO_GRANDE -> R.string.ia_error_archivo_grande
            ErrorIA.OTRO -> R.string.ia_error_otro
        }
    )
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
            if (error != ErrorIA.SIN_CLAVE && error != ErrorIA.CLAVE_INVALIDA) {
                TextButton(onClick = onReintentar) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.ia_reintentar))
                }
            }
        }
    }
}

@Composable
private fun PastillaAccion(texto: String, icono: ImageVector, enabled: Boolean, alTocar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .superficieVidrio(RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = alTocar)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun BarraEscribir(
    texto: String,
    onCambiar: (String) -> Unit,
    habilitado: Boolean,
    respondiendo: Boolean,
    onDetener: () -> Unit,
    onEnviar: () -> Unit,
    modifier: Modifier
) {
    Row(verticalAlignment = Alignment.Bottom, modifier = modifier.fillMaxWidth()) {
        TextField(
            value = texto,
            onValueChange = onCambiar,
            placeholder = { Text(stringResource(R.string.ia_escribir)) },
            maxLines = 5,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            shape = RoundedCornerShape(26.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.weight(1f).border(bordeVidrio(), RoundedCornerShape(26.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        // Mientras la respuesta se escribe, el botón la corta.
        val puede = respondiendo || (habilitado && texto.isNotBlank())
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (puede) LocalDegradado.current else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceContainer))
                .clickable(enabled = puede, onClick = if (respondiendo) onDetener else onEnviar)
        ) {
            Icon(
                if (respondiendo) Icons.Outlined.Stop else Icons.AutoMirrored.Outlined.Send,
                contentDescription = stringResource(if (respondiendo) R.string.ia_detener else R.string.ia_enviar),
                tint = if (puede) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Elegir una nota (para resumir, preguntas o tarjetas), la más nueva primero.
@Composable
private fun DialogoElegirNota(notas: List<NotaConMateria>, onElegir: (NotaConMateria) -> Unit, onCancelar: () -> Unit) {
    DialogoApp(
        titulo = stringResource(R.string.ia_elegir_nota),
        icono = Icons.Outlined.AutoAwesome,
        textoConfirmar = null,
        onConfirmar = {},
        onCancelar = onCancelar
    ) {
        if (notas.isEmpty()) {
            Text(stringResource(R.string.ia_sin_notas), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        notas.take(40).forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onElegir(item) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(item.colorMateria)))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(item.nota.titulo, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.nombreMateria, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun DialogoExplicar(onConfirmar: (String) -> Unit, onCancelar: () -> Unit) {
    var tema by remember { mutableStateOf("") }
    DialogoApp(
        titulo = stringResource(R.string.ia_explicar),
        icono = Icons.Outlined.Lightbulb,
        textoConfirmar = stringResource(R.string.ia_enviar),
        onConfirmar = { if (tema.isNotBlank()) onConfirmar(tema) },
        onCancelar = onCancelar
    ) {
        CampoTexto(
            value = tema,
            onValueChange = { tema = it },
            label = { Text(stringResource(R.string.ia_que_entender)) },
            placeholder = { Text(stringResource(R.string.ia_que_entender_ejemplo)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Revisar las tarjetas que propuso la IA: se guardan solo las marcadas.
@Composable
private fun DialogoTarjetas(propuestas: TarjetasPropuestas, onGuardar: (List<TarjetaSugerida>) -> Unit, onCancelar: () -> Unit) {
    val marcadas = remember(propuestas) { mutableStateListOf(*propuestas.tarjetas.map { true }.toTypedArray()) }
    DialogoApp(
        titulo = stringResource(R.string.ia_tarjetas_titulo),
        icono = Icons.Outlined.Style,
        textoConfirmar = stringResource(R.string.ia_tarjetas_guardar, marcadas.count { it }),
        onConfirmar = { onGuardar(propuestas.tarjetas.filterIndexed { i, _ -> marcadas[i] }) },
        onCancelar = onCancelar
    ) {
        Text(
            stringResource(R.string.ia_tarjetas_detalle_dialogo, propuestas.nota.nombreMateria),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        propuestas.tarjetas.forEachIndexed { i, tarjeta ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { marcadas[i] = !marcadas[i] }
                    .padding(end = 12.dp, top = 4.dp, bottom = 10.dp)
            ) {
                Checkbox(checked = marcadas[i], onCheckedChange = { marcadas[i] = it })
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(tarjeta.pregunta, style = MaterialTheme.typography.bodyMedium)
                    Text(tarjeta.respuesta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DialogoGuardarNota(materias: List<Materia>, onGuardar: (Materia, String) -> Unit, onCancelar: () -> Unit) {
    var materia by remember { mutableStateOf<Materia?>(null) }
    var titulo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    DialogoApp(
        titulo = stringResource(R.string.ia_guardar_nota),
        icono = Icons.Outlined.NoteAdd,
        textoConfirmar = stringResource(R.string.guardar),
        onConfirmar = {
            val elegida = materia
            error = when {
                elegida == null -> R.string.error_elegir_materia
                titulo.isBlank() -> R.string.error_titulo_nota
                else -> null
            }
            if (elegida != null && error == null) onGuardar(elegida, titulo)
        },
        onCancelar = onCancelar
    ) {
        EtiquetaCampo(stringResource(R.string.elegir_materia))
        SelectorMateria(materias, materia) { materia = it; error = null }
        CampoTexto(
            value = titulo,
            onValueChange = { titulo = it; error = null },
            label = { Text(stringResource(R.string.titulo)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        com.example.estudia.ui.componentes.TextoError(error?.let { stringResource(it) })
    }
}

// Plan de estudio: elegir el examen o la entrega (los que vienen, el más cercano primero).
@Composable
private fun DialogoElegirExamen(examenes: List<ExamenProximo>, onElegir: (ExamenProximo) -> Unit, onCancelar: () -> Unit) {
    val formato = remember { DateTimeFormatter.ofPattern("EEE d/M", Locale.forLanguageTag("es-AR")) }
    DialogoApp(
        titulo = stringResource(R.string.ia_elegir_examen),
        icono = Icons.Outlined.EventNote,
        textoConfirmar = null,
        onConfirmar = {},
        onCancelar = onCancelar
    ) {
        if (examenes.isEmpty()) {
            Text(stringResource(R.string.ia_sin_examenes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        examenes.take(30).forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onElegir(item) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(item.colorMateria)))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.evento.titulo, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.nombreMateria, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text(item.evento.fecha.format(formato), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// Revisar el plan que propuso la IA: se guardan solo las sesiones marcadas.
@Composable
private fun DialogoPlan(plan: PlanPropuesto, onGuardar: (List<SesionPlan>) -> Unit, onCancelar: () -> Unit) {
    val marcadas = remember(plan) { mutableStateListOf(*plan.sesiones.map { true }.toTypedArray()) }
    val formato = remember { DateTimeFormatter.ofPattern("EEE d/M", Locale.forLanguageTag("es-AR")) }
    DialogoApp(
        titulo = stringResource(R.string.ia_plan_titulo, plan.examen.evento.titulo),
        icono = Icons.Outlined.EventNote,
        textoConfirmar = stringResource(R.string.ia_plan_guardar, marcadas.count { it }),
        onConfirmar = { onGuardar(plan.sesiones.filterIndexed { i, _ -> marcadas[i] }) },
        onCancelar = onCancelar
    ) {
        Text(
            stringResource(R.string.ia_plan_detalle_dialogo, plan.examen.nombreMateria),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        plan.sesiones.forEachIndexed { i, sesion ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { marcadas[i] = !marcadas[i] }
                    .padding(end = 12.dp, top = 4.dp, bottom = 10.dp)
            ) {
                Checkbox(checked = marcadas[i], onCheckedChange = { marcadas[i] = it })
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(sesion.tema, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.ia_plan_fecha, sesion.fecha.format(formato), sesion.hora.toString(), sesion.horaFin.toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Foto de un apunte: sacarla con la cámara o elegirla de la galería.
@Composable
private fun DialogoFoto(horario: Boolean, hojas: Int, onCamara: () -> Unit, onGaleria: () -> Unit, onListo: () -> Unit, onCancelar: () -> Unit) {
    DialogoApp(
        titulo = stringResource(if (horario) R.string.ia_horario_como else R.string.ia_foto_como),
        icono = if (horario) Icons.Outlined.CalendarViewWeek else Icons.Outlined.DocumentScanner,
        // Un apunte puede tener varias hojas: se van sumando y después se pasan juntas.
        textoConfirmar = if (!horario && hojas > 0) stringResource(R.string.ia_foto_listo, hojas) else null,
        onConfirmar = onListo,
        onCancelar = onCancelar
    ) {
        Text(
            stringResource(if (horario) R.string.ia_horario_explicacion else R.string.ia_foto_explicacion),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!horario && hojas > 0) {
            Text(
                pluralStringResource(R.plurals.ia_foto_hojas, hojas, hojas, MAXIMO_FOTOS_APUNTE),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        val puedeMas = horario || hojas < MAXIMO_FOTOS_APUNTE
        listOf(
            Triple(if (hojas > 0) R.string.ia_foto_otra else R.string.ia_foto_camara, Icons.Outlined.PhotoCamera, onCamara),
            Triple(if (horario) R.string.ia_foto_galeria else R.string.ia_foto_galeria_varias, Icons.Outlined.Image, onGaleria)
        ).forEach { (texto, icono, alTocar) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = puedeMas, onClick = alTocar)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(texto), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

// Simulacro: elegir las notas (hasta MAXIMO_NOTAS_SIMULACRO).
@Composable
private fun DialogoSimulacro(notas: List<NotaConMateria>, onEmpezar: (List<NotaConMateria>) -> Unit, onCancelar: () -> Unit) {
    val elegidas = remember { mutableStateListOf<Int>() }
    DialogoApp(
        titulo = stringResource(R.string.ia_simulacro_elegir, MAXIMO_NOTAS_SIMULACRO),
        icono = Icons.Outlined.Assignment,
        textoConfirmar = stringResource(R.string.ia_simulacro_empezar, elegidas.size),
        onConfirmar = { if (elegidas.isNotEmpty()) onEmpezar(notas.filter { it.nota.id in elegidas }) },
        onCancelar = onCancelar
    ) {
        if (notas.isEmpty()) {
            Text(stringResource(R.string.ia_sin_notas), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        notas.take(40).forEach { item ->
            val marcada = item.nota.id in elegidas
            val alternar = {
                if (marcada) elegidas.remove(item.nota.id) else if (elegidas.size < MAXIMO_NOTAS_SIMULACRO) elegidas.add(item.nota.id)
                Unit
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = alternar)
                    .padding(end = 12.dp)
            ) {
                Checkbox(checked = marcada, onCheckedChange = { alternar() })
                Column {
                    Text(item.nota.titulo, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.nombreMateria, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

// Horario leído de la foto: se marcan las clases a guardar. Las materias que
// no están cargadas dicen "nueva" (se crean al guardar).
@Composable
private fun DialogoHorario(clases: List<ClaseDelHorario>, onGuardar: (List<ClaseDelHorario>) -> Unit, onCancelar: () -> Unit) {
    val marcadas = remember(clases) { mutableStateListOf(*clases.map { true }.toTypedArray()) }
    val dias = stringArrayResource(R.array.dias_semana_largo)
    DialogoApp(
        titulo = stringResource(R.string.ia_horario_titulo),
        icono = Icons.Outlined.CalendarViewWeek,
        textoConfirmar = stringResource(R.string.ia_plan_guardar, marcadas.count { it }),
        onConfirmar = { onGuardar(clases.filterIndexed { i, _ -> marcadas[i] }) },
        onCancelar = onCancelar
    ) {
        Text(stringResource(R.string.ia_horario_detalle_dialogo), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        clases.forEachIndexed { i, item ->
            val clase = item.clase
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { marcadas[i] = !marcadas[i] }
                    .padding(end = 12.dp, top = 4.dp, bottom = 10.dp)
            ) {
                Checkbox(checked = marcadas[i], onCheckedChange = { marcadas[i] = it })
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.materia?.nombre ?: clase.materia,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (item.materia == null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.ia_horario_nueva),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        stringResource(
                            R.string.ia_horario_clase,
                            clase.titulo,
                            dias[clase.dia.value - 1],
                            clase.inicio.toString(),
                            clase.fin.toString()
                        ) + (clase.lugar?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// El nombre de un archivo elegido con el selector del sistema (ej: "Guía 3.pdf").
private fun nombreDeArchivo(contexto: android.content.Context, uri: android.net.Uri): String? =
    runCatching {
        contexto.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

// Aviso arriba de la charla: hay sesiones del plan que ya pasaron.
@Composable
private fun TarjetaPlanAtrasado(cantidad: Int, onRevisar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .superficieVidrio(RoundedCornerShape(22.dp))
            .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
    ) {
        IconoDegradado(Icons.Outlined.EventRepeat, BloqueNaranja, tamano = 36.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            pluralStringResource(R.plurals.ia_plan_atrasado, cantidad, cantidad),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRevisar) { Text(stringResource(R.string.ia_plan_revisar)) }
    }
}

// Barra del simulacro en curso, arriba de donde se escribe.
@Composable
private fun BarraSimulacro(habilitado: Boolean, onTerminar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
            .padding(start = 14.dp, end = 6.dp)
    ) {
        Icon(Icons.Outlined.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.ia_simulacro_en_curso), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.weight(1f))
        TextButton(onClick = onTerminar, enabled = habilitado) { Text(stringResource(R.string.ia_simulacro_terminar)) }
    }
}

// Elegir un PDF: los adjuntos de las materias o uno del teléfono.
@Composable
private fun DialogoPdf(pdfs: List<PdfDisponible>, onElegir: (PdfDisponible) -> Unit, onDelTelefono: () -> Unit, onCancelar: () -> Unit) {
    DialogoApp(
        titulo = stringResource(R.string.ia_pdf_elegir),
        icono = Icons.Outlined.PictureAsPdf,
        textoConfirmar = null,
        onConfirmar = {},
        onCancelar = onCancelar
    ) {
        Text(stringResource(R.string.ia_pdf_explicacion), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                .clickable(onClick = onDelTelefono)
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(R.string.ia_pdf_telefono), style = MaterialTheme.typography.bodyLarge)
        }
        if (pdfs.isEmpty()) {
            Text(stringResource(R.string.ia_pdf_sin_adjuntos), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        pdfs.take(40).forEach { pdf ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onElegir(pdf) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(pdf.archivo.nombre, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${pdf.nombreMateria} · ${pdf.archivo.tamanoLegible()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// Charlas guardadas (se tocan para seguirlas) y los últimos simulacros.
@Composable
private fun DialogoHistorial(
    conversaciones: List<ConversacionGuardada>,
    simulacros: List<ResultadoSimulacro>,
    actual: Long?,
    onAbrir: (ConversacionGuardada) -> Unit,
    onBorrar: (ConversacionGuardada) -> Unit,
    onCerrar: () -> Unit
) {
    val formato = remember { DateTimeFormatter.ofPattern("d/M HH:mm") }
    val formatoDia = remember { DateTimeFormatter.ofPattern("d/M") }
    // Filtro por materia (las charlas con una nota o un PDF quedan con su materia).
    val materias = remember(conversaciones) { conversaciones.mapNotNull { it.materia }.distinct() }
    var filtro by remember { mutableStateOf<String?>(null) }
    DialogoApp(
        titulo = stringResource(R.string.ia_historial),
        icono = Icons.Outlined.History,
        textoConfirmar = null,
        onConfirmar = {},
        onCancelar = onCerrar,
        textoCancelar = stringResource(R.string.ia_cerrar)
    ) {
        if (simulacros.isNotEmpty()) {
            Text(stringResource(R.string.ia_historial_simulacros), style = MaterialTheme.typography.titleSmall)
            simulacros.take(5).forEach { s ->
                val nota = if (s.nota % 1.0 == 0.0) s.nota.toInt().toString() else s.nota.toString()
                Text(
                    "${s.fecha.format(formatoDia)} · ${s.materia} · $nota/10" + if (s.temasFlojos.isEmpty()) "" else "\n" + stringResource(R.string.ia_simulacro_repasar, s.temasFlojos.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(stringResource(R.string.ia_historial_charlas), style = MaterialTheme.typography.titleSmall)
        if (materias.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                FilterChip(selected = filtro == null, onClick = { filtro = null }, label = { Text(stringResource(R.string.ia_historial_todas)) })
                materias.forEach { m -> FilterChip(selected = filtro == m, onClick = { filtro = if (filtro == m) null else m }, label = { Text(m) }) }
            }
        }
        val visibles = conversaciones.filter { filtro == null || it.materia == filtro }
        if (visibles.isEmpty()) {
            Text(stringResource(R.string.ia_historial_vacio), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        visibles.forEach { c ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (c.id == actual) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onAbrir(c) }
                    .padding(start = 14.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(c.titulo, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(c.actualizada.format(formato), c.materia).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onBorrar(c) }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.ia_historial_borrar))
                }
            }
        }
    }
}

// Cómo le fue en el simulacro: nota, resumen, temas a repasar y tarjetas creadas.
@Composable
private fun DialogoResultadoSimulacro(resultado: EvaluacionSimulacro, onCerrar: () -> Unit) {
    val nota = if (resultado.nota % 1.0 == 0.0) resultado.nota.toInt().toString() else resultado.nota.toString()
    DialogoApp(
        titulo = stringResource(R.string.ia_simulacro_resultado),
        icono = Icons.Outlined.Assignment,
        textoConfirmar = null,
        onConfirmar = {},
        onCancelar = onCerrar,
        textoCancelar = stringResource(R.string.ia_simulacro_listo)
    ) {
        Text("$nota/10", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        if (resultado.resumen.isNotBlank()) Text(resultado.resumen, style = MaterialTheme.typography.bodyMedium)
        if (resultado.temasFlojos.isNotEmpty()) {
            Text(stringResource(R.string.ia_simulacro_repasar, resultado.temasFlojos.joinToString(", ")), style = MaterialTheme.typography.bodyMedium)
        }
        if (resultado.tarjetas.isNotEmpty()) {
            Text(
                pluralStringResource(R.plurals.ia_simulacro_tarjetas, resultado.tarjetas.size, resultado.tarjetas.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Las sesiones del plan que pasaron: se marcan las que no se hicieron y se
// reacomodan antes del examen.
@Composable
private fun DialogoReacomodar(sesiones: List<SesionAtrasada>, onReacomodar: (List<SesionAtrasada>) -> Unit, onCancelar: () -> Unit) {
    val noHechas = remember(sesiones) { mutableStateListOf(*sesiones.map { true }.toTypedArray()) }
    val formato = remember { DateTimeFormatter.ofPattern("EEE d/M", Locale.forLanguageTag("es-AR")) }
    val cantidad = noHechas.count { it }
    DialogoApp(
        titulo = stringResource(R.string.ia_plan_atrasado_titulo),
        icono = Icons.Outlined.EventRepeat,
        textoConfirmar = if (cantidad == 0) stringResource(R.string.ia_plan_todas_hechas) else stringResource(R.string.ia_plan_reacomodar, cantidad),
        onConfirmar = { onReacomodar(sesiones.filterIndexed { i, _ -> noHechas[i] }) },
        onCancelar = onCancelar
    ) {
        Text(stringResource(R.string.ia_plan_atrasado_explicacion), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        sesiones.forEachIndexed { i, s ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { noHechas[i] = !noHechas[i] }
                    .padding(end = 12.dp)
            ) {
                Checkbox(checked = noHechas[i], onCheckedChange = { noHechas[i] = it })
                Column {
                    Text(s.evento.titulo, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.ia_plan_atrasado_fila, s.evento.fecha.format(formato), s.evento.horaInicio.toString(), s.examen.titulo, s.examen.fecha.format(formato)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
