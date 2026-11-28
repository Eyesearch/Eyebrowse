/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.core

import mozilla.components.concept.engine.EngineSession.TrackingProtectionPolicy.TrackingCategory
import mozilla.components.concept.engine.content.blocking.TrackerLog
import org.junit.Assert.assertEquals
import org.junit.Test

class EyeBlockTrackingCountTest {
    @Test
    fun `counts only log entries that report blocking`() {
        val logs = listOf(
            TrackerLog("https://loaded.example", loadedCategories = listOf(TrackingCategory.AD)),
            TrackerLog("https://blocked.example", blockedCategories = listOf(TrackingCategory.AD)),
            TrackerLog("https://cookie.example", cookiesHasBeenBlocked = true),
            TrackerLog("https://both.example", blockedCategories = listOf(TrackingCategory.AD), cookiesHasBeenBlocked = true),
        )
        assertEquals(3, countTrackingBlocks(logs))
        assertEquals(0, countTrackingBlocks(emptyList()))
    }
}
