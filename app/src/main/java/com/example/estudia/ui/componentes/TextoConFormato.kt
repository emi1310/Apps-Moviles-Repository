package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.PATRON_FORMULA
import com.example.estudia.modelo.formulaLegible

// Un bloque del texto: un párrafo, un título, un ítem de lista o código.
private sealed interface Bloque {
    data class Parrafo(val texto: String) : Bloque
    data class Titulo(val texto: String, val nivel: Int) : Bloque
    data class Item(val marca: String, val texto: String, val sangria: Int) : Bloque
    data class Codigo(val texto: String) : Bloque
    data class Formula(val texto: String) : Bloque
}

private fun bloques(markdown: String): List<Bloque> {
    val resultado = mutableListOf<Bloque>()
    val parrafo = StringBuilder()
    fun cerrarParrafo() {
        if (parrafo.isNotBlank()) resultado += Bloque.Parrafo(parrafo.toString().trim())
        parrafo.clear()
    }
    val lineas = markdown.lines()
    var i = 0
    while (i < lineas.size) {
        val linea = lineas[i]
        val sinEspacios = linea.trim()
        when {
            sinEspacios.startsWith("```") -> {
                cerrarParrafo()
                val codigo = StringBuilder()
                i++
                while (i < lineas.size && !lineas[i].trim().startsWith("```")) codigo.appendLine(lineas[i++])
                resultado += Bloque.Codigo(codigo.toString().trimEnd())
            }
            sinEspacios.startsWith("$$") && sinEspacios.endsWith("$$") && sinEspacios.length > 4 -> {
                cerrarParrafo()
                resultado += Bloque.Formula(sinEspacios.removeSurrounding("$$"))
            }
            sinEspacios.isEmpty() -> cerrarParrafo()
            Regex("^#{1,6}\\s+").containsMatchIn(sinEspacios) -> {
                cerrarParrafo()
                val nivel = sinEspacios.takeWhile { it == '#' }.length
                resultado += Bloque.Titulo(sinEspacios.drop(nivel).trim(), nivel)
            }
            Regex("^\\s*([-*•])\\s+").containsMatchIn(linea) -> {
                cerrarParrafo()
                val sangria = linea.takeWhile { it == ' ' }.length / 2
                resultado += Bloque.Item("•", linea.trim().drop(1).trim(), sangria)
            }
            Regex("^\\s*\\d{1,3}[.)]\\s+").containsMatchIn(linea) -> {
                cerrarParrafo()
                val sangria = linea.takeWhile { it == ' ' }.length / 2
                val numero = linea.trim().takeWhile { it.isDigit() }
                resultado += Bloque.Item("$numero.", linea.trim().dropWhile { it.isDigit() }.drop(1).trim(), sangria)
            }
            else -> {
                if (parrafo.isNotEmpty()) parrafo.append('\n')
                parrafo.append(sinEspacios)
            }
        }
        i++
    }
    cerrarParrafo()
    return resultado
}

// Negrita, cursiva, código y fórmulas dentro de una línea.
private val PATRON_EN_LINEA = Regex(
    PATRON_FORMULA.pattern + """|\*\*(.+?)\*\*|__(.+?)__|(?<![\w*])\*(?!\s)(.+?)(?<!\s)\*(?![\w*])|`([^`]+)`"""
)

private fun enLinea(texto: String, colorFormula: Color, fondoCodigo: Color): AnnotatedString = buildAnnotatedString {
    var desde = 0
    PATRON_EN_LINEA.findAll(texto).forEach { m ->
        append(texto.substring(desde, m.range.first))
        val g = m.groupValues
        when {
            g[1].isNotEmpty() || g[2].isNotEmpty() ->
                withStyle(SpanStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, color = colorFormula)) {
                    append(formulaLegible(g[1].ifEmpty { g[2] }))
                }
            g[3].isNotEmpty() || g[4].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(enLinea(g[3].ifEmpty { g[4] }, colorFormula, fondoCodigo)) }
            g[5].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(g[5]) }
            g[6].isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = fondoCodigo)) { append(g[6]) }
        }
        desde = m.range.last + 1
    }
    append(texto.substring(desde))
}

// Dibuja el Markdown simple de las respuestas de la IA. Mientras la respuesta
// llega (escribiendo), al final se ve un cursor.
@Composable
fun TextoConFormato(markdown: String, estilo: TextStyle, modifier: Modifier = Modifier, escribiendo: Boolean = false) {
    val colorFormula = MaterialTheme.colorScheme.primary
    val fondoCodigo = MaterialTheme.colorScheme.surfaceVariant
    val lista = remember(markdown) { bloques(if (escribiendo) "$markdown ▍" else markdown) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lista.forEach { bloque ->
            when (bloque) {
                is Bloque.Parrafo -> Text(enLinea(bloque.texto, colorFormula, fondoCodigo), style = estilo)
                is Bloque.Titulo -> Text(
                    enLinea(bloque.texto, colorFormula, fondoCodigo),
                    style = if (bloque.nivel <= 2) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                is Bloque.Item -> Row(modifier = Modifier.padding(start = (bloque.sangria * 16).dp)) {
                    Text(bloque.marca, style = estilo, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(if (bloque.marca.length > 2) 28.dp else 18.dp))
                    Text(enLinea(bloque.texto, colorFormula, fondoCodigo), style = estilo)
                }
                is Bloque.Codigo -> Text(
                    bloque.texto,
                    style = estilo.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(fondoCodigo)
                        .horizontalScroll(rememberScrollState())
                        .padding(10.dp)
                )
                is Bloque.Formula -> Text(
                    formulaLegible(bloque.texto),
                    style = estilo.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic),
                    color = colorFormula,
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 2.dp)
                )
            }
        }
    }
}
