/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.ui

import androidx.core.net.toUri
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.eyesearch.eyebrowse.customannotations.SmokeTest
import org.eyesearch.eyebrowse.helpers.AppAndSystemHelper.assertExternalAppOpens
import org.eyesearch.eyebrowse.helpers.AppAndSystemHelper.deleteDownloadedFileOnStorage
import org.eyesearch.eyebrowse.helpers.AppAndSystemHelper.setNetworkEnabled
import org.eyesearch.eyebrowse.helpers.Constants.PackageName.GOOGLE_APPS_PHOTOS
import org.eyesearch.eyebrowse.helpers.Constants.PackageName.GOOGLE_DOCS
import org.eyesearch.eyebrowse.helpers.HomeActivityIntentTestRule
import org.eyesearch.eyebrowse.helpers.MatcherHelper.itemWithText
import org.eyesearch.eyebrowse.helpers.TestAssetHelper
import org.eyesearch.eyebrowse.helpers.TestHelper.clickSnackbarButton
import org.eyesearch.eyebrowse.helpers.TestHelper.exitMenu
import org.eyesearch.eyebrowse.helpers.TestHelper.mDevice
import org.eyesearch.eyebrowse.helpers.TestSetup
import org.eyesearch.eyebrowse.ui.robots.browserScreen
import org.eyesearch.eyebrowse.ui.robots.clickPageObject
import org.eyesearch.eyebrowse.ui.robots.downloadRobot
import org.eyesearch.eyebrowse.ui.robots.homeScreen
import org.eyesearch.eyebrowse.ui.robots.navigationToolbar
import org.eyesearch.eyebrowse.ui.robots.notificationShade

/**
 *  Tests for verifying basic functionality of download
 *
 *  - Initiates a download
 *  - Verifies download prompt
 *  - Verifies download notification and actions
 *  - Verifies managing downloads inside the Downloads listing.
 **/
class DownloadTest : TestSetup() {
    /* Remote test page managed by Mozilla Mobile QA team at https://github.com/mozilla-mobile/testapp */
    private val downloadTestPage = "https://storage.googleapis.com/mobile_test_assets/test_app/downloads.html"
    private var downloadFile: String = ""

