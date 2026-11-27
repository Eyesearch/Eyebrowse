/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.ui

import android.view.View
import org.junit.Rule
import org.junit.Test
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.customannotations.SmokeTest
import org.eyesearch.eyebrowse.helpers.AppAndSystemHelper.registerAndCleanupIdlingResources
import org.eyesearch.eyebrowse.helpers.HomeActivityIntentTestRule
import org.eyesearch.eyebrowse.helpers.RetryTestRule
import org.eyesearch.eyebrowse.helpers.TestAssetHelper
import org.eyesearch.eyebrowse.helpers.TestHelper.mDevice
import org.eyesearch.eyebrowse.helpers.TestSetup
import org.eyesearch.eyebrowse.helpers.ViewVisibilityIdlingResource
import org.eyesearch.eyebrowse.ui.robots.browserScreen
import org.eyesearch.eyebrowse.ui.robots.navigationToolbar

/**
 *  Tests for verifying basic functionality of content context menus
 *
 *  - Verifies Reader View entry and detection when available UI and functionality
 *  - Verifies Reader View exit UI and functionality
 *  - Verifies Reader View appearance controls UI and functionality
 *
 */

class ReaderViewTest : TestSetup() {
    private val estimatedReadingTime = "1 - 2 minutes"

    @get:Rule
    val activityIntentTestRule = HomeActivityIntentTestRule.withDefaultSettingsOverrides()

    @Rule
    @JvmField
    val retryTestRule = RetryTestRule(3)

    /**
     *  Verify that Reader View capable pages
     *
     *   - Show the toggle button in the navigation bar
     *
     */
    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/250592
    @Test
    fun verifyReaderModePageDetectionTest() {
        val readerViewPage =
            TestAssetHelper.getLoremIpsumAsset(mockWebServer)
        val genericPage =
            TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(readerViewPage.url) {
            mDevice.waitForIdle()
        }

        registerAndCleanupIdlingResources(
            ViewVisibilityIdlingResource(
                activityIntentTestRule.activity.findViewById(R.id.mozac_browser_toolbar_page_actions),
                View.VISIBLE,
            ),
        ) {}

        navigationToolbar {
            verifyReaderViewDetected(true)
        }.enterURLAndEnterToBrowser(genericPage.url) {
        }
        navigationToolbar {
            verifyReaderViewDetected(false)
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/250585
    @SmokeTest
    @Test
    fun verifyReaderModeControlsTest() {
        val readerViewPage =
            TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(readerViewPage.url) {
            mDevice.waitForIdle()
        }

        registerAndCleanupIdlingResources(
            ViewVisibilityIdlingResource(
                activityIntentTestRule.activity.findViewById(R.id.mozac_browser_toolbar_page_actions),
                View.VISIBLE,
            ),
        ) {}

        navigationToolbar {
            verifyReaderViewDetected(true)
            toggleReaderView()
            mDevice.waitForIdle()
        }

        browserScreen {
            verifyPageContent(estimatedReadingTime)
        }.openThreeDotMenu {
            verifyReaderViewAppearance(true)
        }.openReaderViewAppearance {
            verifyAppearanceFontGroup(true)
            verifyAppearanceFontSansSerif(true)
            verifyAppearanceFontSerif(true)
            verifyAppearanceFontIncrease(true)
            verifyAppearanceFontDecrease(true)
            verifyAppearanceFontSize(3)
            verifyAppearanceColorGroup(true)
            verifyAppearanceColorDark(true)
            verifyAppearanceColorLight(true)
            verifyAppearanceColorSepia(true)
        }.toggleSansSerif {
            verifyAppearanceFontIsActive("SANSSERIF")
        }.toggleSerif {
            verifyAppearanceFontIsActive("SERIF")
        }.toggleFontSizeIncrease {
            verifyAppearanceFontSize(4)
        }.toggleFontSizeIncrease {
            verifyAppearanceFontSize(5)
        }.toggleFontSizeIncrease {
            verifyAppearanceFontSize(6)
        }.toggleFontSizeDecrease {
            verifyAppearanceFontSize(5)
        }.toggleFontSizeDecrease {
            verifyAppearanceFontSize(4)
        }.toggleFontSizeDecrease {
            verifyAppearanceFontSize(3)
        }.toggleColorSchemeChangeDark {
            verifyAppearanceColorSchemeChange("DARK")
        }.toggleColorSchemeChangeSepia {
            verifyAppearanceColorSchemeChange("SEPIA")
        }.toggleColorSchemeChangeLight {
            verifyAppearanceColorSchemeChange("LIGHT")
        }.closeAppearanceMenu {
        }
        navigationToolbar {
            toggleReaderView()
            mDevice.waitForIdle()
            verifyReaderViewDetected(true)
        }.openThreeDotMenu {
            verifyReaderViewAppearance(false)
        }
    }
}
