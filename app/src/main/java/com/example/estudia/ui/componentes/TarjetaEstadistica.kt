package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// Tarjetita con un ícono, un número grande y una etiqueta, usada para las
// estadísticas del perfil (materias, horas de estudio, notas).
@Composable
fun TarjetaEstadistica(valor: String, etiqueta: String, icono: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconoCircular(icono, color, tamano = 36.dp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = valor, style = MaterialTheme.typography.titleLarge)
            Text(text = etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
