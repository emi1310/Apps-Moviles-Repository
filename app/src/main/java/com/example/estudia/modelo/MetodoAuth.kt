package com.example.estudia.modelo

sealed class MetodoAuth {
    object LOCAL : MetodoAuth()
    object GOOGLE : MetodoAuth()
}
