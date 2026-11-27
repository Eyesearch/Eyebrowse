/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.ui

import org.junit.Rule
import org.junit.Test
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.customannotations.SmokeTest
import org.eyesearch.eyebrowse.ext.settings
import org.eyesearch.eyebrowse.helpers.AppAndSystemHelper.registerAndCleanupIdlingResources
import org.eyesearch.eyebrowse.helpers.HomeActivityIntentTestRule
import org.eyesearch.eyebrowse.helpers.RecyclerViewIdlingResource
import org.eyesearch.eyebrowse.helpers.TestAssetHelper.getEnhancedTrackingProtectionAsset
import org.eyesearch.eyebrowse.helpers.TestHelper
import org.eyesearch.eyebrowse.helpers.TestHelper.verifySnackBarText
import org.eyesearch.eyebrowse.helpers.TestHelper.waitUntilSnackbarGone
import org.eyesearch.eyebrowse.helpers.TestSetup
import org.eyesearch.eyebrowse.ui.robots.addonsMenu
import org.eyesearch.eyebrowse.ui.robots.homeScreen

/**
 *  Tests for verifying the functionality of installing or removing addons
 *
 */
class SettingsAddonsTest : TestSetup() {
    @get:Rule
    val activityTestRule = HomeActivityIntentTestRule.withDefaultSettingsOverrides()

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/875780
    // Walks through settings add-ons menu to ensure all items are present
    @Test
    fun verifyAddonsListItemsTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            verifyAdvancedHeading()
            verifyAddons()
        }.openAddonsManagerMenu {
            registerAndCleanupIdlingResources(
                RecyclerViewIdlingResource(activityTestRule.activity.findViewById(R.id.add_ons_list), 1),
            ) {
                verifyAddonsItems()
            }
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/875781
    // Installs an add-on from the Add-ons menu and verifies the prompts
    @Test
    fun installAddonFromMainMenuTest() {
        val addonName = "uBlock Origin"

        homeScreen {}
            .openThreeDotMenu {}
            .openAddonsManagerMenu {
                registerAndCleanupIdlingResources(
                    RecyclerViewIdlingResource(
                        activityTestRule.activity.findViewById(R.id.add_ons_list),
                        1,
                    ),
                ) {
                    clickInstallAddon(addonName)
                }
                verifyAddonDownloadOverlay()
                verifyAddonPermissionPrompt(addonName)
                cancelInstallAddon()
                clickInstallAddon(addonName)
                acceptPermissionToInstallAddon()
                verifyAddonInstallCompleted(addonName, activityTestRule)
                verifyAddonInstallCompletedPrompt(addonName)
                closeAddonInstallCompletePrompt()
                verifyAddonIsInstalled(addonName)
                verifyEnabledTitleDisplayed()
            }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/561597
    // Installs an addon, then uninstalls it
    @Test
    fun verifyAddonsCanBeUninstalledTest() {
        val addonName = "uBlock Origin"

        addonsMenu {
            installAddon(addonName, activityTestRule)
            closeAddonInstallCompletePrompt()
        }.openDetailedMenuForAddon(addonName) {
        }.removeAddon(activityTestRule) {
            verifySnackBarText("Successfully uninstalled $addonName")
            waitUntilSnackbarGone()
        }.goBack {
        }.openThreeDotMenu {
        }.openAddonsManagerMenu {
            verifyAddonCanBeInstalled(addonName)
        }
    }

    // TODO: Harden to dynamically install addons from position
    //   in list of detected addons on screen instead of hard-coded values.
    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/561600
    // Installs 2 add-on and checks that the app doesn't crash while navigating the app
    @SmokeTest
    @Test
    fun noCrashWithAddonInstalledTest() {
        // setting ETP to Strict mode to test it works with add-ons
        activityTestRule.activity.settings().setStrictETP()

        val uBlockAddon = "uBlock Origin"
        val darkReaderAddon = "Dark Reader"
        val trackingProtectionPage = getEnhancedTrackingProtectionAsset(mockWebServer)

        addonsMenu {
            installAddon(uBlockAddon, activityTestRule)
            closeAddonInstallCompletePrompt()
            installAddon(darkReaderAddon, activityTestRule)
            closeAddonInstallCompletePrompt()
        }.goBack {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(trackingProtectionPage.url) {
            verifyUrl(trackingProtectionPage.url.toString())
        }.goToHomescreen {
        }.openTopSiteTabWithTitle("Top Articles") {
        }.openThreeDotMenu {
        }.openSettings {
            verifySettingsView()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/561594
    @SmokeTest
    @Test
    fun verifyUBlockWorksInPrivateModeTest() {
        TestHelper.appContext.settings().shouldShowCookieBannersCFR = false
        val addonName = "uBlock Origin"

        addonsMenu {
            installAddon(addonName, activityTestRule)
            selectAllowInPrivateBrowsing()
            closeAddonInstallCompletePrompt()
        }.goBack {
        }.openContextMenuOnSponsoredShortcut("Top Articles") {
        }.openTopSiteInPrivateTab {
            waitForPageToLoad()
        }.openThreeDotMenu {
            openAddonsSubList()
            verifyAddonAvailableInMainMenu(addonName)
            verifyTrackersBlockedByUblock()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/875785
    @Test
    fun verifyUBlockWorksInNormalModeTest() {
        val addonName = "uBlock Origin"

        addonsMenu {
            installAddon(addonName, activityTestRule)
            closeAddonInstallCompletePrompt()
        }.goBack {
        }.openTopSiteTabWithTitle("Top Articles") {
            waitForPageToLoad()
        }.openThreeDotMenu {
            openAddonsSubList()
            verifyTrackersBlockedByUblock()
        }
    }
}
