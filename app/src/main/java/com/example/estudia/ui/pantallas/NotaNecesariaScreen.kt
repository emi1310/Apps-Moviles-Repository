package com.example.estudia.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Warning
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
import com.example.estudia.modelo.CalculoNotas
import com.example.estudia.modelo.Calificacion
import com.example.estudia.modelo.Evaluacion
import com.example.estudia.modelo.NotaNecesaria
import com.example.estudia.ui.componentes.BotonSuave
import com.example.estudia.ui.componentes.CampoTexto
import com.example.estudia.ui.componentes.DialogoApp
import com.example.estudia.ui.componentes.EstadoVacio
import com.example.estudia.ui.componentes.EtiquetaNota
import com.example.estudia.ui.componentes.FilaTarjeta
import com.example.estudia.ui.componentes.MenuOpciones
import com.example.estudia.ui.componentes.OpcionMenu
import com.example.estudia.ui.componentes.PantallaDetalle
import com.example.estudia.ui.componentes.PastillaEstado
import com.example.estudia.ui.componentes.RecolectarAvisos
import com.example.estudia.ui.componentes.SelectorMateria
import com.example.estudia.ui.componentes.TextoError
import com.example.estudia.ui.componentes.TituloLista
import com.example.estudia.ui.componentes.texto
import com.example.estudia.ui.componentes.textoNota
import com.example.estudia.ui.theme.BloqueNaranja
import com.example.estudia.ui.theme.LocalDegradado
import com.example.estudia.viewmodel.Mensaje
import com.example.estudia.viewmodel.NotaNecesariaViewModel

// Pantalla "¿Qué nota necesito?": cómo se compone la nota de la materia
// (evaluaciones con su peso) y cuánto hace falta en lo que queda para
// aprobar (4) o promocionar (7). Se abre desde el menú de cada materia.
@Composable
fun NotaNecesariaScreen(viewModel: NotaNecesariaViewModel, onVolver: () -> Unit) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    RecolectarAvisos(viewModel.avisos)
    // null = cerrado; Evaluacion con id 0 = nueva.
    var editando by remember { mutableStateOf<Evaluacion?>(null) }
    val materia = estado.materia

    PantallaDetalle(
        titulo = stringResource(R.string.nota_necesaria_titulo),
        subtitulo = stringResource(R.string.nota_necesaria_subtitulo),
        onVolver = onVolver,
        textoAgregar = if (materia != null) stringResource(R.string.evaluacion_agregar) else null,
        iconoAgregar = Icons.Outlined.Add,
        onAgregar = { editando = Evaluacion(0, "", 0) }
    ) {
        if (!estado.cargando && materia == null) {
            item { EstadoVacio(Icons.Outlined.Calculate, stringResource(R.string.nota_necesaria_sin_materias), stringResource(R.string.nota_necesaria_sin_materias_sugerencia)) }
            return@PantallaDetalle
        }
        if (materia == null) return@PantallaDetalle
        item { SelectorMateria(estado.materias, materia) { viewModel.elegirMateria(it) } }

        val calculo = estado.calculo
        if (materia.evaluaciones.isEmpty()) {
            item { EstadoVacio(Icons.Outlined.Calculate, stringResource(R.string.nota_necesaria_vacio), stringResource(R.string.nota_necesaria_vacio_sugerencia)) }
        } else if (calculo != null) {
            item { TarjetaResultado(calculo, materia.notaPromocion) }
            if (!calculo.pesoValido) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                        Icon(Icons.Outlined.Warning, contentDescription = null, tint = BloqueNaranja, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.nota_necesaria_pesos, calculo.pesoTotal),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        if (estado.paraImportar.isNotEmpty()) {
            item {
                BotonSuave(
                    stringResource(R.string.nota_necesaria_importar, estado.paraImportar.size),
                    Icons.Outlined.Download,
                    Color(materia.color),
                    viewModel::importarCalificaciones,
                    Modifier.fillMaxWidth()
                )
            }
        }
        if (materia.evaluaciones.isNotEmpty()) {
            item { TituloLista(stringResource(R.string.nota_necesaria_evaluaciones), "${calculo?.pesoTotal ?: 0}%") }
        }
        items(materia.evaluaciones, key = { it.id }) { evaluacion ->
            FilaTarjeta(onClick = { editando = evaluacion }, modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(evaluacion.nombre, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.evaluacion_peso, evaluacion.peso),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val nota = evaluacion.nota
                if (nota != null) EtiquetaNota(nota)
                else PastillaEstado(stringResource(R.string.evaluacion_pendiente), MaterialTheme.colorScheme.outline)
                MenuOpciones(
                    listOf(
                        OpcionMenu(stringResource(R.string.editar), Icons.Outlined.Edit) { editando = evaluacion },
                        OpcionMenu(stringResource(R.string.eliminar), Icons.Outlined.Delete, destructiva = true) { viewModel.eliminar(evaluacion) }
                    ),
                    descripcion = stringResource(R.string.opciones_de, evaluacion.nombre)
                )
            }
        }
    }

    editando?.let { evaluacion ->
        DialogoEvaluacion(
            evaluacion = evaluacion,
            onConfirmar = { nombre, peso, nota ->
                viewModel.guardar(evaluacion.takeIf { it.id != 0 }, nombre, peso, nota).also { if (it == null) editando = null }
            },
            onCancelar = { editando = null }
        )
    }
}

