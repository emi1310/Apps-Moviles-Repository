package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CasillaDia(
    dia: Int?,
    esHoy: Boolean,
    seleccionado: Boolean,
    tieneEventos: Boolean,
    modifier: Modifier = Modifier,
    onSeleccionar: () -> Unit
) {
    val colorFondo = if (seleccionado) {
        MaterialTheme.colorScheme.primary
    } else if (esHoy) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }

    val colorTexto = if (seleccionado) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .background(colorFondo, shape = CircleShape)
            .clickable(enabled = dia != null, onClick = onSeleccionar),
        contentAlignment = Alignment.Center
    ) {
        if (dia != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = dia.toString(), color = colorTexto)
                if (tieneEventos) {
                    Box(
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(4.dp)
                            .background(
                                if (seleccionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}
