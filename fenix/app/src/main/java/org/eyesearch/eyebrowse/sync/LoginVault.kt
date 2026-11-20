/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.sync

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The encryption that makes a saved login safe to upload.
 *
 * The key is generated inside Android's Keystore and never leaves it, so the phone holds the only
 * copy. The server stores the ciphertext and can do nothing with it. That is what lets us promise
 * end to end: not that we choose not to look, but that we cannot.
 */
object LoginVault {

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "eyebrowse_login_vault"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12

    /**
     * Encrypts [plaintext] and returns a single string holding the IV and the ciphertext, which is
     * exactly what gets uploaded.
     */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        iv.copyInto(combined)
        encrypted.copyInto(combined, iv.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /** Reverses [encrypt]. Throws if the blob was tampered with, which is the point of GCM. */
    fun decrypt(stored: String): String {
        val combined = Base64.decode(stored, Base64.NO_WRAP)
        require(combined.size > IV_LENGTH_BYTES) { "ciphertext is too short to be ours" }
        val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}
