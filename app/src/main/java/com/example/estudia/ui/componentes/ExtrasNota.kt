package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.estudia.R
import com.example.estudia.modelo.ParteNota
import com.example.estudia.modelo.VersionNota
import com.example.estudia.modelo.partesDeNota
import java.time.format.DateTimeFormatter
import java.util.Locale

// Partes de las notas que usan la lista y la nota abierta: los colores para
// elegir, el historial, las etiquetas y el texto con casillas.

// Colores para las notas (los mismos tonos vivos que las materias).
val COLORES_NOTA = listOf(
    0xFF6C5CE7, 0xFF0984E3, 0xFF00B894, 0xFFFDA43C,
    0xFFE17055, 0xFFE84393, 0xFF00CEC9, 0xFF636E72
)

// Elegir el color de una nota. "El de la materia" lo vuelve al de siempre.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogoColorNota(actual: Long?, colorMateria: Long, onElegir: (Long?) -> Unit, onCancelar: () -> Unit) {
    DialogoApp(
        titulo = stringResource(R.string.nota_color_titulo),
        icono = Icons.Outlined.Palette,
        textoConfirmar = null,
        onConfirmar = {},
        onCancelar = onCancelar
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            COLORES_NOTA.forEachIndexed { i, color ->
                CirculoColor(Color(color), elegido = actual == color, descripcion = stringResource(R.string.nota_color_elegir, i + 1)) { onElegir(color) }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .clickable(role = Role.Button) { onElegir(null) }
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(colorMateria)),
                contentAlignment = Alignment.Center
            ) {
                if (actual == null) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                else Icon(Icons.Outlined.FormatColorReset, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(R.string.nota_color_materia), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun CirculoColor(color: Color, elegido: Boolean, descripcion: String, alTocar: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .then(if (elegido) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
            .clickable(role = Role.Button, onClick = alTocar)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center
    ) {
        if (elegido) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White)
    }
}

// Historial: las versiones anteriores, de la más nueva a la más vieja.
// Tocar una la despliega; "Volver a esta versión" la restaura.
@Composable
fun DialogoHistorialNota(versiones: List<VersionNota>, onRestaurar: (VersionNota) -> Unit, onCerrar: () -> Unit) {
    val formato = remember { DateTimeFormatter.ofPattern("EEE d/M · HH:mm", Locale.forLanguageTag("es-AR")) }
    var abierta by remember { mutableStateOf<Int?>(null) }
    DialogoApp(
        titulo = stringResource(R.string.nota_historial),
        icono = Icons.Outlined.History,
        textoConfirmar = null,
        onConfirmar = {},
        onCancelar = onCerrar,
        textoCancelar = stringResource(R.string.nota_historial_cerrar)
    ) {
        if (versiones.isEmpty()) {
            TextoDialogo(stringResource(R.string.nota_historial_vacio))
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())
            ) {
                versiones.forEach { version ->
                    val fecha = version.fecha.format(formato)
                    val desplegada = abierta == version.id
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .clickable(onClickLabel = stringResource(R.string.nota_historial_ver, fecha)) {
                                abierta = if (desplegada) null else version.id
                            }
                            .padding(12.dp)
                    ) {
                        Text(fecha, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(version.titulo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            version.contenido.ifBlank { "—" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (desplegada) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (desplegada) {
                            Spacer(modifier = Modifier.size(8.dp))
                            BotonSuave(
                                stringResource(R.string.nota_historial_restaurar),
                                Icons.Outlined.History,
                                MaterialTheme.colorScheme.primary,
                                { onRestaurar(version) },
                                Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

// Una etiqueta (#palabra) en una pastilla del color de la nota.
@Composable
fun PastillaEtiqueta(etiqueta: String, color: Color, alTocar: (() -> Unit)? = null) {
    val acento = colorAcentoTexto(color)
    Text(
        "#$etiqueta",
        style = MaterialTheme.typography.labelMedium,
        color = acento,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(fondoSuave(color))
            .then(
                if (alTocar == null) Modifier
                else Modifier.clickable(onClickLabel = stringResource(R.string.nota_etiqueta_filtrar, etiqueta), onClick = alTocar)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

// El texto de la nota abierta: los bloques de texto se pueden seleccionar y
// las casillas se marcan tocándolas.
@Composable
fun TextoNotaConCasillas(contenido: String, acento: Color, alAlternar: (linea: Int) -> Unit) {
    val partes = remember(contenido) { partesDeNota(contenido) }
    val estilo = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        partes.forEach { parte ->
            when (parte) {
                is ParteNota.Texto -> SelectionContainer { Text(parte.texto, style = estilo) }
                is ParteNota.Casilla -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Checkbox) { alAlternar(parte.linea) }
                        .semantics { toggleableState = ToggleableState(parte.marcada) }
                        .padding(vertical = 6.dp, horizontal = 2.dp)
                ) {
                    Icon(
                        if (parte.marcada) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (parte.marcada) acento else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        parte.texto,
                        style = estilo,
                        color = if (parte.marcada) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (parte.marcada) TextDecoration.LineThrough else null
                    )
                }
            }
        }
    }
}
