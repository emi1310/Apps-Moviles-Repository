package com.example.estudia.modelo

// Representa los distintos tipos de evento que puede tener el calendario.
// Se usa un enum porque son opciones fijas y cerradas: no van a existir
// tipos de evento nuevos que el usuario pueda inventar.
enum class TipoEvento {
    CLASE,
    EXAMEN,
    ENTREGA,
    BLOQUE_ESTUDIO
}