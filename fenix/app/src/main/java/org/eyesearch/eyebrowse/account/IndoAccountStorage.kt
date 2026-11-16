/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.account

import android.content.Context
import android.util.Base64
import mozilla.components.lib.dataprotect.SecureAbove22Preferences
import java.security.SecureRandom

/**
 * The app token and the one pending sign-in.
 *
 * The token lives in a Keystore backed preference file, never in plain preferences, never in a log.
 * The state and its PKCE verifier are written before the browser leaves and taken exactly once, so a
 * second deep link carrying the same state is refused.
 */
class IndoAccountStorage(context: Context) {

    private val prefs = SecureAbove22Preferences(
        context = context.applicationContext,
        name = PREFERENCES_NAME,
    )

    /** Writes a fresh state and verifier and hands both back for the sign-in URL. */
    fun startSignIn(): SignInStart {
        val state = randomState()
        val verifier = Pkce.newVerifier()
        prefs.putString(KEY_PENDING_STATE, state)
        prefs.putString(KEY_PENDING_VERIFIER, verifier)
        return SignInStart(state = state, verifier = verifier, challenge = Pkce.challenge(verifier))
    }

    fun takePendingState(): String? {
        val state = prefs.getString(KEY_PENDING_STATE)
        prefs.remove(KEY_PENDING_STATE)
        return state
    }

    fun takePendingVerifier(): String? {
        val verifier = prefs.getString(KEY_PENDING_VERIFIER)
        prefs.remove(KEY_PENDING_VERIFIER)
        return verifier
    }

    fun saveToken(token: String) = prefs.putString(KEY_TOKEN, token)

    /**
     * The identity that came with the token, kept so the account card can be drawn on the first
     * frame instead of flickering from signed-out to signed-in while the network answers.
     */
    fun saveIdentity(identity: IndoIdentity) {
        prefs.putString(KEY_USER_ID, identity.userId)
        prefs.putString(KEY_INDO_ID, identity.indoId)
        prefs.putString(KEY_FULL_NAME, identity.fullName)
        prefs.putString(KEY_NICKNAME, identity.nickname)
        prefs.putString(KEY_LANGUAGE, identity.language)
    }

    fun identity(): IndoIdentity? {
        val indoId = prefs.getString(KEY_INDO_ID) ?: return null
        if (indoId.isBlank()) {
            return null
        }
        return IndoIdentity(
            userId = prefs.getString(KEY_USER_ID).orEmpty(),
            indoId = indoId,
            fullName = prefs.getString(KEY_FULL_NAME).orEmpty(),
            nickname = prefs.getString(KEY_NICKNAME).orEmpty(),
            language = prefs.getString(KEY_LANGUAGE).orEmpty(),
        )
    }

    fun token(): String? = prefs.getString(KEY_TOKEN)?.takeIf { it.isNotBlank() }

    fun isSignedIn(): Boolean = token() != null

    fun signOut() {
        prefs.remove(KEY_TOKEN)
        prefs.remove(KEY_USER_ID)
        prefs.remove(KEY_INDO_ID)
        prefs.remove(KEY_FULL_NAME)
        prefs.remove(KEY_NICKNAME)
        prefs.remove(KEY_LANGUAGE)
        prefs.remove(KEY_PENDING_STATE)
        prefs.remove(KEY_PENDING_VERIFIER)
    }

    private fun randomState(): String {
        val bytes = ByteArray(STATE_BYTES)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
    }

    data class SignInStart(val state: String, val verifier: String, val challenge: String)

    companion object {
        private const val PREFERENCES_NAME = "indo_account"
        private const val KEY_TOKEN = "indo_account_token"
        private const val KEY_USER_ID = "indo_account_user_id"
        private const val KEY_INDO_ID = "indo_account_indo_id"
        private const val KEY_FULL_NAME = "indo_account_full_name"
        private const val KEY_NICKNAME = "indo_account_nickname"
        private const val KEY_LANGUAGE = "indo_account_language"
        private const val KEY_PENDING_STATE = "indo_account_pending_state"
        private const val KEY_PENDING_VERIFIER = "indo_account_pending_verifier"
        private const val STATE_BYTES = 32
    }
}
