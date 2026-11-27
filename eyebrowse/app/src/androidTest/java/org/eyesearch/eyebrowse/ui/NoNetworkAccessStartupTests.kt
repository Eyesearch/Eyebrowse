/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.ui

import androidx.core.net.toUri
import org.junit.Rule
import org.junit.Test
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.customannotations.SmokeTest
import org.eyesearch.eyebrowse.helpers.AppAndSystemHelper.setNetworkEnabled
import org.eyesearch.eyebrowse.helpers.HomeActivityTestRule
import org.eyesearch.eyebrowse.helpers.TestHelper.packageName
import org.eyesearch.eyebrowse.helpers.TestHelper.verifyUrl
import org.eyesearch.eyebrowse.helpers.TestSetup
import org.eyesearch.eyebrowse.ui.robots.browserScreen
import org.eyesearch.eyebrowse.ui.robots.homeScreen
import org.eyesearch.eyebrowse.ui.robots.navigationToolbar

/**
 * Tests to verify some main UI flows with Network connection off
 *
 */

class NoNetworkAccessStartupTests : TestSetup() {

    @get:Rule
    val activityTestRule = HomeActivityTestRule.withDefaultSettingsOverrides(launchActivity = false)

    // Test running on beta/release builds in CI:
    // caution when making changes to it, so they don't block the builds
    // Based on STR from https://github.com/mozilla-mobile/fenix/issues/16886
    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2240542
    @Test
    fun noNetworkConnectionStartupTest() {
        setNetworkEnabled(false)

        activityTestRule.launchActivity(null)

        homeScreen {
            verifyHomeScreen()
        }
    }

    // Based on STR from https://github.com/mozilla-mobile/fenix/issues/16886
    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2240722
    @Test
    fun networkInterruptedFromBrowserToHomeTest() {
        val url = "example.com"

        activityTestRule.launchActivity(null)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(url.toUri()) {}

        setNetworkEnabled(false)

        browserScreen {
        }.goToHomescreen {
            verifyHomeScreen()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2240723
    @Test
    fun testPageReloadAfterNetworkInterrupted() {
        val url = "example.com"

        activityTestRule.launchActivity(null)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(url.toUri()) {}

        setNetworkEnabled(false)

        browserScreen {
        }.openThreeDotMenu {
        }.refreshPage { }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2240721
    @SmokeTest
    @Test
    fun testSignInPageWithNoNetworkConnection() {
        setNetworkEnabled(false)

        activityTestRule.launchActivity(null)

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openTurnOnSyncMenu {
            tapOnUseEmailToSignIn()
            verifyUrl(
                "eyebrowse.com",
                "$packageName:id/mozac_browser_toolbar_url_view",
                R.id.mozac_browser_toolbar_url_view,
            )
        }
    }
}
