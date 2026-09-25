package com.example.estudia.modelo

sealed class TipoEvento {
    object CLASE : TipoEvento()
    object EXAMEN : TipoEvento()
    object ENTREGA : TipoEvento()
    object BLOQUE_ESTUDIO : TipoEvento()
}
