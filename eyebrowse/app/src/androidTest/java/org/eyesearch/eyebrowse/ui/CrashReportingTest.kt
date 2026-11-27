/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.ui

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.customannotations.SmokeTest
import org.eyesearch.eyebrowse.helpers.DataGenerationHelper.getStringResource
import org.eyesearch.eyebrowse.helpers.HomeActivityIntentTestRule
import org.eyesearch.eyebrowse.helpers.MatcherHelper.itemWithResId
import org.eyesearch.eyebrowse.helpers.TestAssetHelper
import org.eyesearch.eyebrowse.helpers.TestHelper.mDevice
import org.eyesearch.eyebrowse.helpers.TestHelper.packageName
import org.eyesearch.eyebrowse.helpers.TestSetup
import org.eyesearch.eyebrowse.ui.robots.clickPageObject
import org.eyesearch.eyebrowse.ui.robots.homeScreen
import org.eyesearch.eyebrowse.ui.robots.navigationToolbar

class CrashReportingTest : TestSetup() {
    private val tabCrashMessage = getStringResource(R.string.tab_crash_title_2)

    @get:Rule
    val activityTestRule = AndroidComposeTestRule(
        HomeActivityIntentTestRule(
            isPocketEnabled = false,
            isJumpBackInCFREnabled = false,
            isWallpaperOnboardingEnabled = false,
            isTCPCFREnabled = false,
        ),
    ) { it.activity }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/308906
    @Test
    fun closeTabFromCrashedTabReporterTest() {
        homeScreen {
        }.openNavigationToolbar {
        }.openTabCrashReporter {
        }.clickTabCrashedCloseButton {
        }.openTabDrawer {
            verifyNoOpenTabsInNormalBrowsing()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2336134
    @Ignore("Test failure caused by: https://github.com/mozilla-mobile/fenix/issues/19964")
    @Test
    fun restoreTabFromTabCrashedReporterTest() {
        val website = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(website.url) {}

        navigationToolbar {
        }.openTabCrashReporter {
            clickPageObject(itemWithResId("$packageName:id/restoreTabButton"))
            verifyPageContent(website.content)
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1681928
    @SmokeTest
    @Test
    fun useAppWhileTabIsCrashedTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 2)

        homeScreen {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            mDevice.waitForIdle()
        }.openTabDrawer {
        }.openNewTab {
        }.submitQuery(secondWebPage.url.toString()) {
            waitForPageToLoad()
        }

        navigationToolbar {
        }.openTabCrashReporter {
            verifyPageContent(tabCrashMessage)
        }.openTabDrawer {
            verifyExistingOpenTabs(firstWebPage.title)
            verifyExistingOpenTabs("about:crashcontent")
        }.closeTabDrawer {
        }.goToHomescreen {
            verifyExistingTopSitesList()
        }.openThreeDotMenu {
            verifySettingsButton()
        }
    }
}
