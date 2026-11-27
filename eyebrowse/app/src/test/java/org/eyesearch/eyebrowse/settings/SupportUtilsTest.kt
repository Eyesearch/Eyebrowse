/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.settings

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class SupportUtilsTest {

    @Test
    fun getSumoURLForTopic() {
        assertEquals(
            "https://eyesearch.github.io/Eyebrowse/",
            SupportUtils.getSumoURLForTopic(mockContext("1.6"), SupportUtils.SumoTopic.PRIVATE_BROWSING_MYTHS, Locale("en", "US")),
        )
        assertEquals(
            "https://eyesearch.github.io/Eyebrowse/",
            SupportUtils.getSumoURLForTopic(mockContext("2 0"), SupportUtils.SumoTopic.TRACKING_PROTECTION, Locale("fr")),
        )
        assertEquals(
            "https://eyesearch.github.io/Eyebrowse/",
            SupportUtils.WHATS_NEW_URL,
        )
    }

    @Test
    fun getGenericSumoURLForTopic() {
        assertEquals(
            "https://eyesearch.github.io/Eyebrowse/",
            SupportUtils.getGenericSumoURLForTopic(SupportUtils.SumoTopic.HELP, Locale("en", "GB")),
        )
        assertEquals(
            "https://eyesearch.github.io/Eyebrowse/",
            SupportUtils.getGenericSumoURLForTopic(SupportUtils.SumoTopic.YOUR_RIGHTS, Locale("de")),
        )
    }

    @Test
    fun getMozillaPageUrl() {
        assertEquals(
            "https://eyesearch.github.io/Eyebrowse/",
            SupportUtils.getMozillaPageUrl(SupportUtils.MozillaPage.MANIFESTO, Locale("en", "US")),
        )
        assertEquals(
            "https://eyesearch.github.io/Eyebrowse/",
            SupportUtils.getMozillaPageUrl(SupportUtils.MozillaPage.PRIVATE_NOTICE, Locale("zh")),
        )
    }

    private fun mockContext(versionName: String): Context {
        val context: Context = mockk()
        val packageManager: PackageManager = mockk()
        val packageInfo = PackageInfo()

        every { context.packageName } returns "org.mozilla.fenix"
        every { context.packageManager } returns packageManager
        @Suppress("DEPRECATION")
        every { packageManager.getPackageInfo("org.mozilla.fenix", 0) } returns packageInfo
        packageInfo.versionName = versionName

        return context
    }
}
