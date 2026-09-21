package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Fila que muestra una tarea pendiente dentro de la pantalla de Inicio:
// un círculo de color (según la materia), el título de la tarea,
// el nombre de la materia, y la fecha límite a la derecha.
@Composable
fun TarjetaTareaPendiente(titulo: String, nombreMateria: String, fechaLimite: String, colorMateria: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(colorMateria, shape = CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // weight(1f) hace que esta columna ocupe el espacio sobrante
        // y empuje la fecha hacia la derecha.
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titulo, style = MaterialTheme.typography.bodyLarge)
            Text(text = nombreMateria, style = MaterialTheme.typography.bodySmall)
        }

        Text(text = fechaLimite, style = MaterialTheme.typography.bodySmall)
    }
}
