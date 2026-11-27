/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.ui

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import org.junit.Rule
import org.junit.Test
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.customannotations.SmokeTest
import org.eyesearch.eyebrowse.helpers.AppAndSystemHelper.registerAndCleanupIdlingResources
import org.eyesearch.eyebrowse.helpers.HomeActivityIntentTestRule
import org.eyesearch.eyebrowse.helpers.RecyclerViewIdlingResource
import org.eyesearch.eyebrowse.helpers.TestAssetHelper.getGenericAsset
import org.eyesearch.eyebrowse.helpers.TestHelper.longTapSelectItem
import org.eyesearch.eyebrowse.helpers.TestHelper.mDevice
import org.eyesearch.eyebrowse.helpers.TestSetup
import org.eyesearch.eyebrowse.ui.robots.browserScreen
import org.eyesearch.eyebrowse.ui.robots.homeScreen
import org.eyesearch.eyebrowse.ui.robots.navigationToolbar

/**
 *  Tests for verifying basic functionality of recently closed tabs history
 *
 */
class RecentlyClosedTabsTest : TestSetup() {
    @get:Rule
    val activityTestRule = AndroidComposeTestRule(
        HomeActivityIntentTestRule.withDefaultSettingsOverrides(
            tabsTrayRewriteEnabled = true,
        ),
    ) { it.activity }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1065414
    // Verifies that a recently closed item is properly opened
    @SmokeTest
    @Test
    fun openRecentlyClosedItemTest() {
        val website = getGenericAsset(mockWebServer, 1)

        homeScreen {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(website.url) {
            mDevice.waitForIdle()
        }.openComposeTabDrawer(activityTestRule) {
            closeTab()
        }
        homeScreen {
        }.openThreeDotMenu {
        }.openHistory {
        }.openRecentlyClosedTabs {
            waitForListToExist()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.recently_closed_list), 1),
            ) {
                verifyRecentlyClosedTabsMenuView()
                verifyRecentlyClosedTabsPageTitle("Test_Page_1")
                verifyRecentlyClosedTabsUrl(website.url)
            }
        }.clickRecentlyClosedItem("Test_Page_1") {
            verifyUrl(website.url.toString())
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2195812
    // Verifies that tapping the "x" button removes a recently closed item from the list
    @SmokeTest
    @Test
    fun deleteRecentlyClosedTabsItemTest() {
        val website = getGenericAsset(mockWebServer, 1)

        homeScreen {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(website.url) {
            mDevice.waitForIdle()
        }.openComposeTabDrawer(activityTestRule) {
            closeTab()
        }
        homeScreen {
        }.openThreeDotMenu {
        }.openHistory {
        }.openRecentlyClosedTabs {
            waitForListToExist()
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.recently_closed_list), 1),
            ) {
                verifyRecentlyClosedTabsMenuView()
            }
            clickDeleteRecentlyClosedTabs()
            verifyEmptyRecentlyClosedTabsList()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1605515
    @Test
    fun openMultipleRecentlyClosedTabsTest() {
        val firstPage = getGenericAsset(mockWebServer, 1)
        val secondPage = getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstPage.url) {
            waitForPageToLoad()
        }.openComposeTabDrawer(activityTestRule) {
        }.openNewTab {
        }.submitQuery(secondPage.url.toString()) {
            waitForPageToLoad()
        }.openComposeTabDrawer(activityTestRule) {
        }.openThreeDotMenu {
        }.closeAllTabs {
        }.openThreeDotMenu {
        }.openHistory {
        }.openRecentlyClosedTabs {
            waitForListToExist()
            longTapSelectItem(firstPage.url)
            longTapSelectItem(secondPage.url)
            openActionBarOverflowOrOptionsMenu(activityTestRule.activity)
        }.clickOpenInNewTab(activityTestRule) {
            // URL verification to be removed once https://bugzilla.mozilla.org/show_bug.cgi?id=1839179 is fixed.
            browserScreen {
                verifyPageContent(secondPage.content)
                verifyUrl(secondPage.url.toString())
            }.openComposeTabDrawer(activityTestRule) {
                verifyNormalBrowsingButtonIsSelected(true)
                verifyExistingOpenTabs(firstPage.title, secondPage.title)
            }
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2198690
    @Test
    fun openRecentlyClosedTabsInPrivateBrowsingTest() {
        val firstPage = getGenericAsset(mockWebServer, 1)
        val secondPage = getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstPage.url) {
            waitForPageToLoad()
        }.openComposeTabDrawer(activityTestRule) {
        }.openNewTab {
        }.submitQuery(secondPage.url.toString()) {
            waitForPageToLoad()
        }.openComposeTabDrawer(activityTestRule) {
        }.openThreeDotMenu {
        }.closeAllTabs {
        }.openThreeDotMenu {
        }.openHistory {
        }.openRecentlyClosedTabs {
            waitForListToExist()
            longTapSelectItem(firstPage.url)
            longTapSelectItem(secondPage.url)
            openActionBarOverflowOrOptionsMenu(activityTestRule.activity)
        }.clickOpenInPrivateTab(activityTestRule) {
            // URL verification to be removed once https://bugzilla.mozilla.org/show_bug.cgi?id=1839179 is fixed.
            browserScreen {
                verifyPageContent(secondPage.content)
                verifyUrl(secondPage.url.toString())
            }.openComposeTabDrawer(activityTestRule) {
                verifyPrivateBrowsingButtonIsSelected(true)
                verifyExistingOpenTabs(firstPage.title, secondPage.title)
            }
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1605514
    @Test
    fun shareMultipleRecentlyClosedTabsTest() {
        val firstPage = getGenericAsset(mockWebServer, 1)
        val secondPage = getGenericAsset(mockWebServer, 2)
        val sharingApp = "Gmail"
        val urlString = "${firstPage.url}\n\n${secondPage.url}"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstPage.url) {
            waitForPageToLoad()
        }.openComposeTabDrawer(activityTestRule) {
        }.openNewTab {
        }.submitQuery(secondPage.url.toString()) {
            waitForPageToLoad()
        }.openComposeTabDrawer(activityTestRule) {
        }.openThreeDotMenu {
        }.closeAllTabs {
        }.openThreeDotMenu {
        }.openHistory {
        }.openRecentlyClosedTabs {
            waitForListToExist()
            longTapSelectItem(firstPage.url)
            longTapSelectItem(secondPage.url)
        }.clickShare {
            verifyShareTabsOverlay(firstPage.title, secondPage.title)
            verifySharingWithSelectedApp(sharingApp, urlString, "${firstPage.title}, ${secondPage.title}")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1065438
    @Test
    fun closedPrivateTabsAreNotSavedInRecentlyClosedTabsTest() {
        val firstPage = getGenericAsset(mockWebServer, 1)
        val secondPage = getGenericAsset(mockWebServer, 2)

        homeScreen {}.togglePrivateBrowsingMode()
        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstPage.url) {
            waitForPageToLoad()
        }.openComposeTabDrawer(activityTestRule) {
        }.openNewTab {
        }.submitQuery(secondPage.url.toString()) {
            waitForPageToLoad()
        }.openComposeTabDrawer(activityTestRule) {
        }.openThreeDotMenu {
        }.closeAllTabs {
        }.openThreeDotMenu {
        }.openHistory {
        }.openRecentlyClosedTabs {
            verifyEmptyRecentlyClosedTabsList()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1065439
    @Test
    fun deletingBrowserHistoryClearsRecentlyClosedTabsListTest() {
        val firstPage = getGenericAsset(mockWebServer, 1)
        val secondPage = getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstPage.url) {
            waitForPageToLoad()
        }.openComposeTabDrawer(activityTestRule) {
        }.openNewTab {
        }.submitQuery(secondPage.url.toString()) {
            waitForPageToLoad()
        }.openComposeTabDrawer(activityTestRule) {
        }.openThreeDotMenu {
        }.closeAllTabs {
        }.openThreeDotMenu {
        }.openHistory {
        }.openRecentlyClosedTabs {
            waitForListToExist()
        }.goBackToHistoryMenu {
            clickDeleteAllHistoryButton()
            selectEverythingOption()
            confirmDeleteAllHistory()
            verifyEmptyHistoryView()
        }.openRecentlyClosedTabs {
            verifyEmptyRecentlyClosedTabsList()
        }
    }
}
