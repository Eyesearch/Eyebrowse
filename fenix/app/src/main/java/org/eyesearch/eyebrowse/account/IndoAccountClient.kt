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
import mozilla.components.concept.fetch.isSuccess
import org.json.JSONObject

/**
 * The identity block, exactly the five fields the contract allows. No email, no mobile, no date of
 * birth, no address.
 */
data class IndoIdentity(
    val userId: String,
    val indoId: String,
    val fullName: String,
    val nickname: String,
    val language: String,
) {
    /** What the settings card shows: the full name if there is one, else the nickname, else the ID. */
    val displayName: String
        get() = fullName.ifBlank { nickname }.ifBlank { indoId }
}

/** What the app is, as the account names it. */
data class IndoApp(val key: String, val name: String)

/** What a successful exchange hands back. */
data class IndoSession(
    val token: String,
    val expiresInSeconds: Long,
    val sessionId: String?,
    val app: IndoApp?,
    val identity: IndoIdentity?,
)

/** A failed call, carrying the status so the caller can tell 401 from everything else. */
class IndoAccountError(val status: Int, message: String) : Exception(message)

/**
 * Talks to Indo Account, against the frozen contract: exchange a one-time code with its PKCE
 * verifier at /api/v1/app/session, then read identity at /api/v1/app/me.
 *
 * /api/v1/me is deliberately never called: an app token is refused there with 403 by design.
 * Nothing here logs a token, a code, a verifier or a deep link.
 */
class IndoAccountClient(private val client: Client) {

    suspend fun exchangeCode(code: String, codeVerifier: String, device: String): IndoSession =
        withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put("app", IndoAccountConfig.APP_ID)
                .put("code", code)
                .put("code_verifier", codeVerifier)
                .put("device", device)
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

                IndoSession(
                    token = token,
                    expiresInSeconds = json.optLong("expires_in", 0L),
                    sessionId = json.optString("session_id").takeIf { value -> value.isNotBlank() },
                    app = json.optJSONObject("app")?.let { app ->
                        IndoApp(
                            key = app.optString("key"),
                            name = app.optString("name"),
                        )
                    },
                    identity = json.optJSONObject("identity")?.toIdentity(),
                )
            }
        }

    suspend fun identity(token: String): IndoIdentity = withContext(Dispatchers.IO) {
        val response = client.fetch(
            Request(
                url = IndoAccountConfig.API_BASE + IndoAccountConfig.IDENTITY_PATH,
                method = Request.Method.GET,
                headers = MutableHeaders(
                    "Authorization" to "Bearer $token",
                    Headers.Names.CONTENT_TYPE to Headers.Values.CONTENT_TYPE_APPLICATION_JSON,
                ),
            ),
        )

        response.use {
            val text = it.body.useBufferedReader { reader -> reader.readText() }
            if (!it.isSuccess) {
                throw IndoAccountError(it.status, "The account could not be read.")
            }
            JSONObject(text).toIdentity()
        }
    }

    private fun JSONObject.toIdentity(): IndoIdentity {
        val indoId = optString("indo_id")
        return IndoIdentity(
            userId = optString("user_id"),
            indoId = indoId,
            fullName = optString("full_name"),
            nickname = optString("nickname"),
            language = optString("language"),
        )
    }
}
