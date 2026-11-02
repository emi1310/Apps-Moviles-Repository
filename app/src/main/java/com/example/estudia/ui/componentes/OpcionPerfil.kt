package com.example.estudia.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// Fila clickeable del menú de perfil (ej: "Editar perfil", "Ayuda"): ícono
// en un círculo de color, texto y flecha. Las destructivas van en rojo.
@Composable
fun OpcionPerfil(texto: String, icono: ImageVector, esDestructiva: Boolean = false, alTocar: () -> Unit) {
    val color = if (esDestructiva) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { alTocar() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoCircular(icono, color, tamano = 36.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyLarge,
            color = if (esDestructiva) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
