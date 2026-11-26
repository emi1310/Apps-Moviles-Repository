package com.example.estudia.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.estudia.R
import com.example.estudia.modelo.MesaFinal
import com.example.estudia.ui.componentes.BotonSuave
import com.example.estudia.ui.componentes.CampoFecha
import com.example.estudia.ui.componentes.CampoTexto
import com.example.estudia.ui.componentes.DialogoApp
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.EtiquetaCampo
import com.example.estudia.ui.componentes.EtiquetaNota
import com.example.estudia.ui.componentes.FilaTarjeta
import com.example.estudia.ui.componentes.MenuOpciones
import com.example.estudia.ui.componentes.OpcionMenu
import com.example.estudia.ui.componentes.PantallaDetalle
import com.example.estudia.ui.componentes.PastillaEstado
import com.example.estudia.ui.componentes.RecolectarAvisos
import com.example.estudia.ui.componentes.TextoError
import com.example.estudia.ui.componentes.TituloLista
import com.example.estudia.ui.componentes.texto
import com.example.estudia.ui.theme.BloqueAzul
import com.example.estudia.ui.theme.BloqueNaranja
import com.example.estudia.ui.theme.BloqueVerde
import com.example.estudia.util.aTexto
import com.example.estudia.viewmodel.FinalesViewModel
import com.example.estudia.viewmodel.Mensaje
import java.time.LocalDate

private val RojoFinal = Color(0xFFE74C3C)

