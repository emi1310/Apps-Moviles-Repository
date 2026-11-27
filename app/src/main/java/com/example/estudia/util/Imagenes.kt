package com.example.estudia.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Lectura y guardado de imágenes del teléfono. Es trabajo de archivos (en
// segundo plano), por eso está acá y no en las pantallas: ellas solo lo llaman.

// Lee una imagen de la app (content://) achicada a "ladoMaximo" píxeles y
// derecha: las fotos de la cámara guardan la rotación en los datos EXIF.
// Se llama desde una corrutina: trabaja en Dispatchers.IO.
suspend fun leerImagen(context: Context, uri: Uri, ladoMaximo: Int): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val resolver = context.contentResolver
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medidas) }
        var muestra = 1
        while (medidas.outWidth / (muestra * 2) >= ladoMaximo && medidas.outHeight / (muestra * 2) >= ladoMaximo) muestra *= 2
        val imagen = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra })
        } ?: return@withContext null
        val grados = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (grados == 0f) imagen
        else Bitmap.createBitmap(imagen, 0, 0, imagen.width, imagen.height, Matrix().apply { postRotate(grados) }, true)
    } catch (e: Exception) {
        null
    }
}

// Guarda la imagen como PNG en cacheDir/mapas (borra las anteriores) y
// devuelve un enlace content:// para compartirla con otras apps.
suspend fun guardarImagenParaCompartir(contexto: Context, imagen: Bitmap, titulo: String): Uri = withContext(Dispatchers.IO) {
    val carpeta = File(contexto.cacheDir, "mapas").apply { mkdirs() }
    carpeta.listFiles()?.forEach { it.delete() }
    val nombre = titulo.replace(Regex("""[^\p{L}\p{N} _-]"""), "").trim().ifEmpty { "mapa" }.take(40)
    val archivo = File(carpeta, "$nombre.png")
    archivo.outputStream().use { imagen.compress(Bitmap.CompressFormat.PNG, 100, it) }
    FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", archivo)
}

// La foto como JPEG achicado (para mandarla a la IA). null si no se pudo leer.
suspend fun leerJpeg(context: Context, uri: Uri, ladoMaximo: Int = 1600, calidad: Int = 85): ByteArray? {
    val imagen = leerImagen(context, uri, ladoMaximo) ?: return null
    return withContext(Dispatchers.IO) {
        java.io.ByteArrayOutputStream().also { imagen.compress(Bitmap.CompressFormat.JPEG, calidad, it) }.toByteArray()
    }
}

// Lee un archivo (content://) entero, hasta "maximo" bytes. Si es más grande,
// devuelve maximo + 1 bytes (así quien llama sabe que se pasó sin cargarlo
// todo en memoria). null si no se pudo leer.
suspend fun leerBytes(context: Context, uri: Uri, maximo: Int): ByteArray? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            val salida = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            while (salida.size() <= maximo) {
                val leidos = entrada.read(buffer)
                if (leidos < 0) break
                salida.write(buffer, 0, leidos)
            }
            salida.toByteArray().let { if (it.size > maximo) it.copyOf(maximo + 1) else it }
        }
    } catch (e: Exception) {
        null
    }
}

// ---------- Imágenes de las tarjetas de estudio ----------

// Carpeta privada de la app con las imágenes de las tarjetas.
fun carpetaImagenesTarjetas(contexto: Context): File = File(contexto.filesDir, "tarjetas")

// Copia la imagen elegida como JPEG achicado (1280 px) y devuelve el nombre
// del archivo (lo que se guarda en la tarjeta). null si no se pudo leer.
suspend fun guardarImagenTarjeta(contexto: Context, uri: Uri): String? {
    val bytes = leerJpeg(contexto, uri, ladoMaximo = 1280, calidad = 85) ?: return null
    return withContext(Dispatchers.IO) {
        val carpeta = carpetaImagenesTarjetas(contexto).apply { mkdirs() }
        val nombre = java.util.UUID.randomUUID().toString() + ".jpg"
        File(carpeta, nombre).writeBytes(bytes)
        nombre
    }
}

// Lee la imagen de una tarjeta (null si se borró o no se pudo leer).
suspend fun leerImagenTarjeta(contexto: Context, nombre: String): Bitmap? = withContext(Dispatchers.IO) {
    runCatching { BitmapFactory.decodeFile(File(carpetaImagenesTarjetas(contexto), nombre).path) }.getOrNull()
}
