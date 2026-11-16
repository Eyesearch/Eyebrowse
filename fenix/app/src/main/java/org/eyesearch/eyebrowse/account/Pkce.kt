/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.account

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * PKCE, S256. The account refuses the handoff without a challenge, so this is not optional.
 *
 * The verifier never leaves the app except as the hash in the sign-in URL, and it is sent back once,
 * in the exchange, exactly as the contract asks.
 */
object Pkce {

    private const val VERIFIER_BYTES = 32

    fun newVerifier(): String {
        val bytes = ByteArray(VERIFIER_BYTES)
        SecureRandom().nextBytes(bytes)
        return base64Url(bytes)
    }

    fun challenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return base64Url(digest)
    }

    private fun base64Url(bytes: ByteArray): String = Base64.encodeToString(
        bytes,
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )
}
