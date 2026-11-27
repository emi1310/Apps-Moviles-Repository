package com.example.estudia.modelo

import java.text.Normalizer
import kotlin.random.Random

// ---------- Respuesta escrita en el repaso ----------

// Qué tan bien coincide lo que escribió con la respuesta de la tarjeta.
enum class ResultadoRespuesta { CORRECTA, CASI, INCORRECTA }

// Sin mayúsculas, tildes, signos ni espacios de más: "¡La Célula!" -> "la celula".
fun normalizarRespuesta(texto: String): String =
    Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()

// CORRECTA si es igual (sin contar tildes, mayúsculas ni signos). CASI si
// tiene un error de tipeo cada cinco letras como mucho, o si en una
// respuesta larga escribió casi todas las palabras. El usuario decide
// igual si "la sabía": esto solo lo ayuda a ser honesto.
fun compararRespuesta(escrita: String, correcta: String): ResultadoRespuesta {
    val a = normalizarRespuesta(escrita)
    val b = normalizarRespuesta(correcta)
    if (a.isEmpty()) return ResultadoRespuesta.INCORRECTA
    if (a == b) return ResultadoRespuesta.CORRECTA
    val tolerancia = maxOf(1, b.length / 5)
    if (distanciaEdicion(a, b) <= tolerancia) return ResultadoRespuesta.CASI
    val palabrasCorrectas = b.split(' ').filter { it.length > 2 }.toSet()
    if (palabrasCorrectas.size >= 4) {
        val escritas = a.split(' ').toSet()
        val acertadas = palabrasCorrectas.count { it in escritas }
        if (acertadas * 10 >= palabrasCorrectas.size * 7) return ResultadoRespuesta.CASI
    }
    return ResultadoRespuesta.INCORRECTA
}

// Distancia de Levenshtein: cuántas letras hay que agregar, borrar o cambiar.
fun distanciaEdicion(a: String, b: String): Int {
    var anterior = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        val actual = IntArray(b.length + 1)
        actual[0] = i
        for (j in 1..b.length) {
            val costo = if (a[i - 1] == b[j - 1]) 0 else 1
            actual[j] = minOf(anterior[j] + 1, actual[j - 1] + 1, anterior[j - 1] + costo)
        }
        anterior = actual
    }
    return anterior[b.length]
}

// ---------- Importar tarjetas (CSV, Quizlet, Anki) ----------

const val MAXIMO_TARJETAS_IMPORTADAS = 500

// Lee pares pregunta/respuesta de un texto:
//  - Quizlet ("Exportar"): término y definición separados por tabulación,
//    una tarjeta por línea.
//  - Anki ("Exportar" -> "Notas en texto plano"): separado por tabulación,
//    con líneas de configuración que empiezan con # y a veces HTML.
//  - CSV de Excel o Google Sheets: separado por coma o punto y coma, con
//    comillas si el texto tiene comas o saltos de línea.
// Se usan las dos primeras columnas; las filas sin pregunta o sin respuesta
// se saltean.
fun leerTarjetasDeTexto(texto: String): List<Pair<String, String>> {
    val limpio = texto.removePrefix("﻿").replace("\r\n", "\n").replace('\r', '\n')
    val lineas = limpio.lines().filter { !it.startsWith("#") }
    val utiles = lineas.filter { it.isNotBlank() }
    if (utiles.isEmpty()) return emptyList()
    val separador = when {
        utiles.any { '\t' in it } -> '\t'
        utiles.count { ';' in it } >= utiles.count { ',' in it } && utiles.any { ';' in it } -> ';'
        utiles.any { ',' in it } -> ','
        else -> return emptyList()
    }
    return filasCsv(lineas.joinToString("\n"), separador)
        .mapNotNull { columnas ->
            val pregunta = sinHtml(columnas.getOrNull(0).orEmpty())
            val respuesta = sinHtml(columnas.getOrNull(1).orEmpty())
            if (pregunta.isBlank() || respuesta.isBlank()) null else pregunta to respuesta
        }
        .distinctBy { it.first.lowercase() }
        .take(MAXIMO_TARJETAS_IMPORTADAS)
}

