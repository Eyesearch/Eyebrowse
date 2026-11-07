/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.qr.text

import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanSelectionTest {

    private fun word(text: String, left: Int, line: Int = 0) = ScanWord(
        text = text,
        bounds = Rect(left, 0, left + 100, 40),
        confidence = 90,
        lineIndex = line,
    )

    private val words = listOf(
        word("www", 0),
        word("eyebrowse", 110),
        word("in", 220),
    )

    @Test
    fun `one word is selected on its own`() {
        val selection = ScanSelection.at(words, 1)!!

        assertEquals("eyebrowse", selection.text)
        assertEquals(1, selection.startIndex)
        assertEquals(1, selection.endIndex)
        assertTrue(selection.contains(1))
        assertFalse(selection.contains(2))
    }

    @Test
    fun `an empty frame selects nothing`() {
        assertNull(ScanSelection.at(emptyList(), 0))
        assertNull(ScanSelection.at(words, 3))
    }

    @Test
    fun `the end handle grows the selection word by word`() {
        val selection = ScanSelection.at(words, 0)!!.withEnd(2)

        assertEquals("www eyebrowse in", selection.text)
        assertEquals(2, selection.endIndex)
    }

    @Test
    fun `the start handle never crosses the end handle`() {
        val selection = ScanSelection.at(words, 1)!!.withStart(2)

        assertEquals(1, selection.startIndex)
        assertEquals(1, selection.endIndex)
    }

    @Test
    fun `the end handle never goes back past the start handle`() {
        val selection = ScanSelection.at(words, 1)!!.withEnd(0)

        assertEquals(1, selection.startIndex)
        assertEquals(1, selection.endIndex)
    }

    @Test
    fun `the bounds cover every selected word`() {
        val selection = ScanSelection.at(words, 0)!!.withEnd(2)

        assertEquals(Rect(0, 0, 320, 40), selection.bounds)
    }
}
