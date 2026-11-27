/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.home.recentvisits.view

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.LifecycleOwner
import mozilla.components.lib.state.ext.observeAsComposableState
import mozilla.components.service.glean.private.NoExtras
import org.eyesearch.eyebrowse.GleanMetrics.History
import org.eyesearch.eyebrowse.GleanMetrics.RecentlyVisitedHomepage
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.components.components
import org.eyesearch.eyebrowse.compose.ComposeViewHolder
import org.eyesearch.eyebrowse.home.recentvisits.RecentlyVisitedItem
import org.eyesearch.eyebrowse.home.recentvisits.RecentlyVisitedItem.RecentHistoryGroup
import org.eyesearch.eyebrowse.home.recentvisits.RecentlyVisitedItem.RecentHistoryHighlight
import org.eyesearch.eyebrowse.home.recentvisits.interactor.RecentVisitsInteractor
import org.eyesearch.eyebrowse.wallpapers.WallpaperState

/**
 * View holder for [RecentlyVisitedItem]s.
 *
 * @param composeView [ComposeView] which will be populated with Jetpack Compose UI content.
 * @param viewLifecycleOwner [LifecycleOwner] to which this Composable will be tied to.
 * @param interactor [RecentVisitsInteractor] which will have delegated to all user interactions.
 */
class RecentlyVisitedViewHolder(
    composeView: ComposeView,
    viewLifecycleOwner: LifecycleOwner,
    private val interactor: RecentVisitsInteractor,
) : ComposeViewHolder(composeView, viewLifecycleOwner) {

    @Composable
    override fun Content() {
        val recentVisits = components.appStore
            .observeAsComposableState { state -> state.recentHistory }
        val wallpaperState = components.appStore
            .observeAsComposableState { state -> state.wallpaperState }.value ?: WallpaperState.default

        RecentlyVisited(
            recentVisits = recentVisits.value ?: emptyList(),
            menuItems = listOfNotNull(
                RecentVisitMenuItem(
                    title = stringResource(R.string.recently_visited_menu_item_remove),
                    onClick = { visit ->
                        when (visit) {
                            is RecentHistoryGroup -> interactor.onRemoveRecentHistoryGroup(visit.title)
                            is RecentHistoryHighlight -> interactor.onRemoveRecentHistoryHighlight(
                                visit.url,
                            )
                        }
                    },
                ),
            ),
            backgroundColor = wallpaperState.wallpaperCardColor,
            onRecentVisitClick = { recentlyVisitedItem, pageNumber ->
                when (recentlyVisitedItem) {
                    is RecentHistoryHighlight -> {
                        RecentlyVisitedHomepage.historyHighlightOpened.record(NoExtras())
                        interactor.onRecentHistoryHighlightClicked(recentlyVisitedItem)
                    }
                    is RecentHistoryGroup -> {
                        RecentlyVisitedHomepage.searchGroupOpened.record(NoExtras())
                        History.recentSearchesTapped.record(
                            History.RecentSearchesTappedExtra(
                                pageNumber.toString(),
                            ),
                        )
                        interactor.onRecentHistoryGroupClicked(recentlyVisitedItem)
                    }
                }
            },
        )
    }

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }
}
