/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.account

import android.content.Context
import android.os.Build
import mozilla.components.concept.fetch.Client
import org.mozilla.fenix.R

/**
 * The half of the handoff that both doors share: the in-app WebView and the deep link fallback.
 *
 * It validates the state against the one we stored, exchanges the code once with its PKCE verifier,
 * and keeps the token. It returns the string resource to show when something is wrong, and null when
 * the sign-in is done. It never logs the URL, the code or the token.
 */
class IndoAccountHandoff(
    context: Context,
    private val client: Client,
) {
    private val storage = IndoAccountStorage(context)

    suspend fun complete(code: String?, state: String?): Int? {
        if (code.isNullOrBlank() || state.isNullOrBlank()) {
            return R.string.indo_account_error_missing_code
        }

        val expectedState = storage.takePendingState()
        val codeVerifier = storage.takePendingVerifier()

        if (expectedState == null || expectedState != state || codeVerifier.isNullOrBlank()) {
            return R.string.indo_account_error_state
        }

        return try {
            val session = IndoAccountClient(client).exchangeCode(
                code = code,
                codeVerifier = codeVerifier,
                device = Build.MODEL.orEmpty(),
            )
            storage.saveToken(session.token)
            session.identity?.let { identity -> storage.saveIdentity(identity) }
            null
        } catch (error: IndoAccountError) {
            if (error.status == STATUS_UNAUTHORIZED) {
                storage.signOut()
            }
            R.string.indo_account_error_exchange
        } catch (error: Exception) {
            R.string.indo_account_error_exchange
        }
    }

    companion object {
        private const val STATUS_UNAUTHORIZED = 401
    }
}
