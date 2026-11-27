package com.example.estudia.ui.componentes

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.example.estudia.R
import com.example.estudia.util.leerImagen

// Avatar del usuario: su foto de perfil en un círculo o, si no tiene (o
// mientras se carga), la inicial sobre el fondo que se le pase.
@Composable
fun Avatar(
    nombre: String,
    fotoUri: String?,
    tamano: Dp,
    fondo: Brush,
    colorInicial: Color,
    estiloInicial: TextStyle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lado = with(LocalDensity.current) { tamano.roundToPx() }
    // La foto se lee en segundo plano (Dispatchers.IO), achicada al tamaño
    // del círculo. Se vuelve a leer si cambia la foto.
    val imagen by produceState<Bitmap?>(null, fotoUri, lado) {
        value = fotoUri?.let { leerImagen(context, Uri.parse(it), lado) }
    }
    Box(
        modifier = modifier.size(tamano).clip(CircleShape).background(fondo),
        contentAlignment = Alignment.Center
    ) {
        val foto = imagen
        if (foto != null && fotoUri != null) {
            Image(
                bitmap = foto.asImageBitmap(),
                contentDescription = stringResource(R.string.foto_perfil),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(text = nombre.take(1).uppercase(), style = estiloInicial, color = colorInicial)
        }
    }
}

// Opciones de la foto de perfil: sacar una con la cámara, elegir una de la
// galería o quitarla (si ya hay una). Cada opción es un bloque.
@Composable
fun DialogoFotoPerfil(
    tieneFoto: Boolean,
    onSacarFoto: () -> Unit,
    onElegirDeGaleria: () -> Unit,
    onQuitar: () -> Unit,
    onCerrar: () -> Unit
) {
    DialogoApp(
        titulo = stringResource(R.string.foto_perfil),
        icono = Icons.Outlined.AccountCircle,
        textoConfirmar = null,
        onConfirmar = {},
        onCancelar = onCerrar
    ) {
        TextoDialogo(stringResource(R.string.foto_perfil_texto))
        CampoBloque(Icons.Outlined.PhotoCamera, stringResource(R.string.foto_sacar), onSacarFoto, Modifier.fillMaxWidth())
        CampoBloque(Icons.Outlined.PhotoLibrary, stringResource(R.string.foto_galeria), onElegirDeGaleria, Modifier.fillMaxWidth())
        if (tieneFoto) {
            CampoBloque(Icons.Outlined.Delete, stringResource(R.string.foto_quitar), onQuitar, Modifier.fillMaxWidth())
        }
    }
}
