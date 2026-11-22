/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.sync

import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.concept.storage.BookmarkRoot
import mozilla.components.concept.storage.BookmarksStorage
import org.json.JSONObject

/**
 * Bookmarks, as the first type we sync, because a bookmark is easy to check with the eyes.
 *
 * The browser already keeps bookmarks in its own storage, and that stays the only copy on the
 * device. This class only says what changed: an item's id is its guid, its timestamp is the
 * node's last modified time, and its payload is the small JSON the server will hand back to the
 * other phone.
 */
class BookmarkSync(
    private val bookmarks: BookmarksStorage,
    private val recorder: SyncRecorder,
) {

    /** Walks the bookmark tree and records anything new, changed or gone. */
    suspend fun recordChanges(): SyncRecorder.Result {
        val root = bookmarks.getTree(BookmarkRoot.Root.id, recursive = true)
            ?: return SyncRecorder.Result(0, 0)

        val items = mutableListOf<SyncRecorder.SnapshotItem>()
        collect(root, items)
        return recorder.reconcile(SyncType.BOOKMARKS, items)
    }

    /** The payload for one bookmark, or null when it is not a bookmark we can send. */
    fun payloadFor(guid: String): String? = payloads[guid]

    /**
     * Applies a bookmark that arrived from the other phone. A tombstone removes it here, a live
     * record adds it if it is missing.
     */
    suspend fun apply(record: SyncClient.Record) {
        if (record.deleted) {
            bookmarks.deleteNode(record.id)
            return
        }
        val existing = bookmarks.getBookmark(record.id)
        if (existing != null) {
            return
        }
        val payload = runCatching { JSONObject(record.payload) }.getOrNull() ?: return
        val url = payload.optString("url")
        val title = payload.optString("title")
        val parent = payload.optString("parent").ifBlank { BookmarkRoot.Root.id }
        if (url.isBlank()) {
            return
        }
        bookmarks.addItem(parentGuid = parent, url = url, title = title, position = null)
    }

    private fun collect(node: BookmarkNode, into: MutableList<SyncRecorder.SnapshotItem>) {
        node.children?.forEach { child ->
            if (child.type == BookmarkNodeType.ITEM && child.url != null) {
                into += SyncRecorder.SnapshotItem(child.guid, child.dateAdded)
                payloads[child.guid] = JSONObject()
                    .put("url", child.url)
                    .put("title", child.title ?: "")
                    .put("parent", child.parentGuid ?: "")
                    .toString()
            } else if (child.type == BookmarkNodeType.FOLDER) {
                payloads[child.guid] = JSONObject()
                    .put("folder", true)
                    .put("title", child.title ?: "")
                    .put("parent", child.parentGuid ?: "")
                    .toString()
                into += SyncRecorder.SnapshotItem(child.guid, child.dateAdded)
            }
            collect(child, into)
        }
    }

    /** Built while walking the tree, so the uploader can ask for a payload by id. */
    private val payloads = mutableMapOf<String, String>()
}
