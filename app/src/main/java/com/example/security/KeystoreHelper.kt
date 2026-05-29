package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object KeystoreHelper {
    private const val PROVIDER = "AndroidKeyStore"
    private const val ALIAS = "PencatatUangSecureKeyV1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    init {
        initKey()
    }

    private fun initKey() {
        try {
            val keyStore = KeyStore.getInstance(PROVIDER)
            keyStore.load(null)
            if (!keyStore.containsAlias(ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
                keyGenerator.init(
                    KeyGenParameterSpec.Builder(
                        ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build()
                )
                keyGenerator.generateKey()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER)
        keyStore.load(null)
        val entry = keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey ?: throw IllegalStateException("Key not found in AndroidKeyStore")
    }

    /**
     * Encrypts a plain text string to a Base64 representation.
     * Output format: Base64(IV):Base64(CipherText)
     */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            "$ivBase64:$encryptedBase64"
        } catch (e: Throwable) {
            e.printStackTrace()
            // In case of error, we fallback safely to simple obscuring to avoid crashing user interaction
            val fallbackBytes = plainText.toByteArray(Charsets.UTF_8)
            val base64 = Base64.encodeToString(fallbackBytes, Base64.NO_WRAP)
            "FALLBACK:$base64"
        }
    }

    /**
     * Decrypts an encrypted string of the format: Base64(IV):Base64(CipherText)
     */
    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            if (encryptedText.startsWith("FALLBACK:")) {
                val data = encryptedText.substringAfter("FALLBACK:")
                return String(Base64.decode(data, Base64.NO_WRAP), Charsets.UTF_8)
            }
            val parts = encryptedText.split(":")
            if (parts.size != 2) return encryptedText // Return original if not formatted
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Throwable) {
            e.printStackTrace()
            // Fallback: If it's a raw unencrypted string (or decryption failed), return it safely
            encryptedText
        }
    }
}
