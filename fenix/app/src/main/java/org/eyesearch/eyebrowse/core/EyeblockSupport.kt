/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.core

import android.util.Log
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.webextension.WebExtension

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

    private const val EXTENSION_URL = "resource://android/assets/extensions/ublock_origin/"
    private const val TAG = "EyeblockSupport"

    /**
     * EyeShield, the small extension of ours that cancels every request to Mozilla inside the
     * browser. It is separate from the blocker on purpose: the blocker stops ads and trackers,
     * this stops the engine talking to the people who built it.
     */
    private const val PREFS = "eyebrowse_shield"
    private const val KEY_SHIELD = "shield_enabled"

    private const val SHIELD_ID = "eyeshield@eyesearch.org"
    private const val SHIELD_URL = "resource://android/assets/extensions/eyeshield/"

    // Engine extension callbacks and Settings changes are dispatched on the main thread.
    // Serialize operations so a quick toggle cannot overwrite a newer preference.
    private var desiredEnabled = true
    private var operationInProgress = false

    fun install(engine: Engine, enabled: Boolean, shieldDefault: Boolean = true) {
        installShield(engine, shieldEnabled = shieldDefault)
        setEnabled(engine, enabled)
    }

    /** Reconcile the actual extension state, installing it if it is missing. */
    fun setEnabled(engine: Engine, enabled: Boolean) {
        desiredEnabled = enabled
        if (operationInProgress) return
        operationInProgress = true
        engine.installBuiltInWebExtension(
            id = EXTENSION_ID,
            url = EXTENSION_URL,
            onSuccess = { extension ->
                Log.i(TAG, "EyeBlock installed: ${extension.id}")
                if (extension.isAllowedInPrivateBrowsing()) {
                    applyDesiredState(engine, extension)
                } else {
                    engine.setAllowedInPrivateBrowsing(
                        extension,
                        allowed = true,
                        onSuccess = { updated -> applyDesiredState(engine, updated) },
                        onError = { error ->
                            Log.w(TAG, "EyeBlock private permission failed", error)
                            applyDesiredState(engine, extension)
                        },
                    )
                }
            },
            onError = { error ->
                operationInProgress = false
                Log.e(TAG, "EyeBlock install failed; tracking protection remains available", error)
            },
        )
    }

    private fun applyDesiredState(engine: Engine, extension: WebExtension) {
        val target = desiredEnabled
        if (extension.isEnabled() == target) {
            operationInProgress = false
            Log.i(TAG, "EyeBlock enabled=$target; private=${extension.isAllowedInPrivateBrowsing()}")
            return
        }

        val success: (WebExtension) -> Unit = { updated ->
            Log.i(TAG, "EyeBlock enabled=${updated.isEnabled()}; private=${updated.isAllowedInPrivateBrowsing()}")
            if (desiredEnabled != target) {
                applyDesiredState(engine, updated)
            } else {
                operationInProgress = false
                if (updated.isEnabled() != target) {
                    Log.e(TAG, "EyeBlock did not reach the requested state")
                }
            }
        }
        val failure: (Throwable) -> Unit = { error ->
            operationInProgress = false
            Log.e(TAG, "EyeBlock state change failed", error)
            if (desiredEnabled != target) setEnabled(engine, desiredEnabled)
        }
        if (target) {
            engine.enableWebExtension(extension, onSuccess = success, onError = failure)
        } else {
            engine.disableWebExtension(extension, onSuccess = success, onError = failure)
        }
    }

    /** Whether EyeShield should run. Stored in the app's own preferences, default on. */
    fun isShieldEnabled(context: android.content.Context): Boolean =
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .getBoolean(KEY_SHIELD, true)

    fun setShieldEnabled(context: android.content.Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHIELD, enabled).apply()
    }

    /** Turns the running extension on or off without a restart. */
    fun setShieldRunning(engine: Engine, enabled: Boolean) {
        engine.listInstalledWebExtensions(
            onSuccess = { extensions ->
                val extension = extensions.firstOrNull { it.id == SHIELD_ID } ?: return@listInstalledWebExtensions
                if (enabled) {
                    engine.enableWebExtension(extension, onSuccess = { }, onError = { })
                } else {
                    engine.disableWebExtension(extension, onSuccess = { }, onError = { })
                }
            },
            onError = { error -> Log.w(TAG, "could not list extensions: ${error.message}") },
        )
    }

    /**
     * EyeShield is installed whatever the blocker switch says. Turning EyeBlock off is a choice
     * about ads; it is not a choice about whether our browser is allowed to phone Mozilla.
     */
    private fun installShield(engine: Engine, shieldEnabled: Boolean) {
        engine.installBuiltInWebExtension(
            id = SHIELD_ID,
            url = SHIELD_URL,
            onSuccess = { extension ->
                engine.setAllowedInPrivateBrowsing(extension, allowed = true)
                if (!shieldEnabled) {
                    engine.disableWebExtension(extension, onSuccess = { }, onError = { })
                }
            },
            onError = { error ->
                Log.w(TAG, "EyeShield could not be installed: ${error.message}")
            },
        )
    }

}
