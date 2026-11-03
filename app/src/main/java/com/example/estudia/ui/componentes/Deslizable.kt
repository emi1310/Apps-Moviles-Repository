package com.example.estudia.ui.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.estudia.R

// Envuelve una fila o tarjeta para poder deslizarla:
//  - hacia la derecha -> onCompletar (si no es null): la fila vuelve a su lugar.
//  - hacia la izquierda -> onEliminar (si no es null): la fila se va (y el
//    aviso ofrece "Deshacer").
// Para TalkBack, las mismas acciones quedan en el menú de acciones del elemento.
//
// margenVertical y forma hacen que el fondo de color tenga el mismo tamaño
// y las mismas esquinas que la tarjeta de adentro. fondoContenido se usa
// cuando el contenido es transparente (una fila dentro de una tarjeta),
// para que el color no se vea a través del texto.
@Composable
fun Deslizable(
    modifier: Modifier = Modifier,
    onEliminar: (() -> Unit)? = null,
    onCompletar: (() -> Unit)? = null,
    completada: Boolean = false,
    forma: Shape = MaterialTheme.shapes.medium,
    margenVertical: Dp = 0.dp,
    fondoContenido: Color = Color.Transparent,
    contenido: @Composable () -> Unit
) {
    // rememberUpdatedState: el estado del deslizamiento se recuerda una sola
    // vez, pero tiene que llamar siempre a la versión más nueva de cada acción.
    val alEliminar by rememberUpdatedState(onEliminar)
    val alCompletar by rememberUpdatedState(onCompletar)
    val estado = rememberSwipeToDismissBoxState(
        confirmValueChange = { valor ->
            when (valor) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    alCompletar?.invoke()
                    false            // no se va: vuelve a su lugar ya completada
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    alEliminar?.invoke()
                    alEliminar != null
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        // Hay que deslizar al menos un 35 % del ancho (evita borrar sin querer).
        positionalThreshold = { ancho -> ancho * 0.35f }
    )

    val textoCompletar = stringResource(if (completada) R.string.deslizar_desmarcar else R.string.deslizar_completar)
    val textoEliminar = stringResource(R.string.deslizar_eliminar)

    SwipeToDismissBox(
        state = estado,
        enableDismissFromStartToEnd = onCompletar != null,
        enableDismissFromEndToStart = onEliminar != null,
        modifier = modifier.semantics {
            customActions = listOfNotNull(
                onCompletar?.let { CustomAccessibilityAction(textoCompletar) { it(); true } },
                onEliminar?.let { CustomAccessibilityAction(textoEliminar) { it(); true } }
            )
        },
        backgroundContent = {
            val direccion = estado.dismissDirection
            val color by animateColorAsState(
                when (direccion) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondary
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                label = "fondo"
            )
            val colorIcono = if (direccion == SwipeToDismissBoxValue.StartToEnd)
                MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onError
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = margenVertical)
                    .clip(forma)
                    .background(color)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (direccion == SwipeToDismissBoxValue.StartToEnd) Arrangement.Start else Arrangement.End
            ) {
                if (direccion == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(if (completada) Icons.AutoMirrored.Filled.Undo else Icons.Filled.Check, contentDescription = null, tint = colorIcono)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(textoCompletar, color = colorIcono, style = MaterialTheme.typography.labelLarge)
                } else if (direccion == SwipeToDismissBoxValue.EndToStart) {
                    Text(textoEliminar, color = colorIcono, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = colorIcono)
                }
            }
        }
    ) {
        // Las filas transparentes se pintan con fondoContenido; las tarjetas ya tienen su color.
        val fondo = if (fondoContenido == Color.Transparent) Modifier else Modifier.clip(forma).background(fondoContenido)
        Box(modifier = fondo) {
            contenido()
        }
    }
}
