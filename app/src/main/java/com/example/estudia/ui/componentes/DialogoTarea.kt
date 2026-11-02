package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Tarea
import java.time.LocalDate

// Ventana emergente para crear (tarea == null) o editar una tarea.
// La fecha límite se elige con el calendario y es opcional.
@Composable
fun DialogoTarea(
    tarea: Tarea?,
    onConfirmar: (titulo: String, fechaLimite: LocalDate?) -> Unit,
    onCancelar: () -> Unit
) {
    var titulo by remember { mutableStateOf(tarea?.titulo ?: "") }
    // Una tarea nueva arranca con fecha de hoy; al editar se respeta la que tenía.
    var fecha by remember { mutableStateOf(if (tarea == null) LocalDate.now() else tarea.fechaLimite) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(if (tarea == null) R.string.nueva_tarea else R.string.editar_tarea)) },
        text = {
            Column {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = {
                        titulo = it
                        error = false
                    },
                    label = { Text(stringResource(R.string.titulo)) },
                    isError = error,
                    supportingText = { if (error) Text(stringResource(R.string.error_titulo_tarea)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.fecha_limite), style = MaterialTheme.typography.labelLarge)
                CampoFecha(fecha = fecha, onCambiar = { fecha = it }, permitirVacio = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (titulo.isBlank()) error = true else onConfirmar(titulo.trim(), fecha)
            }) { Text(stringResource(if (tarea == null) R.string.crear else R.string.guardar)) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) }
        }
    )
}
