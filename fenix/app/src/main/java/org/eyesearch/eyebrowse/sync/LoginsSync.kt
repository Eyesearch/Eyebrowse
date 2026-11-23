/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.sync

import mozilla.components.concept.storage.Login
import mozilla.components.concept.storage.LoginEntry
import mozilla.components.concept.storage.LoginsStorage

/**
 * Saved logins, sealed before they travel.
 *
 * The browser keeps its own password store, and that stays the only readable copy. What leaves the
 * phone is the same entry, encrypted with the Keystore key, so the server holds a string it cannot
 * open and a second phone can only use it after the user has signed in on that phone.
 */
class LoginsSync(
    private val logins: LoginsStorage,
    private val recorder: SyncRecorder,
) {

    private val sealed = mutableMapOf<String, String>()

    /** Records every saved login, sealed. Returns what changed. */
    suspend fun recordChanges(): SyncRecorder.Result {
        val items = logins.list().map { login ->
            sealed[login.guid] = LoginsPayload.seal(
                host = login.origin,
                username = login.username,
                password = login.password,
            )
            SyncRecorder.SnapshotItem(login.guid, login.timeLastUsed)
        }
        return recorder.reconcile(SyncType.LOGINS, items)
    }

    /** The sealed payload for one login, or null when it was not part of the last snapshot. */
    fun payloadFor(guid: String): String? = sealed[guid]

    /**
     * Applies a login that arrived from the other phone. A payload this phone cannot open is
     * skipped rather than stored as garbage.
     */
    suspend fun apply(record: SyncClient.Record) {
        if (record.deleted) {
            logins.delete(record.id)
            return
        }
        if (logins.get(record.id) != null) {
            return
        }
        val json = LoginsPayload.open(record.payload) ?: return
        val host = json.optString("host")
        val username = json.optString("username")
        val password = json.optString("password")
        if (host.isBlank() || password.isBlank()) {
            return
        }
        logins.add(
            LoginEntry(
                origin = host,
                username = username,
                password = password,
            ),
        )
    }

    /** Kept for the caller that needs the plain entry list without touching storage twice. */
    suspend fun entries(): List<Login> = logins.list()
}
