package com.example.estudia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Estilos de texto de la app. Parte de los de Material 3 y les da más peso
// a los títulos, para que se distinga mejor qué es título y qué es texto.
private val Base = Typography()

val Typography = Typography(
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp)
)
