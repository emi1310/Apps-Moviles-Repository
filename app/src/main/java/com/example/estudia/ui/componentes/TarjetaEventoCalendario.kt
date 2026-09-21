package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Fila que muestra un evento del calendario con su horario completo,
// usada en la lista de eventos de "Hoy" y "Mañana".
@Composable
fun TarjetaEventoCalendario(titulo: String, nombreMateria: String, horaInicio: String, horaFin: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(text = titulo, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "$nombreMateria • $horaInicio - $horaFin",
            style = MaterialTheme.typography.bodySmall
        )
    }
}