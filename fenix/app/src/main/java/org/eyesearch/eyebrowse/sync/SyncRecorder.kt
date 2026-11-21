/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.sync

/**
 * Turns "here is what exists now" into "here is what changed", for any type.
 *
 * Every feature in the browser already keeps its own copy of its data, and every one of them can
 * say when each item was last touched. So the recorder does not watch the features, it compares
 * two snapshots: what the log remembers, and what the feature reports now. An item that is new or
 * newer is recorded; an item that has disappeared is recorded as a tombstone, so the delete travels
 * to the other phone instead of the item quietly coming back on the next pull.
 *
 * That keeps the sync code out of every feature's hot path, which is where a bug would cost someone
 * their bookmarks.
 */
class SyncRecorder(private val changeLog: SyncChangeLog) {

    /** One item as the feature reports it: an id, when it was last touched, and nothing else. */
    data class SnapshotItem(val id: String, val updatedAt: Long)

    data class Result(val added: Int, val removed: Int)

    /**
     * Records the difference between what the log last saw for [type] and [current].
     *
     * The log holds at most one row per item, so a second run with an unchanged snapshot records
     * nothing at all, which is what makes it safe to call this as often as we like.
     */
    fun reconcile(type: SyncType, current: List<SnapshotItem>, since: Long = 0L): Result {
        val known = changeLog.changesSince(type, since).associateBy { it.itemId }
        val currentIds = current.mapTo(HashSet()) { it.id }

        var added = 0
        current.forEach { item ->
            val previous = known[item.id]
            val isNewOrNewer = previous == null || item.updatedAt > previous.updatedAt || previous.deleted
            if (isNewOrNewer) {
                changeLog.record(type, item.id, item.updatedAt, deleted = false)
                added += 1
            }
        }

        var removed = 0
        known.values.forEach { row ->
            if (!row.deleted && row.itemId !in currentIds) {
                // A tombstone carries the time we noticed, not the time it was deleted, because
                // nobody recorded that. It only has to be newer than the last thing we sent.
                changeLog.record(type, row.itemId, System.currentTimeMillis(), deleted = true)
                removed += 1
            }
        }

        return Result(added, removed)
    }
}
