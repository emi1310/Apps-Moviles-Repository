package com.example.estudia.modelo

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

// Representa una materia del usuario (ej: "Matemática II").
// Es dueña de sus propias tareas, eventos y notas: si esta Materia
// se elimina, sus tareas/eventos/notas dejan de existir con ella
// (esto refleja la composición que definimos en el diagrama UML).
data class Materia(
    val id: Int,
    val nombre: String,
    val color: String,       // guardamos el color como texto hexadecimal, ej: "#6C5CE7"
    var archivada: Boolean = false,
    val tareas: MutableList<Tarea> = mutableListOf(),
    val eventos: MutableList<Evento> = mutableListOf(),
    val notas: SnapshotStateList<Nota> = mutableStateListOf()
) {
    // Calcula el % de tareas completadas sobre el total.
    // Si no hay tareas cargadas, devuelve 100 (para que no rompa la barra de progreso).
    fun calcularProgreso(): Int {
        if (tareas.isEmpty()) return 100
        val completadas = tareas.count { it.completada }
        return (completadas * 100) / tareas.size
    }
}