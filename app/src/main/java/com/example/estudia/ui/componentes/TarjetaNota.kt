package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.Nota
import com.example.estudia.ui.theme.AmarilloFavorita
import com.example.estudia.util.aTexto

// Tarjeta que muestra una nota: materia (etiqueta de color), título,
// contenido (hasta 4 líneas), archivos adjuntos y fecha. Tiene la estrella
// de favorita y un menú con Adjuntar, Editar y Eliminar. Los íconos están
// dentro de un IconButton para que el área táctil sea de 48x48dp.
@Composable
fun TarjetaNota(
    nota: Nota,
    nombreMateria: String,
    colorMateria: Long,
    alTocarEstrella: () -> Unit,
    alAdjuntar: () -> Unit,
    alEditar: () -> Unit,
    alEliminar: () -> Unit,
    alAbrirArchivo: (Archivo) -> Unit,
    alEliminarArchivo: (Archivo) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) { ChipMateria(nombreMateria, colorMateria) }
                IconButton(onClick = alTocarEstrella) {
                    Icon(
                        imageVector = if (nota.esFavorita) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = stringResource(if (nota.esFavorita) R.string.quitar_favorita else R.string.marcar_favorita),
                        tint = if (nota.esFavorita) AmarilloFavorita else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                MenuOpciones(
                    listOf(
                        OpcionMenu(stringResource(R.string.adjuntar_archivo), Icons.Filled.AttachFile, alAdjuntar),
                        OpcionMenu(stringResource(R.string.editar), Icons.Filled.Edit, alEditar),
                        OpcionMenu(stringResource(R.string.eliminar), Icons.Filled.Delete, alEliminar)
                    ),
                    descripcion = stringResource(R.string.opciones_de, nota.titulo)
                )
            }
            Column(modifier = Modifier.padding(end = 12.dp)) {
                Text(text = nota.titulo, style = MaterialTheme.typography.titleMedium)
                if (nota.contenido.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = nota.contenido,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4
                    )
                }
                if (nota.archivos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    ListaArchivos(nota.archivos, MaterialTheme.colorScheme.primary, alAbrirArchivo, alEliminarArchivo)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = nota.fechaCreacion.aTexto(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
