package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.estudia.R

// Una opción del menú "⋮" (ej: Editar, Eliminar).
data class OpcionMenu(val texto: String, val icono: ImageVector, val alTocar: () -> Unit)

// Botón de tres puntos que abre un menú con opciones. Lo usan las tarjetas
// de materias, tareas, eventos y notas.
@Composable
fun MenuOpciones(opciones: List<OpcionMenu>, descripcion: String = stringResource(R.string.mas_opciones)) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = descripcion)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion.texto) },
                    leadingIcon = { Icon(opcion.icono, contentDescription = null) },
                    onClick = {
                        abierto = false
                        opcion.alTocar()
                    }
                )
            }
        }
    }
}
