/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.qr.text

import android.graphics.Rect

/**
 * One recognised word and where it sits in the captured image.
 *
 * @property text The word as recognised.
 * @property bounds Where it is, in image pixel coordinates. This is what makes a word
 * touchable: the overlay draws a box here and a tap is hit-tested against it.
 * @property confidence Recognition confidence, 0 to 100.
 * @property lineIndex Words that were on the same line share this index, which is what lets us
 * rebuild a line and decide whether a whole line is a link.
 */
data class ScanWord(
    val text: String,
    val bounds: Rect,
    val confidence: Int,
    val lineIndex: Int,
)

/**
 * A recognised line, built from the [ScanWord]s that share a [ScanWord.lineIndex].
 *
 * @property text The line with its words joined by single spaces.
 * @property bounds The union of the word bounds.
 * @property words The words in reading order.
 */
data class ScanLine(
    val text: String,
    val bounds: Rect,
    val words: List<ScanWord>,
) {
    companion object {
        /**
         * Groups words into lines by [ScanWord.lineIndex], keeping their order.
         */
        fun fromWords(words: List<ScanWord>): List<ScanLine> = words
            .groupBy { it.lineIndex }
            .toSortedMap()
            .map { (_, lineWords) ->
                val sorted = lineWords.sortedBy { it.bounds.left }
                ScanLine(
                    text = sorted.joinToString(" ") { it.text },
                    bounds = sorted.map { it.bounds }.reduce { acc, rect ->
                        Rect(acc).apply { union(rect) }
                    },
                    words = sorted,
                )
            }
    }
}

/**
 * The run of recognised words the user has selected, in reading order.
 *
 * Selection is word level rather than character level: the recogniser reports one box per word, so
 * the handles snap from word to word. For links and addresses that is what the user wants, because
 * half a URL is never useful.
 *
 * @property words Every word in the frame, in reading order. The selection is an index range into
 * this list, which is why a selection keeps its own copy of it.
 * @property startIndex Index of the first selected word.
 * @property endIndex Index of the last selected word, never smaller than [startIndex].
 */
data class ScanSelection(
    val words: List<ScanWord>,
    val startIndex: Int,
    val endIndex: Int,
) {
    /** The selected words, first to last. */
    val selectedWords: List<ScanWord>
        get() = words.subList(startIndex, endIndex + 1)

    /** The selected words joined by single spaces, which is what Copy and Search act on. */
    val text: String
        get() = selectedWords.joinToString(" ") { it.text }

    /** The smallest rectangle holding every selected word, in image pixel coordinates. */
    val bounds: Rect
        get() = selectedWords
            .map { it.bounds }
            .reduce { acc, rect -> Rect(acc).apply { union(rect) } }

    /**
     * @return true when [index] points at one of the selected words.
     */
    fun contains(index: Int): Boolean = index in startIndex..endIndex

    /**
     * Moves the start handle to [index], never past the end handle.
     */
    fun withStart(index: Int): ScanSelection = copy(startIndex = index.coerceIn(0, endIndex))

    /**
     * Moves the end handle to [index], never before the start handle.
     */
    fun withEnd(index: Int): ScanSelection =
        copy(endIndex = index.coerceIn(startIndex, words.lastIndex))

    companion object {
        /**
         * @return a selection holding only the word at [index], or null when there is no such word.
         */
        fun at(words: List<ScanWord>, index: Int): ScanSelection? =
            if (index in words.indices) ScanSelection(words, index, index) else null
    }
}
