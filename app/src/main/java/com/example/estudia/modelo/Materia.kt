package com.example.estudia.modelo

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color

data class Materia(
    val id: Int,
    val nombre: String,
    val color: Color,
    var archivada: Boolean = false,
    val tareas: SnapshotStateList<Tarea> = mutableStateListOf(),
    val eventos: SnapshotStateList<Evento> = mutableStateListOf(),
    val notas: SnapshotStateList<Nota> = mutableStateListOf()
) {
    fun calcularProgreso(): Int {
        if (tareas.isEmpty()) return 100
        val completadas = tareas.count { it.completada }
        return (completadas * 100) / tareas.size
    }
}
