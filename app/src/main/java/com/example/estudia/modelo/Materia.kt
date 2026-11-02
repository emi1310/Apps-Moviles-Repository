package com.example.estudia.modelo

// Representa una materia del usuario (ej: "Matemática II").
// Es dueña de sus propias tareas, eventos y notas: si esta Materia
// se elimina, sus tareas/eventos/notas dejan de existir con ella
// (esto refleja la composición que definimos en el diagrama UML;
// en Room se resuelve con ForeignKey + CASCADE).
//
// Es un modelo de dominio: no depende de Compose ni de Room. Las listas
// son List comunes (inmutables); cuando algo cambia, Room emite una
// Materia nueva por el Flow y la pantalla se redibuja.
data class Materia(
    val id: Int,
    val nombre: String,
    val color: Long,         // color en formato ARGB, ej: 0xFF6C5CE7
    val archivada: Boolean = false,
    val tareas: List<Tarea> = emptyList(),
    val eventos: List<Evento> = emptyList(),
    val notas: List<Nota> = emptyList(),
    val archivos: List<Archivo> = emptyList()   // adjuntos de la materia (no de sus notas)
) {
    // Archivos de la materia y de todas sus notas (para borrarlos del disco).
    fun todosLosArchivos(): List<Archivo> = archivos + notas.flatMap { it.archivos }

    // Calcula el % de tareas completadas sobre el total.
    // Si no hay tareas cargadas, devuelve 100 (para que no rompa el porcentaje).
    fun calcularProgreso(): Int {
        if (tareas.isEmpty()) return 100
        val completadas = tareas.count { it.completada }
        return (completadas * 100) / tareas.size
    }
}
