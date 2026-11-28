/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.core

import android.view.View
import android.widget.TextView
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.menu.BrowserMenuItem
import mozilla.components.concept.engine.content.blocking.TrackerLog
import org.eyesearch.eyebrowse.R

/**
 * A browser menu row that displays Gecko tracker log entries with blocking events.
 *
 * The count comes from the engine and takes a moment, so the row shows a neutral label first and
 * fills in the number when it arrives.
 *
 * @param loadingLabel shown until the engine answers.
 * @param countLabel formats the number of tracker log entries with blocks.
 * @param unavailableLabel is shown when tracking logs could not be read.
 * @param loadCount reports the number or null if unavailable.
 * @param onTapped invoked when the row is tapped.
 * @param visibleProvider decides whether the row is shown at all.
 */
class EyeblockMenuRow(
    private val loadingLabel: String,
    private val countLabel: (Int) -> String,
    private val unavailableLabel: String,
    private val loadCount: (onResult: (Int?) -> Unit) -> Unit,
    private val onTapped: () -> Unit = {},
    visibleProvider: () -> Boolean = { true },
) : BrowserMenuItem {

    override val visible: () -> Boolean = visibleProvider

    override fun getLayoutResource(): Int = R.layout.eyeblock_menu_row

    override fun bind(menu: BrowserMenu, view: View) {
        val title = view.findViewById<TextView>(R.id.eyeblock_row_title)
        title.text = loadingLabel

        loadCount { count ->
            // The engine answers from its own thread; the text can only change on the main one.
            view.post { title.text = count?.let(countLabel) ?: unavailableLabel }
        }

        view.setOnClickListener {
            menu.dismiss()
            onTapped()
        }
    }
}

/** Count log entries with a blocking event, not loaded-only trackers or uBlock requests. */
internal fun countTrackingBlocks(logs: List<TrackerLog>): Int = logs.count {
    it.blockedCategories.isNotEmpty() || it.cookiesHasBeenBlocked
}
