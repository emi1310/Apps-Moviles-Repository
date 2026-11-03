package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.util.aMilisUtc
import com.example.estudia.util.aTexto
import com.example.estudia.util.fechaDesdeMilisUtc
import java.time.LocalDate
import java.time.LocalTime

// Campos reutilizables de los formularios (diálogos).

// Botón que muestra la opción elegida y abre un menú para elegir otra
// (se usa para elegir la materia y el tipo de evento).
@Composable
fun <T> SelectorOpcion(
    etiqueta: String,
    opciones: List<T>,
    seleccionada: T?,
    texto: @Composable (T) -> String,
    onSeleccionar: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (seleccionada != null) texto(seleccionada) else etiqueta,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(texto(opcion)) },
                    onClick = {
                        onSeleccionar(opcion)
                        abierto = false
                    }
                )
            }
        }
    }
}

// Botón que muestra una fecha y abre el calendario de Material 3 para
// elegirla. Si permitirVacio es true, se puede quitar la fecha.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    fecha: LocalDate?,
    onCambiar: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    permitirVacio: Boolean = false,
    // Texto del botón cuando no hay fecha (por defecto "Sin fecha").
    textoVacio: String? = null
) {
    var abierto by remember { mutableStateOf(false) }
    Row(modifier = modifier) {
        OutlinedButton(onClick = { abierto = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(fecha?.aTexto() ?: textoVacio ?: stringResource(R.string.sin_fecha))
        }
        if (permitirVacio && fecha != null) {
            IconButton(onClick = { onCambiar(null) }) {
                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.quitar_fecha))
            }
        }
    }
    if (abierto) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = (fecha ?: LocalDate.now()).aMilisUtc())
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { onCambiar(fechaDesdeMilisUtc(it)) }
                    abierto = false
                }) { Text(stringResource(R.string.aceptar)) }
            },
            dismissButton = {
                TextButton(onClick = { abierto = false }) { Text(stringResource(R.string.cancelar)) }
            }
        ) {
            DatePicker(state = estado)
        }
    }
}

// Botón que muestra una hora y abre el reloj de Material 3 para elegirla.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoHora(
    etiqueta: String,
    hora: LocalTime,
    onCambiar: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { abierto = true }, modifier = modifier) {
        Icon(Icons.Filled.Schedule, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("$etiqueta ${hora.aTexto()}")
    }
    if (abierto) {
        val estado = rememberTimePickerState(initialHour = hora.hour, initialMinute = hora.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    onCambiar(LocalTime.of(estado.hour, estado.minute))
                    abierto = false
                }) { Text(stringResource(R.string.aceptar)) }
            },
            dismissButton = {
                TextButton(onClick = { abierto = false }) { Text(stringResource(R.string.cancelar)) }
            },
            text = { TimePicker(state = estado) }
        )
    }
}

// Campo de búsqueda con lupa y botón para borrar el texto.
@Composable
fun Buscador(texto: String, onCambiar: (String) -> Unit, sugerencia: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = texto,
        onValueChange = onCambiar,
        placeholder = { Text(sugerencia) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = { onCambiar("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.borrar_busqueda))
                }
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

// "Hoy", "Mañana", "Ayer" o la fecha completa.
@Composable
fun textoFecha(fecha: LocalDate, hoy: LocalDate = LocalDate.now()): String = when (fecha) {
    hoy -> stringResource(R.string.hoy)
    hoy.plusDays(1) -> stringResource(R.string.manana)
    hoy.minusDays(1) -> stringResource(R.string.ayer)
    else -> fecha.aTexto()
}

// Texto de error de un formulario (en rojo).
@Composable
fun TextoError(mensaje: String?) {
    if (mensaje != null) {
        Text(mensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
