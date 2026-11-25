package com.example.estudia.ui.componentes

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.example.estudia.R

// Dictado por voz con el reconocedor del teléfono (el de Google en la
// mayoría). La app no pide el permiso del micrófono: lo maneja la ventana
// del reconocedor, que es otra app. Devuelve la función que abre el dictado.
// onSinDictado: el teléfono no tiene reconocedor de voz instalado.
@Composable
fun rememberDictado(onTexto: (String) -> Unit, onSinDictado: () -> Unit): () -> Unit {
    val alTexto by rememberUpdatedState(onTexto)
    val indicacion = stringResource(R.string.dictar_indicacion)
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
        if (resultado.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val texto = resultado.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
        if (!texto.isNullOrEmpty()) alTexto(texto)
    }
    return {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, indicacion)
        try {
            lanzador.launch(intent)
        } catch (e: ActivityNotFoundException) {
            onSinDictado()
        }
    }
}

// Suma lo dictado al final del texto que ya había, separado por un espacio.
fun agregarDictado(texto: String, dictado: String): String =
    if (texto.isBlank()) dictado else texto.trimEnd() + " " + dictado
