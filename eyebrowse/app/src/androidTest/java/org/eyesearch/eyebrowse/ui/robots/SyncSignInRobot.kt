/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.ui.robots

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers.allOf
import org.eyesearch.eyebrowse.helpers.Constants.TAG
import org.eyesearch.eyebrowse.helpers.MatcherHelper.assertUIObjectExists
import org.eyesearch.eyebrowse.helpers.MatcherHelper.itemWithResId
import org.eyesearch.eyebrowse.helpers.TestAssetHelper.waitingTime
import org.eyesearch.eyebrowse.helpers.TestHelper.mDevice
import org.eyesearch.eyebrowse.helpers.TestHelper.packageName
import org.eyesearch.eyebrowse.helpers.click

/**
 * Implementation of Robot Pattern for Sync Sign In sub menu.
 */
class SyncSignInRobot {

    fun verifyTurnOnSyncMenu() {
        Log.i(TAG, "verifyTurnOnSyncMenu: Waiting for $waitingTime ms for sign in to sync menu to exist")
        mDevice.findObject(UiSelector().resourceId("$packageName:id/container")).waitForExists(waitingTime)
        Log.i(TAG, "verifyTurnOnSyncMenu: Waited for $waitingTime ms for sign in to sync menu to exist")
        assertUIObjectExists(
            itemWithResId("$packageName:id/signInScanButton"),
            itemWithResId("$packageName:id/signInEmailButton"),
        )
    }

    class Transition {
        fun goBack(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            Log.i(TAG, "goBack: Trying to click the navigate up button")
            goBackButton().click()
            Log.i(TAG, "goBack: Clicked the navigate up button")

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }
    }
}

private fun goBackButton() =
    onView(allOf(withContentDescription("Navigate up")))
