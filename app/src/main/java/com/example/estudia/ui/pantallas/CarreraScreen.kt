package com.example.estudia.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.modelo.EstadoPlan
import com.example.estudia.modelo.MateriaPlan
import com.example.estudia.ui.componentes.BotonSuave
import com.example.estudia.ui.componentes.CampoTexto
import com.example.estudia.ui.componentes.DialogoApp
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.EtiquetaCampo
import com.example.estudia.ui.componentes.FilaTarjeta
import com.example.estudia.ui.componentes.MenuOpciones
import com.example.estudia.ui.componentes.OpcionMenu
import com.example.estudia.ui.componentes.PantallaDetalle
import com.example.estudia.ui.componentes.PastillaEstado
import com.example.estudia.ui.componentes.RecolectarAvisos
import com.example.estudia.ui.componentes.TextoError
import com.example.estudia.ui.componentes.progresoAnimado
import com.example.estudia.ui.componentes.texto
import com.example.estudia.ui.componentes.textoNota
import com.example.estudia.ui.theme.BloqueAzul
import com.example.estudia.ui.theme.BloqueNaranja
import com.example.estudia.ui.theme.BloqueVerde
import com.example.estudia.ui.theme.LocalDegradado
import com.example.estudia.viewmodel.CarreraUiState
import com.example.estudia.viewmodel.CarreraViewModel

