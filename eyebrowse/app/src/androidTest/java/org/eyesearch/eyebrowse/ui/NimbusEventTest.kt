/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.ui

import android.content.Intent
import io.mockk.mockk
import mozilla.components.concept.sync.AuthType
import mozilla.components.service.fxa.EyebrowseAccount
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.eyesearch.eyebrowse.components.TelemetryAccountObserver
import org.eyesearch.eyebrowse.helpers.Experimentation
import org.eyesearch.eyebrowse.helpers.HomeActivityIntentTestRule
import org.eyesearch.eyebrowse.helpers.RetryTestRule
import org.eyesearch.eyebrowse.helpers.TestHelper.appContext
import org.eyesearch.eyebrowse.helpers.TestSetup

class NimbusEventTest : TestSetup() {
    @get:Rule
    val homeActivityTestRule = HomeActivityIntentTestRule.withDefaultSettingsOverrides()
        .withIntent(
            Intent().apply {
                action = Intent.ACTION_VIEW
            },
        )

    @Rule
    @JvmField
    val retryTestRule = RetryTestRule(3)

    @Test
    fun homeScreenNimbusEventsTest() {
        Experimentation.withHelper {
            assertTrue(evalJexl("'app_opened'|eventSum('Days', 28, 0) > 0"))
        }
    }

    @Test
    fun telemetryAccountObserverTest() {
        val observer = TelemetryAccountObserver(appContext)
        // replacing interface mock with implementation mock.
        observer.onAuthenticated(mockk<EyebrowseAccount>(), AuthType.Signin)

        Experimentation.withHelper {
            assertTrue(evalJexl("'sync_auth.sign_in'|eventSum('Days', 28, 0) > 0"))
        }
    }
}
