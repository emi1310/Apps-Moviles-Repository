package com.example.estudia.ui.componentes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap

// La app de Gemini de Google y su versión web (si no está instalada).
const val PAQUETE_GEMINI = "com.google.android.apps.bard"
const val WEB_GEMINI = "https://gemini.google.com/app"

// Abre la app de Gemini; si el teléfono no la tiene, abre la web en el
// navegador. Gemini usa la cuenta de Google del teléfono: no hace falta clave.
// Devuelve false si no se pudo abrir nada.
fun abrirGemini(context: Context): Boolean {
    val intent = (context.packageManager.getLaunchIntentForPackage(PAQUETE_GEMINI)
        ?: Intent(Intent.ACTION_VIEW, Uri.parse(WEB_GEMINI)))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

// El ícono de la app de Gemini instalada (el mismo que muestra el teléfono),
// para el botón que la abre. null si no está instalada.
fun iconoAppGemini(context: Context): androidx.compose.ui.graphics.ImageBitmap? =
    try {
        context.packageManager.getApplicationIcon(PAQUETE_GEMINI).toBitmap(144, 144).asImageBitmap()
    } catch (e: Exception) {
        null
    }

// Ícono de Gemini: el de la app instalada, recortado en círculo. Si no está
// instalada, una estrella de cuatro puntas con un degradado azul y violeta.
@Composable
fun IconoGemini(tamano: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val icono = remember { iconoAppGemini(context) }
    if (icono != null) {
        Image(icono, contentDescription = null, modifier = modifier.size(tamano).clip(CircleShape))
    } else {
        Canvas(modifier = modifier.size(tamano)) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = size.minDimension / 2
            // Cuatro puntas con los lados curvos hacia adentro.
            val estrella = Path().apply {
                moveTo(c.x, c.y - r)
                quadraticTo(c.x, c.y, c.x + r, c.y)
                quadraticTo(c.x, c.y, c.x, c.y + r)
                quadraticTo(c.x, c.y, c.x - r, c.y)
                quadraticTo(c.x, c.y, c.x, c.y - r)
                close()
            }
            drawPath(estrella, Brush.linearGradient(listOf(Color(0xFF4F8DF7), Color(0xFF9B72F2), Color(0xFFD96570)), Offset(0f, size.height), Offset(size.width, 0f)))
        }
    }
}
