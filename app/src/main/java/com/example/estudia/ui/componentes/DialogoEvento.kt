package com.example.estudia.ui.componentes

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.TipoEvento
import java.time.LocalDate
import java.time.LocalTime

// Nombre legible de cada tipo de evento (desde strings.xml).
@StringRes
fun nombreTipo(tipo: TipoEvento): Int = when (tipo) {
    TipoEvento.CLASE -> R.string.tipo_clase
    TipoEvento.EXAMEN -> R.string.tipo_examen
    TipoEvento.ENTREGA -> R.string.tipo_entrega
    else -> R.string.tipo_bloque_estudio
}

private val TIPOS = listOf(TipoEvento.CLASE, TipoEvento.EXAMEN, TipoEvento.ENTREGA, TipoEvento.BLOQUE_ESTUDIO)

// Ventana emergente para crear (evento == null) o editar un evento.
// onConfirmar devuelve el error a mostrar (ej: la hora de fin es anterior
// a la de inicio) o null si se guardó; esa regla la decide el ViewModel.
@Composable
fun DialogoEvento(
    evento: Evento?,
    materiaIdInicial: Int?,
    materias: List<Materia>,
    fechaInicial: LocalDate,
    onConfirmar: (
        materiaId: Int, titulo: String, tipo: TipoEvento, fecha: LocalDate,
        inicio: LocalTime, fin: LocalTime, semanal: Boolean, repetirHasta: LocalDate?
    ) -> Int?,
    onCancelar: () -> Unit
) {
    var materia by remember { mutableStateOf(materias.firstOrNull { it.id == materiaIdInicial }) }
    var tipo by remember { mutableStateOf(evento?.tipo) }
    var titulo by remember { mutableStateOf(evento?.titulo ?: "") }
    var fecha by remember { mutableStateOf(evento?.fecha ?: fechaInicial) }
    var inicio by remember { mutableStateOf(evento?.horaInicio ?: LocalTime.of(9, 0)) }
    var fin by remember { mutableStateOf(evento?.horaFin ?: LocalTime.of(10, 0)) }
    var semanal by remember { mutableStateOf(evento?.semanal ?: false) }
    var repetirHasta by remember { mutableStateOf(evento?.repetirHasta) }
    var error by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(if (evento == null) R.string.nuevo_evento else R.string.editar_evento)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                SelectorOpcion(
                    etiqueta = stringResource(R.string.elegir_materia),
                    opciones = materias,
                    seleccionada = materia,
                    texto = { it.nombre },
                    onSeleccionar = { materia = it; error = null },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                SelectorOpcion(
                    etiqueta = stringResource(R.string.elegir_tipo),
                    opciones = TIPOS,
                    seleccionada = tipo,
                    texto = { stringResource(nombreTipo(it)) },
                    onSeleccionar = { tipo = it; error = null },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it; error = null },
                    label = { Text(stringResource(R.string.titulo)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                CampoFecha(fecha = fecha, onCambiar = { if (it != null) fecha = it }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    CampoHora(stringResource(R.string.desde), inicio, { inicio = it; error = null }, Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    CampoHora(stringResource(R.string.hasta), fin, { fin = it; error = null }, Modifier.weight(1f))
                }
                // Solo las clases se pueden repetir: se muestran todas las
                // semanas, el mismo día, hasta la última clase (si se eligió).
                if (tipo == TipoEvento.CLASE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().toggleable(
                            value = semanal,
                            role = Role.Checkbox,
                            onValueChange = { semanal = it; error = null }
                        )
                    ) {
                        Checkbox(checked = semanal, onCheckedChange = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(stringResource(R.string.repetir_semanal), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = stringResource(R.string.cada_dia_semana, nombreDiaSemana(fecha)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (semanal) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stringResource(R.string.repetir_hasta), style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        CampoFecha(
                            fecha = repetirHasta,
                            onCambiar = { repetirHasta = it; error = null },
                            modifier = Modifier.fillMaxWidth(),
                            permitirVacio = true,
                            textoVacio = stringResource(R.string.repetir_sin_fin)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextoError(error?.let { stringResource(it) })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val m = materia
                val t = tipo
                error = when {
                    m == null -> R.string.error_elegir_materia
                    t == null -> R.string.error_elegir_tipo
                    titulo.isBlank() -> R.string.error_titulo_evento
                    else -> onConfirmar(m.id, titulo, t, fecha, inicio, fin, semanal, repetirHasta)
                }
            }) { Text(stringResource(if (evento == null) R.string.crear else R.string.guardar)) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) }
        }
    )
}

// Nombre del día de la semana en minúscula (ej: "lunes"), para textos
// como "Cada lunes".
@Composable
fun nombreDiaSemana(fecha: LocalDate): String =
    stringArrayResource(R.array.dias_semana_largo)[fecha.dayOfWeek.value - 1].lowercase()
