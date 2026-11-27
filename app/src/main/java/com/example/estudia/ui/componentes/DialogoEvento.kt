package com.example.estudia.ui.componentes

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.util.aTexto
import com.example.estudia.modelo.Evento
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.TipoEvento
import com.example.estudia.modelo.sePuedeRepetir
import com.example.estudia.ui.theme.LocalDegradado
import java.time.LocalDate
import java.time.LocalTime

// Nombre legible de cada tipo de evento (desde strings.xml).
@StringRes
fun nombreTipo(tipo: TipoEvento): Int = when (tipo) {
    TipoEvento.CLASE -> R.string.tipo_clase
    TipoEvento.EXAMEN -> R.string.tipo_examen
    TipoEvento.ENTREGA -> R.string.tipo_entrega
    else -> R.string.tipo_bloque_estudio
}

private val TIPOS = listOf(TipoEvento.CLASE, TipoEvento.EXAMEN, TipoEvento.ENTREGA, TipoEvento.BLOQUE_ESTUDIO)

// Ventana emergente para crear (evento == null) o editar un evento.
// La materia se elige con pastillas de colores y el tipo con una grilla de
// bloques con su ícono. onConfirmar devuelve el error a mostrar (ej: la hora
// de fin es anterior a la de inicio) o null si se guardó; esa regla la
// decide el ViewModel.
@Composable
fun DialogoEvento(
    evento: Evento?,
    materiaIdInicial: Int?,
    materias: List<Materia>,
    fechaInicial: LocalDate,
    onConfirmar: (
        materiaId: Int, titulo: String, tipo: TipoEvento, fecha: LocalDate,
        inicio: LocalTime, fin: LocalTime, semanal: Boolean, repetirHasta: LocalDate?,
        virtual: Boolean, lugar: String, enlace: String, cadaSemanas: Int
    ) -> Int?,
    onCancelar: () -> Unit
) {
    var materia by remember { mutableStateOf(materias.firstOrNull { it.id == materiaIdInicial }) }
    var tipo by remember { mutableStateOf(evento?.tipo) }
    var titulo by remember { mutableStateOf(evento?.titulo ?: "") }
    var fecha by remember { mutableStateOf(evento?.fecha ?: fechaInicial) }
    var inicio by remember { mutableStateOf(evento?.horaInicio ?: LocalTime.of(9, 0)) }
    var fin by remember { mutableStateOf(evento?.horaFin ?: LocalTime.of(10, 0)) }
    var semanal by remember { mutableStateOf(evento?.semanal ?: false) }
    var repetirHasta by remember { mutableStateOf(evento?.repetirHasta) }
    var cadaSemanas by remember { mutableIntStateOf(evento?.cadaSemanas ?: 1) }
    var virtual by remember { mutableStateOf(evento?.virtual ?: false) }
    var lugar by remember { mutableStateOf(evento?.lugar ?: "") }
    var enlace by remember { mutableStateOf(evento?.enlace ?: "") }
    var error by remember { mutableStateOf<Int?>(null) }

    DialogoApp(
        titulo = stringResource(if (evento == null) R.string.nuevo_evento else R.string.editar_evento),
        icono = Icons.Outlined.Event,
        textoConfirmar = stringResource(if (evento == null) R.string.crear else R.string.guardar),
        onConfirmar = {
            val m = materia
            val t = tipo
            error = when {
                m == null -> R.string.error_elegir_materia
                t == null -> R.string.error_elegir_tipo
                titulo.isBlank() -> R.string.error_titulo_evento
                else -> onConfirmar(m.id, titulo, t, fecha, inicio, fin, semanal, repetirHasta, virtual, lugar, enlace, cadaSemanas)
            }
        },
        onCancelar = onCancelar
    ) {
        EtiquetaCampo(stringResource(R.string.elegir_materia))
        SelectorMateria(materias, materia) { materia = it; error = null }

        EtiquetaCampo(stringResource(R.string.elegir_tipo))
        TIPOS.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { opcion ->
                    BloqueTipo(opcion, elegido = tipo == opcion, modifier = Modifier.weight(1f)) {
                        tipo = opcion
                        error = null
                    }
                }
            }
        }

        CampoTexto(
            value = titulo,
            onValueChange = { titulo = it; error = null },
            label = { Text(stringResource(R.string.titulo)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        CampoFecha(fecha = fecha, onCambiar = { if (it != null) fecha = it }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoHora(stringResource(R.string.desde), inicio, { inicio = it; error = null }, Modifier.weight(1f))
            CampoHora(stringResource(R.string.hasta), fin, { fin = it; error = null }, Modifier.weight(1f))
        }

        // Presencial (con aula o lugar) o virtual (con el link). Los bloques de
        // estudio son de uno, así que no lo piden.
        if (tipo != null && tipo != TipoEvento.BLOQUE_ESTUDIO) {
            EtiquetaCampo(stringResource(R.string.evento_modalidad))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BloqueOpcion(Icons.Outlined.Place, stringResource(R.string.evento_presencial), !virtual, Modifier.weight(1f)) { virtual = false }
                BloqueOpcion(Icons.Outlined.Videocam, stringResource(R.string.evento_virtual), virtual, Modifier.weight(1f)) { virtual = true }
            }
            if (virtual) {
                CampoTexto(
                    value = enlace,
                    onValueChange = { enlace = it },
                    label = { Text(stringResource(R.string.evento_enlace)) },
                    placeholder = { Text(stringResource(R.string.evento_enlace_ejemplo)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                CampoTexto(
                    value = lugar,
                    onValueChange = { lugar = it },
                    label = { Text(stringResource(R.string.evento_lugar)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Solo las clases se pueden repetir: se muestran todas las semanas,
        // el mismo día, hasta la última clase (si se eligió).
        if (tipo?.sePuedeRepetir == true) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .toggleable(value = semanal, role = Role.Switch, onValueChange = { semanal = it; error = null })
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.repetir_semanal), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.cada_dia_semana, nombreDiaSemana(fecha)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = semanal, onCheckedChange = null)
            }
            if (semanal) {
                // Semana A / semana B: la clase va semana por medio, empezando
                // por la semana de la fecha elegida.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PastillaFiltro(stringResource(R.string.semanas_todas), elegida = cadaSemanas == 1) { cadaSemanas = 1 }
                    PastillaFiltro(stringResource(R.string.semanas_por_medio), elegida = cadaSemanas == 2) { cadaSemanas = 2 }
                }
                if (cadaSemanas == 2) TextoDialogo(stringResource(R.string.semanas_por_medio_explicacion, fecha.aTexto()))
                EtiquetaCampo(stringResource(R.string.repetir_hasta))
                CampoFecha(
                    fecha = repetirHasta,
                    onCambiar = { repetirHasta = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    permitirVacio = true,
                    textoVacio = stringResource(R.string.repetir_sin_fin)
                )
            }
        }
        TextoError(error?.let { stringResource(it) })
    }
}

// Bloque elegible de un tipo de evento: su ícono y nombre. El elegido va con
// el degradado violeta y el texto en blanco.
@Composable
private fun BloqueTipo(tipo: TipoEvento, elegido: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colorContenido = if (elegido) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (elegido) Modifier.background(LocalDegradado.current)
                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            )
            .selectable(selected = elegido, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            iconoTipo(tipo),
            contentDescription = null,
            tint = if (elegido) Color.White else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        TextoAjustable(
            stringResource(nombreTipo(tipo)), MaterialTheme.typography.labelLarge, Modifier.padding(vertical = 6.dp),
            color = colorContenido, maxLines = 2
        )
    }
}

// Nombre del día de la semana en minúscula (ej: "lunes"), para textos
// como "Cada lunes".
@Composable
fun nombreDiaSemana(fecha: LocalDate): String =
    stringArrayResource(R.array.dias_semana_largo)[fecha.dayOfWeek.value - 1].lowercase()
