package com.example.estudia.datos

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// La clave de IA de cada usuario (cada uno conecta su propia cuenta de
// Gemini). Es una interfaz para que los tests usen una en memoria.
interface ClavesIA {
    // null = ese usuario no cargó su clave.
    fun clave(usuarioId: Int): Flow<String?>
    // null = borrarla.
    suspend fun guardar(usuarioId: Int, clave: String?)
}

private val Context.clavesDataStore by preferencesDataStore(name = "claves_ia")

// Guarda cada clave cifrada (AES-GCM) con una llave del Keystore de Android:
// la llave no sale nunca del teléfono, así que el archivo solo no sirve para
// leer las claves. No es parte de la copia de seguridad de Estudia+. Si
// Android restaura el archivo en otro teléfono, no se puede descifrar y
// simplemente hay que volver a conectar la clave.
class ClavesIACifradas(private val context: Context) : ClavesIA {

    override fun clave(usuarioId: Int): Flow<String?> =
        context.clavesDataStore.data.map { it[nombre(usuarioId)]?.let(::descifrar) }

    override suspend fun guardar(usuarioId: Int, clave: String?) {
        context.clavesDataStore.edit {
            if (clave.isNullOrBlank()) it.remove(nombre(usuarioId)) else it[nombre(usuarioId)] = cifrar(clave.trim())
        }
    }

    private fun nombre(usuarioId: Int) = stringPreferencesKey("clave_$usuarioId")

    private fun llave(): SecretKey {
        val almacen = KeyStore.getInstance(ALMACEN).apply { load(null) }
        (almacen.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ALMACEN).apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
        }.generateKey()
    }

    // Se guarda "iv:cifrado", los dos en Base64.
    private fun cifrar(texto: String): String {
        val cifrador = Cipher.getInstance(TRANSFORMACION).apply { init(Cipher.ENCRYPT_MODE, llave()) }
        val cifrado = cifrador.doFinal(texto.toByteArray(Charsets.UTF_8))
        return base64(cifrador.iv) + ":" + base64(cifrado)
    }

    private fun descifrar(guardado: String): String? = try {
        val (iv, cifrado) = guardado.split(':', limit = 2).map { Base64.decode(it, Base64.NO_WRAP) }
        val cifrador = Cipher.getInstance(TRANSFORMACION).apply { init(Cipher.DECRYPT_MODE, llave(), GCMParameterSpec(128, iv)) }
        String(cifrador.doFinal(cifrado), Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }

    private fun base64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private companion object {
        const val ALMACEN = "AndroidKeyStore"
        const val ALIAS = "estudia_claves_ia"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
    }
}
