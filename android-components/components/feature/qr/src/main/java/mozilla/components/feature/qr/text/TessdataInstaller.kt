/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.qr.text

import android.content.Context
import java.io.File

/**
 * Copies the language models bundled in the APK assets into the directory Tesseract reads from.
 *
 * Tesseract needs a real file at `<dataPath>/tessdata/<language>.traineddata`; it cannot read an
 * asset directly. Copying once per language keeps the first scan slow and every later scan fast.
 *
 * @param context Application context, used to read the assets and to find the files directory.
 */
internal class TessdataInstaller(private val context: Context) {

    /**
     * Ensures every model in [languages] exists on disk.
     *
     * @return the data path to pass to [TextRecognizer].
     */
    fun install(languages: List<String>): String {
        val dataPath = TextRecognizer.dataPath(context)
        val tessdata = File(dataPath, "tessdata")
        if (!tessdata.exists()) {
            tessdata.mkdirs()
        }
        languages.forEach { language ->
            val target = File(tessdata, "$language.traineddata")
            if (target.exists() && target.length() > 0) return@forEach
            copyAsset("tessdata/$language.traineddata", target)
        }
        return dataPath
    }

    private fun copyAsset(assetPath: String, target: File) {
        val assets = context.assets
        val available = runCatching { assets.list("tessdata").orEmpty().toList() }.getOrDefault(emptyList())
        if (available.none { assetPath.endsWith(it) }) return
        runCatching {
            assets.open(assetPath).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }.onFailure {
            if (target.exists()) target.delete()
        }
    }
}
