/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.vpn

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Store only authenticated ciphertext; never put provider keys in ordinary preferences. */
internal object VpnProfileStore {
    private const val PREFS = "eyebrowse_vpn_profiles"
    private const val VALUE = "encrypted_profiles"
    private const val ALIAS = "eyebrowse_vpn_profiles_v1"
    private const val TRANSFORM = "AES/GCM/NoPadding"

    fun hasSaved(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(VALUE)

    @TargetApi(Build.VERSION_CODES.M)
    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        check(create) { "VPN profile key unavailable" }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    fun load(context: Context): List<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()
        val data = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(VALUE, null)
            ?: return emptyList()
        return runCatching {
            val bytes = Base64.decode(data, Base64.NO_WRAP)
            require(bytes.size > 28)
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, key(false), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            val plain = cipher.doFinal(bytes, 12, bytes.size - 12)
            try {
                val array = JSONArray(plain.toString(Charsets.UTF_8))
                (0 until minOf(array.length(), 5)).map { array.getString(it) }
            } finally {
                plain.fill(0)
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, profiles: List<String>) {
        // Android 5.x: explicitly session-only, no plaintext fallback.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val plain = JSONArray(profiles).toString().toByteArray(Charsets.UTF_8)
        try {
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.ENCRYPT_MODE, key(true))
            require(cipher.iv.size == 12)
            val data = Base64.encodeToString(cipher.iv + cipher.doFinal(plain), Base64.NO_WRAP)
            check(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(VALUE, data).commit())
        } finally {
            plain.fill(0)
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(VALUE).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(ALIAS)
            }
        }
    }
}
