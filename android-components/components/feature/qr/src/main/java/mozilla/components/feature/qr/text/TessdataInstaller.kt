/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.qr.text

import android.content.Context
import mozilla.components.support.base.log.logger.Logger
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Makes sure the Tesseract language models exist on disk.
 *
 * Two sources, in order. First the APK assets, which is the fast path when a build bundles a model.
 * Then a download, because a model is 3 to 4 MB and bundling two hundred languages in an APK is not
 * possible. Downloaded models are cached, so this runs once per language per device.
 *
 * @param context Application context, used for the assets and the files directory.
 * @param modelBaseUrls Where to look for `<language>.traineddata`, in order. The first entry is our
 * own host; the second is the upstream Tesseract project, kept as a fallback until our host serves
 * the files.
 */
internal class TessdataInstaller(
    private val context: Context,
    private val modelBaseUrls: List<String> = DEFAULT_MODEL_BASE_URLS,
) {
    private val logger = Logger("TessdataInstaller")

    /**
     * Ensures every model in [languages] exists on disk.
     *
     * @return the data path to pass to [TextRecognizer], and the languages that are usable.
     */
    fun install(languages: List<String>): InstallResult {
        val dataPath = TextRecognizer.dataPath(context)
        val tessdata = File(dataPath, "tessdata")
        if (!tessdata.exists()) {
            tessdata.mkdirs()
        }
        val ready = mutableListOf<String>()
        languages.forEach { language ->
            val target = File(tessdata, "$language.traineddata")
            if (target.exists() && target.length() > 0) {
                ready += language
                return@forEach
            }
            val installed = copyFromAssets(language, target) || download(language, target)
            if (installed) ready += language
        }
        return InstallResult(dataPath = dataPath, languages = ready)
    }

    private fun copyFromAssets(language: String, target: File): Boolean {
        val assetPath = "tessdata/$language.traineddata"
        val available = runCatching { context.assets.list("tessdata").orEmpty().toList() }
            .getOrDefault(emptyList())
        if (available.none { assetPath.endsWith(it) }) return false
        return runCatching {
            context.assets.open(assetPath).use { input ->
                writeAtomically(target) { output -> input.copyTo(output) }
            }
            true
        }.getOrElse {
            logger.warn("Could not copy $assetPath from assets")
            false
        }
    }

    private fun download(language: String, target: File): Boolean {
        modelBaseUrls.forEach { base ->
            val url = base.trimEnd('/') + "/$language.traineddata"
            val ok = runCatching { fetch(url, target) }.getOrElse { error ->
                logger.warn("Download failed from $url: ${error.message}")
                false
            }
            if (ok) {
                logger.info("Downloaded $language model from $url")
                return true
            }
        }
        logger.warn("No model for $language, text recognition stays off for it")
        return false
    }

    private fun fetch(url: String, target: File): Boolean {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return false
            writeAtomically(target) { output ->
                connection.inputStream.use { input -> input.copyTo(output) }
            }
            true
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Writes to a temporary file and renames it, so a half-downloaded model never looks usable.
     */
    private fun writeAtomically(target: File, writer: (java.io.OutputStream) -> Unit) {
        val temporary = File(target.parentFile, target.name + ".tmp")
        temporary.outputStream().use(writer)
        if (target.exists()) target.delete()
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
    }

    /**
     * @property dataPath Directory Tesseract should read `tessdata` from.
     * @property languages The models that ended up available.
     */
    data class InstallResult(
        val dataPath: String,
        val languages: List<String>,
    )

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 60_000

        /**
         * Our host first, then upstream Tesseract as a fallback. Replace the fallback with our own
         * mirror once it serves the models, so the app talks to nobody else.
         */
        internal val DEFAULT_MODEL_BASE_URLS = listOf(
            "https://eyebrowse.in/models/tessdata",
            "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main",
        )
    }
}
