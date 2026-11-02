package com.example.estudia.modelo

import java.time.LocalDate

// Archivo adjunto a una materia o a una nota (imagen, PDF, documento...).
// notaId es null si el archivo está adjunto directamente a la materia.
// "ruta" es el nombre del archivo dentro del almacenamiento interno de la
// app; la UI nunca lo lee directo, lo pide al ViewModel.
data class Archivo(
    val id: Int,
    val nombre: String,
    val tipoMime: String,
    val ruta: String,
    val tamanoBytes: Long,
    val fechaAgregado: LocalDate,
    val notaId: Int? = null
) {
    val esImagen: Boolean get() = tipoMime.startsWith("image/")
    val esPdf: Boolean get() = tipoMime == "application/pdf"

    // Tamaño legible, ej: "193 B", "350 KB" o "2.1 MB".
    fun tamanoLegible(): String {
        if (tamanoBytes < 1024) return "$tamanoBytes B"
        val kb = tamanoBytes / 1024.0
        return if (kb < 1024) "${kb.toInt()} KB" else String.format("%.1f MB", kb / 1024)
    }
}
