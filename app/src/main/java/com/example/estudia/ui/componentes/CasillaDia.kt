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

// Un casillero del calendario: muestra el número de día, y si es "hoy",
// lo resalta con un círculo de fondo.
@Composable
fun CasillaDia(dia: Int?, esHoy: Boolean) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .then(
                if (esHoy) Modifier.background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (dia != null) {
            Text(
                text = dia.toString(),
                color = if (esHoy) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}