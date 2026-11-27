/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.library.history.state

import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import mozilla.components.service.glean.private.NoExtras
import org.eyesearch.eyebrowse.GleanMetrics.Events
import org.eyesearch.eyebrowse.library.history.History
import org.eyesearch.eyebrowse.library.history.HistoryFragmentAction
import org.eyesearch.eyebrowse.library.history.HistoryFragmentState
import org.eyesearch.eyebrowse.library.history.RemoveTimeFrame
import org.eyesearch.eyebrowse.GleanMetrics.History as GleanHistory

/**
 * A [Middleware] for recording telemetry based on [HistoryFragmentAction]s that are dispatched to
 * the [HistoryFragmentStore].
 *
 * @param isInPrivateMode Whether the app is currently in private browsing mode.
 */
class HistoryTelemetryMiddleware(
    private val isInPrivateMode: Boolean,
) : Middleware<HistoryFragmentState, HistoryFragmentAction> {
    override fun invoke(
        context: MiddlewareContext<HistoryFragmentState, HistoryFragmentAction>,
        next: (HistoryFragmentAction) -> Unit,
        action: HistoryFragmentAction,
    ) {
        val currentState = context.state
        next(action)
        when (action) {
            is HistoryFragmentAction.EnterRecentlyClosed -> {
                Events.recentlyClosedTabsOpened.record(NoExtras())
            }
            is HistoryFragmentAction.HistoryItemClicked -> {
                if (currentState.mode.selectedItems.isEmpty()) {
                    when (val item = action.item) {
                        is History.Regular -> {
                            GleanHistory.openedItem.record(
                                GleanHistory.OpenedItemExtra(
                                    isRemote = item.isRemote,
                                    timeGroup = item.historyTimeGroup.toString(),
                                    isPrivate = isInPrivateMode,
                                ),
                            )
                        }
                        is History.Group -> GleanHistory.searchTermGroupTapped.record(NoExtras())
                        else -> Unit
                    }
                }
            }
            is HistoryFragmentAction.DeleteItems -> {
                for (item in action.items) {
                    GleanHistory.removed.record(NoExtras())
                }
            }
            is HistoryFragmentAction.DeleteTimeRange -> when (action.timeFrame) {
                RemoveTimeFrame.LastHour -> GleanHistory.removedLastHour.record(NoExtras())
                RemoveTimeFrame.TodayAndYesterday -> GleanHistory.removedTodayAndYesterday.record(NoExtras())
                null -> GleanHistory.removedAll.record(NoExtras())
            }
            else -> Unit
        }
    }
}
