package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.estudia.R

// Base de las pantallas que se abren desde otra (Mi carrera, Asistencia,
// Finales...): flecha para volver, título, subtítulo y una lista. Si se
// pasa textoAgregar, abajo a la derecha aparece el botón para agregar.
@Composable
fun PantallaDetalle(
    titulo: String,
    onVolver: () -> Unit,
    subtitulo: String? = null,
    textoAgregar: String? = null,
    iconoAgregar: ImageVector? = null,
    onAgregar: () -> Unit = {},
    acciones: @Composable RowScope.() -> Unit = {},
    contenido: LazyListScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            // Abajo queda lugar para el botón de agregar y la barra del sistema.
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.volver))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = titulo,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.semantics { heading() }
                        )
                        if (subtitulo != null) {
                            Text(
                                text = subtitulo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    acciones()
                }
            }
            contenido()
        }
        if (textoAgregar != null && iconoAgregar != null) {
            ExtendedFloatingActionButton(
                onClick = onAgregar,
                icon = { Icon(iconoAgregar, contentDescription = null) },
                text = { Text(textoAgregar) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(20.dp)
            )
        }
    }
}

// Título de un grupo dentro de la lista (ej: "1.º año", "Rendidos").
@Composable
fun TituloLista(texto: String, detalle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
        Text(
            text = texto,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        if (detalle != null) {
            Text(text = detalle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Botón de fondo suave del color dado (como los de las tarjetas de materia).
@Composable
fun BotonSuave(
    texto: String,
    icono: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    relleno: Boolean = false
) {
    val colorTexto = if (relleno) Color.White else colorAcentoTexto(color)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (relleno) color else color.copy(alpha = 0.14f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = colorTexto, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.dp))
        TextoAjustable(texto, MaterialTheme.typography.labelLarge, color = colorTexto, maxLines = 2, textAlign = TextAlign.Center)
    }
}

// Pastilla chica con un estado (ej: "Inscripto", "Regular").
@Composable
fun PastillaEstado(texto: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = colorAcentoTexto(color),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

// Fila de tarjeta simple: fondo de superficie y esquinas redondeadas.
@Composable
fun FilaTarjeta(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, contenido: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(bordeVidrio(), RoundedCornerShape(22.dp))
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = contenido
    )
}
