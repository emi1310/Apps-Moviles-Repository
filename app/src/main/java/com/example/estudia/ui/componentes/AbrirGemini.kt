package com.example.estudia.ui.componentes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import com.example.estudia.ui.theme.BloqueAzul
import com.example.estudia.ui.theme.DegradadoFin
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp

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

// Ícono de Gemini dibujado por la app, con su estilo: la estrella de cuatro
// puntas (lados curvos hacia adentro, apenas llenos) con un brillo de vidrio
// arriba a la izquierda y un borde fino claro.
//  - blanco = false: con un degradado azul, violeta y rosa (los tonos de la app).
//  - blanco = true: blanca con un tinte lila abajo, para ir sobre el violeta.
@Composable
fun IconoGemini(tamano: Dp, modifier: Modifier = Modifier, blanco: Boolean = false) {
    Canvas(modifier = modifier.size(tamano)) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2
        // Los puntos de control van un poco hacia cada esquina: así los lados
        // quedan curvos pero no tan finos.
        val k = r * 0.14f
        val estrella = Path().apply {
            moveTo(c.x, c.y - r)
            quadraticTo(c.x + k, c.y - k, c.x + r, c.y)
            quadraticTo(c.x + k, c.y + k, c.x, c.y + r)
            quadraticTo(c.x - k, c.y + k, c.x - r, c.y)
            quadraticTo(c.x - k, c.y - k, c.x, c.y - r)
            close()
        }
        val relleno = if (blanco) {
            Brush.linearGradient(listOf(Color.White, Color.White, Color(0xFFDCD0FF)), Offset(0f, 0f), Offset(size.width, size.height))
        } else {
            Brush.linearGradient(listOf(BloqueAzul, DegradadoFin, Color(0xFFE57AA8)), Offset(0f, size.height), Offset(size.width, 0f))
        }
        drawPath(estrella, relleno)
        // Brillo de vidrio.
        clipPath(estrella) {
            drawCircle(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = if (blanco) 0.0f else 0.55f), Color.Transparent),
                    center = Offset(c.x - r * 0.35f, c.y - r * 0.35f),
                    radius = r * 0.8f
                ),
                radius = r * 0.8f,
                center = Offset(c.x - r * 0.35f, c.y - r * 0.35f)
            )
        }
        drawPath(
            estrella,
            if (blanco) Color(0xFF8E5CF0).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.45f),
            style = Stroke(width = (r * 0.05f).coerceAtLeast(1f))
        )
    }
}
