package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Receso
import com.example.estudia.util.aTexto
import com.example.estudia.viewmodel.Mensaje
import java.time.LocalDate

// Recesos: días sin clases (vacaciones de invierno, semana de parciales...).
// Primero la lista (tocar uno lo edita, la X lo borra) y "Agregar receso";
// al agregar o editar: nombre, desde y hasta. onGuardar devuelve el error a
// mostrar o null si se guardó.
@Composable
fun DialogoRecesos(
    recesos: List<Receso>,
    onGuardar: (existente: Receso?, nombre: String, desde: LocalDate, hasta: LocalDate) -> Mensaje?,
    onEliminar: (Receso) -> Unit,
    onCerrar: () -> Unit
) {
    // null = mostrando la lista; si no, el receso que se edita (id 0 = nuevo).
    var editando by remember { mutableStateOf<Receso?>(null) }
    val actual = editando
    if (actual == null) {
        DialogoApp(
            titulo = stringResource(R.string.recesos_titulo),
            icono = Icons.Outlined.BeachAccess,
            textoConfirmar = null,
            onConfirmar = {},
            onCancelar = onCerrar,
            textoCancelar = stringResource(R.string.nota_historial_cerrar)
        ) {
            TextoDialogo(stringResource(R.string.recesos_explicacion))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recesos.forEach { receso ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .clickable(onClickLabel = stringResource(R.string.editar)) { editando = receso }
                            .padding(start = 14.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(receso.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.recesos_rango, receso.desde.aTexto(), receso.hasta.aTexto()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onEliminar(receso) }) {
                            Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.recesos_eliminar, receso.nombre))
                        }
                    }
                }
            }
            CampoBloque(
                icono = Icons.Outlined.Add,
                texto = stringResource(R.string.recesos_agregar),
                onClick = { editando = Receso(0, "", LocalDate.now(), LocalDate.now().plusDays(6)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    } else {
        var nombre by remember(actual) { mutableStateOf(actual.nombre) }
        var desde by remember(actual) { mutableStateOf(actual.desde) }
        var hasta by remember(actual) { mutableStateOf(actual.hasta) }
        var error by remember(actual) { mutableStateOf<Int?>(null) }
        DialogoApp(
            titulo = stringResource(if (actual.id == 0) R.string.recesos_nuevo else R.string.recesos_editar),
            icono = Icons.Outlined.BeachAccess,
            textoConfirmar = stringResource(R.string.guardar),
            onConfirmar = {
                val resultado = onGuardar(actual.takeIf { it.id != 0 }, nombre, desde, hasta)
                if (resultado == null) editando = null else error = resultado.texto()
            },
            onCancelar = { editando = null }
        ) {
            CampoTexto(
                value = nombre,
                onValueChange = { nombre = it; error = null },
                label = { Text(stringResource(R.string.recesos_nombre)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            EtiquetaCampo(stringResource(R.string.recesos_desde))
            CampoFecha(fecha = desde, onCambiar = { if (it != null) { desde = it; error = null } }, modifier = Modifier.fillMaxWidth())
            EtiquetaCampo(stringResource(R.string.recesos_hasta))
            CampoFecha(fecha = hasta, onCambiar = { if (it != null) { hasta = it; error = null } }, modifier = Modifier.fillMaxWidth())
            TextoError(error?.let { stringResource(it) })
        }
    }
}
