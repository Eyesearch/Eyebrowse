/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.debugsettings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.rememberNavController
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.ext.observeAsState
import org.eyesearch.eyebrowse.compose.annotation.LightDarkPreview
import org.eyesearch.eyebrowse.debugsettings.navigation.DebugDrawerRoute
import org.eyesearch.eyebrowse.debugsettings.store.DebugDrawerAction
import org.eyesearch.eyebrowse.debugsettings.store.DebugDrawerNavigationMiddleware
import org.eyesearch.eyebrowse.debugsettings.store.DebugDrawerStore
import org.eyesearch.eyebrowse.debugsettings.store.DrawerStatus
import org.eyesearch.eyebrowse.debugsettings.tabs.TabTools
import org.eyesearch.eyebrowse.theme.EyebrowseTheme
import org.eyesearch.eyebrowse.theme.Theme

/**
 * Overlay for presenting Fenix-wide debugging content.
 *
 * @param browserStore [BrowserStore] used to access tab data for [TabTools].
 * @param inactiveTabsEnabled Whether the inactive tabs feature is enabled.
 */
@Composable
fun EyebrowseOverlay(
    browserStore: BrowserStore,
    inactiveTabsEnabled: Boolean,
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val debugDrawerStore = remember {
        DebugDrawerStore(
            middlewares = listOf(
                DebugDrawerNavigationMiddleware(
                    navController = navController,
                    scope = coroutineScope,
                ),
            ),
        )
    }
    val debugDrawerDestinations = remember {
        DebugDrawerRoute.generateDebugDrawerDestinations(
            debugDrawerStore = debugDrawerStore,
            browserStore = browserStore,
            inactiveTabsEnabled = inactiveTabsEnabled,
        )
    }
    val drawerStatus by debugDrawerStore.observeAsState(initialValue = DrawerStatus.Closed) { state ->
        state.drawerStatus
    }

    EyebrowseTheme(theme = Theme.getTheme(allowPrivateTheme = false)) {
        DebugOverlay(
            navController = navController,
            drawerStatus = drawerStatus,
            debugDrawerDestinations = debugDrawerDestinations,
            onDrawerOpen = {
                debugDrawerStore.dispatch(DebugDrawerAction.DrawerOpened)
            },
            onDrawerClose = {
                debugDrawerStore.dispatch(DebugDrawerAction.DrawerClosed)
            },
            onDrawerBackButtonClick = {
                debugDrawerStore.dispatch(DebugDrawerAction.OnBackPressed)
            },
        )
    }
}

@LightDarkPreview
@Composable
private fun FenixOverlayPreview() {
    EyebrowseOverlay(
        browserStore = BrowserStore(),
        inactiveTabsEnabled = true,
    )
}
