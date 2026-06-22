package com.project.core.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android [SecureStorage] backed by the **Android Keystore**.
 *
 * ## Strategy / Decisions
 * - **Hardware-backed AES-256-GCM:** A symmetric key is generated inside the Android Keystore under a
 *   fixed alias and never leaves secure hardware. Values are encrypted with AES/GCM/NoPadding and only
 *   the resulting ciphertext (plus IV) is persisted — in a dedicated [android.content.SharedPreferences]
 *   file — so nothing sensitive is stored in plaintext.
 * - **IV per write:** GCM requires a unique IV per encryption. The Keystore generates a random 12-byte
 *   IV on each `init`; it's prepended to the ciphertext (`IV‖ciphertext`) so decryption can recover it.
 *
 * ## How It Works
 * - `putString`: encrypt → `Base64(IV‖ciphertext)` → store under the key.
 * - `getString`: read → split IV/ciphertext → decrypt → UTF-8 string (or `null` if absent).
 * - `remove`: delete the stored entry.
 */
class KeystoreSecureStorage(
    context: Context,
) : SecureStorage {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun putString(key: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(value.encodeToByteArray())

        val combined = ByteArray(iv.size + ciphertext.size)
        iv.copyInto(combined, destinationOffset = 0)
        ciphertext.copyInto(combined, destinationOffset = iv.size)

        prefs.edit {
            putString(key, Base64.encodeToString(combined, Base64.NO_WRAP))
        }
    }

    override fun getString(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        val combined = Base64.decode(stored, Base64.NO_WRAP)

        val iv = combined.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = combined.copyOfRange(IV_SIZE_BYTES, combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_BITS, iv),
        )
        return cipher.doFinal(ciphertext).decodeToString()
    }

    override fun remove(key: String) {
        prefs.edit {
            remove(key)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "chirp_secure_storage_key"
        const val PREFS_NAME = "chirp_secure_prefs"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE_BYTES = 12
        const val GCM_TAG_BITS = 128
        const val KEY_SIZE_BITS = 256
    }
}
