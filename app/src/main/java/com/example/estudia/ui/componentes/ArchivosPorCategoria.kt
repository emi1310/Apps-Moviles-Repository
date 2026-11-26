package com.example.estudia.ui.componentes

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.estudia.R
import com.example.estudia.modelo.Archivo
import com.example.estudia.modelo.CategoriaArchivo

@StringRes
fun nombreCategoria(categoria: CategoriaArchivo): Int = when (categoria) {
    CategoriaArchivo.TEORIA -> R.string.categoria_teoria
    CategoriaArchivo.TPS -> R.string.categoria_tps
    CategoriaArchivo.INFORMACION -> R.string.categoria_informacion
    CategoriaArchivo.EXAMENES -> R.string.categoria_examenes
    CategoriaArchivo.OTROS -> R.string.categoria_otros
}

fun iconoCategoria(categoria: CategoriaArchivo): ImageVector = when (categoria) {
    CategoriaArchivo.TEORIA -> Icons.AutoMirrored.Outlined.MenuBook
    CategoriaArchivo.TPS -> Icons.AutoMirrored.Outlined.Assignment
    CategoriaArchivo.INFORMACION -> Icons.Outlined.Info
    CategoriaArchivo.EXAMENES -> Icons.Outlined.Quiz
    CategoriaArchivo.OTROS -> Icons.Outlined.Folder
}

// Archivos de una materia agrupados por categoría (Teoría, TPs, Información
// de la materia, Exámenes y Otros). Arriba, pastillas para filtrar; con
// "Todos" se ven agrupados. Lo que se adjunta va a la categoría elegida en
// el filtro; con "Todos" se pregunta a cuál. Cada archivo tiene un botón
// para cambiarle la categoría.
@Composable
fun ArchivosPorCategoria(
    archivos: List<Archivo>,
    color: Color,
    onAbrir: (Archivo) -> Unit,
    onEliminar: (Archivo) -> Unit,
    onCambiarCategoria: (Archivo, CategoriaArchivo) -> Unit,
    onAdjuntar: (CategoriaArchivo) -> Unit,
    onSacarFoto: (CategoriaArchivo) -> Unit
) {
    // null = "Todos".
    var filtro by rememberSaveable { mutableStateOf<CategoriaArchivo?>(null) }
    var cambiando by remember { mutableStateOf<Archivo?>(null) }
    // Qué hacer cuando se elija la categoría de lo que se va a agregar.
    var agregarCon by remember { mutableStateOf<((CategoriaArchivo) -> Unit)?>(null) }

    if (archivos.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = filtro == null,
                onClick = { filtro = null },
                label = { Text(stringResource(R.string.categoria_todos, archivos.size)) }
            )
            CategoriaArchivo.entries.forEach { categoria ->
                val cantidad = archivos.count { it.categoria == categoria }
                FilterChip(
                    selected = filtro == categoria,
                    onClick = { filtro = categoria },
                    label = { Text("${stringResource(nombreCategoria(categoria))} ($cantidad)") }
                )
            }
        }
    }

    val elegida = filtro
    if (elegida == null) {
        val grupos = archivos.groupBy { it.categoria }
        // Si todo está en "Otros" no hace falta el título del grupo.
        val conTitulos = grupos.keys.any { it != CategoriaArchivo.OTROS }
        CategoriaArchivo.entries.forEach { categoria ->
            val delGrupo = grupos[categoria] ?: return@forEach
            if (conTitulos) {
                Text(
                    text = stringResource(nombreCategoria(categoria)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
            ListaArchivos(delGrupo, color, onAbrir, onEliminar, onCambiarCategoria = { cambiando = it })
        }
    } else {
        val delFiltro = archivos.filter { it.categoria == elegida }
        if (delFiltro.isEmpty()) {
            Text(
                text = stringResource(R.string.categoria_vacia, stringResource(nombreCategoria(elegida))),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        ListaArchivos(delFiltro, color, onAbrir, onEliminar, onCambiarCategoria = { cambiando = it })
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BotonBloque(stringResource(R.string.adjuntar_archivo), Icons.Outlined.AttachFile, color, {
            val f = filtro
            if (f != null) onAdjuntar(f) else agregarCon = onAdjuntar
        }, Modifier.weight(1f))
        BotonBloque(stringResource(R.string.foto_sacar), Icons.Outlined.PhotoCamera, color, {
            val f = filtro
            if (f != null) onSacarFoto(f) else agregarCon = onSacarFoto
        }, Modifier.weight(1f))
    }

    cambiando?.let { archivo ->
        DialogoCategoria(
            titulo = stringResource(R.string.categoria_cambiar),
            actual = archivo.categoria,
            onElegir = { onCambiarCategoria(archivo, it); cambiando = null },
            onCancelar = { cambiando = null }
        )
    }
    agregarCon?.let { accion ->
        DialogoCategoria(
            titulo = stringResource(R.string.categoria_donde_guardar),
            actual = null,
            onElegir = { agregarCon = null; accion(it) },
            onCancelar = { agregarCon = null }
        )
    }
}

// Elegir una categoría: un bloque por cada una.
@Composable
private fun DialogoCategoria(
    titulo: String,
    actual: CategoriaArchivo?,
    onElegir: (CategoriaArchivo) -> Unit,
    onCancelar: () -> Unit
) {
    DialogoApp(
        titulo = titulo,
        icono = Icons.Outlined.Label,
        textoConfirmar = null,
        onConfirmar = {},
        onCancelar = onCancelar
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoriaArchivo.entries.forEach { categoria ->
                BloqueOpcion(
                    icono = iconoCategoria(categoria),
                    texto = stringResource(nombreCategoria(categoria)),
                    elegido = categoria == actual,
                    modifier = Modifier.fillMaxWidth()
                ) { onElegir(categoria) }
            }
        }
    }
}
