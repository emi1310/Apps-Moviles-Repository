package com.example.estudia.datos

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

// Fuente de datos para los archivos adjuntos (imágenes, PDFs, etc.).
//
// Buenas prácticas de almacenamiento (scoped storage):
//  - La app NO pide permisos de almacenamiento (READ/WRITE_EXTERNAL_STORAGE
//    ni MANAGE_EXTERNAL_STORAGE). El usuario elige el archivo con el selector
//    del sistema, que le da a la app acceso solo a ESE archivo.
//  - El archivo se copia al almacenamiento interno de la app
//    (filesDir/archivos): es privado, no lo ven otras apps, no necesita
//    permisos y se borra si se desinstala la app.
//  - Para abrirlo con otra app (visor de PDF, galería) se comparte con un
//    FileProvider, que da acceso temporal de solo lectura a ese archivo.
//  - Todo el trabajo con archivos se hace en Dispatchers.IO, nunca en el
//    hilo principal.
class AlmacenamientoArchivos(context: Context) {

    private val contexto = context.applicationContext
    private val carpeta = File(contexto.filesDir, "archivos")

    // Resultado de copiar un archivo elegido por el usuario.
    data class ArchivoCopiado(
        val nombre: String,
        val tipoMime: String,
        val ruta: String,
        val tamanoBytes: Long
    )

    // Copia el contenido de la Uri (elegida en el selector) a la carpeta
    // interna. Devuelve null si el archivo supera el tamaño máximo.
    suspend fun copiarDesde(uri: Uri): ArchivoCopiado? = withContext(Dispatchers.IO) {
        val resolver = contexto.contentResolver
        var nombre = "archivo"
        var tamano = -1L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnaNombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val columnaTamano = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (columnaNombre != -1) nombre = cursor.getString(columnaNombre) ?: nombre
                if (columnaTamano != -1 && !cursor.isNull(columnaTamano)) tamano = cursor.getLong(columnaTamano)
            }
        }
        if (tamano > TAMANO_MAXIMO_BYTES) return@withContext null

        val tipoMime = resolver.getType(uri) ?: "application/octet-stream"
        // Nombre único en disco, para que dos archivos con el mismo nombre
        // no se pisen. Se quitan caracteres raros del nombre original.
        val ruta = UUID.randomUUID().toString() + "_" + nombre.replace(Regex("[^A-Za-z0-9._-]"), "_")
        carpeta.mkdirs()
        val destino = File(carpeta, ruta)

        resolver.openInputStream(uri)?.use { entrada ->
            destino.outputStream().use { salida -> entrada.copyTo(salida) }
        } ?: return@withContext null

        // Si el selector no informó el tamaño, se controla después de copiar.
        if (destino.length() > TAMANO_MAXIMO_BYTES) {
            destino.delete()
            return@withContext null
        }
        ArchivoCopiado(nombre, tipoMime, ruta, destino.length())
    }

    suspend fun eliminar(ruta: String) = withContext(Dispatchers.IO) {
        File(carpeta, ruta).delete()
    }

    // Borra los archivos de la carpeta que ya no tienen fila en la base
    // (por ejemplo, si la app se cerró antes de confirmar un "Deshacer").
    suspend fun limpiarHuerfanos(rutasValidas: Set<String>) = withContext(Dispatchers.IO) {
        carpeta.listFiles()?.filter { it.name !in rutasValidas }?.forEach { it.delete() }
    }

    // Uri content:// para que otra app pueda abrir el archivo (solo lectura).
    fun uriParaAbrir(ruta: String): Uri {
        return FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", File(carpeta, ruta))
    }

    companion object {
        // Límite para no llenar el almacenamiento del teléfono: 20 MB por archivo.
        const val TAMANO_MAXIMO_BYTES = 20L * 1024 * 1024
    }
}
