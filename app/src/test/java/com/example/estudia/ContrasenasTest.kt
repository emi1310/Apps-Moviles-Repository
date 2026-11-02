package com.example.estudia

import com.example.estudia.util.Contrasenas
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrasenasTest {

    @Test
    fun laContrasenaCorrectaSeVerifica() {
        val hash = Contrasenas.hashear("secreto123")
        assertTrue(Contrasenas.verificar("secreto123", hash))
    }

    @Test
    fun unaContrasenaIncorrectaNoSeVerifica() {
        val hash = Contrasenas.hashear("secreto123")
        assertFalse(Contrasenas.verificar("Secreto123", hash))
        assertFalse(Contrasenas.verificar("", hash))
    }

    @Test
    fun elHashNoContieneLaContrasena() {
        val hash = Contrasenas.hashear("secreto123")
        assertFalse(hash.contains("secreto123"))
        assertTrue(hash.startsWith("pbkdf2$"))
    }

    // Por la sal al azar, la misma contraseña da hashes distintos: así no se
    // puede saber si dos usuarios usan la misma contraseña.
    @Test
    fun laMismaContrasenaDaHashesDistintos() {
        assertNotEquals(Contrasenas.hashear("demo"), Contrasenas.hashear("demo"))
    }

    @Test
    fun unValorGuardadoInvalidoNoRompe() {
        assertFalse(Contrasenas.verificar("demo", null))
        assertFalse(Contrasenas.verificar("demo", "demo"))
        assertFalse(Contrasenas.verificar("demo", "pbkdf2\$x\$y"))
    }
}
