package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Tema
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Cuánto falta para un examen: "Es hoy", "Falta 1 día", "Faltan 5 días" o "Ya pasó".
@Composable
fun textoCuentaRegresiva(fecha: LocalDate, hoy: LocalDate): String {
    val dias = ChronoUnit.DAYS.between(hoy, fecha).toInt()
    return when {
        dias < 0 -> stringResource(R.string.examen_paso)
        dias == 0 -> stringResource(R.string.examen_hoy)
        else -> pluralStringResource(R.plurals.examen_faltan, dias, dias)
    }
}

// Temario de un examen: la cuenta regresiva, cuántos temas van estudiados
// (con una barra) y la lista de temas para marcar. Abajo se agregan temas
// nuevos. Los cambios se guardan al instante (no hay "Guardar").
@Composable
fun DialogoTemario(
    evento: Evento,
    color: Color,
    hoy: LocalDate,
    onAgregar: (String) -> Unit,
    onAlternar: (Tema) -> Unit,
    onEliminar: (Tema) -> Unit,
    onCerrar: () -> Unit
) {
    var nuevo by remember { mutableStateOf("") }
    val acento = colorAcentoTexto(color)

    DialogoApp(
        titulo = evento.titulo,
        icono = Icons.Outlined.Quiz,
        textoConfirmar = null,
        onConfirmar = {},
        onCancelar = onCerrar,
        textoCancelar = stringResource(R.string.cerrar)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = textoCuentaRegresiva(evento.fecha, hoy),
                style = MaterialTheme.typography.labelLarge,
                color = acento,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(fondoSuave(color))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            if (evento.temas.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.temario_progreso, evento.temasEstudiados, evento.temas.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (evento.temas.isEmpty()) {
            TextoDialogo(stringResource(R.string.temario_vacio))
        } else {
            LinearProgressIndicator(
                progress = { evento.temasEstudiados.toFloat() / evento.temas.size },
                color = color,
                trackColor = color.copy(alpha = 0.16f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
        }
        evento.temas.forEach { tema ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .toggleable(value = tema.estudiado, role = Role.Checkbox, onValueChange = { onAlternar(tema) })
                    .padding(start = 12.dp)
            ) {
                Icon(
                    imageVector = if (tema.estudiado) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (tema.estudiado) color else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = tema.titulo,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (tema.estudiado) TextDecoration.LineThrough else null,
                    color = if (tema.estudiado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onEliminar(tema) }) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.temario_quitar))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CampoTexto(
                value = nuevo,
                onValueChange = { nuevo = it },
                placeholder = { Text(stringResource(R.string.temario_nuevo_tema)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    onAgregar(nuevo)
                    nuevo = ""
                },
                enabled = nuevo.isNotBlank()
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.temario_agregar), tint = acento)
            }
        }
    }
}
