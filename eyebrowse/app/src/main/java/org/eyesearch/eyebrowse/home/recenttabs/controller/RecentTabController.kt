/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.home.recenttabs.controller

import androidx.navigation.NavController
import mozilla.components.feature.tabs.TabsUseCases.SelectTabUseCase
import mozilla.components.service.glean.private.NoExtras
import org.eyesearch.eyebrowse.GleanMetrics.RecentTabs
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.components.AppStore
import org.eyesearch.eyebrowse.components.appstate.AppAction
import org.eyesearch.eyebrowse.home.HomeFragment
import org.eyesearch.eyebrowse.home.HomeFragmentDirections
import org.eyesearch.eyebrowse.home.recenttabs.RecentTab
import org.eyesearch.eyebrowse.home.recenttabs.interactor.RecentTabInteractor

/**
 * An interface that handles the view manipulation of the recent tabs in the Home screen.
 */
interface RecentTabController {

    /**
     * @see [RecentTabInteractor.onRecentTabClicked]
     */
    fun handleRecentTabClicked(tabId: String)

    /**
     * @see [RecentTabInteractor.onRecentTabShowAllClicked]
     */
    fun handleRecentTabShowAllClicked()

    /**
     * @see [RecentTabInteractor.onRemoveRecentTab]
     */
    fun handleRecentTabRemoved(tab: RecentTab.Tab)
}

/**
 * The default implementation of [RecentTabController].
 *
 * @param selectTabUseCase [SelectTabUseCase] used selecting a tab.
 * @param navController [NavController] used for navigation.
 * @param appStore The [AppStore] that holds the state of the [HomeFragment].
 */
class DefaultRecentTabsController(
    private val selectTabUseCase: SelectTabUseCase,
    private val navController: NavController,
    private val appStore: AppStore,
) : RecentTabController {

    override fun handleRecentTabClicked(tabId: String) {
        RecentTabs.recentTabOpened.record(NoExtras())

        selectTabUseCase.invoke(tabId)
        navController.navigate(R.id.browserFragment)
    }

    override fun handleRecentTabShowAllClicked() {
        RecentTabs.showAllClicked.record(NoExtras())
        navController.navigate(HomeFragmentDirections.actionGlobalTabsTrayFragment())
    }

    override fun handleRecentTabRemoved(tab: RecentTab.Tab) {
        appStore.dispatch(AppAction.RemoveRecentTab(tab))
    }
}
