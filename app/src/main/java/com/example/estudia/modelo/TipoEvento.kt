package com.example.estudia.modelo

// Tipos de evento del calendario. Se usa sealed class (opciones fijas),
// igual que Screen en la navegación.
sealed class TipoEvento {
    object CLASE : TipoEvento()
    object EXAMEN : TipoEvento()
    object ENTREGA : TipoEvento()
    object BLOQUE_ESTUDIO : TipoEvento()
}
