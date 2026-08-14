package com.example.estudia.modelo

// Indica si el usuario creó su cuenta con usuario/contraseña propios (LOCAL)
// o si inició sesión con su cuenta de Google (GOOGLE).
enum class MetodoAuth {
    LOCAL,
    GOOGLE
}