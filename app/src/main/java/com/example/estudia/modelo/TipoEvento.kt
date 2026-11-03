package com.example.estudia.modelo

// Tipos de evento del calendario. Se usa sealed class (opciones fijas),
// igual que Screen en la navegación.
sealed class TipoEvento {
    object CLASE : TipoEvento()
    object EXAMEN : TipoEvento()
    object ENTREGA : TipoEvento()
    object BLOQUE_ESTUDIO : TipoEvento()
}

// Solo las clases se pueden repetir cada semana. La regla está en el modelo
// para que el ViewModel (que la valida) y el diálogo (que muestra o no la
// opción) usen la misma.
val TipoEvento.sePuedeRepetir: Boolean get() = this == TipoEvento.CLASE