// Lo que necesita para aprobar y para promocionar, y el promedio actual.
@Composable
private fun TarjetaResultado(calculo: CalculoNotas, notaPromocion: Double) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(LocalDegradado.current)
            .padding(20.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LineaResultado(stringResource(R.string.nota_necesaria_aprobar, textoNota(Calificacion.NOTA_APROBACION)), calculo.paraAprobar)
        // Si la materia no es promocional, solo se calcula para aprobar.
        calculo.paraPromocionar?.let { LineaResultado(stringResource(R.string.nota_necesaria_promocionar, textoNota(notaPromocion)), it) }
        Text(
            text = calculo.promedioActual?.let { stringResource(R.string.nota_necesaria_promedio, textoNota(it), calculo.pesoRendido) }
                ?: stringResource(R.string.nota_necesaria_sin_rendir),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun LineaResultado(meta: String, resultado: NotaNecesaria) {
    Column {
        Text(meta, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
        Text(
            text = when (resultado) {
                NotaNecesaria.Asegurada -> stringResource(R.string.nota_necesaria_asegurada)
                NotaNecesaria.NoAlcanza -> stringResource(R.string.nota_necesaria_no_alcanza)
                is NotaNecesaria.Necesita -> stringResource(R.string.nota_necesaria_necesitas, textoNota(resultado.nota))
                is NotaNecesaria.Terminada -> stringResource(if (resultado.alcanzo) R.string.nota_necesaria_lo_lograste else R.string.nota_necesaria_no_llegaste)
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun DialogoEvaluacion(
    evaluacion: Evaluacion,
    onConfirmar: (nombre: String, peso: String, nota: String) -> Mensaje?,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf(evaluacion.nombre) }
    var peso by remember { mutableStateOf(if (evaluacion.peso == 0) "" else evaluacion.peso.toString()) }
    var nota by remember { mutableStateOf(evaluacion.nota?.let { textoNota(it) } ?: "") }
    var error by remember { mutableStateOf<Mensaje?>(null) }

    DialogoApp(
        titulo = stringResource(if (evaluacion.id == 0) R.string.evaluacion_nueva else R.string.evaluacion_editar),
        icono = Icons.Outlined.Calculate,
        textoConfirmar = stringResource(if (evaluacion.id == 0) R.string.agregar else R.string.guardar),
        onConfirmar = { error = onConfirmar(nombre, peso, nota) },
        onCancelar = onCancelar
    ) {
        CampoTexto(
            value = nombre,
            onValueChange = { nombre = it; error = null },
            label = { Text(stringResource(R.string.evaluacion_nombre)) },
            placeholder = { Text(stringResource(R.string.calificacion_examen_ejemplo)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            value = peso,
            onValueChange = { peso = it.filter(Char::isDigit).take(3); error = null },
            label = { Text(stringResource(R.string.evaluacion_peso_campo)) },
            supportingText = { Text(stringResource(R.string.evaluacion_peso_ayuda)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            value = nota,
            onValueChange = { nota = it; error = null },
            label = { Text(stringResource(R.string.evaluacion_nota_campo)) },
            supportingText = { Text(stringResource(R.string.evaluacion_nota_ayuda)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        TextoError(error?.let { stringResource(it.texto()) })
    }
}
