package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.estudia.ui.theme.colorDesdeHex

// Fila que muestra una tarea pendiente dentro de la pantalla de Inicio:
// un círculo de color (según la materia), el título de la tarea,
// el nombre de la materia, y la fecha límite a la derecha.
@Composable
fun TarjetaTareaPendiente(titulo: String, nombreMateria: String, fechaLimite: String, colorMateria: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(colorDesdeHex(colorMateria), shape = CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, style = MaterialTheme.typography.bodyLarge)
            Text(text = nombreMateria, style = MaterialTheme.typography.bodySmall)
        }

        Text(text = fechaLimite, style = MaterialTheme.typography.bodySmall)
    }
}