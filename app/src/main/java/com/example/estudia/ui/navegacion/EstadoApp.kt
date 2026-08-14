package com.example.estudia.ui.navegacion

// Controla en qué etapa general está la app: antes de entrar
// (bienvenida, login, registro) o ya adentro navegando entre pantallas.
enum class EstadoApp {
    BIENVENIDA,
    LOGIN,
    REGISTRO,
    PRINCIPAL
}