package com.example.estudia.ui.componentes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

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
