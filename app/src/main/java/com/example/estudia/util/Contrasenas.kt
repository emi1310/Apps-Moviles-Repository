package com.example.estudia.util

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// Manejo seguro de contraseñas: NUNCA se guarda la contraseña, solo un
// "hash" que no se puede revertir.
//
// Se usa PBKDF2: a la contraseña se le agrega una "sal" (bytes al azar,
// distintos para cada usuario) y se la pasa muchas veces (ITERACIONES) por
// una función de hash. Así, aunque alguien copie la base de datos, no puede
// leer las contraseñas, y probar contraseñas al azar le resulta muy lento.
//
// Formato guardado: "pbkdf2$<algoritmo>$<iteraciones>$<sal hex>$<hash hex>".
// Guardar el algoritmo y las iteraciones permite cambiarlos en el futuro
// sin romper las contraseñas que ya estaban guardadas.
object Contrasenas {

    private const val PREFIJO = "pbkdf2"
    private const val ITERACIONES = 120_000
    private const val BYTES_SAL = 16
    private const val BITS_HASH = 256

    fun hashear(contrasena: String): String {
        val sal = ByteArray(BYTES_SAL).also { SecureRandom().nextBytes(it) }
        val algoritmo = algoritmoDisponible()
        val hash = pbkdf2(contrasena, sal, ITERACIONES, algoritmo)
        return listOf(PREFIJO, algoritmo, ITERACIONES.toString(), sal.aHex(), hash.aHex()).joinToString("$")
    }

    // Recalcula el hash con la misma sal y compara. MessageDigest.isEqual
    // compara en tiempo constante (no da pistas por cuánto tarda).
    fun verificar(contrasena: String, guardado: String?): Boolean {
        val partes = guardado?.split("$") ?: return false
        if (partes.size != 5 || partes[0] != PREFIJO) return false
        val iteraciones = partes[2].toIntOrNull() ?: return false
        val sal = partes[3].desdeHex()
        val esperado = partes[4].desdeHex()
        val calculado = pbkdf2(contrasena, sal, iteraciones, partes[1])
        return MessageDigest.isEqual(esperado, calculado)
    }

    // SHA256 existe desde Android 8; en Android 7 se usa SHA1 (sigue siendo
    // seguro para PBKDF2).
    private fun algoritmoDisponible(): String {
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            "PBKDF2WithHmacSHA256"
        } catch (e: NoSuchAlgorithmException) {
            "PBKDF2WithHmacSHA1"
        }
    }

    private fun pbkdf2(contrasena: String, sal: ByteArray, iteraciones: Int, algoritmo: String): ByteArray {
        val spec = PBEKeySpec(contrasena.toCharArray(), sal, iteraciones, BITS_HASH)
        try {
            return SecretKeyFactory.getInstance(algoritmo).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.aHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.desdeHex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