    @get:Rule
    val activityTestRule = HomeActivityIntentTestRule.withDefaultSettingsOverrides()

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/243844
    @Test
    fun verifyTheDownloadPromptsTest() {
        downloadRobot {
            openPageAndDownloadFile(url = downloadTestPage.toUri(), downloadFile = "web_icon.png")
            verifyDownloadCompleteNotificationPopup()
        }.clickOpen("image/png") {}
        downloadRobot {
            verifyPhotosAppOpens()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2299405
    @Test
    fun verifyTheDownloadFailedNotificationsTest() {
        downloadRobot {
            openPageAndDownloadFile(url = downloadTestPage.toUri(), downloadFile = "1GB.zip")
            setNetworkEnabled(enabled = false)
            verifyDownloadFailedPrompt("1GB.zip")
            setNetworkEnabled(enabled = true)
            clickTryAgainButton()
        }
        mDevice.openNotification()
        notificationShade {
            verifySystemNotificationDoesNotExist("Download failed")
            verifySystemNotificationExists("1GB.zip")
        }.closeNotificationTray {}
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2298616
    @Test
    fun verifyDownloadCompleteNotificationTest() {
        downloadRobot {
            openPageAndDownloadFile(url = downloadTestPage.toUri(), downloadFile = "web_icon.png")
            verifyDownloadCompleteNotificationPopup()
        }
        mDevice.openNotification()
        notificationShade {
            verifySystemNotificationExists("Download completed")
            clickNotification("Download completed")
            assertExternalAppOpens(GOOGLE_APPS_PHOTOS)
            mDevice.pressBack()
            mDevice.openNotification()
            verifySystemNotificationExists("Download completed")
            swipeDownloadNotification(
                direction = "Left",
                shouldDismissNotification = true,
                canExpandNotification = false,
            )
            verifySystemNotificationDoesNotExist("Eyebrowse Fenix")
        }.closeNotificationTray {}
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/451563
    @Ignore("Failing: Bug https://bugzilla.mozilla.org/show_bug.cgi?id=1813521")
    @SmokeTest
    @Test
    fun pauseResumeCancelDownloadTest() {
        downloadRobot {
            openPageAndDownloadFile(url = downloadTestPage.toUri(), downloadFile = "3GB.zip")
        }
        mDevice.openNotification()
        notificationShade {
            verifySystemNotificationExists("Eyebrowse Fenix")
            expandNotificationMessage()
            clickDownloadNotificationControlButton("PAUSE")
            verifySystemNotificationExists("Download paused")
            clickDownloadNotificationControlButton("RESUME")
            clickDownloadNotificationControlButton("CANCEL")
            verifySystemNotificationDoesNotExist("3GB.zip")
            mDevice.pressBack()
        }
        browserScreen {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            verifyEmptyDownloadsList()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2301474
    @Test
    fun openDownloadedFileFromDownloadsMenuTest() {
        downloadRobot {
            openPageAndDownloadFile(url = downloadTestPage.toUri(), downloadFile = "web_icon.png")
            verifyDownloadCompleteNotificationPopup()
        }
        browserScreen {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            verifyDownloadedFileName("web_icon.png")
            openDownloadedFile("web_icon.png")
            verifyPhotosAppOpens()
            mDevice.pressBack()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1114970
    @Test
    fun deleteDownloadedFileTest() {
        downloadRobot {
            openPageAndDownloadFile(url = downloadTestPage.toUri(), downloadFile = "smallZip.zip")
        }
        browserScreen {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            verifyDownloadedFileName("smallZip.zip")
            deleteDownloadedItem("smallZip.zip")
            clickSnackbarButton("UNDO")
            verifyDownloadedFileName("smallZip.zip")
            deleteDownloadedItem("smallZip.zip")
            verifyEmptyDownloadsList()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2302662
    @Test
    fun deleteMultipleDownloadedFilesTest() {
        val firstDownloadedFile = "smallZip.zip"
        val secondDownloadedFile = "textfile.txt"

        downloadRobot {
            openPageAndDownloadFile(url = downloadTestPage.toUri(), downloadFile = firstDownloadedFile)
            verifyDownloadedFileName(firstDownloadedFile)
        }.closeDownloadPrompt {
        }.clickDownloadLink(secondDownloadedFile) {
            verifyDownloadPrompt(secondDownloadedFile)
        }.clickDownload {
            verifyDownloadedFileName(secondDownloadedFile)
        }
        browserScreen {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            verifyDownloadedFileName(firstDownloadedFile)
            verifyDownloadedFileName(secondDownloadedFile)
            longClickDownloadedItem(firstDownloadedFile)
            selectDownloadedItem(secondDownloadedFile)
            openMultiSelectMoreOptionsMenu()
            clickMultiSelectRemoveButton()
            clickSnackbarButton("UNDO")
            verifyDownloadedFileName(firstDownloadedFile)
            verifyDownloadedFileName(secondDownloadedFile)
            longClickDownloadedItem(firstDownloadedFile)
            selectDownloadedItem(secondDownloadedFile)
            openMultiSelectMoreOptionsMenu()
            clickMultiSelectRemoveButton()
            verifyEmptyDownloadsList()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2301537
    @Test
    fun fileDeletedFromStorageIsDeletedEverywhereTest() {
        downloadRobot {
            openPageAndDownloadFile(url = downloadTestPage.toUri(), downloadFile = "smallZip.zip")
            verifyDownloadCompleteNotificationPopup()
        }
        browserScreen {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            waitForDownloadsListToExist()
            verifyDownloadedFileName("smallZip.zip")
            deleteDownloadedFileOnStorage("smallZip.zip")
        }.exitDownloadsManagerToBrowser {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            verifyEmptyDownloadsList()
            exitMenu()
        }

        downloadRobot {
            openPageAndDownloadFile(url = downloadTestPage.toUri(), downloadFile = "smallZip.zip")
            verifyDownloadCompleteNotificationPopup()
        }
        browserScreen {
        }.openThreeDotMenu {
        }.openDownloadsManager {
            waitForDownloadsListToExist()
            verifyDownloadedFileName("smallZip.zip")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/457112
    @Test
    fun systemNotificationCantBeDismissedWhileInProgressTest() {
        downloadRobot {
            openPageAndDownloadFile(url = downloadTestPage.toUri(), downloadFile = "3GB.zip")
        }
        browserScreen {
        }.openNotificationShade {
            verifySystemNotificationExists("Eyebrowse Fenix")
            expandNotificationMessage()
            swipeDownloadNotification(direction = "Left", shouldDismissNotification = false)
            clickDownloadNotificationControlButton("PAUSE")
            swipeDownloadNotification(direction = "Right", shouldDismissNotification = false)
            clickDownloadNotificationControlButton("CANCEL")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2299297
    @Ignore("Failing, see: https://bugzilla.mozilla.org/show_bug.cgi?id=1842154")
    @Test
    fun notificationCanBeDismissedIfDownloadIsInterruptedTest() {
        downloadRobot {
            openPageAndDownloadFile(url = downloadTestPage.toUri(), downloadFile = "1GB.zip")
        }

        setNetworkEnabled(enabled = false)

        browserScreen {
        }.openNotificationShade {
            expandNotificationMessage()
            verifySystemNotificationExists("Download failed")
            swipeDownloadNotification("Left", true)
            verifySystemNotificationDoesNotExist("Eyebrowse Fenix")
        }.closeNotificationTray {}

        downloadRobot {
        }.closeDownloadPrompt {
            verifyDownloadPromptIsDismissed()
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/1632384
    @Test
    fun warningWhenClosingPrivateTabsWhileDownloadingTest() {
        homeScreen {
        }.togglePrivateBrowsingMode()
        downloadRobot {
            openPageAndDownloadFile(url = downloadTestPage.toUri(), downloadFile = "3GB.zip")
        }
        browserScreen {
        }.openTabDrawer {
            closeTab()
        }
        browserScreen {
            verifyCancelPrivateDownloadsPrompt("1")
            clickStayInPrivateBrowsingPromptButton()
        }.openNotificationShade {
            verifySystemNotificationExists("Eyebrowse Fenix")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2302663
    @Test
    fun cancelActivePrivateBrowsingDownloadsTest() {
        homeScreen {
        }.togglePrivateBrowsingMode()
        downloadRobot {
            openPageAndDownloadFile(url = downloadTestPage.toUri(), downloadFile = "3GB.zip")
        }
        browserScreen {
        }.openTabDrawer {
            closeTab()
        }
        browserScreen {
            verifyCancelPrivateDownloadsPrompt("1")
            clickCancelPrivateDownloadsPromptButton()
        }.openNotificationShade {
            verifySystemNotificationDoesNotExist("Eyebrowse Fenix")
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2048448
    // Save edited PDF file from the share overlay
    @SmokeTest
    @Test
    fun saveAsPdfFunctionalityTest() {
        val genericURL =
            TestAssetHelper.getGenericAsset(mockWebServer, 3)
        downloadFile = "pdfForm.pdf"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericURL.url) {
            clickPageObject(itemWithText("PDF form file"))
            waitForPageToLoad()
            fillPdfForm("Eyebrowse")
        }.openThreeDotMenu {
        }.clickShareButton {
        }.clickSaveAsPDF {
            verifyDownloadPrompt("pdfForm.pdf")
        }.clickDownload {
        }.clickOpen("application/pdf") {
            assertExternalAppOpens(GOOGLE_DOCS)
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/244125
    @Test
    fun restartDownloadFromAppNotificationAfterConnectionIsInterruptedTest() {
        downloadFile = "3GB.zip"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(downloadTestPage.toUri()) {
            waitForPageToLoad()
        }.clickDownloadLink(downloadFile) {
            verifyDownloadPrompt(downloadFile)
            setNetworkEnabled(false)
        }.clickDownload {
            verifyDownloadFailedPrompt(downloadFile)
            setNetworkEnabled(true)
            clickTryAgainButton()
        }
        browserScreen {
        }.openNotificationShade {
            verifySystemNotificationExists("Eyebrowse Fenix")
            expandNotificationMessage()
            clickDownloadNotificationControlButton("CANCEL")
        }
    }
}
