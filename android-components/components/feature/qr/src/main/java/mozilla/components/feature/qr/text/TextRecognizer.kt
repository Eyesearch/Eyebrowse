/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.qr.text

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import mozilla.components.support.base.log.logger.Logger

/**
 * Recognises text in a captured frame and reports each word with its box.
 *
 * The engine is ML Kit's on-device recogniser. It is a build dependency, so it ships inside the APK
 * and works offline from the first scan: nothing is downloaded, nothing is stored, and no model
 * files live in this repository.
 *
 * The bundled Latin model covers English and the other Latin-script languages; ML Kit also ships
 * separate Chinese, Japanese, Korean and Devanagari recognisers which can be added the same way.
 */
class TextRecognizer {

    private val logger = Logger("TextRecognizer")
    private val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Recognises [bitmap] and reports its words in reading order.
     *
     * @param bitmap The captured frame, already scaled down to a sensible size.
     * @param onResult Called on the main thread with the words, empty when nothing was found or
     * the engine failed. Never called after [close].
     */
    fun recognize(bitmap: Bitmap, onResult: (List<ScanWord>) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        client.process(image)
            .addOnSuccessListener { text ->
                val words = mutableListOf<ScanWord>()
                var lineIndex = -1
                for (block in text.textBlocks) {
                    for (line in block.lines) {
                        lineIndex++
                        for (element in line.elements) {
                            val box = element.boundingBox
                            val content = element.text.trim()
                            if (box == null || box.isEmpty || content.isEmpty()) continue
                            words += ScanWord(
                                text = content,
                                bounds = Rect(box),
                                confidence = element.confidence.toInt(),
                                lineIndex = lineIndex,
                            )
                        }
                    }
                }
                onResult(words)
            }
            .addOnFailureListener { error ->
                logger.warn("Text recognition failed: ${error.message}")
                onResult(emptyList())
            }
    }

    /**
     * Releases the engine.
     */
    fun close() {
        client.close()
    }
}
