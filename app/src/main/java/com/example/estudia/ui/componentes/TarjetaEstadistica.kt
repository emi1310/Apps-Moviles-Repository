package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// Muestra un número grande con una etiqueta debajo, usado para las
// estadísticas del perfil (materias, horas de estudio, notas, racha).
@Composable
fun TarjetaEstadistica(valor: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = valor, style = MaterialTheme.typography.titleLarge)
        Text(text = etiqueta, style = MaterialTheme.typography.bodySmall)
    }
}