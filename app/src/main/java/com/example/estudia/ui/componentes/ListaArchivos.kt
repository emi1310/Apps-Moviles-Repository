package com.example.estudia.ui.componentes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Archivo
import com.example.estudia.util.aTexto

// Lista de archivos adjuntos (de una materia o de una nota). Tocar un
// archivo lo abre; la X lo elimina (con "Deshacer").
@Composable
fun ListaArchivos(
    archivos: List<Archivo>,
    color: Color,
    onAbrir: (Archivo) -> Unit,
    onEliminar: (Archivo) -> Unit
) {
    Column {
        archivos.forEach { archivo ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAbrir(archivo) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        archivo.esImagen -> Icons.Filled.Image
                        archivo.esPdf -> Icons.Filled.PictureAsPdf
                        else -> Icons.AutoMirrored.Filled.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = color
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = archivo.nombre,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${archivo.tamanoLegible()} · ${archivo.fechaAgregado.aTexto()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = { onEliminar(archivo) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.eliminar_archivo, archivo.nombre))
                }
            }
        }
    }
}

// Abre el archivo con la app que el usuario tenga para ese tipo (visor de
// PDF, galería...). FLAG_GRANT_READ_URI_PERMISSION le da a esa app permiso
// temporal de solo lectura sobre este archivo, nada más.
// Devuelve false si no hay ninguna app que pueda abrirlo.
fun abrirArchivo(context: Context, uri: Uri, tipoMime: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, tipoMime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.abrir_con)))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

// Tipos que acepta el selector de archivos: imágenes, PDFs y cualquier otro.
val TIPOS_ARCHIVO = arrayOf("image/*", "application/pdf", "*/*")
