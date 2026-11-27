/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.sync

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * The local record of what changed, so the app can later ask "what is new since this cursor"
 * without a rewrite of every feature.
 *
 * It holds no browsing data of its own: a row is a type, an id, a time and a tombstone. The data
 * stays where it already lives, in the browser's own storage. That is deliberate, because a second
 * copy of someone's history is a liability, not a feature.
 */
class SyncChangeLog(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COLUMN_TYPE TEXT NOT NULL,
                $COLUMN_ITEM_ID TEXT NOT NULL,
                $COLUMN_UPDATED_AT INTEGER NOT NULL,
                $COLUMN_DELETED INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY ($COLUMN_TYPE, $COLUMN_ITEM_ID)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX ${TABLE}_updated ON $TABLE ($COLUMN_UPDATED_AT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Nothing has shipped yet; when a migration is needed it gets written here rather than
        // dropping the table, because dropping it would silently stop a sync.
    }

    /**
     * Records that one item changed. Calling it twice for the same item keeps one row and moves
     * the time forward, which is what a sync cursor needs.
     */
    fun record(type: SyncType, itemId: String, updatedAt: Long = System.currentTimeMillis(), deleted: Boolean = false) {
        val values = ContentValues().apply {
            put(COLUMN_TYPE, type.key)
            put(COLUMN_ITEM_ID, itemId)
            put(COLUMN_UPDATED_AT, updatedAt)
            put(COLUMN_DELETED, if (deleted) 1 else 0)
        }
        writableDatabase.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /** Everything of one type that changed after [since], oldest first, ready to be uploaded. */
    fun changesSince(type: SyncType, since: Long): List<SyncChange> {
        val rows = readableDatabase.query(
            TABLE,
            arrayOf(COLUMN_ITEM_ID, COLUMN_UPDATED_AT, COLUMN_DELETED),
            "$COLUMN_TYPE = ? AND $COLUMN_UPDATED_AT > ?",
            arrayOf(type.key, since.toString()),
            null,
            null,
            "$COLUMN_UPDATED_AT ASC",
        )
        return rows.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        SyncChange(
                            type = type,
                            itemId = cursor.getString(0),
                            updatedAt = cursor.getLong(1),
                            deleted = cursor.getInt(2) == 1,
                        ),
                    )
                }
            }
        }
    }

    /** The newest time in the log, which is the cursor to hand back after a successful upload. */
    fun newestCursor(): Long {
        return readableDatabase.rawQuery("SELECT MAX($COLUMN_UPDATED_AT) FROM $TABLE", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
    }

    /**
     * Forgets rows the server already has. Kept separate from [record] so a mistake in the sync
     * code can never delete something that has not been sent yet.
     */
    fun forget(type: SyncType, upTo: Long) {
        writableDatabase.delete(
            TABLE,
            "$COLUMN_TYPE = ? AND $COLUMN_UPDATED_AT <= ?",
            arrayOf(type.key, upTo.toString()),
        )
    }

    companion object {
        private const val DATABASE_NAME = "eyebrowse_sync_log.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE = "changes"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_ITEM_ID = "item_id"
        private const val COLUMN_UPDATED_AT = "updated_at"
        private const val COLUMN_DELETED = "deleted"
    }
}

/** The six things that sync, each switched on separately by the user. */
enum class SyncType(val key: String) {
    BOOKMARKS("bookmarks"),
    TABS("tabs"),
    HISTORY("history"),
    LOGINS("logins"),
    SETTINGS("settings"),
    PINNED("pinned"),
}

/** One changed item, as the server will see it. */
data class SyncChange(
    val type: SyncType,
    val itemId: String,
    val updatedAt: Long,
    val deleted: Boolean,
)
