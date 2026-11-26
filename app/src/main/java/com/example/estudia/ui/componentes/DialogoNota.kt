package com.example.estudia.ui.componentes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Fuente
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.Nota
import com.example.estudia.modelo.PlantillaNota
import com.example.estudia.modelo.agregarCita
import com.example.estudia.modelo.aplicarPlantilla
import com.example.estudia.modelo.citaEnTexto

// Ventana emergente para crear (nota == null) o editar una nota.
// Al crear hay que elegir la materia (pastillas de colores); al editar, la
// materia no cambia.
// - Plantillas (solo al crear): Cornell, esquema, cuadro comparativo o línea
//   de tiempo llenan el contenido con la estructura para completar.
// - Citar: si hay bibliografía cargada, agrega "(Autor, año)" al final del
//   contenido. Primero van las fuentes de la materia de la nota.
@Composable
fun DialogoNota(
    nota: Nota?,
    materias: List<Materia>,
    onConfirmar: (materiaId: Int?, titulo: String, contenido: String) -> Unit,
    onCancelar: () -> Unit,
    fuentes: List<Fuente> = emptyList(),
    materiaDeLaNota: String? = null
) {
    var materia by remember { mutableStateOf<Materia?>(null) }
    var titulo by remember { mutableStateOf(nota?.titulo ?: "") }
    var contenido by remember { mutableStateOf(nota?.contenido ?: "") }
    var error by remember { mutableStateOf<Int?>(null) }
    var plantilla by remember { mutableStateOf(PlantillaNota.LIBRE) }
    var textoPlantilla by remember { mutableStateOf<String?>(null) }
    var eligiendoCita by remember { mutableStateOf(false) }
    val textos = textosPlantillas()

    DialogoApp(
        titulo = stringResource(if (nota == null) R.string.nueva_nota else R.string.editar_nota),
        icono = Icons.Outlined.StickyNote2,
        textoConfirmar = stringResource(if (nota == null) R.string.crear else R.string.guardar),
        onConfirmar = {
            error = when {
                nota == null && materia == null -> R.string.error_elegir_materia
                titulo.isBlank() -> R.string.error_titulo_nota
                else -> null
            }
            if (error == null) onConfirmar(materia?.id, titulo, contenido)
        },
        onCancelar = onCancelar
    ) {
        if (nota == null) {
            EtiquetaCampo(stringResource(R.string.elegir_materia))
            SelectorMateria(materias, materia) { materia = it; error = null }
            EtiquetaCampo(stringResource(R.string.plantilla_elegir))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                PlantillaNota.entries.forEach { opcion ->
                    PastillaFiltro(nombrePlantilla(opcion), elegida = plantilla == opcion) {
                        val nueva = textos.getValue(opcion)
                        contenido = aplicarPlantilla(contenido, textoPlantilla, nueva)
                        textoPlantilla = nueva
                        plantilla = opcion
                    }
                }
            }
        }
        CampoTexto(
            value = titulo,
            onValueChange = { titulo = it; error = null },
            label = { Text(stringResource(R.string.titulo)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // El micrófono suma lo dictado al final del contenido.
        val dictar = rememberDictado(
            onTexto = { contenido = agregarDictado(contenido, it) },
            onSinDictado = { error = R.string.error_sin_dictado }
        )
        CampoTexto(
            value = contenido,
            onValueChange = { contenido = it },
            label = { Text(stringResource(R.string.contenido)) },
            minLines = if (plantilla == PlantillaNota.LIBRE) 4 else 8,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = dictar) {
                    Icon(Icons.Outlined.Mic, contentDescription = stringResource(R.string.dictar))
                }
            }
        )
        if (fuentes.isNotEmpty()) {
            val deLaMateria = (materia?.nombre ?: materiaDeLaNota)?.trim()
            val ordenadas = fuentes.sortedByDescending { it.materia.trim().equals(deLaMateria, ignoreCase = true) }
            Box {
                BotonSuave(
                    stringResource(R.string.plantilla_citar),
                    Icons.Outlined.FormatQuote,
                    MaterialTheme.colorScheme.primary,
                    { eligiendoCita = true },
                    Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = eligiendoCita,
                    onDismissRequest = { eligiendoCita = false },
                    containerColor = colorVentanaVidrio(),
                    border = bordeVidrio()
                ) {
                    DesenfocarDetrasDelPopup()
                    ordenadas.forEach { fuente ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${citaEnTexto(fuente)} ${fuente.titulo}",
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                contenido = agregarCita(contenido, citaEnTexto(fuente))
                                eligiendoCita = false
                            }
                        )
                    }
                }
            }
        }
        TextoError(error?.let { stringResource(it) })
    }
}

@Composable
private fun nombrePlantilla(plantilla: PlantillaNota): String = stringResource(
    when (plantilla) {
        PlantillaNota.LIBRE -> R.string.plantilla_libre
        PlantillaNota.CORNELL -> R.string.plantilla_cornell
        PlantillaNota.ESQUEMA -> R.string.plantilla_esquema
        PlantillaNota.COMPARATIVO -> R.string.plantilla_comparativo
        PlantillaNota.LINEA_TIEMPO -> R.string.plantilla_linea_tiempo
    }
)

@Composable
private fun textosPlantillas(): Map<PlantillaNota, String> = mapOf(
    PlantillaNota.LIBRE to "",
    PlantillaNota.CORNELL to stringResource(R.string.plantilla_texto_cornell),
    PlantillaNota.ESQUEMA to stringResource(R.string.plantilla_texto_esquema),
    PlantillaNota.COMPARATIVO to stringResource(R.string.plantilla_texto_comparativo),
    PlantillaNota.LINEA_TIEMPO to stringResource(R.string.plantilla_texto_linea_tiempo)
)
