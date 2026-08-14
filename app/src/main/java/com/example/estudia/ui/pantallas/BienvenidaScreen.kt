package com.example.estudia.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Primera pantalla que ve el usuario al abrir la app.
@Composable
fun BienvenidaScreen(onComenzar: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Estudia+", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Organiza. Estudia. Logra.", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onComenzar, modifier = Modifier.fillMaxWidth()) {
            Text("Comenzar")
        }
    }
}