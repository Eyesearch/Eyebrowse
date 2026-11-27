/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.tabstray.browser

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import mozilla.components.lib.state.ext.observeAsComposableState
import mozilla.telemetry.glean.private.NoExtras
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.components.EyebrowseSnackbar
import org.eyesearch.eyebrowse.components.components
import org.eyesearch.eyebrowse.compose.ComposeViewHolder
import org.eyesearch.eyebrowse.tabstray.TabsTrayFragment
import org.eyesearch.eyebrowse.tabstray.TabsTrayState
import org.eyesearch.eyebrowse.tabstray.TabsTrayStore
import org.eyesearch.eyebrowse.tabstray.TrayPagerAdapter
import org.eyesearch.eyebrowse.tabstray.inactivetabs.InactiveTabsList
import org.eyesearch.eyebrowse.GleanMetrics.TabsTray as TabsTrayMetrics

/**
 * The [ComposeViewHolder] for displaying the section of inactive tabs in [TrayPagerAdapter].
 *
 * @param composeView [ComposeView] which will be populated with Jetpack Compose UI content.
 * @param lifecycleOwner [LifecycleOwner] to which this Composable will be tied to.
 * @param tabsTrayStore [TabsTrayStore] used to listen for changes to [TabsTrayState.inactiveTabs].
 * @param interactor [InactiveTabsInteractor] used to respond to interactions with the inactive tabs header
 * and the auto close dialog.
 */
class InactiveTabViewHolder(
    composeView: ComposeView,
    lifecycleOwner: LifecycleOwner,
    private val tabsTrayStore: TabsTrayStore,
    private val interactor: InactiveTabsInteractor,
) : ComposeViewHolder(composeView, lifecycleOwner) {

    @Composable
    override fun Content() {
        val expanded = components.appStore
            .observeAsComposableState { state -> state.inactiveTabsExpanded }.value ?: false
        val inactiveTabs = tabsTrayStore
            .observeAsComposableState { state -> state.inactiveTabs }.value ?: emptyList()
        val showInactiveTabsAutoCloseDialog =
            components.settings.shouldShowInactiveTabsAutoCloseDialog(inactiveTabs.size)
        var showAutoClosePrompt by remember { mutableStateOf(showInactiveTabsAutoCloseDialog) }

        if (showInactiveTabsAutoCloseDialog) {
            TabsTrayMetrics.autoCloseSeen.record(NoExtras())
        }

        if (inactiveTabs.isNotEmpty()) {
            InactiveTabsList(
                inactiveTabs = inactiveTabs,
                expanded = expanded,
                showAutoCloseDialog = showAutoClosePrompt,
                onHeaderClick = { interactor.onInactiveTabsHeaderClicked(!expanded) },
                onDeleteAllButtonClick = interactor::onDeleteAllInactiveTabsClicked,
                onAutoCloseDismissClick = {
                    interactor.onAutoCloseDialogCloseButtonClicked()
                    showAutoClosePrompt = !showAutoClosePrompt
                },
                onEnableAutoCloseClick = {
                    interactor.onEnableAutoCloseClicked()
                    showAutoClosePrompt = !showAutoClosePrompt
                    showConfirmationSnackbar()
                },
                onTabClick = interactor::onInactiveTabClicked,
                onTabCloseClick = interactor::onInactiveTabClosed,
            )
        }
    }

    override val allowPrivateTheme: Boolean
        get() = false

    private fun showConfirmationSnackbar() {
        val context = composeView.context
        val text = context.getString(R.string.inactive_tabs_auto_close_message_snackbar)
        val snackbar = EyebrowseSnackbar.make(
            view = composeView,
            duration = EyebrowseSnackbar.LENGTH_SHORT,
            isDisplayedWithBrowserToolbar = true,
        ).setText(text)
        snackbar.view.elevation = TabsTrayFragment.ELEVATION
        snackbar.show()
    }

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }
}
