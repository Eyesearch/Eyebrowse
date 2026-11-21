/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eyesearch.eyebrowse.account.IndoAccountConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * The app's side of the sync contract, and nothing else.
 *
 * Every method here mirrors one endpoint that Indo Account publishes, with the field names they
 * return rather than names of our own. When they change the contract, this file is the only place
 * that has to change.
 *
 * The token is handed in, never held: a sync call cannot outlive the session it was made for, and
 * nothing here writes a token anywhere.
 */
class SyncClient(
    private val tokenProvider: () -> String?,
    private val baseUrl: String = IndoAccountConfig.API_BASE,
) {

    /** What one record looks like on the wire. `payload` is opaque, we only pass it through. */
    data class Record(
        val id: String,
        val updatedAt: String,
        val deleted: Boolean,
        val payload: String,
    )

    data class PullResult(val records: List<Record>, val cursor: String, val hasMore: Boolean)

    data class PushResult(val accepted: Int, val conflicts: List<Record>, val cursor: String)

    data class TypeStatus(val type: String, val enabled: Boolean, val count: Int, val lastUpdate: String?)

    /** The outcome of one call, in a shape the caller can act on without parsing strings. */
    sealed interface Outcome<out T> {
        data class Ok<T>(val value: T) : Outcome<T>

        /** 401: the session is over, the caller must sign out rather than retry. */
        data object SessionOver : Outcome<Nothing>

        /** 403: this type is switched off for the account on the server. */
        data object TypeDisabled : Outcome<Nothing>

        /** 429: come back after [seconds]. */
        data class RateLimited(val seconds: Long) : Outcome<Nothing>

        /** 413: the batch was too big and must be split. */
        data object TooLarge : Outcome<Nothing>

        data class Failed(val message: String) : Outcome<Nothing>
    }

    suspend fun status(): Outcome<List<TypeStatus>> = call("GET", "/api/v1/app/sync/status") { body ->
        val types = body.getJSONArray("types")
        buildList {
            for (i in 0 until types.length()) {
                val entry = types.getJSONObject(i)
                add(
                    TypeStatus(
                        type = entry.getString("type"),
                        enabled = entry.getBoolean("enabled"),
                        count = entry.optInt("count"),
                        lastUpdate = entry.optString("last_update").takeIf { it.isNotEmpty() && it != "null" },
                    ),
                )
            }
        }
    }

    suspend fun pull(type: SyncType, since: String?): Outcome<PullResult> {
        val query = if (since.isNullOrBlank()) "" else "?since=$since"
        return call("GET", "/api/v1/app/sync/${type.key}$query") { body ->
            PullResult(
                records = body.getJSONArray("records").toRecords(),
                cursor = body.optString("cursor"),
                hasMore = body.optBoolean("has_more"),
            )
        }
    }

    suspend fun push(type: SyncType, records: List<Record>): Outcome<PushResult> {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject().apply {
                    put("id", record.id)
                    put("updated_at", record.updatedAt)
                    put("deleted", record.deleted)
                    // A logins payload is already ciphertext, so it goes as a string and comes back
                    // byte for byte. Everything else is JSON and goes as JSON.
                    put("payload", payloadValue(record))
                },
            )
        }
        val body = JSONObject().put("records", array)
        return call("POST", "/api/v1/app/sync/${type.key}", body.toString()) { response ->
            PushResult(
                accepted = response.optInt("accepted"),
                conflicts = response.optJSONArray("conflicts")?.toRecords() ?: emptyList(),
                cursor = response.optString("cursor"),
            )
        }
    }

    suspend fun deleteAll(type: SyncType): Outcome<Int> =
        call("DELETE", "/api/v1/app/sync/${type.key}") { body -> body.optInt("deleted") }

    suspend fun setEnabled(type: SyncType, enabled: Boolean): Outcome<Boolean> {
        val body = JSONObject().put("enabled", enabled)
        return call("PUT", "/api/v1/app/sync/${type.key}", body.toString()) { response ->
            response.optBoolean("enabled")
        }
    }

    private fun payloadValue(record: Record): Any {
        // Logins are ciphertext, so they are a string. Anything that already looks like JSON is
        // passed as JSON so the server stores exactly what the feature produced.
        return try {
            JSONObject(record.payload)
        } catch (e: Exception) {
            record.payload
        }
    }

    private fun JSONArray.toRecords(): List<Record> = buildList {
        for (i in 0 until length()) {
            val entry = getJSONObject(i)
            val payload = entry.opt("payload")
            add(
                Record(
                    id = entry.getString("id"),
                    updatedAt = entry.getString("updated_at"),
                    deleted = entry.optBoolean("deleted"),
                    payload = when (payload) {
                        null, JSONObject.NULL -> ""
                        is String -> payload
                        else -> payload.toString()
                    },
                ),
            )
        }
    }

    private suspend fun <T> call(
        method: String,
        path: String,
        body: String? = null,
        parse: (JSONObject) -> T,
    ): Outcome<T> = withContext(Dispatchers.IO) {
        val token = tokenProvider()
            ?: return@withContext Outcome.SessionOver

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }

            val code = connection.responseCode
            when {
                code in 200..299 -> {
                    val text = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    Outcome.Ok(parse(JSONObject(text)))
                }
                code == 401 -> Outcome.SessionOver
                code == 403 -> Outcome.TypeDisabled
                code == 413 -> Outcome.TooLarge
                code == 429 -> Outcome.RateLimited(
                    connection.getHeaderField("Retry-After")?.toLongOrNull() ?: DEFAULT_RETRY_SECONDS,
                )
                else -> Outcome.Failed("HTTP $code")
            }
        } catch (e: Exception) {
            Outcome.Failed(e.message ?: "network error")
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private const val DEFAULT_RETRY_SECONDS = 60L
    }
}
