/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.ui

import org.junit.Rule
import org.junit.Test
import org.eyesearch.eyebrowse.helpers.AppAndSystemHelper.assertNativeAppOpens
import org.eyesearch.eyebrowse.helpers.Constants
import org.eyesearch.eyebrowse.helpers.HomeActivityTestRule
import org.eyesearch.eyebrowse.helpers.MatcherHelper.itemContainingText
import org.eyesearch.eyebrowse.helpers.MatcherHelper.itemWithDescription
import org.eyesearch.eyebrowse.helpers.MatcherHelper.itemWithResId
import org.eyesearch.eyebrowse.helpers.MatcherHelper.itemWithResIdAndText
import org.eyesearch.eyebrowse.helpers.TestAssetHelper
import org.eyesearch.eyebrowse.helpers.TestAssetHelper.getHTMLControlsFormAsset
import org.eyesearch.eyebrowse.helpers.TestSetup
import org.eyesearch.eyebrowse.ui.robots.clickPageObject
import org.eyesearch.eyebrowse.ui.robots.navigationToolbar
import java.time.LocalDate

/**
 *  Tests for verifying basic interactions with web control elements
 *
 */

class WebControlsTest : TestSetup() {
    private val hour = 10
    private val minute = 10
    private val colorHexValue = "#5b2067"
    private val emailLink = "mailto://example@example.com"
    private val phoneLink = "tel://1234567890"

    @get:Rule
    val activityTestRule = HomeActivityTestRule(
        isJumpBackInCFREnabled = false,
        isTCPCFREnabled = false,
    )

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2316067
    @Test
    fun verifyCalendarFormInteractionsTest() {
        val currentDate = LocalDate.now()
        val currentDay = currentDate.dayOfMonth
        val currentMonth = currentDate.month
        val currentYear = currentDate.year
        val htmlControlsPage = getHTMLControlsFormAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.url) {
            clickPageObject(itemWithResId("calendar"))
            clickPageObject(itemContainingText("CANCEL"))
            clickPageObject(itemWithResId("submitDate"))
            verifyNoDateIsSelected()
            clickPageObject(itemWithResId("calendar"))
            clickPageObject(itemWithDescription("$currentDay $currentMonth $currentYear"))
            clickPageObject(itemContainingText("OK"))
            clickPageObject(itemWithResId("submitDate"))
            verifySelectedDate()
            clickPageObject(itemWithResId("calendar"))
            clickPageObject(itemContainingText("CLEAR"))
            clickPageObject(itemWithResId("submitDate"))
            verifyNoDateIsSelected()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2316069
    @Test
    fun verifyClockFormInteractionsTest() {
        val htmlControlsPage = getHTMLControlsFormAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.url) {
            clickPageObject(itemWithResId("clock"))
            clickPageObject(itemContainingText("CANCEL"))
            clickPageObject(itemWithResId("submitTime"))
            verifyNoTimeIsSelected(hour, minute)
            clickPageObject(itemWithResId("clock"))
            selectTime(hour, minute)
            clickPageObject(itemContainingText("OK"))
            clickPageObject(itemWithResId("submitTime"))
            verifySelectedTime(hour, minute)
            clickPageObject(itemWithResId("clock"))
            clickPageObject(itemContainingText("CLEAR"))
            clickPageObject(itemWithResId("submitTime"))
            verifyNoTimeIsSelected(hour, minute)
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2316068
    @Test
    fun verifyColorPickerInteractionsTest() {
        val htmlControlsPage = getHTMLControlsFormAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.url) {
            clickPageObject(itemWithResId("colorPicker"))
            clickPageObject(itemWithDescription(colorHexValue))
            clickPageObject(itemContainingText("CANCEL"))
            clickPageObject(itemWithResId("submitColor"))
            verifyColorIsNotSelected(colorHexValue)
            clickPageObject(itemWithResId("colorPicker"))
            clickPageObject(itemWithDescription(colorHexValue))
            clickPageObject(itemContainingText("SET"))
            clickPageObject(itemWithResId("submitColor"))
            verifySelectedColor(colorHexValue)
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2316070
    @Test
    fun verifyDropdownMenuInteractionsTest() {
        val htmlControlsPage = getHTMLControlsFormAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.url) {
            clickPageObject(itemWithResId("dropDown"))
            clickPageObject(itemContainingText("The National"))
            clickPageObject(itemWithResId("submitOption"))
            verifySelectedDropDownOption("The National")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2316071
    @Test
    fun verifyEmailLinkTest() {
        val externalLinksPage = TestAssetHelper.getExternalLinksAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(itemContainingText("Email link"))
            clickPageObject(itemWithResIdAndText("android:id/button1", "OPEN"))
            assertNativeAppOpens(Constants.PackageName.GMAIL_APP, emailLink)
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/834205
    @Test
    fun verifyTelephoneLinkTest() {
        val externalLinksPage = TestAssetHelper.getExternalLinksAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(externalLinksPage.url) {
            clickPageObject(itemContainingText("Telephone link"))
            clickPageObject(itemWithResIdAndText("android:id/button1", "OPEN"))
            assertNativeAppOpens(Constants.PackageName.PHONE_APP, phoneLink)
        }
    }
}
