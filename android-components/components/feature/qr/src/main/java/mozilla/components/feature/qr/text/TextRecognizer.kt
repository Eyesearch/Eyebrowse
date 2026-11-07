/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.qr.text

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.googlecode.tesseract.android.TessBaseAPI
import mozilla.components.support.base.log.logger.Logger

/**
 * Recognises text in a captured frame and reports each word with its box.
 *
 * The engine is Tesseract, running fully on the device. Language models are not bundled in the
 * APK: [dataPath] is the directory that holds `tessdata/<language>.traineddata`, which the app
 * downloads per language. This keeps the APK small while still covering every language we ship.
 *
 * @param dataPath Directory containing the `tessdata` folder.
 */
class TextRecognizer(
    private val dataPath: String,
) {
    private val logger = Logger("TextRecognizer")
    private var engine: TessBaseAPI? = null
    private var language: String? = null

    /**
     * True when a language model was loaded and the engine is ready.
     */
    val isReady: Boolean
        get() = engine != null

    /**
     * Loads [language] (for example "eng" or "kan") from [dataPath]. Returns false when the model
     * is missing, in which case nothing is recognised rather than the app crashing.
     */
    @Synchronized
    fun load(language: String): Boolean {
        if (this.language == language && engine != null) return true
        close()
        return try {
            val api = TessBaseAPI()
            val initialised = api.init(dataPath, language)
            if (!initialised) {
                logger.warn("No Tesseract model for $language in $dataPath")
                api.end()
                false
            } else {
                engine = api
                this.language = language
                true
            }
        } catch (e: RuntimeException) {
            logger.error("Failed to load Tesseract model for $language", e)
            false
        }
    }

    /**
     * Recognises [bitmap] and returns its words in reading order. Returns an empty list when no
     * model is loaded or nothing was found.
     */
    @Synchronized
    fun recognize(bitmap: Bitmap): List<ScanWord> {
        val api = engine ?: return emptyList()
        return try {
            api.setImage(bitmap)
            val words = mutableListOf<ScanWord>()
            var lineIndex = -1
            var previousTop = -1
            var iterator = api.resultIterator
            while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD)) {
                val text = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                    ?.trim()
                    .orEmpty()
                val box = iterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                val confidence = iterator.confidence(TessBaseAPI.PageIteratorLevel.RIL_WORD).toInt()
                if (text.isEmpty() || box == null || box.isEmpty) continue
                // A new line starts when the word sits clearly below the previous one.
                if (previousTop < 0 || box.top > previousTop + box.height() / 2) {
                    lineIndex++
                    previousTop = box.top
                }
                words += ScanWord(
                    text = text,
                    bounds = Rect(box),
                    confidence = confidence,
                    lineIndex = lineIndex,
                )
            }
            words
        } catch (e: RuntimeException) {
            logger.error("Recognition failed", e)
            emptyList()
        } finally {
            api.clear()
        }
    }

    /**
     * Releases the native engine. Call this from the owner's teardown.
     */
    @Synchronized
    fun close() {
        engine?.end()
        engine = null
        language = null
    }

    companion object {
        /**
         * @return the directory that must contain `tessdata` for [TextRecognizer].
         */
        fun dataPath(context: Context): String = context.filesDir.absolutePath
    }
}
