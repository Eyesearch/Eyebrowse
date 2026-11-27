/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.ui

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import mozilla.components.concept.engine.mediasession.MediaSession
import org.junit.Rule
import org.junit.Test
import org.eyesearch.eyebrowse.customannotations.SmokeTest
import org.eyesearch.eyebrowse.helpers.HomeActivityTestRule
import org.eyesearch.eyebrowse.helpers.MatcherHelper
import org.eyesearch.eyebrowse.helpers.RetryTestRule
import org.eyesearch.eyebrowse.helpers.TestAssetHelper
import org.eyesearch.eyebrowse.helpers.TestHelper.mDevice
import org.eyesearch.eyebrowse.helpers.TestHelper.verifySnackBarText
import org.eyesearch.eyebrowse.helpers.TestSetup
import org.eyesearch.eyebrowse.ui.robots.browserScreen
import org.eyesearch.eyebrowse.ui.robots.clickPageObject
import org.eyesearch.eyebrowse.ui.robots.homeScreen
import org.eyesearch.eyebrowse.ui.robots.navigationToolbar
import org.eyesearch.eyebrowse.ui.robots.notificationShade

/**
 *  Tests for verifying basic functionality of media notifications:
 *  - video and audio playback system notifications appear and can pause/play the media content
 *  - a media notification icon is displayed on the homescreen for the tab playing media content
 *  Note: this test only verifies media notifications, not media itself
 */
class ComposeMediaNotificationTest : TestSetup() {
    @get:Rule(order = 0)
    val composeTestRule =
        AndroidComposeTestRule(
            HomeActivityTestRule.withDefaultSettingsOverrides(
                tabsTrayRewriteEnabled = true,
            ),
        ) { it.activity }

    @Rule(order = 1)
    @JvmField
    val retryTestRule = RetryTestRule(3)

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1347033
    @SmokeTest
    @Test
    fun verifyVideoPlaybackSystemNotificationTest() {
        val videoTestPage = TestAssetHelper.getVideoPageAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(videoTestPage.url) {
            mDevice.waitForIdle()
            clickPageObject(MatcherHelper.itemWithText("Play"))
            assertPlaybackState(browserStore, MediaSession.PlaybackState.PLAYING)
        }.openNotificationShade {
            verifySystemNotificationExists(videoTestPage.title)
            clickMediaNotificationControlButton("Pause")
            verifyMediaSystemNotificationButtonState("Play")
        }

        mDevice.pressBack()

        browserScreen {
            assertPlaybackState(browserStore, MediaSession.PlaybackState.PAUSED)
        }.openComposeTabDrawer(composeTestRule) {
            closeTab()
        }

        mDevice.openNotification()

        notificationShade {
            verifySystemNotificationDoesNotExist(videoTestPage.title)
        }

        // close notification shade before the next test
        mDevice.pressBack()
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2316010
    @SmokeTest
    @Test
    fun verifyAudioPlaybackSystemNotificationTest() {
        val audioTestPage = TestAssetHelper.getAudioPageAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(audioTestPage.url) {
            mDevice.waitForIdle()
            clickPageObject(MatcherHelper.itemWithText("Play"))
            assertPlaybackState(browserStore, MediaSession.PlaybackState.PLAYING)
        }.openNotificationShade {
            verifySystemNotificationExists(audioTestPage.title)
            clickMediaNotificationControlButton("Pause")
            verifyMediaSystemNotificationButtonState("Play")
        }

        mDevice.pressBack()

        browserScreen {
            assertPlaybackState(browserStore, MediaSession.PlaybackState.PAUSED)
        }.openComposeTabDrawer(composeTestRule) {
            closeTab()
        }

        mDevice.openNotification()

        notificationShade {
            verifySystemNotificationDoesNotExist(audioTestPage.title)
        }

        // close notification shade before the next test
        mDevice.pressBack()
    }

    // TestRail: https://testrail.stage.mozaws.net/index.php?/cases/view/903595
    @Test
    fun mediaSystemNotificationInPrivateModeTest() {
        val audioTestPage = TestAssetHelper.getAudioPageAsset(mockWebServer)

        homeScreen {
        }.openComposeTabDrawer(composeTestRule) {
        }.toggleToPrivateTabs {
        }.openNewTab {
        }.submitQuery(audioTestPage.url.toString()) {
            mDevice.waitForIdle()
            clickPageObject(MatcherHelper.itemWithText("Play"))
            assertPlaybackState(browserStore, MediaSession.PlaybackState.PLAYING)
        }.openNotificationShade {
            verifySystemNotificationExists("A site is playing media")
            clickMediaNotificationControlButton("Pause")
            verifyMediaSystemNotificationButtonState("Play")
        }

        mDevice.pressBack()

        browserScreen {
            assertPlaybackState(browserStore, MediaSession.PlaybackState.PAUSED)
        }.openComposeTabDrawer(composeTestRule) {
            closeTab()
            verifySnackBarText("Private tab closed")
        }

        mDevice.openNotification()

        notificationShade {
            verifySystemNotificationDoesNotExist("A site is playing media")
        }

        // close notification shade before and go back to regular mode before the next test
        mDevice.pressBack()
        homeScreen { }.togglePrivateBrowsingMode()
    }
}
