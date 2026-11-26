package com.example.estudia.datos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.estudia.modelo.Nota
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Exporta una nota a PDF con PdfDocument (viene con Android, no hace falta
// ninguna librería): título, materia y fecha, el texto (en tantas hojas
// como haga falta) y después las fotos adjuntas, achicadas al ancho de la
// hoja. El archivo queda en cacheDir/pdf y se comparte con el FileProvider.
class ExportadorPdf(context: Context) {

    private val contexto = context.applicationContext
    private val carpetaAdjuntos = File(contexto.filesDir, "archivos")
    private val carpetaPdf = File(contexto.cacheDir, "pdf")

    suspend fun exportar(nota: Nota, nombreMateria: String, subtitulo: String): String? = withContext(Dispatchers.IO) {
        try {
            carpetaPdf.mkdirs()
            // Solo se guarda el último PDF de cada nota.
            val archivo = File(carpetaPdf, "${nombreSeguro(nota.titulo)}.pdf")
            val documento = PdfDocument()
            val hoja = Hoja(documento)

            val titulo = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = COLOR_TITULO
            }
            val secundario = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = COLOR_SECUNDARIO }
            val cuerpo = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; color = Color.BLACK }

            hoja.escribir(nota.titulo, titulo)
            hoja.escribir("$nombreMateria · $subtitulo", secundario)
            hoja.espacio(8f)
            hoja.linea()
            hoja.espacio(14f)
            if (nota.contenido.isNotBlank()) hoja.escribir(nota.contenido, cuerpo)

            nota.archivos.filter { it.esImagen }.forEach { adjunto ->
                val imagen = leerImagen(File(carpetaAdjuntos, adjunto.ruta)) ?: return@forEach
                hoja.espacio(16f)
                hoja.dibujarImagen(imagen)
                imagen.recycle()
            }
            hoja.terminar()

            archivo.outputStream().use { documento.writeTo(it) }
            documento.close()
            FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", archivo).toString()
        } catch (e: Exception) {
            null
        }
    }

    // Lee la foto achicada (inSampleSize) para no ocupar demasiada memoria.
    private fun leerImagen(archivo: File): Bitmap? {
        if (!archivo.exists()) return null
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(archivo.path, medidas)
        var escala = 1
        while (medidas.outWidth / (escala * 2) >= ANCHO_IMAGEN_MAXIMO) escala *= 2
        return BitmapFactory.decodeFile(archivo.path, BitmapFactory.Options().apply { inSampleSize = escala })
    }

    private fun nombreSeguro(texto: String): String =
        texto.replace(Regex("[^A-Za-z0-9áéíóúñÁÉÍÓÚÑ _-]"), "").trim().take(40).ifBlank { "nota" }

    // Va escribiendo de arriba hacia abajo y abre una hoja nueva cuando no entra más.
    private class Hoja(private val documento: PdfDocument) {
        private var numero = 0
        private var pagina: PdfDocument.Page? = null
        private var y = 0f
        private val anchoUtil = (ANCHO - 2 * MARGEN).toInt()

        private val canvas: Canvas
            get() = (pagina ?: nueva()).canvas

        private fun nueva(): PdfDocument.Page {
            pagina?.let { documento.finishPage(it) }
            numero++
            val p = documento.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numero).create())
            pagina = p
            y = MARGEN
            return p
        }

        private fun lugarLibre(): Float = ALTO - MARGEN - y

        fun espacio(alto: Float) {
            canvas
            y += alto
        }

        fun linea() {
            val c = canvas
            c.drawLine(MARGEN, y, ANCHO - MARGEN, y, Paint().apply { color = COLOR_LINEA; strokeWidth = 1f })
        }

        // El texto se corta por renglones: si no entra, sigue en la hoja siguiente.
        fun escribir(texto: String, pintura: TextPaint) {
            val layout = StaticLayout.Builder.obtain(texto, 0, texto.length, pintura, anchoUtil)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1f)
                .build()
            var renglon = 0
            while (renglon < layout.lineCount) {
                if (lugarLibre() < layout.getLineBottom(renglon) - layout.getLineTop(renglon)) nueva()
                val c = canvas
                // Cuántos renglones entran en lo que queda de esta hoja.
                var hasta = renglon
                while (hasta < layout.lineCount &&
                    layout.getLineBottom(hasta) - layout.getLineTop(renglon) <= lugarLibre()
                ) hasta++
                val arriba = layout.getLineTop(renglon).toFloat()
                val abajo = layout.getLineBottom(hasta - 1).toFloat()
                c.save()
                c.translate(MARGEN, y - arriba)
                c.clipRect(0f, arriba, anchoUtil.toFloat(), abajo)
                layout.draw(c)
                c.restore()
                y += abajo - arriba
                renglon = hasta
            }
            y += 4f
        }

        fun dibujarImagen(imagen: Bitmap) {
            val escala = minOf(anchoUtil.toFloat() / imagen.width, (ALTO - 2 * MARGEN) / imagen.height)
            val ancho = imagen.width * escala
            val alto = imagen.height * escala
            if (lugarLibre() < alto) nueva()
            canvas.drawBitmap(imagen, null, RectF(MARGEN, y, MARGEN + ancho, y + alto), Paint(Paint.FILTER_BITMAP_FLAG))
            y += alto
        }

        fun terminar() {
            canvas
            pagina?.let { documento.finishPage(it) }
            pagina = null
        }
    }

    companion object {
        // Hoja A4 en puntos (1/72 de pulgada).
        private const val ANCHO = 595
        private const val ALTO = 842
        private const val MARGEN = 48f
        private const val ANCHO_IMAGEN_MAXIMO = 1200
        private val COLOR_TITULO = Color.rgb(0x4B, 0x3B, 0xC4)
        private val COLOR_SECUNDARIO = Color.rgb(0x6B, 0x6B, 0x7B)
        private val COLOR_LINEA = Color.rgb(0xE0, 0xDC, 0xF5)
    }
}
