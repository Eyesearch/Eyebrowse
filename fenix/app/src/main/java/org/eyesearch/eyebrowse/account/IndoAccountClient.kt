/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.account

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.fetch.Headers
import mozilla.components.concept.fetch.MutableHeaders
import mozilla.components.concept.fetch.Request
import mozilla.components.concept.fetch.Response
import mozilla.components.concept.fetch.isSuccess
import org.json.JSONObject

/** What the account tells us about the person holding the phone. */
data class IndoProfile(
    val indoId: String,
    val displayName: String,
)

/** What a successful sign-in hands back. */
data class IndoSession(
    val token: String,
    val profile: IndoProfile?,
)

/** A failed call, carrying the status so the caller can tell 401 from everything else. */
class IndoAccountError(val status: Int, message: String) : Exception(message)

/**
 * Talks to Indo Account. Two calls only, because identity is the whole job of this step: exchange a
 * one-time code for a scoped token, and read the profile live with that token.
 *
 * Nothing here logs a token, a code or a deep link.
 */
class IndoAccountClient(private val client: Client) {

    suspend fun exchangeCode(code: String): IndoSession = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("app", IndoAccountConfig.APP_ID)
            .put("code", code)
            .toString()

        val response = client.fetch(
            Request(
                url = IndoAccountConfig.API_BASE + IndoAccountConfig.EXCHANGE_PATH,
                method = Request.Method.POST,
                headers = MutableHeaders(
                    Headers.Names.CONTENT_TYPE to Headers.Values.CONTENT_TYPE_APPLICATION_JSON,
                ),
                body = Request.Body.fromString(payload),
            ),
        )

        response.use {
            val text = it.body.useBufferedReader { reader -> reader.readText() }
            if (!it.isSuccess) {
                throw IndoAccountError(it.status, "Sign in could not be completed.")
            }
            val json = JSONObject(text)
            val token = json.optString("token").takeIf { value -> value.isNotBlank() }
                ?: throw IndoAccountError(it.status, "The account did not return a token.")
            IndoSession(token = token, profile = json.optJSONObject("profile")?.toProfile())
        }
    }

    suspend fun profile(token: String): IndoProfile = withContext(Dispatchers.IO) {
        val response = client.fetch(
            Request(
                url = IndoAccountConfig.API_BASE + IndoAccountConfig.PROFILE_PATH,
                method = Request.Method.GET,
                headers = MutableHeaders(
                    Headers.Names.AUTHORIZATION to "Bearer $token",
                    Headers.Names.CONTENT_TYPE to Headers.Values.CONTENT_TYPE_APPLICATION_JSON,
                ),
            ),
        )

        response.use {
            val text = it.body.useBufferedReader { reader -> reader.readText() }
            if (!it.isSuccess) {
                throw IndoAccountError(it.status, "The account could not be read.")
            }
            JSONObject(text).toProfile()
        }
    }

    private fun JSONObject.toProfile(): IndoProfile {
        val id = optString("indo_id")
            .ifBlank { optString("indoId") }
            .ifBlank { optString("handle") }
        val name = optString("display_name")
            .ifBlank { optString("displayName") }
            .ifBlank { optString("name") }
        return IndoProfile(indoId = id, displayName = name.ifBlank { id })
    }
}
