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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.abs
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
// Parte del ancho que hay que arrastrar (y mantener al soltar) para borrar o completar.
const val RECORRIDO_MINIMO = 0.5f

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
    // Para no borrar ni completar sin querer al navegar:
    // - hay que arrastrar más de la mitad del ancho, y
    // - un deslizamiento rápido pero corto no cuenta (si al soltar no se
    //   llegó a RECORRIDO_MINIMO del ancho, la fila vuelve a su lugar).
    var ancho by remember { mutableIntStateOf(0) }
    lateinit var estado: SwipeToDismissBoxState
    estado = rememberSwipeToDismissBoxState(
        confirmValueChange = { valor ->
            val recorrido = runCatching { abs(estado.requireOffset()) }.getOrDefault(0f)
            val suficiente = ancho > 0 && recorrido >= ancho * RECORRIDO_MINIMO
            when (valor) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (suficiente) alCompletar?.invoke()
                    false            // no se va: vuelve a su lugar ya completada
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (suficiente) alEliminar?.invoke()
                    suficiente && alEliminar != null
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { total -> total * 0.5f }
    )
    // El estado se guarda por fila (por su id). Si algo borrado vuelve con
    // "Deshacer", vuelve con el mismo id y recuperaría el estado "deslizada
    // para borrar" (corrida, con el fondo rojo). Al aparecer, vuelve a su lugar.
    LaunchedEffect(Unit) {
        if (estado.currentValue != SwipeToDismissBoxValue.Settled) estado.reset()
    }

    val textoCompletar = stringResource(if (completada) R.string.deslizar_desmarcar else R.string.deslizar_completar)
    val textoEliminar = stringResource(R.string.deslizar_eliminar)

    SwipeToDismissBox(
        state = estado,
        enableDismissFromStartToEnd = onCompletar != null,
        enableDismissFromEndToStart = onEliminar != null,
        // Un deslizón rápido y corto cambia de sección (la fila vuelve a su lugar).
        modifier = modifier.onSizeChanged { ancho = it.width }.deslizonCambiaSeccion { ancho * RECORRIDO_MINIMO }.semantics {
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
                    Icon(if (completada) Icons.AutoMirrored.Outlined.Undo else Icons.Outlined.Check, contentDescription = null, tint = colorIcono)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(textoCompletar, color = colorIcono, style = MaterialTheme.typography.labelLarge)
                } else if (direccion == SwipeToDismissBoxValue.EndToStart) {
                    Text(textoEliminar, color = colorIcono, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = colorIcono)
                }
            }
        }
    ) {
        // Las filas transparentes se pintan con fondoContenido; las tarjetas ya tienen su color.
        // Mientras se desliza, abajo va el color del fondo: las tarjetas son de
        // vidrio y si no se vería el verde o el rojo a través.
        val deslizando = estado.dismissDirection != SwipeToDismissBoxValue.Settled
        val base = if (deslizando) Modifier.clip(forma).background(MaterialTheme.colorScheme.background) else Modifier
        val fondo = base.then(if (fondoContenido == Color.Transparent) Modifier else Modifier.clip(forma).background(fondoContenido))
        Box(modifier = fondo) {
            contenido()
        }
    }
}
