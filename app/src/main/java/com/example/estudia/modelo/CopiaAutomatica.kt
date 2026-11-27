package com.example.estudia.modelo

import java.time.LocalDate

// Copia de seguridad automática: una vez por semana se guarda sola una copia
// en la carpeta que eligió el usuario, y se conservan las últimas.

// carpeta: la carpeta elegida (content:// como texto); null = apagada.
data class EstadoCopiaAutomatica(val carpeta: String? = null, val ultima: LocalDate? = null) {
    val activa: Boolean get() = carpeta != null
}

const val COPIAS_A_CONSERVAR = 4

fun prefijoCopiaAutomatica(nombreUsuario: String): String =
    "estudia_auto_" + nombreUsuario.replace(Regex("[^A-Za-z0-9_-]"), "_") + "_"

fun nombreCopiaAutomatica(nombreUsuario: String, fecha: LocalDate): String =
    prefijoCopiaAutomatica(nombreUsuario) + fecha + ".zip"

// De los archivos de la carpeta, las copias automáticas de este usuario que
// sobran (las más viejas). Las fechas AAAA-MM-DD ordenan bien como texto.
fun copiasParaBorrar(nombres: List<String>, nombreUsuario: String, conservar: Int = COPIAS_A_CONSERVAR): List<String> {
    val prefijo = prefijoCopiaAutomatica(nombreUsuario)
    return nombres.filter { it.startsWith(prefijo) && it.endsWith(".zip") }.sortedDescending().drop(conservar)
}
