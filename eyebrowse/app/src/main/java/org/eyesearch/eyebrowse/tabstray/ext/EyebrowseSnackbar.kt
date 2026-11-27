/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.tabstray.ext

import android.view.View
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.components.EyebrowseSnackbar
import org.eyesearch.eyebrowse.tabstray.TabsTrayFragment.Companion.ELEVATION

internal fun EyebrowseSnackbar.collectionMessage(
    tabSize: Int,
    isNewCollection: Boolean = false,
): EyebrowseSnackbar {
    val stringRes = when {
        isNewCollection -> {
            R.string.create_collection_tabs_saved_new_collection
        }
        tabSize > 1 -> {
            R.string.create_collection_tabs_saved
        }
        else -> {
            R.string.create_collection_tab_saved
        }
    }
    setText(context.getString(stringRes))
    return this
}

internal fun EyebrowseSnackbar.bookmarkMessage(
    tabSize: Int,
): EyebrowseSnackbar {
    val stringRes = when {
        tabSize > 1 -> {
            R.string.snackbar_message_bookmarks_saved
        }
        else -> {
            R.string.bookmark_saved_snackbar
        }
    }
    setText(context.getString(stringRes))
    return this
}

internal inline fun EyebrowseSnackbar.anchorWithAction(
    anchor: View?,
    crossinline action: () -> Unit,
): EyebrowseSnackbar {
    anchorView = anchor
    view.elevation = ELEVATION

    setAction(context.getString(R.string.create_collection_view)) {
        action.invoke()
    }

    return this
}

internal fun EyebrowseSnackbar.Companion.make(view: View) = make(
    duration = LENGTH_LONG,
    isDisplayedWithBrowserToolbar = true,
    view = view,
)
