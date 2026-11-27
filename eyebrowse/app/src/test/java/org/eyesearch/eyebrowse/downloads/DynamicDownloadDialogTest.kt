/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.downloads

import android.webkit.MimeTypeMap
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.downloads.DynamicDownloadDialog.Companion.getCannotOpenFileErrorMessage
import org.eyesearch.eyebrowse.helpers.EyebrowseRobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(EyebrowseRobolectricTestRunner::class)
class DynamicDownloadDialogTest {

    @Test
    fun `WHEN calling getCannotOpenFileErrorMessage THEN should return the error message for the download file type`() {
        val download = DownloadState(url = "", fileName = "image.gif")

        shadowOf(MimeTypeMap.getSingleton()).apply {
            addExtensionMimeTypeMapping(".gif", "image/gif")
        }

        val expected = testContext.getString(
            R.string.mozac_feature_downloads_open_not_supported1,
            "gif",
        )

        val result = getCannotOpenFileErrorMessage(testContext, download)
        assertEquals(expected, result)
    }
}
