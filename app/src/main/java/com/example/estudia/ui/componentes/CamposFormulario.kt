package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.example.estudia.ui.theme.LocalDegradado
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.util.aMilisUtc
import com.example.estudia.util.aTexto
import com.example.estudia.util.fechaDesdeMilisUtc
import java.time.LocalDate
import java.time.LocalTime

// Campos reutilizables de los formularios (diálogos).

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
    CampoBloque(
        icono = Icons.Outlined.CalendarMonth,
        texto = fecha?.aTexto() ?: textoVacio ?: stringResource(R.string.sin_fecha),
        onClick = { abierto = true },
        modifier = modifier
    ) {
        if (permitirVacio && fecha != null) {
            IconButton(onClick = { onCambiar(null) }) {
                Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.quitar_fecha))
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
    CampoBloque(
        icono = Icons.Outlined.Schedule,
        texto = "$etiqueta ${hora.aTexto()}",
        onClick = { abierto = true },
        modifier = modifier
    )
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

// Campo de búsqueda con forma de pastilla: fondo de tarjeta, sin borde ni
// línea, la lupa en violeta y una X para borrar el texto.
@Composable
fun Buscador(texto: String, onCambiar: (String) -> Unit, sugerencia: String, modifier: Modifier = Modifier) {
    TextField(
        value = texto,
        onValueChange = onCambiar,
        placeholder = { Text(sugerencia, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = { onCambiar("") }) {
                    Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.borrar_busqueda))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = coloresSinLinea(MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp)
    )
}

// Filtro de una lista (Activas / Archivadas, Todas / Recientes / Favoritas):
// pastilla sin borde, como el buscador. La elegida va con el degradado
// violeta y el texto en blanco.
@Composable
fun PastillaFiltro(texto: String, elegida: Boolean, onClick: () -> Unit) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelLarge,
        color = if (elegida) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (elegida) Modifier.background(LocalDegradado.current)
                else Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
            )
            .selectable(selected = elegida, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
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
