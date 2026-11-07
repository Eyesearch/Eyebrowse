/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.qr.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkDetectorTest {

    @Test
    fun `adds a scheme to a bare domain`() {
        assertEquals("https://eyebrowse.in", LinkDetector.toUrl("eyebrowse.in"))
    }

    @Test
    fun `keeps a scheme that is already there`() {
        assertEquals("http://eyebrowse.in/a", LinkDetector.toUrl("http://eyebrowse.in/a"))
    }

    @Test
    fun `joins an address the recogniser split into words`() {
        assertEquals("https://www.eyebrowse.in", LinkDetector.toUrl("www .eyebrowse. in"))
    }

    @Test
    fun `does not hand the app a link it cannot open`() {
        // The result screen only accepts web links, so an email is copied rather than opened.
        assertNull(LinkDetector.toUrl("hello@eyebrowse.in"))
        assertNull(LinkDetector.toUrl("mailto:hello@eyebrowse.in"))
    }

    @Test
    fun `refuses plain text`() {
        assertNull(LinkDetector.toUrl("hello there"))
        assertNull(LinkDetector.toUrl(""))
    }
}
