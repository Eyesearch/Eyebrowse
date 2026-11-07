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
