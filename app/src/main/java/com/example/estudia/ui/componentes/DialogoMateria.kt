package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.estudia.R
import com.example.estudia.modelo.ErrorReglas
import com.example.estudia.modelo.ReglasCursada
import com.example.estudia.modelo.leerReglas

// Ventana emergente para crear una materia (nombreInicial vacío) o editarla:
// el nombre y las reglas de la cursada (asistencia mínima, si es
// promocional y con qué nota, y si tiene final).
// nombreDisponible lo decide el ViewModel (no se puede repetir el nombre).
@Composable
fun DialogoMateria(
    nombreInicial: String,
    nombreDisponible: (String) -> Boolean,
    onConfirmar: (String, ReglasCursada) -> Unit,
    onCancelar: () -> Unit,
    reglasIniciales: ReglasCursada = ReglasCursada()
) {
    val esNueva = nombreInicial.isEmpty()
    var nombre by remember { mutableStateOf(nombreInicial) }
    var asistencia by remember { mutableStateOf(reglasIniciales.asistenciaMinima.toString()) }
    var promocional by remember { mutableStateOf(reglasIniciales.promocional) }
    var notaPromocion by remember { mutableStateOf(textoNota(reglasIniciales.notaPromocion)) }
    var tieneFinal by remember { mutableStateOf(reglasIniciales.tieneFinal) }
    var errorNombre by remember { mutableStateOf<Int?>(null) }
    var errorReglas by remember { mutableStateOf<ErrorReglas?>(null) }

    DialogoApp(
        titulo = stringResource(if (esNueva) R.string.nueva_materia else R.string.editar_materia),
        icono = Icons.AutoMirrored.Outlined.MenuBook,
        textoConfirmar = stringResource(if (esNueva) R.string.agregar else R.string.guardar),
        onConfirmar = {
            errorNombre = when {
                nombre.isBlank() -> R.string.error_nombre_materia
                !nombreDisponible(nombre) -> R.string.error_materia_repetida
                else -> null
            }
            val lectura = leerReglas(asistencia, promocional, notaPromocion, tieneFinal)
            errorReglas = lectura.error
            val reglas = lectura.reglas
            if (errorNombre == null && reglas != null) onConfirmar(nombre.trim(), reglas)
        },
        onCancelar = onCancelar
    ) {
        CampoTexto(
            value = nombre,
            onValueChange = {
                nombre = it
                errorNombre = null
            },
            label = { Text(stringResource(R.string.nombre_materia)) },
            isError = errorNombre != null,
            supportingText = errorNombre?.let { { Text(stringResource(it)) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        EtiquetaCampo(stringResource(R.string.materia_cursada))
        CampoTexto(
            value = asistencia,
            onValueChange = { asistencia = it.filter(Char::isDigit).take(3); errorReglas = null },
            label = { Text(stringResource(R.string.materia_asistencia_minima)) },
            isError = errorReglas == ErrorReglas.ASISTENCIA,
            supportingText = {
                Text(stringResource(if (errorReglas == ErrorReglas.ASISTENCIA) R.string.error_asistencia_minima else R.string.materia_asistencia_ayuda))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        FilaInterruptor(
            titulo = stringResource(R.string.materia_promocional),
            detalle = stringResource(R.string.materia_promocional_detalle),
            valor = promocional,
            onCambiar = { promocional = it; errorReglas = null }
        )
        if (promocional) {
            CampoTexto(
                value = notaPromocion,
                onValueChange = { notaPromocion = it; errorReglas = null },
                label = { Text(stringResource(R.string.materia_nota_promocion)) },
                isError = errorReglas == ErrorReglas.NOTA_PROMOCION,
                supportingText = if (errorReglas == ErrorReglas.NOTA_PROMOCION) {
                    { Text(stringResource(R.string.error_nota_promocion)) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        FilaInterruptor(
            titulo = stringResource(R.string.materia_tiene_final),
            detalle = stringResource(if (promocional) R.string.materia_tiene_final_detalle_promo else R.string.materia_tiene_final_detalle),
            valor = tieneFinal,
            onCambiar = { tieneFinal = it }
        )
    }
}
