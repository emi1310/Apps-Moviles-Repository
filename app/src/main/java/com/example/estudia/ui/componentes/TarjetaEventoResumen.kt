package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Fila que muestra un evento próximo (examen o entrega) en la pantalla
// de Inicio: título del evento y su fecha.
@Composable
fun TarjetaEventoResumen(titulo: String, fecha: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = titulo, style = MaterialTheme.typography.bodyLarge)
        Text(text = fecha, style = MaterialTheme.typography.bodySmall)
    }
}