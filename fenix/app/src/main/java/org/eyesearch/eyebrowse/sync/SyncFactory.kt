/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.sync

import android.content.Context
import mozilla.components.concept.storage.BookmarksStorage
import org.eyesearch.eyebrowse.account.IndoAccountStorage

/**
 * Builds the sync pieces in one place, so no screen has to know how they fit together.
 *
 * The token is read from the account storage at the moment of the call and passed to the client as
 * a function, so a sync that outlives a sign out simply stops instead of carrying a dead token.
 */
object SyncFactory {

    fun engine(context: Context, bookmarksStorage: BookmarksStorage): SyncEngine {
        val account = IndoAccountStorage(context)
        val client = SyncClient(tokenProvider = { account.token() })
        val changeLog = SyncChangeLog(context)
        val recorder = SyncRecorder(changeLog)
        val bookmarks = BookmarkSync(bookmarksStorage, recorder)
        return SyncEngine(client, changeLog, bookmarks)
    }

    fun client(context: Context): SyncClient {
        val account = IndoAccountStorage(context)
        return SyncClient(tokenProvider = { account.token() })
    }
}
