package com.example.estudia.ui.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.Materia

// Ventana emergente para crear una nota nueva. Como una Nota siempre
// pertenece a una Materia, hay que elegir a cuál va a pertenecer
// (por eso se muestra una lista de materias para elegir).
@Composable
fun DialogoNuevaNota(
    materiasDisponibles: List<Materia>,
    onConfirmar: (materiaElegida: Materia, titulo: String, contenido: String) -> Unit,
    onCancelar: () -> Unit
) {
    var materiaElegida by remember { mutableStateOf<Materia?>(null) }
    var titulo by remember { mutableStateOf("") }
    var contenido by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Nueva nota") },
        text = {
            Column {
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

                TextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = contenido,
                    onValueChange = { contenido = it },
                    label = { Text("Contenido") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
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
                if (materia == null) {
                    error = "Elegí una materia para continuar"
                } else if (titulo.isBlank()) {
                    error = "Escribí un título para la nota"
                } else {
                    onConfirmar(materia, titulo, contenido)
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
