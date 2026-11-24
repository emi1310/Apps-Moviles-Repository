package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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

    DialogoApp(
        titulo = stringResource(if (tarea == null) R.string.nueva_tarea else R.string.editar_tarea),
        icono = Icons.Filled.TaskAlt,
        textoConfirmar = stringResource(if (tarea == null) R.string.crear else R.string.guardar),
        onConfirmar = { if (titulo.isBlank()) error = true else onConfirmar(titulo.trim(), fecha) },
        onCancelar = onCancelar
    ) {
        CampoTexto(
            value = titulo,
            onValueChange = {
                titulo = it
                error = false
            },
            label = { Text(stringResource(R.string.titulo)) },
            isError = error,
            supportingText = if (error) {
                { Text(stringResource(R.string.error_titulo_tarea)) }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        EtiquetaCampo(stringResource(R.string.fecha_limite))
        CampoFecha(fecha = fecha, onCambiar = { fecha = it }, permitirVacio = true, modifier = Modifier.fillMaxWidth())
    }
}
