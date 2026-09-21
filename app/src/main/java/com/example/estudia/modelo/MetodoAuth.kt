package com.example.estudia.modelo

// Indica cómo creó su cuenta el usuario: con usuario/contraseña (LOCAL)
// o con Google (GOOGLE, reservado para cuando se implemente de verdad).
sealed class MetodoAuth {
    object LOCAL : MetodoAuth()
    object GOOGLE : MetodoAuth()
}
