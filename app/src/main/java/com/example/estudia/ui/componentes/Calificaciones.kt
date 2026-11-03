package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Calificacion
import com.example.estudia.util.aTexto
import java.time.LocalDate
import kotlin.math.roundToInt

// Nota con coma y sin decimales de más: 9.0 -> "9", 7.5 -> "7,5", 6.333 -> "6,33".
fun textoNota(nota: Double): String {
    val redondeada = (nota * 100).roundToInt() / 100.0
    return if (redondeada % 1.0 == 0.0) redondeada.toInt().toString()
    else redondeada.toString().trimEnd('0').replace('.', ',')
}

// Verde si aprueba (4 o más), rojo si no.
@Composable
fun colorNota(nota: Double): Color =
    if (nota >= Calificacion.NOTA_APROBACION) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error

// Pastilla con una nota o un promedio (ej: "7,5" o "Prom. 7,5").
@Composable
fun EtiquetaNota(nota: Double, prefijo: String? = null) {
    val color = colorNota(nota)
    Text(
        text = listOfNotNull(prefijo, textoNota(nota)).joinToString(" "),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

// Una nota de examen dentro de la tarjeta de la materia.
@Composable
fun FilaCalificacion(calificacion: Calificacion, onEditar: () -> Unit, onEliminar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(calificacion.titulo, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = calificacion.fecha.aTexto(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        EtiquetaNota(calificacion.nota)
        MenuOpciones(
            listOf(
                OpcionMenu(stringResource(R.string.editar), Icons.Filled.Edit, onEditar),
                OpcionMenu(stringResource(R.string.eliminar), Icons.Filled.Delete, onEliminar)
            ),
            descripcion = stringResource(R.string.opciones_de, calificacion.titulo)
        )
    }
}

// Ventana para cargar (calificacion == null) o editar la nota de un examen.
// onConfirmar devuelve el error a mostrar, o null si se guardó (lo decide
// el ViewModel: título vacío o nota fuera de 1 a 10).
@Composable
fun DialogoCalificacion(
    calificacion: Calificacion?,
    onConfirmar: (titulo: String, nota: String, fecha: LocalDate) -> Int?,
    onCancelar: () -> Unit
) {
    var titulo by remember { mutableStateOf(calificacion?.titulo ?: "") }
    var nota by remember { mutableStateOf(calificacion?.let { textoNota(it.nota) } ?: "") }
    var fecha by remember { mutableStateOf(calificacion?.fecha ?: LocalDate.now()) }
    var error by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(if (calificacion == null) R.string.nueva_calificacion else R.string.editar_calificacion)) },
        text = {
            Column {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it; error = null },
                    label = { Text(stringResource(R.string.calificacion_examen)) },
                    placeholder = { Text(stringResource(R.string.calificacion_examen_ejemplo)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it; error = null },
                    label = { Text(stringResource(R.string.calificacion_nota)) },
                    supportingText = { Text(stringResource(R.string.calificacion_nota_ayuda)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                CampoFecha(fecha = fecha, onCambiar = { if (it != null) fecha = it }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                TextoError(error?.let { stringResource(it) })
            }
        },
        confirmButton = {
            TextButton(onClick = { error = onConfirmar(titulo, nota, fecha) }) {
                Text(stringResource(if (calificacion == null) R.string.agregar else R.string.guardar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) }
        }
    )
}
