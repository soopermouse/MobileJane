package nl.sdthrussell.jane.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Small Android-Keystore wrapper for Jane Mobile secrets and local state. */
class JaneCrypto {
    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "jane_mobile_local_aes_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    fun encryptString(value: String): String = Base64.encodeToString(encrypt(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)

    fun decryptString(value: String): String = String(
        decrypt(Base64.decode(value, Base64.NO_WRAP)),
        Charsets.UTF_8
    )

    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(plain)
        return cipher.iv + encrypted
    }

    fun decrypt(payload: ByteArray): ByteArray {
        require(payload.size > IV_BYTES) { "Encrypted payload is invalid" }
        val iv = payload.copyOfRange(0, IV_BYTES)
        val encrypted = payload.copyOfRange(IV_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }
}
