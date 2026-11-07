/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.qr.text

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.googlecode.tesseract.android.TessBaseAPI
import mozilla.components.support.base.log.logger.Logger
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Recognises text in a captured frame and reports each word with its box.
 *
 * Two engines, in order:
 *
 * 1. ML Kit's on-device Latin recogniser. It is a build dependency, so it ships inside the APK and
 *    works offline from the first scan: nothing is downloaded, nothing is stored.
 * 2. Tesseract, but only when ML Kit found nothing at all. ML Kit publishes recognisers for Latin,
 *    Devanagari, Chinese, Japanese and Korean scripts and none of them reads Kannada, so a frame of
 *    Kannada text is exactly the case where the Latin pass comes back empty. The Kannada model
 *    ships inside the APK as an asset; it is unpacked to the app's own files directory once, on the
 *    device, because Tesseract reads a model from a path and not from a stream.
 *
 * Nothing is written outside the app's private files directory, and no frame is ever saved.
 *
 * @param context Any context; only the application context is held.
 */
class TextRecognizer(context: Context) {

    private val logger = Logger("TextRecognizer")
    private val appContext = context.applicationContext
    private val latin = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Tesseract reads a model from disk and is slow, so it never runs on the main thread.
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var closed = false

    private var tesseract: TessBaseAPI? = null

    // Set once when the model cannot be installed, so a broken install is not retried every frame.
    private var kannadaUnavailable = false

    /**
     * Recognises [bitmap] and reports its words in reading order.
     *
     * @param bitmap The captured frame, already scaled down to a sensible size. The caller keeps
     * ownership and may recycle it as soon as [onResult] runs.
     * @param onResult Called on the main thread with the words, empty when nothing was found or the
     * engine failed. Never called after [close].
     */
    fun recognize(bitmap: Bitmap, onResult: (List<ScanWord>) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        latin.process(image)
            .addOnSuccessListener { text ->
                val words = text.toScanWords()
                if (words.isEmpty()) {
                    recognizeKannada(bitmap, onResult)
                } else {
                    onResult(words)
                }
            }
            .addOnFailureListener { error ->
                logger.warn("Latin text recognition failed: ${error.message}")
                recognizeKannada(bitmap, onResult)
            }
    }

    /**
     * Runs the Kannada pass off the main thread and reports back on it.
     */
    private fun recognizeKannada(bitmap: Bitmap, onResult: (List<ScanWord>) -> Unit) {
        if (closed) return
        worker.execute {
            val engine = kannadaEngine()
            val words = if (engine == null) {
                emptyList()
            } else {
                runCatching { readWords(engine, bitmap) }
                    .onFailure { logger.warn("Kannada recognition failed: ${it.message}") }
                    .getOrDefault(emptyList())
            }
            if (closed) return@execute
            mainHandler.post { onResult(words) }
        }
    }

    /**
     * @return a ready Tesseract engine for Kannada, or null when the model is not in this build.
     */
    private fun kannadaEngine(): TessBaseAPI? {
        tesseract?.let { return it }
        if (kannadaUnavailable) return null

        return try {
            installKannadaModel()
            val engine = TessBaseAPI()
            if (!engine.init(appContext.filesDir.absolutePath, KANNADA_LANGUAGE)) {
                engine.end()
                throw IllegalStateException("$KANNADA_MODEL was not accepted by Tesseract")
            }
            engine.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO
            tesseract = engine
            engine
        } catch (error: Exception) {
            logger.warn("Kannada model unavailable: ${error.message}")
            kannadaUnavailable = true
            null
        }
    }

    /**
     * Unpacks the bundled model to `filesDir/tessdata` once, because Tesseract wants a path.
     */
    private fun installKannadaModel() {
        val target = File(File(appContext.filesDir, TESSDATA_DIRECTORY), KANNADA_MODEL)
        if (target.exists()) return

        target.parentFile?.mkdirs()
        val partial = File(target.parentFile, "$KANNADA_MODEL.part")
        appContext.assets.open("$TESSDATA_DIRECTORY/$KANNADA_MODEL").use { input ->
            partial.outputStream().use { output -> input.copyTo(output) }
        }
        // Rename only once the whole file is on disk, so a half-written model is never used.
        if (!partial.renameTo(target)) {
            partial.delete()
            throw IOException("could not install $KANNADA_MODEL")
        }
    }

    /**
     * Reads words and their boxes out of the engine.
     */
    private fun readWords(engine: TessBaseAPI, bitmap: Bitmap): List<ScanWord> {
        engine.setImage(bitmap)
        val words = mutableListOf<ScanWord>()
        val iterator = engine.resultIterator ?: return emptyList()
        try {
            var lineIndex = -1
            var previousLine: String? = null
            iterator.begin()
            do {
                val text = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD)?.trim().orEmpty()
                if (text.isEmpty()) continue

                // Words on one line share an index, which is how the overlay keeps reading order.
                val line = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE).orEmpty()
                if (line != previousLine) {
                    lineIndex++
                    previousLine = line
                }

                val box = iterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                if (box == null || box.isEmpty) continue
                words += ScanWord(
                    text = text,
                    bounds = Rect(box),
                    confidence = iterator.confidence(TessBaseAPI.PageIteratorLevel.RIL_WORD).toInt(),
                    lineIndex = lineIndex,
                )
            } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD))
        } finally {
            iterator.delete()
            engine.clear()
        }
        return words
    }

    /**
     * Releases both engines.
     */
    fun close() {
        if (closed) return
        closed = true
        latin.close()
        runCatching {
            worker.execute {
                tesseract?.end()
                tesseract = null
            }
            worker.shutdown()
        }
    }

    private companion object {
        const val KANNADA_LANGUAGE = "kan"
        const val KANNADA_MODEL = "kan.traineddata"
        const val TESSDATA_DIRECTORY = "tessdata"
    }
}

/**
 * Flattens an ML Kit result into words with their boxes, in reading order.
 */
private fun Text.toScanWords(): List<ScanWord> {
    val words = mutableListOf<ScanWord>()
    var lineIndex = -1
    for (block in textBlocks) {
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
    return words
}