// Separa en filas y columnas respetando las comillas ("a, b" es una sola
// columna y "" es una comilla dentro del texto).
private fun filasCsv(texto: String, separador: Char): List<List<String>> {
    val filas = mutableListOf<List<String>>()
    var fila = mutableListOf<String>()
    val campo = StringBuilder()
    var entreComillas = false
    var i = 0
    while (i < texto.length) {
        val c = texto[i]
        when {
            entreComillas && c == '"' && texto.getOrNull(i + 1) == '"' -> { campo.append('"'); i++ }
            c == '"' && (entreComillas || campo.isBlank()) -> { entreComillas = !entreComillas; if (entreComillas) campo.clear() }
            !entreComillas && c == separador -> { fila.add(campo.toString()); campo.clear() }
            !entreComillas && c == '\n' -> { fila.add(campo.toString()); campo.clear(); filas.add(fila); fila = mutableListOf() }
            else -> campo.append(c)
        }
        i++
    }
    fila.add(campo.toString())
    filas.add(fila)
    return filas.filter { columnas -> columnas.any { it.isNotBlank() } }
}

// Anki guarda negritas, saltos de línea y espacios como HTML.
private fun sinHtml(texto: String): String =
    texto.replace(Regex("(?i)<br\\s*/?>|</div>|</p>"), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&")
        .lines().joinToString("\n") { it.trim() }
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()

// ---------- Juego "Unir pares" ----------

const val PARES_POR_JUEGO = 6
const val MINIMO_PARES = 3
// Cada error suma un segundo al tiempo final.
const val PENALIZACION_PARES_MILLIS = 1_000L

// Una ficha del tablero: la pregunta o la respuesta de una tarjeta. Dos
// fichas son pareja si tienen el mismo par (el id de la tarjeta).
data class FichaPar(
    val id: Int,
    val par: Int,
    val texto: String,
    val esPregunta: Boolean,
    val imagen: String? = null
)

// Hasta 6 tarjetas al azar (12 fichas mezcladas). Las tarjetas con la misma
// respuesta o la misma pregunta no se usan juntas, para que no haya dos
// parejas posibles. Lista vacía si no alcanzan 3 tarjetas.
fun armarPares(tarjetas: List<Tarjeta>, random: Random, cantidad: Int = PARES_POR_JUEGO): List<FichaPar> {
    val elegidas = tarjetas.shuffled(random)
        .filter { it.respuesta.isNotBlank() && (it.pregunta.isNotBlank() || it.imagen != null) }
        .distinctBy { normalizarRespuesta(it.respuesta) }
        .distinctBy { normalizarRespuesta(it.pregunta) + it.imagen.orEmpty() }
        .take(cantidad)
    if (elegidas.size < MINIMO_PARES) return emptyList()
    return elegidas.flatMap { t ->
        listOf(
            FichaPar(0, t.id, t.pregunta, esPregunta = true, imagen = t.imagen),
            FichaPar(0, t.id, t.respuesta, esPregunta = false)
        )
    }.shuffled(random).mapIndexed { indice, ficha -> ficha.copy(id = indice) }
}

// "1:05.3" (minutos, segundos y décimas).
fun textoTiempoPares(millis: Long): String {
    val decimas = millis / 100
    return "%d:%02d.%d".format(decimas / 600, decimas / 10 % 60, decimas % 10)
}

// ---------- Meta diaria ----------

// Duraciones que se pueden elegir como meta de estudio por día (minutos).
val OPCIONES_META_DIARIA = listOf(30, 60, 90, 120, 180, 240)

// De 0 a 1 para el anillo (0 si no hay meta).
fun avanceMetaDiaria(minutosHoy: Int, meta: Int): Float =
    if (meta <= 0) 0f else (minutosHoy.toFloat() / meta).coerceIn(0f, 1f)
