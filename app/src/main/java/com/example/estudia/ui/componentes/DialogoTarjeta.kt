package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.estudia.R
import com.example.estudia.modelo.Tarjeta

// Ventana para crear (tarjeta == null) o editar una tarjeta de estudio.
// onConfirmar devuelve false si falta la pregunta o la respuesta (lo decide
// el ViewModel) y entonces se muestra el error.
@Composable
fun DialogoTarjeta(
    tarjeta: Tarjeta?,
    onConfirmar: (pregunta: String, respuesta: String) -> Boolean,
    onCancelar: () -> Unit
) {
    var pregunta by remember { mutableStateOf(tarjeta?.pregunta ?: "") }
    var respuesta by remember { mutableStateOf(tarjeta?.respuesta ?: "") }
    var error by remember { mutableStateOf(false) }

    DialogoApp(
        titulo = stringResource(if (tarjeta == null) R.string.tarjeta_nueva else R.string.tarjeta_editar),
        icono = Icons.Outlined.Style,
        textoConfirmar = stringResource(if (tarjeta == null) R.string.crear else R.string.guardar),
        onConfirmar = { error = !onConfirmar(pregunta, respuesta) },
        onCancelar = onCancelar
    ) {
        CampoTexto(
            value = pregunta,
            onValueChange = { pregunta = it; error = false },
            label = { Text(stringResource(R.string.tarjeta_pregunta)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        CampoTexto(
            value = respuesta,
            onValueChange = { respuesta = it; error = false },
            label = { Text(stringResource(R.string.tarjeta_respuesta)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        TextoError(if (error) stringResource(R.string.error_tarjeta) else null)
    }
}
