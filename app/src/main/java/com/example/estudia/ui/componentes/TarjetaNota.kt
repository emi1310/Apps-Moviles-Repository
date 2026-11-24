package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.Nota
import com.example.estudia.ui.theme.AmarilloFavorita
import com.example.estudia.util.aTexto

// Bloque de una nota, del color de su materia: arriba la materia y el título
// en blanco, la estrella de favorita (en un círculo) y el menú con Adjuntar,
// Editar y Eliminar. Debajo el contenido (hasta 4 líneas), los archivos en un
// bloque claro y la fecha en una pastilla. La estrella está dentro de un
// IconButton para que el área táctil sea de 48x48dp.
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
    alEliminarArchivo: (Archivo) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = Color(colorMateria)
    BloqueColor(color = color, modifier = modifier.padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 4.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f).padding(top = 6.dp)) {
                    Text(
                        text = nombreMateria,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = nota.titulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Favorita: círculo blanco con la estrella amarilla; si no, translúcido.
                IconButton(onClick = alTocarEstrella) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (nota.esFavorita) Color.White else BlancoTranslucido),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (nota.esFavorita) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = stringResource(if (nota.esFavorita) R.string.quitar_favorita else R.string.marcar_favorita),
                            tint = if (nota.esFavorita) AmarilloFavorita else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                MenuOpciones(
                    listOf(
                        OpcionMenu(stringResource(R.string.adjuntar_archivo), Icons.Filled.AttachFile, alTocar = alAdjuntar),
                        OpcionMenu(stringResource(R.string.editar), Icons.Filled.Edit, alTocar = alEditar),
                        OpcionMenu(stringResource(R.string.eliminar), Icons.Filled.Delete, destructiva = true, alTocar = alEliminar)
                    ),
                    descripcion = stringResource(R.string.opciones_de, nota.titulo)
                )
            }
            Column(modifier = Modifier.padding(end = 16.dp)) {
                if (nota.contenido.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = nota.contenido,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (nota.archivos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            ListaArchivos(nota.archivos, color, alAbrirArchivo, alEliminarArchivo)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(BlancoTranslucido)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = nota.fechaCreacion.aTexto(), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
