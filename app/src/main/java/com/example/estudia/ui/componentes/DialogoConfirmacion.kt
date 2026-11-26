package com.example.estudia.ui.componentes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.runtime.Composable

// Ventana emergente genérica para confirmar una acción (cerrar sesión,
// eliminar una materia, etc). Va en rojo porque son acciones que no se
// deshacen. textoConfirmar cambia el texto del botón de confirmar para que
// quede claro qué se va a hacer.
@Composable
fun DialogoConfirmacion(
    mensaje: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    DialogoApp(
        titulo = null,
        icono = Icons.Outlined.WarningAmber,
        textoConfirmar = textoConfirmar,
        onConfirmar = onConfirmar,
        onCancelar = onCancelar,
        destructivo = true
    ) {
        TextoDialogo(mensaje)
    }
}
