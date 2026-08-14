package com.example.estudia.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.estudia.modelo.Materia
import com.example.estudia.ui.theme.colorDesdeHex

// Tarjeta que muestra el resumen de una materia: nombre, cantidad de
// tareas pendientes y una barra de progreso. Se usa en la pantalla de
// Materias y también se va a reutilizar en la pantalla de Inicio.
@Composable
fun TarjetaMateria(materia: Materia) {
    val pendientes = materia.tareas.count { !it.completada }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(colorDesdeHex(materia.color), shape = RoundedCornerShape(6.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = materia.nombre, style = MaterialTheme.typography.titleMedium)

                if (pendientes > 0) {
                    Text(text = "$pendientes tareas pendientes", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(text = "No hay tareas pendientes", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { materia.calcularProgreso() / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "${materia.calcularProgreso()}%")
        }
    }
}