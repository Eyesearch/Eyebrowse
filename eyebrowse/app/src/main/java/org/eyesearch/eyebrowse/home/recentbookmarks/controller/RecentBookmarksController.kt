/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.home.recentbookmarks.controller

import androidx.navigation.NavController
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineSession.LoadUrlFlags.Companion.ALLOW_JAVASCRIPT_URL
import mozilla.components.feature.tabs.TabsUseCases
import org.eyesearch.eyebrowse.BrowserDirection
import org.eyesearch.eyebrowse.GleanMetrics.RecentBookmarks
import org.eyesearch.eyebrowse.HomeActivity
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.components.AppStore
import org.eyesearch.eyebrowse.components.appstate.AppAction
import org.eyesearch.eyebrowse.home.HomeFragmentDirections
import org.eyesearch.eyebrowse.home.recentbookmarks.RecentBookmark
import org.eyesearch.eyebrowse.home.recentbookmarks.interactor.RecentBookmarksInteractor

/**
 * An interface that handles the view manipulation of the recently saved bookmarks on the
 * Home screen.
 */
interface RecentBookmarksController {

    /**
     * @see [RecentBookmarksInteractor.onRecentBookmarkClicked]
     */
    fun handleBookmarkClicked(bookmark: RecentBookmark)

    /**
     * @see [RecentBookmarksInteractor.onShowAllBookmarksClicked]
     */
    fun handleShowAllBookmarksClicked()

    /**
     * @see [RecentBookmarksInteractor.onRecentBookmarkRemoved]
     */
    fun handleBookmarkRemoved(bookmark: RecentBookmark)
}

/**
 * The default implementation of [RecentBookmarksController].
 */
class DefaultRecentBookmarksController(
    private val activity: HomeActivity,
    private val navController: NavController,
    private val appStore: AppStore,
    private val browserStore: BrowserStore,
    private val selectTabUseCase: TabsUseCases.SelectTabUseCase,
) : RecentBookmarksController {

    override fun handleBookmarkClicked(bookmark: RecentBookmark) {
        val existingTabForBookmark = browserStore.state.tabs.firstOrNull {
            it.content.url == bookmark.url
        }

        if (existingTabForBookmark == null) {
            activity.openToBrowserAndLoad(
                searchTermOrURL = bookmark.url!!,
                newTab = true,
                from = BrowserDirection.FromHome,
                flags = EngineSession.LoadUrlFlags.select(ALLOW_JAVASCRIPT_URL),
            )
        } else {
            selectTabUseCase.invoke(existingTabForBookmark.id)
            navController.navigate(R.id.browserFragment)
        }

        RecentBookmarks.bookmarkClicked.add()
    }

    override fun handleShowAllBookmarksClicked() {
        RecentBookmarks.showAllBookmarks.add()
        navController.navigate(
            HomeFragmentDirections.actionGlobalBookmarkFragment(BookmarkRoot.Mobile.id),
        )
    }

    override fun handleBookmarkRemoved(bookmark: RecentBookmark) {
        appStore.dispatch(AppAction.RemoveRecentBookmark(bookmark))
    }
}
