package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
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
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Nota

// Ventana emergente para crear (nota == null) o editar una nota.
// Al crear hay que elegir la materia; al editar, la materia no cambia.
@Composable
fun DialogoNota(
    nota: Nota?,
    materias: List<Materia>,
    onConfirmar: (materiaId: Int?, titulo: String, contenido: String) -> Unit,
    onCancelar: () -> Unit
) {
    var materia by remember { mutableStateOf<Materia?>(null) }
    var titulo by remember { mutableStateOf(nota?.titulo ?: "") }
    var contenido by remember { mutableStateOf(nota?.contenido ?: "") }
    var error by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(if (nota == null) R.string.nueva_nota else R.string.editar_nota)) },
        text = {
            Column {
                if (nota == null) {
                    SelectorOpcion(
                        etiqueta = stringResource(R.string.elegir_materia),
                        opciones = materias,
                        seleccionada = materia,
                        texto = { it.nombre },
                        onSeleccionar = { materia = it; error = null },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it; error = null },
                    label = { Text(stringResource(R.string.titulo)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = contenido,
                    onValueChange = { contenido = it },
                    label = { Text(stringResource(R.string.contenido)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextoError(error?.let { stringResource(it) })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    nota == null && materia == null -> R.string.error_elegir_materia
                    titulo.isBlank() -> R.string.error_titulo_nota
                    else -> null
                }
                if (error == null) onConfirmar(materia?.id, titulo, contenido)
            }) { Text(stringResource(if (nota == null) R.string.crear else R.string.guardar)) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) }
        }
    )
}
