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

    val textoResumir = stringResource(R.string.ia_resumir)
    val textoPreguntas = stringResource(R.string.ia_preguntas)
    val textoFoto = stringResource(R.string.ia_foto_etiqueta)
    val prefijoPlan = stringResource(R.string.ia_plan_prefijo)
    fun accion(a: AccionIA) {
        when (a) {
            AccionIA.EXPLICAR -> explicando = true
            AccionIA.PLAN -> eligiendoExamen = true
            AccionIA.FOTO, AccionIA.HORARIO -> { fotoPara = a; eligiendoFoto = true }
            AccionIA.SIMULACRO -> eligiendoSimulacro = true
            else -> eligiendoNota = a
        }
    }

    // Foto de un apunte: con la cámara o de la galería.
    fun usarFoto(uri: String) = if (fotoPara == AccionIA.HORARIO) viewModel.leerHorario(uri) else viewModel.leerApunte(uri, textoFoto)
    val sacarFoto = camaraDeFotos { uri -> usarFoto(uri) }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) usarFoto(uri.toString())
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

    // Siempre se ve lo último de la charla.
    val cantidad = estado.conversacion.size + (if (estado.pensando) 1 else 0) + (if (estado.error != null) 1 else 0)
    LaunchedEffect(cantidad) { if (cantidad > 0) lista.animateScrollToItem(lista.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1) }

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
            if (estado.conversacion.isEmpty()) {
                item { Bienvenida(onAccion = ::accion) }
            }
            items(estado.conversacion) { mensaje ->
                Burbuja(mensaje, onGuardar = { guardandoRespuesta = mensaje.texto })
            }
            if (estado.pensando) item { Pensando() }
            estado.error?.let { error -> item { TarjetaError(error, onReintentar = viewModel::reintentar) } }
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
        DialogoFoto(
            horario = fotoPara == AccionIA.HORARIO,
            onCamara = { eligiendoFoto = false; sacarFoto() },
            onGaleria = {
                eligiendoFoto = false
                galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onCancelar = { eligiendoFoto = false }
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
    Triple(AccionIA.HORARIO, stringResource(R.string.ia_horario), Icons.Outlined.CalendarViewWeek)
)

// Primera vez: las acciones como tarjetas y el aviso de privacidad.
@Composable
private fun Bienvenida(onAccion: (AccionIA) -> Unit) {
    val colores = listOf(BloqueVioleta, BloqueNaranja, BloqueAzul, BloqueVerde, BloqueNaranja, BloqueVioleta, BloqueVerde, BloqueAzul, BloqueVioleta)
    val detalles = listOf(
        R.string.ia_resumir_detalle, R.string.ia_explicar_detalle, R.string.ia_preguntas_detalle, R.string.ia_tarjetas_detalle,
        R.string.ia_plan_detalle, R.string.ia_mapa_detalle, R.string.ia_foto_detalle, R.string.ia_simulacro_detalle,
        R.string.ia_horario_detalle
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
            Text(stringResource(R.string.abrir_gemini), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
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
private fun Burbuja(mensaje: MensajeIA, onGuardar: () -> Unit) {
    val contexto = LocalContext.current
    val copiada = stringResource(R.string.ia_copiada)
    if (mensaje.deUsuario) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                mensaje.mostrado,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 48.dp)
                    .clip(RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp))
                    .background(LocalDegradado.current)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
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
            SelectionContainer {
                Text(mensaje.texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 8.dp))
            }
            Row {
                TextButton(onClick = {
                    contexto.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("respuesta", mensaje.texto))
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
private fun BarraEscribir(texto: String, onCambiar: (String) -> Unit, habilitado: Boolean, onEnviar: () -> Unit, modifier: Modifier) {
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
        val puede = habilitado && texto.isNotBlank()
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (puede) LocalDegradado.current else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceContainer))
                .clickable(enabled = puede, onClick = onEnviar)
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.Send,
                contentDescription = stringResource(R.string.ia_enviar),
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
private fun DialogoFoto(horario: Boolean, onCamara: () -> Unit, onGaleria: () -> Unit, onCancelar: () -> Unit) {
    DialogoApp(
        titulo = stringResource(if (horario) R.string.ia_horario_como else R.string.ia_foto_como),
        icono = if (horario) Icons.Outlined.CalendarViewWeek else Icons.Outlined.DocumentScanner,
        textoConfirmar = null,
        onConfirmar = {},
        onCancelar = onCancelar
    ) {
        Text(stringResource(if (horario) R.string.ia_horario_explicacion else R.string.ia_foto_explicacion), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(
            Triple(R.string.ia_foto_camara, Icons.Outlined.PhotoCamera, onCamara),
            Triple(R.string.ia_foto_galeria, Icons.Outlined.Image, onGaleria)
        ).forEach { (texto, icono, alTocar) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = alTocar)
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
