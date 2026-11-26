package com.example.estudia.modelo

// Una fuente de la bibliografía: un libro, un artículo, una página web o un
// apunte de cátedra. Es del usuario y se agrupa por materia (se guarda el
// nombre, como en los finales: la materia puede estar archivada o ser del
// plan). materia vacía = bibliografía general.
//
// autores: uno por línea o separados por ";", con el formato "Apellido, N."
// (ej: "Cormen, T.; Leiserson, C."). Para una institución, su nombre.
// origen: la editorial (libro), la revista (artículo), el sitio (web) o la
// cátedra / facultad (apunte).
data class Fuente(
    val id: Int,
    val materia: String,
    val tipo: TipoFuente,
    val autores: String,
    val anio: Int?,
    val titulo: String,
    val origen: String = "",
    val enlace: String = "",
    val paginas: String = "",
    val comentario: String = ""
) {
    // Lista de autores ya separados y sin espacios de más.
    val listaAutores: List<String>
        get() = autores.split(';', '\n').map { it.trim() }.filter { it.isNotEmpty() }
}

enum class TipoFuente { LIBRO, ARTICULO, WEB, APUNTE;
    companion object {
        fun desdeTexto(texto: String?): TipoFuente = entries.firstOrNull { it.name == texto } ?: LIBRO
    }
}

// Un pedazo de la cita: en normas APA el título del libro (o el nombre de la
// revista) va en cursiva. La pantalla arma el texto con estilo y el
// portapapeles se lleva el texto plano.
data class TrozoCita(val texto: String, val cursiva: Boolean = false)

fun List<TrozoCita>.textoPlano(): String = joinToString("") { it.texto }

// Cita en la lista de referencias con normas APA 7 (en castellano: "y" entre
// los dos últimos autores y "s.f." si no tiene año).
//  Libro:    Autores (Año). *Título*. Editorial.
//  Artículo: Autores (Año). Título. *Revista*, páginas. enlace
//  Web:      Autores (Año). *Título*. Sitio. enlace
//  Apunte:   Autores (Año). *Título* [Apunte de cátedra]. Cátedra.
fun citaApa(f: Fuente): List<TrozoCita> {
    val trozos = mutableListOf<TrozoCita>()
    fun texto(t: String) { if (t.isNotEmpty()) trozos += TrozoCita(t) }
    fun cursiva(t: String) { if (t.isNotEmpty()) trozos += TrozoCita(t, cursiva = true) }

    val titulo = f.titulo.trim().trimEnd('.')
    val origen = f.origen.trim().trimEnd('.')
    val autores = autoresApa(f.listaAutores)
    val anio = "(${f.anio ?: "s.f."})."
    fun parteTitulo() = when (f.tipo) {
        TipoFuente.ARTICULO -> texto("$titulo. ")
        TipoFuente.APUNTE -> { cursiva(titulo); texto(" [Apunte de cátedra]. ") }
        else -> { cursiva(titulo); texto(". ") }
    }
    if (autores.isEmpty()) {
        // Sin autores, APA pone el título primero y después el año.
        parteTitulo()
        texto("$anio ")
    } else {
        // "Apellido, N." ya termina en punto; una institución no.
        texto((if (autores.endsWith('.')) autores else "$autores.") + " $anio ")
        parteTitulo()
    }
    when (f.tipo) {
        TipoFuente.ARTICULO -> if (origen.isNotEmpty()) {
            cursiva(origen)
            texto(if (f.paginas.isNotBlank()) ", ${f.paginas.trim()}. " else ". ")
        }
        else -> if (origen.isNotEmpty()) texto("$origen. ")
    }
    texto(f.enlace.trim())
    return trozos.let { lista ->
        // Sin espacio al final.
        val ultimo = lista.lastOrNull() ?: return lista
        lista.dropLast(1) + ultimo.copy(texto = ultimo.texto.trimEnd())
    }.filter { it.texto.isNotEmpty() }
}

// Cita dentro del texto: (Apellido, año), (Apellido y Apellido, año) o
// (Apellido et al., año) con tres autores o más.
fun citaEnTexto(f: Fuente): String {
    val apellidos = f.listaAutores.map { it.substringBefore(',').trim() }
    val quien = when (apellidos.size) {
        0 -> f.titulo.trim().trimEnd('.')
        1 -> apellidos[0]
        2 -> "${apellidos[0]} y ${apellidos[1]}"
        else -> "${apellidos[0]} et al."
    }
    return "($quien, ${f.anio ?: "s.f."})"
}

// "A", "A y B", "A, B y C"... (APA 7 nombra hasta 20 autores).
private fun autoresApa(autores: List<String>): String = when (autores.size) {
    0 -> ""
    1 -> autores[0]
    else -> autores.take(20).let { a -> a.dropLast(1).joinToString(", ") + " y " + a.last() }
}
