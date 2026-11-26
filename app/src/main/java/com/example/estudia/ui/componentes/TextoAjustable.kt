package com.example.estudia.ui.componentes

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

// Texto de la app (botones, bloques, pastillas) que tiene que entrar en un
// lugar chico. En vez de cortarse con "…", primero usa hasta maxLines
// renglones y, si igual no entra, achica la letra de a poco hasta "minimo".
// Pasa sobre todo con la letra del teléfono agrandada (Accesibilidad) o con
// pantallas angostas. Solo si ni con la letra mínima entra, se corta.
@Composable
fun TextoAjustable(
    texto: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    minimo: TextUnit = 10.sp
) {
    val colorFinal = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }
    val estilo = style.merge(
        TextStyle(color = colorFinal, fontWeight = fontWeight, textAlign = textAlign ?: TextAlign.Unspecified)
    )
    val maximo = if (estilo.fontSize.isSpecified) estilo.fontSize else 14.sp
    BasicText(
        text = texto,
        modifier = modifier,
        style = estilo,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(
            minFontSize = if (minimo.value < maximo.value) minimo else maximo,
            maxFontSize = maximo,
            stepSize = 0.5.sp
        )
    )
}
