/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.core

import android.util.Log
import mozilla.components.concept.engine.Engine

/**
 * EyeBlock: uBlock Origin, shipped inside the app as a built-in extension.
 *
 * Blocking ads cannot be done by stopping a download, because most ads are fetched by the page
 * itself. A content blocker works from inside the page: it cancels the page's own ad requests
 * before they leave, hides the ad boxes, and handles the players that stitch ads into a stream.
 * That is why this is an extension and not a list of addresses.
 *
 * The extension is bundled in the APK, so it works offline from the first launch and needs no
 * store, no account and no network. The EyeBlock switch in Settings decides whether it is enabled.
 */
object EyeblockSupport {

    /** The id uBlock Origin declares in its own manifest; the engine keys the install on it. */
    const val EXTENSION_ID = "uBlock0@raymondhill.net"

    private const val EXTENSION_URL = "resource://android/assets/extensions/ublock_origin.xpi"
    private const val TAG = "EyeblockSupport"

    /**
     * Installs the bundled blocker once, at startup, and leaves it enabled or disabled to match
     * the user's switch.
     */
    fun install(engine: Engine, enabled: Boolean) {
        engine.installBuiltInWebExtension(
            id = EXTENSION_ID,
            url = EXTENSION_URL,
            onSuccess = { extension ->
                // Blocking is just as relevant in private browsing, and a blocker that stops
                // working there is the first thing people notice.
                engine.setAllowedInPrivateBrowsing(extension, allowed = true)
                if (!enabled) {
                    engine.disableWebExtension(extension, onSuccess = { }, onError = { })
                }
            },
            onError = { error ->
                Log.w(TAG, "EyeBlock could not be installed: ${error.message}")
            },
        )
    }

    /**
     * Turns the installed blocker on or off, without restarting anything.
     */
    fun setEnabled(engine: Engine, enabled: Boolean) {
        engine.listInstalledWebExtensions(
            onSuccess = { extensions ->
                val extension = extensions.firstOrNull { it.id == EXTENSION_ID }
                if (extension == null) {
                    Log.w(TAG, "EyeBlock is not installed yet")
                    return@listInstalledWebExtensions
                }
                if (enabled) {
                    engine.enableWebExtension(extension, onSuccess = { }, onError = { })
                } else {
                    engine.disableWebExtension(extension, onSuccess = { }, onError = { })
                }
            },
            onError = { error ->
                Log.w(TAG, "EyeBlock could not be reached: ${error.message}")
            },
        )
    }
}
