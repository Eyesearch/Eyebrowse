/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.ui

import androidx.core.net.toUri
import mozilla.components.concept.engine.utils.EngineReleaseChannel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.eyesearch.eyebrowse.customannotations.SmokeTest
import org.eyesearch.eyebrowse.ext.components
import org.eyesearch.eyebrowse.helpers.AppAndSystemHelper.assertNativeAppOpens
import org.eyesearch.eyebrowse.helpers.AppAndSystemHelper.assertYoutubeAppOpens
import org.eyesearch.eyebrowse.helpers.AppAndSystemHelper.runWithCondition
import org.eyesearch.eyebrowse.helpers.Constants.PackageName.PRINT_SPOOLER
import org.eyesearch.eyebrowse.helpers.DataGenerationHelper.generateRandomString
import org.eyesearch.eyebrowse.helpers.HomeActivityIntentTestRule
import org.eyesearch.eyebrowse.helpers.MatcherHelper
import org.eyesearch.eyebrowse.helpers.TestAssetHelper
import org.eyesearch.eyebrowse.helpers.TestHelper
import org.eyesearch.eyebrowse.helpers.TestHelper.mDevice
import org.eyesearch.eyebrowse.helpers.TestSetup
import org.eyesearch.eyebrowse.nimbus.FxNimbus
import org.eyesearch.eyebrowse.nimbus.Translations
import org.eyesearch.eyebrowse.ui.robots.browserScreen
import org.eyesearch.eyebrowse.ui.robots.clickContextMenuItem
import org.eyesearch.eyebrowse.ui.robots.clickPageObject
import org.eyesearch.eyebrowse.ui.robots.homeScreen
import org.eyesearch.eyebrowse.ui.robots.longClickPageObject
import org.eyesearch.eyebrowse.ui.robots.navigationToolbar

class MainMenuTest : TestSetup() {
    @get:Rule
    val activityTestRule = HomeActivityIntentTestRule.withDefaultSettingsOverrides()

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/233849

    @Before
    override fun setUp() {
        super.setUp()
        FxNimbus.features.translations.withInitializer { _, _ ->
            // These are FML generated objects and enums
            Translations(
                mainFlowToolbarEnabled = true,
                mainFlowBrowserMenuEnabled = true,
                pageSettingsEnabled = true,
                globalSettingsEnabled = true,
                globalLangSettingsEnabled = true,
                globalSiteSettingsEnabled = true,
                downloadsEnabled = true,
            )
        }
    }

