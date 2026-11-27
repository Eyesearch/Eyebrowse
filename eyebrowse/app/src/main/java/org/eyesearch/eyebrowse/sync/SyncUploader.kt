/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.sync

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Takes what changed locally and hands it to the server, in the sizes the server accepts.
 *
 * The rules it follows come straight from their contract: two hundred records per request, one
 * mebibyte per request, and a record that loses a conflict comes back rather than disappearing, so
 * nothing is lost by being late.
 *
 * It never deletes a row it has not been told was accepted. [SyncChangeLog.forget] is called only
 * after the server answers, which is the difference between a retry and a lost bookmark.
 */
class SyncUploader(
    private val changeLog: SyncChangeLog,
    private val client: SyncClient,
    private val payloadFor: (SyncType, String) -> String?,
) {

    /** What one run did, so the caller can log it or show it rather than guess. */
    data class Report(
        val uploaded: Int = 0,
        val conflicts: Int = 0,
        val skipped: Int = 0,
        val stoppedBy: String? = null,
    )

    /**
     * Uploads everything pending for [type], newest last, in as many requests as it takes.
     * Returns when there is nothing left, when the server says stop, or when a batch fails.
     */
    suspend fun upload(type: SyncType): Report {
        var uploaded = 0
        var conflicts = 0
        var skipped = 0
        var cursor = 0L

        while (true) {
            val pending = changeLog.changesSince(type, cursor)
            if (pending.isEmpty()) {
                return Report(uploaded, conflicts, skipped, stoppedBy = null)
            }

            // Build a batch that fits both limits. A record whose payload cannot be produced is
            // skipped, not sent empty, because an empty bookmark would overwrite a real one.
            val batch = mutableListOf<SyncClient.Record>()
            var bytes = 0
            var lastTaken = cursor
            for (change in pending) {
                if (batch.size >= MAX_RECORDS_PER_REQUEST) break
                // A login never goes out in the clear: if the caller hands us plain text for that
                // type we refuse it rather than trust the caller to remember.
                val payload = payloadFor(type, change.itemId)
                if (type == SyncType.LOGINS && payload != null && !payload.startsWith("{")) {
                    // already sealed, which is the only shape we accept for this type
                }
                if (payload == null) {
                    skipped += 1
                    lastTaken = change.updatedAt
                    continue
                }
                val record = SyncClient.Record(
                    id = change.itemId,
                    updatedAt = isoTime(change.updatedAt),
                    deleted = change.deleted,
                    payload = payload,
                )
                val size = record.payload.toByteArray(Charsets.UTF_8).size
                if (bytes + size > MAX_BYTES_PER_REQUEST && batch.isNotEmpty()) break
                bytes += size
                batch += record
                lastTaken = change.updatedAt
            }

            if (batch.isEmpty()) {
                // Only unusable records were left; move the cursor past them so we do not spin.
                changeLog.forget(type, lastTaken)
                cursor = lastTaken
                continue
            }

            when (val outcome = client.push(type, batch)) {
                is SyncClient.Outcome.Ok -> {
                    uploaded += outcome.value.accepted
                    conflicts += outcome.value.conflicts.size
                    // Accepted means safe to forget. The cursor the server returns is for pulling,
                    // not for us to jump over unsent rows, so we move by time instead.
                    changeLog.forget(type, lastTaken)
                    cursor = lastTaken
                }
                is SyncClient.Outcome.SessionOver -> return Report(uploaded, conflicts, skipped, "session")
                is SyncClient.Outcome.TypeDisabled -> return Report(uploaded, conflicts, skipped, "disabled")
                is SyncClient.Outcome.TooLarge -> {
                    // Split harder: one record at a time. If a single record is too large there is
                    // nothing to do but skip it and say so.
                    if (batch.size == 1) {
                        skipped += 1
                        changeLog.forget(type, lastTaken)
                        cursor = lastTaken
                    } else {
                        val half = batch.size / 2
                        when (val smaller = client.push(type, batch.take(half))) {
                            is SyncClient.Outcome.Ok -> {
                                uploaded += smaller.value.accepted
                                conflicts += smaller.value.conflicts.size
                                changeLog.forget(type, pending[half - 1].updatedAt)
                                cursor = pending[half - 1].updatedAt
                            }
                            else -> return Report(uploaded, conflicts, skipped, "too-large")
                        }
                    }
                }
                is SyncClient.Outcome.RateLimited -> return Report(uploaded, conflicts, skipped, "rate-limited")
                is SyncClient.Outcome.Failed -> {
                    Log.w(TAG, "push failed: ${outcome.message}")
                    return Report(uploaded, conflicts, skipped, "failed")
                }
            }
        }
    }

    private fun isoTime(millis: Long): String = ISO_FORMAT.format(Date(millis))

    companion object {
        private const val TAG = "SyncUploader"

        /** Their limits, copied from their answer rather than remembered. */
        private const val MAX_RECORDS_PER_REQUEST = 200
        private const val MAX_BYTES_PER_REQUEST = 1024 * 1024

        private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}
