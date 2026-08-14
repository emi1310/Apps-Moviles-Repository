package com.example.estudia.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Tarjeta que muestra una nota: título, materia, contenido (recortado
// a 2 líneas), fecha, y una estrella para marcar/desmarcar favorita.
@Composable
fun TarjetaNota(
    titulo: String,
    nombreMateria: String,
    contenido: String,
    fecha: String,
    esFavorita: Boolean,
    alTocarEstrella: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (esFavorita) "★" else "☆",
                    modifier = Modifier.clickable { alTocarEstrella() },
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Text(text = nombreMateria, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = contenido, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = fecha, style = MaterialTheme.typography.bodySmall)
        }
    }
}