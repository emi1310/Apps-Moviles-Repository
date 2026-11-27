package com.example.estudia.ui.pantallas

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.estudia.R
import com.example.estudia.ui.componentes.IconoDegradado
import com.example.estudia.ui.theme.BloqueAzul
import com.example.estudia.ui.theme.BloqueNaranja
import com.example.estudia.ui.theme.BloqueVioleta
import com.example.estudia.ui.theme.LocalDegradado
import kotlinx.coroutines.launch

// Lo que se puede hacer desde la guía (cada paso tiene su botón).
enum class PasoGuia { MATERIAS, HORARIO, RECORDATORIOS }

private data class PaginaGuia(
    val paso: PasoGuia,
    val icono: ImageVector,
    val color: Color,
    val titulo: Int,
    val texto: Int,
    val boton: Int
)

private val paginas = listOf(
    PaginaGuia(PasoGuia.MATERIAS, Icons.Outlined.School, BloqueVioleta, R.string.guia_materias_titulo, R.string.guia_materias_texto, R.string.guia_materias_boton),
    PaginaGuia(PasoGuia.HORARIO, Icons.Outlined.CalendarViewWeek, BloqueAzul, R.string.guia_horario_titulo, R.string.guia_horario_texto, R.string.guia_horario_boton),
    PaginaGuia(PasoGuia.RECORDATORIOS, Icons.Outlined.NotificationsActive, BloqueNaranja, R.string.guia_recordatorios_titulo, R.string.guia_recordatorios_texto, R.string.guia_recordatorios_boton)
)

// Guía de primera vez: tres pantallas que se pasan deslizando (cargar las
// materias, el horario desde una foto y los recordatorios). Cada una tiene
// un botón que cierra la guía y lleva a hacerlo; "Saltar" o "Empezar" la
// cierran sin más. Se muestra una sola vez por usuario (SesionViewModel).
@Composable
fun GuiaPrimeraVez(onPaso: (PasoGuia) -> Unit, onCerrar: () -> Unit) {
    val pager = rememberPagerState { paginas.size }
    val alcance = rememberCoroutineScope()
    val ultima = pager.currentPage == paginas.lastIndex
    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalDegradado.current)
                .systemBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCerrar) { Text(stringResource(R.string.guia_saltar), color = Color.White) }
                }
                HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { i ->
                    val pagina = paginas[i]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        IconoDegradado(pagina.icono, pagina.color, tamano = 96.dp)
                        Spacer(modifier = Modifier.height(28.dp))
                        Text(
                            stringResource(pagina.titulo),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(pagina.texto),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        OutlinedButton(
                            onClick = { onPaso(pagina.paso) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) { Text(stringResource(pagina.boton)) }
                    }
                }
                // Puntitos: cuál de las tres se está viendo.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .semantics { contentDescription = "${pager.currentPage + 1} / ${paginas.size}" },
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(paginas.size) { i ->
                        val activo = i == pager.currentPage
                        val ancho by animateDpAsState(if (activo) 22.dp else 8.dp, label = "punto")
                        val color by animateColorAsState(if (activo) Color.White else Color.White.copy(alpha = 0.4f), label = "colorPunto")
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(ancho)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
                Button(
                    onClick = { if (ultima) onCerrar() else alcance.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BloqueVioleta),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).height(52.dp)
                ) {
                    Text(stringResource(if (ultima) R.string.guia_empezar else R.string.guia_siguiente), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
