package com.example.estudia.ui.theme

import androidx.compose.ui.graphics.Color

// Convierte un color guardado como texto (ej: "#6C5CE7") al tipo Color
// que Jetpack Compose necesita para pintar elementos en pantalla.
fun colorDesdeHex(hex: String): Color {
    return Color(android.graphics.Color.parseColor(hex))
}