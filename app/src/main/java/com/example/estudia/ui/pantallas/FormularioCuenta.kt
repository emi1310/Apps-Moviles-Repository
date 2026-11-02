package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.estudia.ui.componentes.IconoCircular

// Estructura común de Login y Registro: ícono, título, subtítulo y los
// campos dentro de una tarjeta. imePadding + scroll: cuando aparece el
// teclado, el formulario se corre hacia arriba en vez de quedar tapado.
@Composable
fun FormularioCuenta(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    campos: @Composable ColumnScope.() -> Unit,
    pie: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconoCircular(icono, MaterialTheme.colorScheme.primary, tamano = 72.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = titulo, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = subtitulo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), content = campos)
        }
        Spacer(modifier = Modifier.height(16.dp))
        pie()
    }
}
