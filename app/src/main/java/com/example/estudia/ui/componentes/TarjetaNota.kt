package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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

// Bloque de una nota: tarjeta neutra con la materia (en su color) y el
// título, la estrella de favorita (en un círculo) y el menú con Adjuntar,
// Sacar foto, Editar y Eliminar. Debajo el contenido (hasta 4 líneas), los
// archivos y la fecha en una pastilla del color de la materia. La estrella está dentro de un
// IconButton para que el área táctil sea de 48x48dp.
@Composable
fun TarjetaNota(
    nota: Nota,
    nombreMateria: String,
    colorMateria: Long,
    alTocarEstrella: () -> Unit,
    alAdjuntar: () -> Unit,
    alSacarFoto: () -> Unit,
    alEditar: () -> Unit,
    alEliminar: () -> Unit,
    alAbrirArchivo: (Archivo) -> Unit,
    alEliminarArchivo: (Archivo) -> Unit,
    alPasarATexto: (Archivo) -> Unit,
    alExportarPdf: () -> Unit,
    modifier: Modifier = Modifier,
    // Tocar la tarjeta abre la nota completa (NotaDetalleScreen).
    alAbrir: () -> Unit = {}
) {
    val color = Color(colorMateria)
    val acento = colorAcentoTexto(color)
    val textoAbrir = stringResource(R.string.nota_abrir)
    BloqueColor(
        color = color,
        // La tarjeta se agranda hasta ser la nota abierta (y vuelve al cerrarla).
        modifier = modifier
            .padding(vertical = 6.dp)
            .limitesCompartidos("nota-${nota.id}")
            .clip(MaterialTheme.shapes.large)
            .clickable(onClickLabel = textoAbrir, onClick = alAbrir)
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 4.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f).padding(top = 6.dp)) {
                    Text(
                        text = nombreMateria,
                        style = MaterialTheme.typography.labelLarge,
                        color = acento,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = nota.titulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.elementoCompartido("titulo-nota-${nota.id}")
                    )
                }
                // Favorita: estrella amarilla en un círculo amarillo suave; si no, gris.
                IconButton(onClick = alTocarEstrella) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (nota.esFavorita) AmarilloFavorita.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (nota.esFavorita) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = stringResource(if (nota.esFavorita) R.string.quitar_favorita else R.string.marcar_favorita),
                            tint = if (nota.esFavorita) AmarilloFavorita else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                MenuOpciones(
                    listOf(
                        OpcionMenu(stringResource(R.string.adjuntar_archivo), Icons.Outlined.AttachFile, alTocar = alAdjuntar),
                        OpcionMenu(stringResource(R.string.foto_sacar), Icons.Outlined.PhotoCamera, alTocar = alSacarFoto),
                        OpcionMenu(stringResource(R.string.editar), Icons.Outlined.Edit, alTocar = alEditar),
                        OpcionMenu(stringResource(R.string.exportar_pdf), Icons.Outlined.PictureAsPdf, alTocar = alExportarPdf),
                        OpcionMenu(stringResource(R.string.eliminar), Icons.Outlined.Delete, destructiva = true, alTocar = alEliminar)
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (nota.archivos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ListaArchivos(nota.archivos, color, alAbrirArchivo, alEliminarArchivo, alPasarATexto)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(fondoSuave(color))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = acento, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = nota.fechaCreacion.aTexto(), style = MaterialTheme.typography.labelMedium, color = acento)
                }
            }
        }
    }
}
