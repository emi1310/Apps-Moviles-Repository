package com.example.estudia.modelo

import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// Un mapa mental: una idea central y ramas que salen de ella. Es del
// usuario y, como la bibliografía, guarda el nombre de la materia (vacío =
// sin materia).
data class MapaMental(
    val id: Int,
    val materia: String,
    val titulo: String,
    val nodos: List<NodoMapa>,
    val modificado: LocalDate
) {
    val raiz: NodoMapa get() = nodos.first { it.padreId == null }

    fun hijos(id: Int): List<NodoMapa> = nodos.filter { it.padreId == id }

    // Cantidad de ideas sin contar la central.
    val cantidadIdeas: Int get() = nodos.size - 1

    companion object {
        const val ID_RAIZ = 1
        // Distancia (en dp) entre una idea y la que sale de ella.
        const val DISTANCIA_RAMA = 190f
        const val DISTANCIA_SUBRAMA = 160f

        // Un mapa nuevo tiene solo la idea central (con el título), en el centro.
        fun nuevo(id: Int, materia: String, titulo: String, hoy: LocalDate) =
            MapaMental(id, materia, titulo, listOf(NodoMapa(ID_RAIZ, null, titulo, 0f, 0f)), hoy)
    }
}

// Una idea del mapa. x e y en dp, con la idea central en (0, 0). color: un
// índice de PALETA_MAPA, o null = el de su rama.
data class NodoMapa(
    val id: Int,
    val padreId: Int?,
    val texto: String,
    val x: Float,
    val y: Float,
    val color: Int? = null
)

// Cantidad de colores de la paleta de las ramas (los define la pantalla).
const val COLORES_MAPA = 6

// Agrega una idea que sale de padreId y devuelve el mapa y la idea nueva.
// Se ubica sola para no taparse con las demás:
//  - Desde la central, las ramas se reparten alrededor como un reloj.
//  - Desde otra idea, sale hacia afuera (en la dirección central -> padre) y
//    los hermanos se abren en abanico.
fun MapaMental.agregarIdea(padreId: Int, texto: String): Pair<MapaMental, NodoMapa> {
    val padre = nodos.first { it.id == padreId }
    val hermanos = hijos(padreId).size
    val (x, y) = if (padre.padreId == null) {
        // Empieza arriba a la derecha y sigue en el sentido del reloj; cada
        // vuelta un poco más lejos.
        val angulo = Math.toRadians(-60.0 + hermanos * 52.0)
        val distancia = MapaMental.DISTANCIA_RAMA * (1 + hermanos / 7 * 0.45f)
        padre.x + (cos(angulo) * distancia).toFloat() to padre.y + (sin(angulo) * distancia).toFloat()
    } else {
        val abuelo = nodos.first { it.id == padre.padreId }
        val base = atan2((padre.y - abuelo.y).toDouble(), (padre.x - abuelo.x).toDouble())
        // 0, +28°, -28°, +56°, -56°...
        val paso = (hermanos + 1) / 2 * (if (hermanos % 2 == 1) 1 else -1)
        val angulo = base + Math.toRadians(paso * 28.0)
        padre.x + (cos(angulo) * MapaMental.DISTANCIA_SUBRAMA).toFloat() to padre.y + (sin(angulo) * MapaMental.DISTANCIA_SUBRAMA).toFloat()
    }
    val nuevo = NodoMapa((nodos.maxOf { it.id }) + 1, padreId, texto.trim(), x, y)
    return copy(nodos = nodos + nuevo) to nuevo
}

// Cambia el texto de una idea. Si es la central, también el título del mapa.
fun MapaMental.cambiarTexto(id: Int, texto: String): MapaMental {
    val limpio = texto.trim()
    if (limpio.isEmpty()) return this
    val nuevos = nodos.map { if (it.id == id) it.copy(texto = limpio) else it }
    return copy(nodos = nuevos, titulo = if (id == raiz.id) limpio else titulo)
}

fun MapaMental.cambiarColor(id: Int, color: Int?): MapaMental =
    copy(nodos = nodos.map { if (it.id == id) it.copy(color = color) else it })

// Todos los ids de la rama que empieza en id (incluido).
fun MapaMental.rama(id: Int): Set<Int> {
    val ids = mutableSetOf(id)
    var agregados = true
    while (agregados) {
        val nuevos = nodos.filter { it.padreId in ids && it.id !in ids }.map { it.id }
        agregados = nuevos.isNotEmpty()
        ids += nuevos
    }
    return ids
}

// Borra una idea con todo lo que sale de ella. La central no se borra.
fun MapaMental.eliminarIdea(id: Int): MapaMental {
    if (id == raiz.id) return this
    val borrar = rama(id)
    return copy(nodos = nodos.filter { it.id !in borrar })
}

// Mueve una idea y, con ella, todo lo que sale de ella.
fun MapaMental.moverRama(id: Int, dx: Float, dy: Float): MapaMental {
    val mover = rama(id)
    return copy(nodos = nodos.map { if (it.id in mover) it.copy(x = it.x + dx, y = it.y + dy) else it })
}

// Color de una idea: el suyo o el de la rama principal a la que pertenece
// (cada rama que sale de la central tiene uno distinto). La central: null.
fun MapaMental.colorDe(id: Int): Int? {
    var nodo = nodos.first { it.id == id }
    while (true) {
        nodo.color?.let { return it }
        val padre = nodos.first { it.id == (nodo.padreId ?: return null) }
        // nodo sale de la central: es una rama principal, va su color por orden.
        if (padre.padreId == null) return hijos(padre.id).indexOfFirst { it.id == nodo.id }.mod(COLORES_MAPA)
        nodo = padre
    }
}

// ---------- Guardar los nodos como texto ----------
// Una idea por línea: id|padre|x|y|color|texto. En el texto se escapan la
// barra invertida, el "|" y los saltos de línea. Así se guarda en una
// columna de la base y en la copia de seguridad sin depender de otra librería.

fun codificarNodos(nodos: List<NodoMapa>): String = nodos.joinToString("\n") { n ->
    listOf(n.id, n.padreId ?: "", n.x, n.y, n.color ?: "", escapar(n.texto)).joinToString("|")
}

fun decodificarNodos(texto: String): List<NodoMapa> = texto.lines().filter { it.isNotBlank() }.mapNotNull { linea ->
    val partes = linea.split('|', limit = 6)
    if (partes.size < 6) return@mapNotNull null
    NodoMapa(
        id = partes[0].toIntOrNull() ?: return@mapNotNull null,
        padreId = partes[1].toIntOrNull(),
        x = partes[2].toFloatOrNull() ?: 0f,
        y = partes[3].toFloatOrNull() ?: 0f,
        color = partes[4].toIntOrNull(),
        texto = desescapar(partes[5])
    )
}

private fun escapar(t: String) = t.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n")

private fun desescapar(t: String): String {
    val salida = StringBuilder()
    var i = 0
    while (i < t.length) {
        val c = t[i]
        if (c == '\\' && i + 1 < t.length) {
            when (t[i + 1]) {
                'n' -> salida.append('\n')
                'p' -> salida.append('|')
                else -> salida.append(t[i + 1])
            }
            i += 2
        } else {
            salida.append(c)
            i++
        }
    }
    return salida.toString()
}
