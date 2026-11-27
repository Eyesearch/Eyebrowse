/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.tabstray.viewholders

import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.lib.state.ext.observeAsComposableState
import org.eyesearch.eyebrowse.tabstray.SyncedTabsInteractor
import org.eyesearch.eyebrowse.tabstray.TabsTrayState
import org.eyesearch.eyebrowse.tabstray.TabsTrayStore
import org.eyesearch.eyebrowse.tabstray.syncedtabs.SyncedTabsList
import org.eyesearch.eyebrowse.theme.EyebrowseTheme
import org.eyesearch.eyebrowse.theme.Theme

/**
 * Temporary ViewHolder to render [SyncedTabsList] until all of the Tabs Tray is written in Compose.
 *
 * @param composeView Root ComposeView passed-in from TrayPagerAdapter.
 * @param tabsTrayStore Store used as a Composable State to listen for changes to [TabsTrayState.syncedTabs].
 * @param interactor [SyncedTabsInteractor] used to respond to interactions with synced tabs.
 */
class SyncedTabsPageViewHolder(
    private val composeView: ComposeView,
    private val tabsTrayStore: TabsTrayStore,
    private val interactor: SyncedTabsInteractor,
) : AbstractPageViewHolder(composeView) {

    fun bind() {
        composeView.setContent {
            val tabs = tabsTrayStore.observeAsComposableState { state -> state.syncedTabs }.value
            EyebrowseTheme(theme = Theme.getTheme(allowPrivateTheme = false)) {
                SyncedTabsList(
                    syncedTabs = tabs ?: emptyList(),
                    onTabClick = interactor::onSyncedTabClicked,
                )
            }
        }
    }

    override fun bind(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>) = Unit // no-op

    override fun detachedFromWindow() = Unit // no-op

    override fun attachedToWindow() = Unit // no-op

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }
}
