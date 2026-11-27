package com.example.estudia.ui.componentes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Tarjeta
import com.example.estudia.util.guardarImagenTarjeta
import kotlinx.coroutines.launch

// Ventana para crear (tarjeta == null) o editar una tarjeta de estudio.
// La pregunta puede llevar una imagen (un gráfico, un mapa, un preparado
// del microscopio): se elige con el selector de fotos del sistema, que no
// pide permisos, y se copia achicada a la carpeta privada de la app.
// onConfirmar devuelve false si falta la pregunta (o la imagen) o la
// respuesta (lo decide el ViewModel) y entonces se muestra el error.
@Composable
fun DialogoTarjeta(
    tarjeta: Tarjeta?,
    onConfirmar: (pregunta: String, respuesta: String, imagen: String?) -> Boolean,
    onCancelar: () -> Unit
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var pregunta by remember { mutableStateOf(tarjeta?.pregunta ?: "") }
    var respuesta by remember { mutableStateOf(tarjeta?.respuesta ?: "") }
    var imagen by remember { mutableStateOf(tarjeta?.imagen) }
    var cargandoImagen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        cargandoImagen = true
        alcance.launch {
            guardarImagenTarjeta(contexto, uri)?.let { imagen = it; error = false }
            cargandoImagen = false
        }
    }

    DialogoApp(
        titulo = stringResource(if (tarjeta == null) R.string.tarjeta_nueva else R.string.tarjeta_editar),
        icono = Icons.Outlined.Style,
        textoConfirmar = stringResource(if (tarjeta == null) R.string.crear else R.string.guardar),
        onConfirmar = { if (!cargandoImagen) error = !onConfirmar(pregunta, respuesta, imagen) },
        onCancelar = onCancelar
    ) {
        CampoTexto(
            value = pregunta,
            onValueChange = { pregunta = it; error = false },
            label = { Text(stringResource(R.string.tarjeta_pregunta)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        imagen?.let { ImagenTarjeta(it, Modifier.fillMaxWidth(), altoMaximo = 160.dp) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(if (imagen == null) R.string.tarjeta_agregar_imagen else R.string.tarjeta_cambiar_imagen))
            }
            if (cargandoImagen) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            if (imagen != null && !cargandoImagen) {
                TextButton(onClick = { imagen = null }) {
                    Icon(Icons.Outlined.HideImage, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.tarjeta_quitar_imagen))
                }
            }
        }
        CampoTexto(
            value = respuesta,
            onValueChange = { respuesta = it; error = false },
            label = { Text(stringResource(R.string.tarjeta_respuesta)) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        TextoError(if (error) stringResource(R.string.error_tarjeta) else null)
    }
}
