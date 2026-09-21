package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.Materia
import com.example.estudia.modelo.TipoEvento

// Nombres legibles para cada tipo de evento, en el mismo orden en que
// se muestran los botones del diálogo.
private val tiposDisponibles = listOf(
    TipoEvento.CLASE to "Clase",
    TipoEvento.EXAMEN to "Examen",
    TipoEvento.ENTREGA to "Entrega",
    TipoEvento.BLOQUE_ESTUDIO to "Bloque de estudio"
)

// Ventana emergente para agregar un evento nuevo al calendario. Como un
// Evento siempre pertenece a una Materia, primero hay que elegir a cuál
// (igual que en DialogoNuevaNota). La fecha llega precargada con el día
// que el usuario tenía seleccionado en el calendario.
@Composable
fun DialogoNuevoEvento(
    materiasDisponibles: List<Materia>,
    fechaInicial: String,
    onConfirmar: (
        materiaElegida: Materia,
        titulo: String,
        tipo: TipoEvento,
        fecha: String,
        horaInicio: String,
        horaFin: String
    ) -> Unit,
    onCancelar: () -> Unit
) {
    var materiaElegida by remember { mutableStateOf<Materia?>(null) }
    var tipoElegido by remember { mutableStateOf<TipoEvento?>(null) }
    var titulo by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(fechaInicial) }
    var horaInicio by remember { mutableStateOf("") }
    var horaFin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nuevo evento") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Elegí una materia")
                for (materia in materiasDisponibles) {
                    Button(
                        onClick = {
                            materiaElegida = materia
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val nombre = if (materiaElegida == materia) "✓ ${materia.nombre}" else materia.nombre
                        Text(nombre)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Tipo de evento")
                for ((tipo, etiqueta) in tiposDisponibles) {
                    Button(
                        onClick = {
                            tipoElegido = tipo
                            error = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val texto = if (tipoElegido == tipo) "✓ $etiqueta" else etiqueta
                        Text(texto)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = fecha,
                    onValueChange = { fecha = it },
                    label = { Text("Fecha (dd/MM/yyyy)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = horaInicio,
                    onValueChange = { horaInicio = it },
                    label = { Text("Hora inicio (HH:mm)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = horaFin,
                    onValueChange = { horaFin = it },
                    label = { Text("Hora fin (HH:mm)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val materia = materiaElegida
                val tipo = tipoElegido
                when {
                    materia == null -> error = "Elegí una materia para continuar"
                    tipo == null -> error = "Elegí un tipo de evento"
                    titulo.isBlank() -> error = "Escribí un título para el evento"
                    fecha.isBlank() -> error = "Ingresá una fecha"
                    horaInicio.isBlank() || horaFin.isBlank() -> error = "Ingresá hora de inicio y de fin"
                    else -> onConfirmar(materia, titulo.trim(), tipo, fecha.trim(), horaInicio.trim(), horaFin.trim())
                }
            }) {
                Text("Crear")
            }
        },
        dismissButton = {
            Button(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}
