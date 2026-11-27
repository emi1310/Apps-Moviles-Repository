package com.example.estudia.modelo

// Lo que se puede escribir dentro del texto de una nota, además del texto:
//  - Casillas: una línea "- [ ] Leer cap. 3" es una casilla para marcar;
//    "- [x] ..." es una marcada. En la nota abierta se tocan para marcarlas.
//  - Etiquetas: palabras con # (#parcial, #importante). Sirven para filtrar
//    la lista de notas. "# Título" (con espacio) no es una etiqueta.

private val PATRON_CASILLA = Regex("""^(\s*)[-*]\s\[( |x|X)\]\s?(.*)$""")
private val PATRON_ETIQUETA = Regex("""(?<![\p{L}\p{N}_#&/])#([\p{L}\p{N}_][\p{L}\p{N}_-]{1,29})""")

// Una parte de la nota abierta: un bloque de texto o una casilla.
// linea es el número de línea de la casilla dentro del contenido.
sealed interface ParteNota {
    data class Texto(val texto: String) : ParteNota
    data class Casilla(val linea: Int, val texto: String, val marcada: Boolean) : ParteNota
}

// Divide el contenido en bloques de texto y casillas (las líneas seguidas que
// no son casillas van juntas en un mismo bloque).
fun partesDeNota(contenido: String): List<ParteNota> {
    val partes = mutableListOf<ParteNota>()
    val bloque = mutableListOf<String>()
    fun cerrarBloque() {
        if (bloque.isNotEmpty()) {
            val texto = bloque.joinToString("\n").trim('\n')
            if (texto.isNotBlank()) partes += ParteNota.Texto(texto)
            bloque.clear()
        }
    }
    contenido.lines().forEachIndexed { i, linea ->
        val casilla = PATRON_CASILLA.matchEntire(linea)
        if (casilla == null) {
            bloque += linea
        } else {
            cerrarBloque()
            partes += ParteNota.Casilla(i, casilla.groupValues[3], casilla.groupValues[2] != " ")
        }
    }
    cerrarBloque()
    return partes
}

// Marca o desmarca la casilla de esa línea. Si la línea no es una casilla,
// el contenido queda igual.
fun alternarCasilla(contenido: String, linea: Int): String {
    val lineas = contenido.lines().toMutableList()
    val actual = lineas.getOrNull(linea) ?: return contenido
    val casilla = PATRON_CASILLA.matchEntire(actual) ?: return contenido
    val marca = if (casilla.groupValues[2] == " ") "x" else " "
    lineas[linea] = "${casilla.groupValues[1]}- [$marca] ${casilla.groupValues[3]}".trimEnd()
    return lineas.joinToString("\n")
}

// Cuántas casillas hay marcadas y cuántas hay en total.
fun progresoCasillas(contenido: String): Pair<Int, Int> {
    val casillas = contenido.lines().mapNotNull { PATRON_CASILLA.matchEntire(it) }
    return casillas.count { it.groupValues[2] != " " } to casillas.size
}

// Suma una casilla vacía al final, para escribir en ella.
fun agregarCasilla(contenido: String): String = when {
    contenido.isBlank() -> "- [ ] "
    else -> contenido.trimEnd() + "\n- [ ] "
}

// true si entre las dos versiones solo cambiaron casillas marcadas: eso no
// se guarda en el historial (si no, cada toque dejaría una versión).
fun soloCambianCasillas(antes: String, despues: String): Boolean {
    fun sinMarcas(texto: String) = texto.lines().joinToString("\n") { linea ->
        PATRON_CASILLA.matchEntire(linea)?.let { "${it.groupValues[1]}- [ ] ${it.groupValues[3]}".trimEnd() } ?: linea
    }
    return antes != despues && sinMarcas(antes) == sinMarcas(despues)
}

// true si el cambio de una nota tiene que quedar en el historial.
fun cambioParaHistorial(tituloAntes: String, contenidoAntes: String, tituloDespues: String, contenidoDespues: String): Boolean =
    tituloAntes != tituloDespues || (contenidoAntes != contenidoDespues && !soloCambianCasillas(contenidoAntes, contenidoDespues))

// Las etiquetas (#palabra) del texto, en minúscula y sin repetir. Tiene que
// haber al menos una letra (#1 o #2026 no son etiquetas).
fun etiquetasDe(texto: String): List<String> =
    PATRON_ETIQUETA.findAll(texto)
        .map { it.groupValues[1].trimEnd('-', '_').lowercase() }
        .filter { it.length >= 2 && it.any(Char::isLetter) }
        .distinct()
        .toList()

// Las etiquetas de una nota (de su título y su texto).
val Nota.etiquetas: List<String> get() = etiquetasDe(titulo + "\n" + contenido)

// El texto para la tarjeta de la lista: las casillas se ven como ☐ y ☑.
fun vistaPreviaNota(contenido: String): String =
    contenido.lines().joinToString("\n") { linea ->
        PATRON_CASILLA.matchEntire(linea)?.let {
            it.groupValues[1] + (if (it.groupValues[2] == " ") "☐ " else "☑ ") + it.groupValues[3]
        } ?: linea
    }

// Lo que se suma al texto de la nota al guardar una grabación con marcas:
// "🎙 Grabación 27/11 14:05 · marcas: 00:45, 03:10".
fun textoMarcasGrabacion(contenido: String, cuando: String, marcas: List<String>): String {
    if (marcas.isEmpty()) return contenido
    val linea = "🎙 Grabación $cuando · marcas: ${marcas.joinToString(", ")}"
    return if (contenido.isBlank()) linea else contenido.trimEnd() + "\n\n" + linea
}

// Segundos como "mm:ss" (o "h:mm:ss" si pasa la hora).
fun tiempoGrabacion(segundos: Long): String {
    val h = segundos / 3600
    val m = (segundos % 3600) / 60
    val s = segundos % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
