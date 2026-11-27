/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.settings.account

import android.content.Intent
import androidx.navigation.NavController
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.eyesearch.eyebrowse.BrowserDirection
import org.eyesearch.eyebrowse.ext.components
import org.eyesearch.eyebrowse.helpers.EyebrowseRobolectricTestRunner
import org.eyesearch.eyebrowse.utils.Settings

@RunWith(EyebrowseRobolectricTestRunner::class)
class AuthCustomTabActivityTest {

    @Test
    fun `navigateToBrowserOnColdStart does nothing for AuthCustomTabActivity`() {
        val activity = spyk(AuthCustomTabActivity())

        val settings: Settings = mockk()
        every { settings.shouldReturnToBrowser } returns true
        every { activity.components.settings.shouldReturnToBrowser } returns true
        every { activity.openToBrowser(any(), any()) } returns Unit

        activity.navigateToBrowserOnColdStart()

        verify(exactly = 0) { activity.openToBrowser(BrowserDirection.FromGlobal) }
    }

    @Test
    fun `navigateToHome does nothing for AuthCustomTabActivity`() {
        val activity = spyk(AuthCustomTabActivity())
        val navHostController: NavController = mockk()

        activity.navigateToHome(navHostController)
        verify { navHostController wasNot Called }
    }

    @Test
    fun `handleNewIntent does nothing for AuthCustomTabActivity`() {
        val activity = spyk(AuthCustomTabActivity())
        val intent: Intent = mockk(relaxed = true)

        activity.handleNewIntent(intent)
        verify { intent wasNot Called }
    }
}
