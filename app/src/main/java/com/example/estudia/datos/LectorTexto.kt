package com.example.estudia.datos

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// OCR con ML Kit: lee el texto de una foto (ej: el pizarrón). El modelo
// para letras latinas viene dentro de la app, así que funciona sin
// internet y la foto nunca sale del teléfono.
class LectorTexto(private val context: Context) {

    // Devuelve el texto encontrado ("" si no hay), o null si no se pudo
    // leer la imagen.
    suspend fun leer(uri: Uri): String? {
        val imagen = try {
            // fromFilePath respeta la rotación EXIF de la foto.
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            return null
        }
        val reconocedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            suspendCancellableCoroutine { continuacion ->
                reconocedor.process(imagen)
                    .addOnSuccessListener { continuacion.resume(it.text.trim()) }
                    .addOnFailureListener { continuacion.resume(null) }
            }
        } finally {
            reconocedor.close()
        }
    }
}
