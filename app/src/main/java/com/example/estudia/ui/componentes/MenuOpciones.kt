package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.estudia.R

// Una opción del menú "⋮" (ej: Editar, Eliminar). Las destructivas
// (eliminar) se pintan en rojo.
data class OpcionMenu(
    val texto: String,
    val icono: ImageVector,
    val destructiva: Boolean = false,
    val alTocar: () -> Unit
)

// Botón de tres puntos que abre un menú con opciones. Lo usan las tarjetas
// de materias, tareas, eventos y notas. Cada opción es un bloque redondeado
// con su ícono en un cuadradito de color con brillo. El menú es de vidrio.
@Composable
fun MenuOpciones(opciones: List<OpcionMenu>, descripcion: String = stringResource(R.string.mas_opciones)) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = descripcion)
        }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            shape = RoundedCornerShape(24.dp),
            // Vidrio: translúcido, con lo de atrás desenfocado y el borde brillante.
            containerColor = colorVentanaVidrio(),
            border = bordeVidrio()
        ) {
            DesenfocarDetrasDelPopup()
            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                opciones.forEach { opcion ->
                    BloqueOpcion(opcion) {
                        abierto = false
                        opcion.alTocar()
                    }
                }
            }
        }
    }
}

// Rojo fijo y fuerte para "Eliminar": el "error" del tema oscuro es rosado
// y el ícono blanco no se vería encima.
private val RojoMenu = androidx.compose.ui.graphics.Color(0xFFD93A3F)

@Composable
private fun BloqueOpcion(opcion: OpcionMenu, onClick: () -> Unit) {
    val color = if (opcion.destructiva) RojoMenu else com.example.estudia.ui.theme.BloqueVioleta
    Row(
        modifier = Modifier
            .widthIn(min = 184.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.10f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoDegradado(opcion.icono, color, tamano = 34.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = opcion.texto,
            style = MaterialTheme.typography.titleSmall,
            color = if (opcion.destructiva) color else MaterialTheme.colorScheme.onSurface
        )
    }
}
