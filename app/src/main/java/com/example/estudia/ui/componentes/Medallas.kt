package com.example.estudia.ui.componentes

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.HourglassFull
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.ui.theme.BloqueAzul
import com.example.estudia.ui.theme.BloqueNaranja
import com.example.estudia.ui.theme.BloqueVerde
import com.example.estudia.ui.theme.BloqueVioleta
import com.example.estudia.ui.theme.PrioridadAlta
import com.example.estudia.viewmodel.TipoLogro

// Cómo se ve cada logro: ícono, color y textos. El ViewModel solo sabe qué
// logros hay y cuánto se avanzó; esto es lo visual.
data class AspectoLogro(
    val icono: ImageVector,
    val color: Color,
    @StringRes val titulo: Int,
    @StringRes val descripcion: Int
)

fun aspecto(tipo: TipoLogro): AspectoLogro = when (tipo) {
    TipoLogro.PRIMERA_TAREA -> AspectoLogro(Icons.Outlined.TaskAlt, BloqueVerde, R.string.logro_primera_tarea, R.string.logro_primera_tarea_desc)
    TipoLogro.DIEZ_TAREAS -> AspectoLogro(Icons.Outlined.DoneAll, BloqueAzul, R.string.logro_diez_tareas, R.string.logro_diez_tareas_desc)
    TipoLogro.VEINTICINCO_TAREAS -> AspectoLogro(Icons.Outlined.EmojiEvents, BloqueVioleta, R.string.logro_veinticinco_tareas, R.string.logro_veinticinco_tareas_desc)
    TipoLogro.RACHA_3 -> AspectoLogro(Icons.Filled.LocalFireDepartment, BloqueNaranja, R.string.logro_racha_3, R.string.logro_racha_3_desc)
    TipoLogro.RACHA_7 -> AspectoLogro(Icons.Outlined.Whatshot, PrioridadAlta, R.string.logro_racha_7, R.string.logro_racha_7_desc)
    TipoLogro.HORAS_10 -> AspectoLogro(Icons.Outlined.Timer, BloqueAzul, R.string.logro_horas_10, R.string.logro_horas_10_desc)
    TipoLogro.HORAS_50 -> AspectoLogro(Icons.Outlined.HourglassFull, BloqueVioleta, R.string.logro_horas_50, R.string.logro_horas_50_desc)
    TipoLogro.META_CUMPLIDA -> AspectoLogro(Icons.Outlined.Flag, BloqueVerde, R.string.logro_meta, R.string.logro_meta_desc)
    TipoLogro.APROBADO -> AspectoLogro(Icons.Outlined.Grade, BloqueNaranja, R.string.logro_aprobado, R.string.logro_aprobado_desc)
    TipoLogro.PROMEDIO_8 -> AspectoLogro(Icons.Outlined.WorkspacePremium, BloqueVioleta, R.string.logro_promedio, R.string.logro_promedio_desc)
    TipoLogro.CINCO_NOTAS -> AspectoLogro(Icons.Outlined.StickyNote2, BloqueAzul, R.string.logro_notas, R.string.logro_notas_desc)
}

// Medalla: el ícono del logro en un círculo. Conseguida = círculo lleno de
// su color con el ícono blanco; si no, gris.
@Composable
fun Medalla(tipo: TipoLogro, conseguido: Boolean, tamano: Dp = 40.dp) {
    val aspecto = aspecto(tipo)
    if (conseguido) {
        IconoCircular(aspecto.icono, Color.White, tamano = tamano, fondo = aspecto.color)
    } else {
        IconoCircular(
            aspecto.icono,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            tamano = tamano,
            fondo = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
