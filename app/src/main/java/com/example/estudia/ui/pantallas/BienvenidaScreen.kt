package com.example.estudia.ui.pantallas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.ui.theme.LocalDegradado
import com.example.estudia.ui.theme.VioletaPrincipal

// Primera pantalla que ve el usuario al abrir la app: fondo con degradado,
// el logo, el lema y las tres cosas principales que hace la app.
@Composable
fun BienvenidaScreen(onComenzar: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalDegradado.current)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // El logo de la app (el mismo dibujo del ícono) sobre una tarjeta blanca.
            // El dibujo trae margen para el ícono adaptativo: se agranda y se recorta.
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.requiredSize(156.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.lema),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Caracteristica(Icons.Filled.TaskAlt, stringResource(R.string.bienvenida_tareas))
                Caracteristica(Icons.Filled.CalendarMonth, stringResource(R.string.bienvenida_calendario))
                Caracteristica(Icons.Filled.NotificationsActive, stringResource(R.string.bienvenida_recordatorios))
            }

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onComenzar,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = VioletaPrincipal),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text(stringResource(R.string.comenzar), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun Caracteristica(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(14.dp))
        Text(text = texto, style = MaterialTheme.typography.bodyLarge, color = Color.White)
    }
}
