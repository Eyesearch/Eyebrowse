/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.ui

import androidx.core.net.toUri
import org.junit.Rule
import org.junit.Test
import org.eyesearch.eyebrowse.customannotations.SmokeTest
import org.eyesearch.eyebrowse.ext.settings
import org.eyesearch.eyebrowse.helpers.AppAndSystemHelper.runWithCondition
import org.eyesearch.eyebrowse.helpers.HomeActivityIntentTestRule
import org.eyesearch.eyebrowse.helpers.TestHelper.appContext
import org.eyesearch.eyebrowse.helpers.TestSetup
import org.eyesearch.eyebrowse.ui.robots.homeScreen
import org.eyesearch.eyebrowse.ui.robots.navigationToolbar

/**
 *  Tests for verifying the new Cookie banner blocker option and functionality.
 */
class CookieBannerBlockerTest : TestSetup() {
    @get:Rule
    val activityTestRule = HomeActivityIntentTestRule.withDefaultSettingsOverrides(skipOnboarding = true)

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2419260
    @SmokeTest
    @Test
    fun verifyCookieBannerBlockerSettingsOptionTest() {
        runWithCondition(appContext.settings().shouldUseCookieBannerPrivateMode) {
            homeScreen {
            }.openThreeDotMenu {
            }.openSettings {
                verifyCookieBannerBlockerButton(enabled = true)
            }
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2419273
    @SmokeTest
    @Test
    fun verifyCFRAfterBlockingTheCookieBanner() {
        runWithCondition(appContext.settings().shouldUseCookieBannerPrivateMode) {
            homeScreen {
            }.togglePrivateBrowsingMode()

            navigationToolbar {
            }.enterURLAndEnterToBrowser("voetbal24.be".toUri()) {
                waitForPageToLoad()
                verifyCookieBannerExists(exists = false)
                verifyCookieBannerBlockerCFRExists(exists = true)
            }
        }
    }
}
