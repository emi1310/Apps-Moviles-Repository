package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.estudia.R
import com.example.estudia.modelo.Materia
import com.example.estudia.ui.theme.LocalDegradado

private val RojoPeligro = Color(0xFFD93A3F)

// Ventanas emergentes con el estilo de la app: tarjeta de esquinas grandes,
// el ícono en un círculo con el degradado violeta (rojo si la acción es
// destructiva), el título, el contenido y dos botones en forma de pastilla.
// El contenido se desplaza si no entra (ej: con el teclado abierto).
@Composable
fun DialogoApp(
    titulo: String?,
    icono: ImageVector,
    // null = sin botón de confirmar (ej: cuando las opciones están en el
    // contenido); queda solo "Cancelar", de todo el ancho.
    textoConfirmar: String?,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    textoCancelar: String = stringResource(R.string.cancelar),
    destructivo: Boolean = false,
    // Tocar afuera o "Atrás". Por defecto hace lo mismo que Cancelar.
    onDismissRequest: () -> Unit = onCancelar,
    contenido: @Composable ColumnScope.() -> Unit
) {
    // Rojo fijo y fuerte: el "error" del tema oscuro es rosado y el texto
    // blanco no se leería encima.
    val rojo = RojoPeligro
    Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Lo de atrás (la app) queda desenfocado y el diálogo es de vidrio
        // líquido, como las tarjetas (ver vidrioVentana).
        DesenfocarDetras()
        val formaVentana = RoundedCornerShape(32.dp)
        // Adentro, los campos y bloques (que usan surfaceVariant o
        // surfaceContainer) pasan a ser un velo suave, así se ve el vidrio.
        val relleno = rellenoVidrio()
        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surfaceVariant = relleno, surfaceContainer = relleno)) {
        Surface(
            shape = formaVentana,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).vidrioVentana(formaVentana)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .then(if (destructivo) Modifier.background(rojo) else Modifier.background(LocalDegradado.current)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icono, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                if (titulo != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    contenido()
                }
                Spacer(modifier = Modifier.height(20.dp))
                // Los dos botones con el mismo alto aunque uno ocupe dos renglones.
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                    BotonPastilla(
                        texto = textoCancelar,
                        fondo = SolidColor(MaterialTheme.colorScheme.surfaceVariant),
                        colorTexto = MaterialTheme.colorScheme.onSurface,
                        onClick = onCancelar,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    if (textoConfirmar != null) {
                        BotonPastilla(
                            texto = textoConfirmar,
                            fondo = if (destructivo) SolidColor(rojo) else LocalDegradado.current,
                            colorTexto = Color.White,
                            onClick = onConfirmar,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun BotonPastilla(
    texto: String,
    fondo: androidx.compose.ui.graphics.Brush,
    colorTexto: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        TextoAjustable(
            texto, MaterialTheme.typography.titleSmall, Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = colorTexto, maxLines = 2, textAlign = TextAlign.Center
        )
    }
}

// Texto explicativo dentro de un diálogo (ej: para qué se pide un permiso).
@Composable
fun TextoDialogo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

// Campo de texto con forma de bloque: fondo gris suave, esquinas redondeadas
// y sin la línea de abajo.
@Composable
fun CampoTexto(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = false,
    minLines: Int = 1,
    trailingIcon: @Composable (() -> Unit)? = null,
    // Para ocultar lo escrito (ej: la clave de la IA).
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(18.dp),
        colors = coloresSinLinea(MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    )
}

// Colores de un TextField "bloque": el fondo pedido y sin la línea de abajo.
// Los usan los campos de los diálogos y el buscador.
@Composable
fun coloresSinLinea(fondo: Color): TextFieldColors {
    val sinLinea = Color.Transparent
    return TextFieldDefaults.colors(
        focusedContainerColor = fondo,
        unfocusedContainerColor = fondo,
        errorContainerColor = fondo,
        focusedIndicatorColor = sinLinea,
        unfocusedIndicatorColor = sinLinea,
        errorIndicatorColor = sinLinea,
        disabledIndicatorColor = sinLinea
    )
}

// Botón con forma de bloque (como los campos de texto) para elegir una
// fecha, una hora o una opción: ícono violeta, el texto y algo opcional a
// la derecha (ej: la flechita o la X para borrar).
@Composable
fun CampoBloque(
    icono: ImageVector,
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    final: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
        final()
    }
}

// Título chico arriba de un grupo de opciones dentro de un diálogo.
@Composable
fun EtiquetaCampo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
    )
}

// Elegir la materia: una fila de pastillas con el color de cada materia.
// La elegida se pinta entera de su color con el texto en blanco.
@Composable
fun SelectorMateria(materias: List<Materia>, seleccionada: Materia?, onSeleccionar: (Materia) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        materias.forEach { materia ->
            val color = Color(materia.color)
            val elegida = materia.id == seleccionada?.id
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (elegida) color else color.copy(alpha = 0.14f))
                    .clickable(role = Role.RadioButton) { onSeleccionar(materia) }
                    .semantics { this.selected = elegida }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (elegida) Color.White else color))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = materia.nombre,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (elegida) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// Fila con un interruptor dentro de un diálogo (toda la fila se puede tocar).
@Composable
fun FilaInterruptor(titulo: String, valor: Boolean, onCambiar: (Boolean) -> Unit, detalle: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .toggleable(value = valor, role = Role.Switch, onValueChange = onCambiar)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            if (detalle != null) {
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = valor, onCheckedChange = null)
    }
}

// Bloque elegible con ícono y texto (ej: Presencial / Virtual). El elegido
// va con el degradado violeta y el texto en blanco.
@Composable
fun BloqueOpcion(icono: ImageVector, texto: String, elegido: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
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
        Icon(icono, contentDescription = null, tint = if (elegido) Color.White else MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        TextoAjustable(
            texto, MaterialTheme.typography.labelLarge, Modifier.padding(vertical = 6.dp),
            color = if (elegido) Color.White else MaterialTheme.colorScheme.onSurface, maxLines = 2
        )
    }
}
