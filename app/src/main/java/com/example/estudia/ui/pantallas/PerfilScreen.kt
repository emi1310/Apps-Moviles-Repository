package com.example.estudia.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.Usuario
import com.example.estudia.ui.componentes.DialogoConfirmacion
import com.example.estudia.ui.componentes.DialogoEditarPerfil
import com.example.estudia.ui.componentes.OpcionPerfil
import com.example.estudia.ui.componentes.TarjetaEstadistica

// Pantalla "Mi Perfil": datos del usuario, estadísticas calculadas, y
// un menú de opciones. "Editar perfil" abre un diálogo real; las demás
// opciones que todavía no tienen pantalla propia muestran un aviso corto
// (Snackbar) de "función en desarrollo".
// onMostrarMensaje viene de AppNavegacion, que es quien tiene el Snackbar.
@Composable
fun PerfilScreen(
    usuario: Usuario,
    onMostrarMensaje: (String) -> Unit,
    onEditarPerfil: (nombre: String, carrera: String?, anio: Int?, email: String?) -> Unit,
    onCerrarSesion: () -> Unit
) {
    var confirmarCierreSesion by remember { mutableStateOf(false) }
    var mostrarEditarPerfil by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "Mi perfil", style = MaterialTheme.typography.headlineSmall)

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

            val carrera = usuario.carrera
            val carreraTexto = if (carrera != null) carrera else "Carrera no especificada"
            val anio = usuario.anio
            if (anio != null) {
                Text(text = "$anio° año - $carreraTexto", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(text = carreraTexto, style = MaterialTheme.typography.bodyMedium)
            }

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
            TarjetaEstadistica(valor = "${usuario.calcularHorasEstudio()}h", etiqueta = "Estudio")
            TarjetaEstadistica(valor = usuario.contarNotas().toString(), etiqueta = "Notas")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ---------- Menú de opciones ----------
        OpcionPerfil(texto = "Editar perfil") { mostrarEditarPerfil = true }
        OpcionPerfil(texto = "Metas de estudio") { onMostrarMensaje("Las metas de estudio todavía están en desarrollo.") }
        OpcionPerfil(texto = "Recordatorios") { onMostrarMensaje("Los recordatorios todavía están en desarrollo.") }
        OpcionPerfil(texto = "Ayuda y soporte") { onMostrarMensaje("La ayuda y soporte todavía está en desarrollo.") }
        OpcionPerfil(texto = "Cerrar sesión", esDestructiva = true) {
            confirmarCierreSesion = true
        }
    }

    if (confirmarCierreSesion) {
        DialogoConfirmacion(
            mensaje = "¿Seguro que querés cerrar sesión?",
            textoConfirmar = "Cerrar sesión",
            onConfirmar = onCerrarSesion,
            onCancelar = { confirmarCierreSesion = false }
        )
    }

    if (mostrarEditarPerfil) {
        DialogoEditarPerfil(
            usuario = usuario,
            onConfirmar = { nombre, carrera, anio, email ->
                onEditarPerfil(nombre, carrera, anio, email)
                mostrarEditarPerfil = false
            },
            onCancelar = { mostrarEditarPerfil = false }
        )
    }
}
