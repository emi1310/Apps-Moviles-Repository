package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Materias para elegir con un toque abajo del campo "Materia" (Mesas de
// finales, Bibliografía): pastillas con el punto del color de cada materia;
// la elegida se pinta entera de ese color. colores: nombre en minúscula ->
// color de la materia de la app (las del plan van en violeta).
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SugerenciasMateria(sugerencias: List<String>, colores: Map<String, Long>, elegida: String, alElegir: (String) -> Unit) {
    if (sugerencias.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sugerencias.forEach { nombre ->
            PastillaMateria(
                nombre = nombre,
                color = colores[nombre.trim().lowercase()]?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
                elegida = elegida.trim().equals(nombre.trim(), ignoreCase = true),
                alTocar = { alElegir(nombre) }
            )
        }
    }
}

@Composable
fun PastillaMateria(nombre: String, color: Color, elegida: Boolean, alTocar: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (elegida) color else color.copy(alpha = 0.14f))
            .clickable(role = Role.RadioButton, onClick = alTocar)
            .semantics { selected = elegida }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (elegida) Color.White else color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = nombre,
            style = MaterialTheme.typography.labelLarge,
            color = if (elegida) Color.White else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
