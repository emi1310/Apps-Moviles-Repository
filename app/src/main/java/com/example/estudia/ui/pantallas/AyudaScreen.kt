package com.example.estudia.ui.pantallas

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.ui.componentes.LocalAvisos

// Pantalla "Ayuda y soporte": preguntas frecuentes (se expanden al
// tocarlas) y un botón para escribirle a soporte por mail.
// No tiene ViewModel porque no tiene estado ni datos: los textos salen
// de strings.xml y lo único que guarda es qué pregunta está abierta.
@Composable
fun AyudaScreen(onVolver: () -> Unit) {
    val preguntas = stringArrayResource(R.array.ayuda_preguntas)
    val respuestas = stringArrayResource(R.array.ayuda_respuestas)
    var abierta by rememberSaveable { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val avisos = LocalAvisos.current
    val asunto = stringResource(R.string.soporte_asunto)
    val sinMail = stringResource(R.string.sin_app_mail)
    val correo = stringResource(R.string.soporte_email)

    LazyColumn(contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                }
                Text(text = stringResource(R.string.ayuda_y_soporte), style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.preguntas_frecuentes), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        itemsIndexed(preguntas.toList()) { indice, pregunta ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(
                    modifier = Modifier
                        .clickable { abierta = if (abierta == indice) null else indice }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = pregunta, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Icon(if (abierta == indice) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                    }
                    if (abierta == indice) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = respuestas.getOrElse(indice) { "" }, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = stringResource(R.string.no_encontraste_respuesta), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    // ACTION_SENDTO con "mailto:" abre solo apps de correo.
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$correo"))
                        .putExtra(Intent.EXTRA_SUBJECT, asunto)
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        avisos.mostrarTexto(sinMail)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Email, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.escribir_a_soporte))
            }
        }
    }
}
