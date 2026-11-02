package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.Nota
import com.example.estudia.util.aTexto

// Tarjeta que muestra una nota: título, materia, contenido (recortado
// a 3 líneas), fecha, estrella de favorita, archivos adjuntos y un menú
// con Adjuntar, Editar y Eliminar. Los íconos están dentro de un
// IconButton para que el área táctil tenga el tamaño mínimo (48x48dp).
@Composable
fun TarjetaNota(
    nota: Nota,
    nombreMateria: String,
    alTocarEstrella: () -> Unit,
    alAdjuntar: () -> Unit,
    alEditar: () -> Unit,
    alEliminar: () -> Unit,
    alAbrirArchivo: (Archivo) -> Unit,
    alEliminarArchivo: (Archivo) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = nota.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(text = nombreMateria, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = alTocarEstrella) {
                    Icon(
                        imageVector = if (nota.esFavorita) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = stringResource(if (nota.esFavorita) R.string.quitar_favorita else R.string.marcar_favorita),
                        tint = if (nota.esFavorita) Color(0xFFF5B400) else MaterialTheme.colorScheme.onSurfaceVariant
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
            if (nota.contenido.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = nota.contenido, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
            }
            if (nota.archivos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                ListaArchivos(nota.archivos, MaterialTheme.colorScheme.primary, alAbrirArchivo, alEliminarArchivo)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = nota.fechaCreacion.aTexto(), style = MaterialTheme.typography.bodySmall)
        }
    }
}
