package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.estudia.R
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Nota

// Ventana emergente para crear (nota == null) o editar una nota.
// Al crear hay que elegir la materia (pastillas de colores); al editar, la
// materia no cambia.
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

    DialogoApp(
        titulo = stringResource(if (nota == null) R.string.nueva_nota else R.string.editar_nota),
        icono = Icons.Outlined.StickyNote2,
        textoConfirmar = stringResource(if (nota == null) R.string.crear else R.string.guardar),
        onConfirmar = {
            error = when {
                nota == null && materia == null -> R.string.error_elegir_materia
                titulo.isBlank() -> R.string.error_titulo_nota
                else -> null
            }
            if (error == null) onConfirmar(materia?.id, titulo, contenido)
        },
        onCancelar = onCancelar
    ) {
        if (nota == null) {
            EtiquetaCampo(stringResource(R.string.elegir_materia))
            SelectorMateria(materias, materia) { materia = it; error = null }
        }
        CampoTexto(
            value = titulo,
            onValueChange = { titulo = it; error = null },
            label = { Text(stringResource(R.string.titulo)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // El micrófono suma lo dictado al final del contenido.
        val dictar = rememberDictado(
            onTexto = { contenido = agregarDictado(contenido, it) },
            onSinDictado = { error = R.string.error_sin_dictado }
        )
        CampoTexto(
            value = contenido,
            onValueChange = { contenido = it },
            label = { Text(stringResource(R.string.contenido)) },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = dictar) {
                    Icon(Icons.Outlined.Mic, contentDescription = stringResource(R.string.dictar))
                }
            }
        )
        TextoError(error?.let { stringResource(it) })
    }
}
