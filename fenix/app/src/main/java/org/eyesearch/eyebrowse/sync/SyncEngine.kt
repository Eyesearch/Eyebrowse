/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.sync

import android.util.Log

/**
 * One run of sync for one type: record what changed, send it, then bring back what the other phone
 * sent. The order matters. Recording first means a change made while we were offline is not lost;
 * pulling last means what we just uploaded does not come straight back at us.
 */
class SyncEngine(
    private val client: SyncClient,
    private val changeLog: SyncChangeLog,
    private val bookmarks: BookmarkSync,
    private val logins: LoginsSync? = null,
) {

    data class Report(
        val recorded: Int = 0,
        val tombstones: Int = 0,
        val uploaded: Int = 0,
        val downloaded: Int = 0,
        val applied: Int = 0,
        val stoppedBy: String? = null,
    )

    /** Runs bookmarks end to end and returns what happened, so it can be shown rather than guessed. */
    suspend fun runBookmarks(): Report {
        val changes = bookmarks.recordChanges()
        var report = Report(recorded = changes.added, tombstones = changes.removed)

        val uploader = SyncUploader(changeLog, client) { type, id ->
            if (type == SyncType.BOOKMARKS) bookmarks.payloadFor(id) else null
        }
        val pushed = uploader.upload(SyncType.BOOKMARKS)
        report = report.copy(uploaded = pushed.uploaded, stoppedBy = pushed.stoppedBy)
        if (pushed.stoppedBy != null) {
            Log.w(TAG, "upload stopped: ${pushed.stoppedBy}")
            return report
        }

        var cursor: String? = null
        var applied = 0
        var downloaded = 0
        while (true) {
            when (val outcome = client.pull(SyncType.BOOKMARKS, cursor)) {
                is SyncClient.Outcome.Ok -> {
                    downloaded += outcome.value.records.size
                    outcome.value.records.forEach { record ->
                        runCatching { bookmarks.apply(record) }
                            .onSuccess { applied += 1 }
                            .onFailure { Log.w(TAG, "could not apply ${record.id}: ${it.message}") }
                    }
                    cursor = outcome.value.cursor
                    if (!outcome.value.hasMore) {
                        return report.copy(downloaded = downloaded, applied = applied)
                    }
                }
                is SyncClient.Outcome.SessionOver ->
                    return report.copy(downloaded = downloaded, applied = applied, stoppedBy = "session")
                is SyncClient.Outcome.TypeDisabled ->
                    return report.copy(downloaded = downloaded, applied = applied, stoppedBy = "disabled")
                is SyncClient.Outcome.RateLimited ->
                    return report.copy(downloaded = downloaded, applied = applied, stoppedBy = "rate-limited")
                is SyncClient.Outcome.TooLarge ->
                    return report.copy(downloaded = downloaded, applied = applied, stoppedBy = "too-large")
                is SyncClient.Outcome.Failed ->
                    return report.copy(downloaded = downloaded, applied = applied, stoppedBy = "failed")
            }
        }
    }

    /** The same three steps for saved logins. */
    suspend fun runLogins(): Report {
        val sync = logins ?: return Report(stoppedBy = "not-available")
        val changes = sync.recordChanges()
        var report = Report(recorded = changes.added, tombstones = changes.removed)

        val uploader = SyncUploader(changeLog, client) { type, id ->
            if (type == SyncType.LOGINS) sync.payloadFor(id) else null
        }
        val pushed = uploader.upload(SyncType.LOGINS)
        report = report.copy(uploaded = pushed.uploaded, stoppedBy = pushed.stoppedBy)
        if (pushed.stoppedBy != null) return report

        var cursor: String? = null
        var applied = 0
        var downloaded = 0
        while (true) {
            when (val outcome = client.pull(SyncType.LOGINS, cursor)) {
                is SyncClient.Outcome.Ok -> {
                    downloaded += outcome.value.records.size
                    outcome.value.records.forEach { record ->
                        runCatching { sync.apply(record) }.onSuccess { applied += 1 }
                    }
                    cursor = outcome.value.cursor
                    if (!outcome.value.hasMore) {
                        return report.copy(downloaded = downloaded, applied = applied)
                    }
                }
                else -> return report.copy(downloaded = downloaded, applied = applied, stoppedBy = "stopped")
            }
        }
    }

    companion object {
        private const val TAG = "SyncEngine"
    }
}
