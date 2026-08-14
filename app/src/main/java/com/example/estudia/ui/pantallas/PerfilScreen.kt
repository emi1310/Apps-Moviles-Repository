package com.example.estudia.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.Usuario
import com.example.estudia.ui.componentes.DialogoInfo
import com.example.estudia.ui.componentes.OpcionPerfil
import com.example.estudia.ui.componentes.TarjetaEstadistica

// Pantalla "Mi Perfil": datos del usuario, estadísticas calculadas, y
// un menú de opciones. Las opciones que todavía no tienen pantalla propia
// muestran un aviso de "función en desarrollo" en vez de no hacer nada.
@Composable
fun PerfilScreen(usuario: Usuario, onCerrarSesion: () -> Unit) {
    var mensajeDialogo by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Mi perfil", style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------- Avatar + datos principales ----------
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = usuario.nombre.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = usuario.nombre, style = MaterialTheme.typography.titleLarge)

            val carreraTexto = usuario.carrera ?: "Carrera no especificada"
            val anioTexto = usuario.anio?.let { "$it° año" } ?: ""
            Text(text = "$anioTexto - $carreraTexto", style = MaterialTheme.typography.bodyMedium)

            Text(
                text = usuario.email ?: "Email no especificado",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ---------- Estadísticas ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TarjetaEstadistica(valor = usuario.contarMaterias().toString(), etiqueta = "Materias")
            TarjetaEstadistica(valor = "${usuario.calcularHorasEstudioSemanal()}h", etiqueta = "Estudio")
            TarjetaEstadistica(valor = usuario.contarNotas().toString(), etiqueta = "Notas")
            TarjetaEstadistica(valor = usuario.calcularDiasRacha().toString(), etiqueta = "Días racha")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ---------- Menú de opciones ----------
        OpcionPerfil(texto = "Editar perfil") { mensajeDialogo = "La edición de perfil todavía está en desarrollo." }
        Divider()
        OpcionPerfil(texto = "Metas de estudio") { mensajeDialogo = "Las metas de estudio todavía están en desarrollo." }
        Divider()
        OpcionPerfil(texto = "Recordatorios") { mensajeDialogo = "Los recordatorios todavía están en desarrollo." }
        Divider()
        OpcionPerfil(texto = "Respaldo y datos") { mensajeDialogo = "El respaldo de datos todavía está en desarrollo." }
        Divider()
        OpcionPerfil(texto = "Ayuda y soporte") { mensajeDialogo = "La ayuda y soporte todavía está en desarrollo." }
        Divider()
        OpcionPerfil(texto = "Cerrar sesión", esDestructiva = true) {
            onCerrarSesion()
        }
    }

    mensajeDialogo?.let { mensaje ->
        DialogoInfo(mensaje = mensaje, onCerrar = { mensajeDialogo = null })
    }
}