    @Test
    fun verifyTabMainMenuItemsTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            waitForPageToLoad()
        }.openThreeDotMenu {
            verifyPageThreeDotMainMenuItems(isRequestDesktopSiteEnabled = false)
        }
    }

    // Verifies the list of items in the homescreen's 3 dot main menu
    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/233848
    @SmokeTest
    @Test
    fun homeMainMenuItemsTest() {
        homeScreen {
        }.openThreeDotMenu {
            verifyHomeThreeDotMainMenuItems(isRequestDesktopSiteEnabled = false)
        }.openBookmarks {
            verifyBookmarksMenuView()
        }.goBack {
        }.openThreeDotMenu {
        }.openHistory {
            verifyHistoryMenuView()
        }.goBack {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            verifyEmptyDownloadsList()
        }.goBack {
        }.openThreeDotMenu {
        }.openAddonsManagerMenu {
            verifyAddonsListIsDisplayed(true)
        }.goBack {
        }.openThreeDotMenu {
        }.openSyncSignIn {
            verifyTurnOnSyncMenu()
        }.goBack {}
        homeScreen {
        }.openThreeDotMenu {
        }.openWhatsNew {
            verifyWhatsNewURL()
        }.goToHomescreen {
        }.openThreeDotMenu {
        }.openHelp {
            verifyHelpUrl()
        }.goToHomescreen {
        }.openThreeDotMenu {
        }.openCustomizeHome {
            verifyHomePageView()
        }.goBackToHomeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            verifySettingsView()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2284134
    @Test
    fun openNewTabTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.clickNewTabButton {
            verifySearchView()
        }.submitQuery("test") {
            verifyTabCounter("2")
        }
    }

    // Device or AVD requires a Google Services Android OS installation with Play Store installed
    // Verifies the Open in app button when an app is installed
    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/387756
    @SmokeTest
    @Test
    fun openInAppFunctionalityTest() {
        val youtubeURL = "vnd.youtube://".toUri()

        navigationToolbar {
        }.enterURLAndEnterToBrowser(youtubeURL) {
            verifyNotificationDotOnMainMenu()
        }.openThreeDotMenu {
        }.clickOpenInApp {
            assertYoutubeAppOpens()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2284323
    @Test
    fun openSyncAndSaveDataTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.openSyncSignIn {
            verifyTurnOnSyncMenu()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/243840
    @Test
    fun findInPageTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 3)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
            verifyThreeDotMenuExists()
            verifyFindInPageButton()
        }.openFindInPage {
            verifyFindInPageNextButton()
            verifyFindInPagePrevButton()
            verifyFindInPageCloseButton()
            enterFindInPageQuery("a")
            verifyFindInPageResult("1/3")
            clickFindInPageNextButton()
            verifyFindInPageResult("2/3")
            clickFindInPageNextButton()
            verifyFindInPageResult("3/3")
            clickFindInPagePrevButton()
            verifyFindInPageResult("2/3")
            clickFindInPagePrevButton()
            verifyFindInPageResult("1/3")
        }.closeFindInPageWithCloseButton {
            verifyFindInPageBar(false)
        }.openThreeDotMenu {
        }.openFindInPage {
            enterFindInPageQuery("3")
            verifyFindInPageResult("1/1")
        }.closeFindInPageWithBackButton {
            verifyFindInPageBar(false)
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2283303
    @Test
    fun switchDesktopSiteModeOnOffTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.switchDesktopSiteMode {
        }.openThreeDotMenu {
            verifyDesktopSiteModeEnabled(true)
        }.switchDesktopSiteMode {
        }.openThreeDotMenu {
            verifyDesktopSiteModeEnabled(false)
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1314137
    @Test
    fun setDesktopSiteBeforePageLoadTest() {
        val webPage = TestAssetHelper.getGenericAsset(mockWebServer, 4)

        homeScreen {
        }.openThreeDotMenu {
            verifyDesktopSiteModeEnabled(false)
        }.switchDesktopSiteMode {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(webPage.url) {
        }.openThreeDotMenu {
            verifyDesktopSiteModeEnabled(true)
        }.closeBrowserMenuToBrowser {
            clickPageObject(MatcherHelper.itemContainingText("Link 1"))
        }.openThreeDotMenu {
            verifyDesktopSiteModeEnabled(true)
        }.closeBrowserMenuToBrowser {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(webPage.url) {
            longClickPageObject(MatcherHelper.itemWithText("Link 2"))
            clickContextMenuItem("Open link in new tab")
            TestHelper.clickSnackbarButton("SWITCH")
        }.openThreeDotMenu {
            verifyDesktopSiteModeEnabled(false)
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2283302
    @Test
    fun reportSiteIssueTest() {
        runWithCondition(
            // This test will not run on RC builds because the "Report site issue button" is not available.
            activityTestRule.activity.components.core.engine.version.releaseChannel !== EngineReleaseChannel.RELEASE,
        ) {
            val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

            navigationToolbar {
            }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            }.openThreeDotMenu {
            }.openReportSiteIssue {
                verifyUrl("webcompat.com/issues/new")
            }
        }
    }

    // Verifies the Add to home screen option in a tab's 3 dot menu
    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/410724
    @SmokeTest
    @Test
    fun addPageShortcutToHomeScreenTest() {
        val website = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val shortcutTitle = generateRandomString(5)

        homeScreen {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(website.url) {
        }.openThreeDotMenu {
            expandMenu()
        }.openAddToHomeScreen {
            clickCancelShortcutButton()
        }

        browserScreen {
        }.openThreeDotMenu {
            expandMenu()
        }.openAddToHomeScreen {
            verifyShortcutTextFieldTitle("Test_Page_1")
            addShortcutName(shortcutTitle)
            clickAddShortcutButton()
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
            verifyUrl(website.url.toString())
            verifyTabCounter("1")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/329893
    @SmokeTest
    @Test
    fun mainMenuShareButtonTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.clickShareButton {
            verifyShareTabLayout()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/233604
    @Test
    fun navigateBackAndForwardTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val nextWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            mDevice.waitForIdle()
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(nextWebPage.url) {
            verifyUrl(nextWebPage.url.toString())
        }.openThreeDotMenu {
        }.goToPreviousPage {
            mDevice.waitForIdle()
            verifyUrl(defaultWebPage.url.toString())
        }.openThreeDotMenu {
        }.goForward {
            verifyUrl(nextWebPage.url.toString())
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2195819
    @SmokeTest
    @Test
    fun refreshPageButtonTest() {
        val refreshWebPage = TestAssetHelper.getRefreshAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(refreshWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
            verifyThreeDotMenuExists()
        }.refreshPage {
            verifyPageContent("REFRESHED")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2265657
    @Test
    fun forceRefreshPageTest() {
        val refreshWebPage = TestAssetHelper.getRefreshAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(refreshWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
            verifyThreeDotMenuExists()
        }.forceRefreshPage {
            verifyPageContent("REFRESHED")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2282411
    @Test
    fun printWebPageFromMainMenuTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.clickPrintButton {
            assertNativeAppOpens(PRINT_SPOOLER)
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2282408
    @Test
    fun printWebPageFromShareMenuTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            mDevice.waitForIdle()
        }.openThreeDotMenu {
        }.clickShareButton {
        }.clickPrintButton {
            assertNativeAppOpens(PRINT_SPOOLER)
        }
    }
}