// Pantalla "Mesas de finales": los próximos finales con la fecha de la mesa,
// el cierre de la inscripción y el vencimiento de la regularidad; y los ya
// rendidos con su nota. Los avisos llegan con el resumen diario.
@Composable
fun FinalesScreen(viewModel: FinalesViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RecolectarAvisos(viewModel.avisos)
    var editando by remember { mutableStateOf<MesaFinal?>(null) }   // id 0 = nuevo
    var cargandoNota by remember { mutableStateOf<MesaFinal?>(null) }

    PantallaDetalle(
        titulo = stringResource(R.string.finales_titulo),
        subtitulo = stringResource(R.string.finales_subtitulo),
        onVolver = onVolver,
        textoAgregar = stringResource(R.string.finales_agregar),
        iconoAgregar = Icons.Outlined.Add,
        onAgregar = { editando = MesaFinal(0, "", estado.hoy.plusWeeks(2)) }
    ) {
        if (!estado.cargando && estado.proximos.isEmpty() && estado.rendidos.isEmpty()) {
            item { EstadoVacio(Icons.Outlined.WorkspacePremium, stringResource(R.string.finales_vacio), stringResource(R.string.finales_vacio_sugerencia)) }
        }
        if (estado.proximos.isNotEmpty()) item { TituloLista(stringResource(R.string.finales_proximos)) }
        items(estado.proximos, key = { "p${it.id}" }) { final ->
            TarjetaFinal(
                final = final,
                hoy = estado.hoy,
                onInscripto = { viewModel.alternarInscripto(final) },
                onNota = { cargandoNota = final },
                onEditar = { editando = final },
                onEliminar = { viewModel.eliminar(final) }
            )
        }
        if (estado.rendidos.isNotEmpty()) item { TituloLista(stringResource(R.string.finales_rendidos)) }
        items(estado.rendidos, key = { "r${it.id}" }) { final ->
            FilaTarjeta {
                Column(modifier = Modifier.weight(1f)) {
                    Text(final.materia, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(final.fechaMesa.aTexto(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                final.nota?.let { EtiquetaNota(it) }
                MenuOpciones(
                    listOf(
                        OpcionMenu(stringResource(R.string.finales_cambiar_nota), Icons.Outlined.Grade) { cargandoNota = final },
                        OpcionMenu(stringResource(R.string.eliminar), Icons.Outlined.Delete, destructiva = true) { viewModel.eliminar(final) }
                    ),
                    descripcion = stringResource(R.string.opciones_de, final.materia)
                )
            }
        }
    }

    editando?.let { final ->
        DialogoFinal(
            final = final,
            sugerencias = estado.sugerencias,
            colores = estado.colores,
            onConfirmar = { materia, mesa, cierre, vencimiento, inscripto ->
                viewModel.guardar(final.takeIf { it.id != 0 }, materia, mesa, cierre, vencimiento, inscripto)
                    .also { if (it == null) editando = null }
            },
            onCancelar = { editando = null }
        )
    }
    cargandoNota?.let { final ->
        DialogoNotaFinal(
            final = final,
            onConfirmar = { nota -> viewModel.cargarNota(final, nota).also { if (it == null) cargandoNota = null } },
            onCancelar = { cargandoNota = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaFinal(
    final: MesaFinal,
    hoy: LocalDate,
    onInscripto: () -> Unit,
    onNota: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val dias = final.diasParaMesa(hoy)
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 18.dp, end = 6.dp, top = 14.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(final.materia, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = stringResource(R.string.finales_mesa, final.fechaMesa.aTexto()) + " · " + when {
                            dias == 0L -> stringResource(R.string.hoy)
                            dias > 0 -> pluralStringResource(R.plurals.finales_faltan_dias, dias.toInt(), dias.toInt())
                            else -> stringResource(R.string.finales_ya_paso)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MenuOpciones(
                    listOf(
                        OpcionMenu(stringResource(R.string.editar), Icons.Outlined.Edit, alTocar = onEditar),
                        OpcionMenu(stringResource(R.string.eliminar), Icons.Outlined.Delete, destructiva = true, alTocar = onEliminar)
                    ),
                    descripcion = stringResource(R.string.opciones_de, final.materia)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val cierre = final.cierreInscripcion
                when {
                    final.inscripto -> PastillaEstado(stringResource(R.string.finales_inscripto), BloqueVerde)
                    final.inscripcionPerdida(hoy) -> PastillaEstado(stringResource(R.string.finales_inscripcion_cerrada), RojoFinal)
                    cierre != null -> PastillaEstado(stringResource(R.string.finales_inscripcion_hasta, cierre.aTexto()), BloqueNaranja)
                    else -> PastillaEstado(stringResource(R.string.finales_sin_inscripcion), MaterialTheme.colorScheme.outline)
                }
                final.vencimientoRegularidad?.let { vence ->
                    val color = when {
                        final.regularidadVencida(hoy) -> RojoFinal
                        final.regularidadVencePronto(hoy) -> BloqueNaranja
                        else -> BloqueAzul
                    }
                    PastillaEstado(
                        stringResource(if (final.regularidadVencida(hoy)) R.string.finales_regularidad_vencida else R.string.finales_regularidad_vence, vence.aTexto()),
                        color
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(end = 12.dp)) {
                BotonSuave(
                    stringResource(if (final.inscripto) R.string.finales_inscripto else R.string.finales_me_inscribi),
                    if (final.inscripto) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    BloqueVerde,
                    onInscripto,
                    Modifier.weight(1f)
                )
                BotonSuave(stringResource(R.string.finales_cargar_nota), Icons.Outlined.Grade, MaterialTheme.colorScheme.primary, onNota, Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoFinal(
    final: MesaFinal,
    sugerencias: List<String>,
    colores: Map<String, Long>,
    onConfirmar: (String, LocalDate?, LocalDate?, LocalDate?, Boolean) -> Mensaje?,
    onCancelar: () -> Unit
) {
    var materia by remember { mutableStateOf(final.materia) }
    var mesa by remember { mutableStateOf<LocalDate?>(final.fechaMesa) }
    var cierre by remember { mutableStateOf(final.cierreInscripcion) }
    var vencimiento by remember { mutableStateOf(final.vencimientoRegularidad) }
    var inscripto by remember { mutableStateOf(final.inscripto) }
    var error by remember { mutableStateOf<Mensaje?>(null) }

    DialogoApp(
        titulo = stringResource(if (final.id == 0) R.string.finales_nuevo else R.string.finales_editar),
        icono = Icons.Outlined.WorkspacePremium,
        textoConfirmar = stringResource(if (final.id == 0) R.string.agregar else R.string.guardar),
        onConfirmar = { error = onConfirmar(materia, mesa, cierre, vencimiento, inscripto) },
        onCancelar = onCancelar
    ) {
        CampoTexto(
            value = materia,
            onValueChange = { materia = it; error = null },
            label = { Text(stringResource(R.string.finales_materia)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // Las materias de la app y las regulares del plan, para elegir con un toque.
        if (sugerencias.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sugerencias.forEach { nombre ->
                    PastillaSugerencia(
                        nombre = nombre,
                        color = colores[nombre.lowercase()]?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
                        elegida = materia.trim().equals(nombre, ignoreCase = true),
                        alTocar = { materia = nombre; error = null }
                    )
                }
            }
        }
        EtiquetaCampo(stringResource(R.string.finales_fecha_mesa))
        CampoFecha(fecha = mesa, onCambiar = { mesa = it; error = null }, modifier = Modifier.fillMaxWidth())
        EtiquetaCampo(stringResource(R.string.finales_cierre))
        CampoFecha(
            fecha = cierre,
            onCambiar = { cierre = it; error = null },
            permitirVacio = true,
            textoVacio = stringResource(R.string.finales_sin_fecha),
            modifier = Modifier.fillMaxWidth()
        )
        EtiquetaCampo(stringResource(R.string.finales_vencimiento))
        CampoFecha(
            fecha = vencimiento,
            onCambiar = { vencimiento = it },
            permitirVacio = true,
            textoVacio = stringResource(R.string.finales_sin_fecha),
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
            Icon(Icons.Outlined.HowToReg, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(R.string.finales_ya_inscripto), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = inscripto, onCheckedChange = { inscripto = it })
        }
        TextoError(error?.let { stringResource(it.texto()) })
    }
}

// Pastilla para elegir una materia sugerida: con el punto de su color; la
// elegida se pinta entera de ese color.
@Composable
private fun PastillaSugerencia(nombre: String, color: Color, elegida: Boolean, alTocar: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (elegida) color else color.copy(alpha = 0.14f))
            .clickable(role = Role.RadioButton, onClick = alTocar)
            .semantics { selected = elegida }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (elegida) Color.White else color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = nombre,
            style = MaterialTheme.typography.labelLarge,
            color = if (elegida) Color.White else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DialogoNotaFinal(final: MesaFinal, onConfirmar: (String) -> Mensaje?, onCancelar: () -> Unit) {
    var nota by remember { mutableStateOf(final.nota?.let { com.example.estudia.ui.componentes.textoNota(it) } ?: "") }
    var error by remember { mutableStateOf<Mensaje?>(null) }
    DialogoApp(
        titulo = final.materia,
        icono = Icons.Outlined.Grade,
        textoConfirmar = stringResource(R.string.guardar),
        onConfirmar = { error = onConfirmar(nota) },
        onCancelar = onCancelar
    ) {
        CampoTexto(
            value = nota,
            onValueChange = { nota = it; error = null },
            label = { Text(stringResource(R.string.finales_nota)) },
            supportingText = { Text(stringResource(R.string.finales_nota_ayuda)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        TextoError(error?.let { stringResource(it.texto()) })
    }
}