// Pantalla "Mi carrera": el plan de estudios por año, con el estado de cada
// materia, qué se puede cursar o rendir según las correlativas y el % de
// la carrera aprobado. Se abre desde Materias.
@Composable
fun CarreraScreen(viewModel: CarreraViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RecolectarAvisos(viewModel.avisos)

    // null = cerrado; MateriaPlan con id 0 = nueva.
    var editando by remember { mutableStateOf<MateriaPlan?>(null) }
    var cambiandoEstado by remember { mutableStateOf<MateriaPlan?>(null) }

    PantallaDetalle(
        titulo = stringResource(R.string.carrera_titulo),
        subtitulo = estado.carrera ?: stringResource(R.string.carrera_subtitulo),
        onVolver = onVolver,
        textoAgregar = stringResource(R.string.carrera_agregar),
        iconoAgregar = Icons.Outlined.Add,
        onAgregar = { editando = MateriaPlan(0, "", estado.anioActual) }
    ) {
        if (estado.vacio) {
            item {
                EstadoVacio(Icons.Outlined.School, stringResource(R.string.carrera_vacio), stringResource(R.string.carrera_vacio_sugerencia))
            }
        } else if (!estado.cargando) {
            item { CabeceraCarrera(estado) }
        }
        if (estado.paraImportar.isNotEmpty() && !estado.cargando) {
            item {
                BotonSuave(
                    stringResource(R.string.carrera_importar, estado.paraImportar.size),
                    Icons.Outlined.Download,
                    MaterialTheme.colorScheme.primary,
                    viewModel::importarMateriasDeLaApp,
                    Modifier.fillMaxWidth()
                )
            }
        }
        estado.anios.forEach { anio ->
            item(key = "anio${anio.anio}") {
                Text(
                    text = stringResource(R.string.carrera_anio, anio.anio),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }
            items(anio.materias, key = { it.id }) { materia ->
                FilaMateriaPlan(
                    materia = materia,
                    plan = estado.plan,
                    enLaApp = materia.nombre.trim().lowercase() in estado.enLaApp,
                    onCambiarEstado = { cambiandoEstado = materia },
                    onEditar = { editando = materia },
                    onEliminar = { viewModel.eliminar(materia) }
                )
            }
        }
    }

    editando?.let { materia ->
        DialogoMateriaPlan(
            materia = materia,
            plan = estado.plan,
            onConfirmar = { nombre, anio, correlativas ->
                viewModel.guardar(materia.takeIf { it.id != 0 }, nombre, anio, correlativas).also { if (it == null) editando = null }
            },
            onCancelar = { editando = null }
        )
    }
    cambiandoEstado?.let { materia ->
        DialogoEstadoPlan(
            materia = materia,
            onConfirmar = { nuevo, nota -> viewModel.cambiarEstado(materia, nuevo, nota).also { if (it == null) cambiandoEstado = null } },
            onCancelar = { cambiandoEstado = null }
        )
    }
}

@Composable
fun colorEstado(estado: EstadoPlan): Color = when (estado) {
    EstadoPlan.PENDIENTE -> MaterialTheme.colorScheme.outline
    EstadoPlan.CURSANDO -> BloqueAzul
    EstadoPlan.REGULAR -> BloqueNaranja
    EstadoPlan.APROBADA -> BloqueVerde
}

@Composable
fun textoEstado(estado: EstadoPlan): String = stringResource(
    when (estado) {
        EstadoPlan.PENDIENTE -> R.string.estado_pendiente
        EstadoPlan.CURSANDO -> R.string.estado_cursando
        EstadoPlan.REGULAR -> R.string.estado_regular
        EstadoPlan.APROBADA -> R.string.estado_aprobada
    }
)

// Avance de la carrera: barra con el % aprobado y los números.
@Composable
private fun CabeceraCarrera(estado: CarreraUiState) {
    val avance = estado.avance
    val progreso = progresoAnimado(avance.porcentaje / 100f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(LocalDegradado.current)
            .padding(20.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = "${avance.porcentaje}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.carrera_completada),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progreso },
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.25f),
            drawStopIndicator = {},
            gapSize = 0.dp,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.carrera_aprobadas, avance.aprobadas, avance.total),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        Text(
            text = buildString {
                append(stringResource(R.string.carrera_regulares_cursando, avance.regulares, avance.cursando))
                avance.promedio?.let { append(" · ").append(stringResource(R.string.carrera_promedio, textoNota(it))) }
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

// Una materia del plan: punto del color del estado, nombre, qué le falta
// (o si ya se puede cursar/rendir) y el menú.
@Composable
private fun FilaMateriaPlan(
    materia: MateriaPlan,
    plan: List<MateriaPlan>,
    enLaApp: Boolean,
    onCambiarEstado: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val color = colorEstado(materia.estado)
    val detalle: Pair<String, Color>? = when (materia.estado) {
        EstadoPlan.PENDIENTE -> {
            val faltan = materia.faltanParaCursar(plan)
            if (faltan.isEmpty()) stringResource(R.string.carrera_puede_cursar) to BloqueVerde
            else stringResource(R.string.carrera_falta_cursar, faltan.joinToString { it.nombre }) to MaterialTheme.colorScheme.onSurfaceVariant
        }
        EstadoPlan.REGULAR -> {
            val faltan = materia.faltanParaRendir(plan)
            if (faltan.isEmpty()) stringResource(R.string.carrera_puede_rendir) to BloqueVerde
            else stringResource(R.string.carrera_falta_rendir, faltan.joinToString { it.nombre }) to MaterialTheme.colorScheme.onSurfaceVariant
        }
        EstadoPlan.APROBADA -> materia.notaFinal?.let { stringResource(R.string.carrera_nota_final, textoNota(it)) to MaterialTheme.colorScheme.onSurfaceVariant }
        EstadoPlan.CURSANDO -> null
    }
    FilaTarjeta(onClick = onCambiarEstado, modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(materia.nombre, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                PastillaEstado(textoEstado(materia.estado), color)
                if (enLaApp) PastillaEstado(stringResource(R.string.carrera_en_la_app), MaterialTheme.colorScheme.primary)
            }
            if (detalle != null) {
                Text(
                    text = detalle.first,
                    style = MaterialTheme.typography.bodySmall,
                    color = detalle.second,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        MenuOpciones(
            listOf(
                OpcionMenu(stringResource(R.string.carrera_cambiar_estado), Icons.Outlined.SwapHoriz, alTocar = onCambiarEstado),
                OpcionMenu(stringResource(R.string.editar), Icons.Outlined.Edit, alTocar = onEditar),
                OpcionMenu(stringResource(R.string.eliminar), Icons.Outlined.Delete, destructiva = true, alTocar = onEliminar)
            ),
            descripcion = stringResource(R.string.opciones_de, materia.nombre)
        )
    }
}

// Agregar o editar una materia del plan: nombre, año y correlativas.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoMateriaPlan(
    materia: MateriaPlan,
    plan: List<MateriaPlan>,
    onConfirmar: (nombre: String, anio: String, correlativas: List<Int>) -> com.example.estudia.viewmodel.Mensaje?,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf(materia.nombre) }
    var anio by remember { mutableStateOf(materia.anio.toString()) }
    var correlativas by remember { mutableStateOf(materia.correlativas.toSet()) }
    var error by remember { mutableStateOf<com.example.estudia.viewmodel.Mensaje?>(null) }
    val otras = plan.filter { it.id != materia.id }

    DialogoApp(
        titulo = stringResource(if (materia.id == 0) R.string.carrera_nueva_materia else R.string.carrera_editar_materia),
        icono = Icons.Outlined.School,
        textoConfirmar = stringResource(if (materia.id == 0) R.string.agregar else R.string.guardar),
        onConfirmar = { error = onConfirmar(nombre, anio, correlativas.toList()) },
        onCancelar = onCancelar
    ) {
        CampoTexto(
            value = nombre,
            onValueChange = { nombre = it; error = null },
            label = { Text(stringResource(R.string.carrera_nombre)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            value = anio,
            onValueChange = { anio = it.filter(Char::isDigit).take(1); error = null },
            label = { Text(stringResource(R.string.carrera_anio_campo)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        EtiquetaCampo(stringResource(R.string.carrera_correlativas))
        if (otras.isEmpty()) {
            Text(
                stringResource(R.string.carrera_sin_otras),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            otras.forEach { otra ->
                val elegida = otra.id in correlativas
                FilterChip(
                    selected = elegida,
                    onClick = { correlativas = if (elegida) correlativas - otra.id else correlativas + otra.id },
                    label = { Text(otra.nombre) }
                )
            }
        }
        TextoError(error?.let { stringResource(it.texto()) })
    }
}

// Cambiar el estado de una materia (y cargar la nota si quedó aprobada).
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoEstadoPlan(
    materia: MateriaPlan,
    onConfirmar: (EstadoPlan, String) -> com.example.estudia.viewmodel.Mensaje?,
    onCancelar: () -> Unit
) {
    var elegido by remember { mutableStateOf(materia.estado) }
    var nota by remember { mutableStateOf(materia.notaFinal?.let { textoNota(it) } ?: "") }
    var error by remember { mutableStateOf<com.example.estudia.viewmodel.Mensaje?>(null) }

    DialogoApp(
        titulo = materia.nombre,
        icono = Icons.Outlined.SwapHoriz,
        textoConfirmar = stringResource(R.string.guardar),
        onConfirmar = { error = onConfirmar(elegido, nota) },
        onCancelar = onCancelar
    ) {
        EtiquetaCampo(stringResource(R.string.carrera_estado_titulo))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EstadoPlan.entries.forEach { estado ->
                FilterChip(
                    selected = elegido == estado,
                    onClick = { elegido = estado; error = null },
                    label = { Text(textoEstado(estado)) },
                    leadingIcon = { Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(colorEstado(estado))) }
                )
            }
        }
        if (elegido == EstadoPlan.APROBADA) {
            CampoTexto(
                value = nota,
                onValueChange = { nota = it; error = null },
                label = { Text(stringResource(R.string.carrera_nota_final_campo)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        TextoError(error?.let { stringResource(it.texto()) })
    }
}
