package com.example.estudia.ui.componentes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.estudia.R
import com.example.estudia.modelo.Prioridad
import com.example.estudia.modelo.Subtarea
import com.example.estudia.modelo.Tarea
import java.time.LocalDate

// Ventana emergente para crear (tarea == null) o editar una tarea.
// La fecha límite se elige con el calendario y es opcional. Además se elige
// la prioridad y se pueden cargar pasos (subtareas); los vacíos se ignoran.
@Composable
fun DialogoTarea(
    tarea: Tarea?,
    onConfirmar: (titulo: String, fechaLimite: LocalDate?, prioridad: Prioridad, subtareas: List<Subtarea>, avisoDiasAntes: Int?) -> Unit,
    onCancelar: () -> Unit
) {
    var titulo by remember { mutableStateOf(tarea?.titulo ?: "") }
    // Una tarea nueva arranca con fecha de hoy; al editar se respeta la que tenía.
    var fecha by remember { mutableStateOf(if (tarea == null) LocalDate.now() else tarea.fechaLimite) }
    var prioridad by remember { mutableStateOf(tarea?.prioridad ?: Prioridad.MEDIA) }
    var aviso by remember { mutableStateOf(tarea?.avisoDiasAntes) }
    // Las subtareas que ya existían conservan su id y si estaban hechas.
    val pasos = remember { (tarea?.subtareas ?: emptyList()).toMutableStateList() }
    var error by remember { mutableStateOf(false) }

    DialogoApp(
        titulo = stringResource(if (tarea == null) R.string.nueva_tarea else R.string.editar_tarea),
        icono = Icons.Outlined.TaskAlt,
        textoConfirmar = stringResource(if (tarea == null) R.string.crear else R.string.guardar),
        onConfirmar = {
            if (titulo.isBlank()) error = true
            else onConfirmar(titulo.trim(), fecha, prioridad, pasos.toList(), aviso.takeIf { fecha != null })
        },
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

        // Aviso: solo con fecha límite. Pastillas: No, El día, 1, 2 y 7 días antes.
        EtiquetaCampo(stringResource(R.string.aviso_tarea))
        if (fecha == null) {
            TextoDialogo(stringResource(R.string.aviso_sin_fecha))
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                PastillaFiltro(stringResource(R.string.aviso_no), elegida = aviso == null) { aviso = null }
                Tarea.OPCIONES_AVISO.forEach { dias ->
                    val texto = if (dias == 0) stringResource(R.string.aviso_mismo_dia)
                    else pluralStringResource(R.plurals.aviso_dias_antes, dias, dias)
                    PastillaFiltro(texto, elegida = aviso == dias) { aviso = dias }
                }
            }
        }

        EtiquetaCampo(stringResource(R.string.prioridad))
        SelectorPrioridad(prioridad) { prioridad = it }

        EtiquetaCampo(stringResource(R.string.subtareas))
        pasos.forEachIndexed { indice, paso ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                CampoTexto(
                    value = paso.titulo,
                    onValueChange = { pasos[indice] = paso.copy(titulo = it) },
                    placeholder = { Text(stringResource(R.string.subtarea_numero, indice + 1)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { pasos.removeAt(indice) }) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.quitar_subtarea))
                }
            }
        }
        CampoBloque(
            icono = Icons.Outlined.Add,
            texto = stringResource(R.string.agregar_subtarea),
            onClick = { pasos.add(Subtarea(id = 0, titulo = "")) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
