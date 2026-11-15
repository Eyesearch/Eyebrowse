/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.account

import android.content.Context
import mozilla.components.lib.dataprotect.SecureAbove22Preferences
import java.security.SecureRandom

/**
 * The scoped token and the pending sign-in state.
 *
 * The token lives in a Keystore backed preference file, never in plain preferences, never in a log.
 * The state is written before the browser leaves for the sign-in page and taken exactly once, so a
 * second deep link carrying the same state is refused.
 */
class IndoAccountStorage(context: Context) {

    private val prefs = SecureAbove22Preferences(
        context = context.applicationContext,
        name = PREFERENCES_NAME,
    )

    fun startSignIn(): String {
        val state = randomState()
        prefs.putString(KEY_PENDING_STATE, state)
        return state
    }

    fun takePendingState(): String? {
        val state = prefs.getString(KEY_PENDING_STATE)
        prefs.remove(KEY_PENDING_STATE)
        return state
    }

    fun saveToken(token: String) = prefs.putString(KEY_TOKEN, token)

    fun token(): String? = prefs.getString(KEY_TOKEN)?.takeIf { it.isNotBlank() }

    fun isSignedIn(): Boolean = token() != null

    fun signOut() {
        prefs.remove(KEY_TOKEN)
        prefs.remove(KEY_PENDING_STATE)
    }

    private fun randomState(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }

    companion object {
        private const val PREFERENCES_NAME = "indo_account"
        private const val KEY_TOKEN = "indo_account_token"
        private const val KEY_PENDING_STATE = "indo_account_pending_state"
    }
}
