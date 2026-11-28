/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.core

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.webextension.WebExtension
import mozilla.components.concept.engine.webextension.WebExtensionInstallException

/** EyeBlock installs the official signed uBlock Origin release only after user opt-in. */
object EyeblockSupport {

    /** The id uBlock Origin declares in its own manifest; the engine keys the install on it. */
    const val EXTENSION_ID = "uBlock0@raymondhill.net"

    internal const val EXTENSION_VERSION = "1.77.4"
    internal const val EXTENSION_URL =
        "https://github.com/gorhill/uBlock/releases/download/1.77.4/uBlock0_1.77.4.firefox.signed.xpi"
    private const val TAG = "EyeblockSupport"
    private const val RESTORE_WAIT_MS = 3000L

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
    private var desiredEnabled = false
    private var installPending = false
    private var operationInProgress = false

    enum class Status {
        CHECKING, DOWNLOADING, INSTALLING, ENABLING, DISABLING,
        ENABLED, PRIVATE_UNAVAILABLE, DISABLED, ERROR;

        val busy: Boolean
            get() = this in setOf(CHECKING, DOWNLOADING, INSTALLING, ENABLING, DISABLING)
        val active: Boolean
            get() = this == ENABLED || this == PRIVATE_UNAVAILABLE
    }

    private val mutableStatus = MutableStateFlow(Status.CHECKING)
    val status = mutableStatus.asStateFlow()

    // A safe, user-visible failure reason; no URLs, credentials or raw exception messages.
    internal var failureDetail: String? = null
        private set

    internal fun describeFailure(error: Throwable): String = when (error) {
        is WebExtensionInstallException.NotSigned -> "Extension signature rejected"
        is WebExtensionInstallException.NetworkFailure -> "Download failed; check connection"
        is WebExtensionInstallException.CorruptFile -> "Extension package rejected"
        is WebExtensionInstallException.Incompatible -> "Extension incompatible with browser engine"
        is WebExtensionInstallException.Blocklisted -> "Extension blocked by engine"
        is WebExtensionInstallException.UserCancelled -> "Installation permission rejected"
        else -> "Engine error: ${error.javaClass.simpleName}"
    }

    private fun reportState(extension: WebExtension, target: Boolean) {
        if (extension.isEnabled() != target) failureDetail = "Engine did not confirm requested state"
        mutableStatus.value = when {
            extension.isEnabled() != target -> Status.ERROR
            !target -> Status.DISABLED
            !extension.isAllowedInPrivateBrowsing() -> Status.PRIVATE_UNAVAILABLE
            else -> Status.ENABLED
        }
    }

    fun install(engine: Engine, enabled: Boolean, shieldDefault: Boolean = true) {
        installShield(engine, shieldEnabled = shieldDefault)
        setEnabled(engine, enabled)
    }

    /** Only query existing extensions when off. Never download in response to an off request. */
    fun setEnabled(engine: Engine, enabled: Boolean) {
        desiredEnabled = enabled
        if (operationInProgress) return
        operationInProgress = true
        failureDetail = null
        mutableStatus.value = Status.CHECKING
        engine.listInstalledWebExtensions(
            onSuccess = { extensions ->
                val extension = extensions.firstOrNull { it.id == EXTENSION_ID }
                when {
                    extension == null && !desiredEnabled -> {
                        operationInProgress = false
                        mutableStatus.value = Status.DISABLED
                    }
                    extension == null -> downloadAndInstall(engine)
                    desiredEnabled && extension.isBuiltIn() -> {
                        // Old APKs shipped a built-in copy. Replace it only after explicit opt-in.
                        mutableStatus.value = Status.INSTALLING
                        engine.uninstallWebExtension(
                            extension,
                            onSuccess = {
                                if (desiredEnabled) {
                                    downloadAndInstall(engine)
                                } else {
                                    operationInProgress = false
                                    mutableStatus.value = Status.DISABLED
                                }
                            },
                            onError = { _, error -> fail(error) },
                        )
                    }
                    else -> applyDesiredState(engine, extension)
                }
            },
            onError = { error -> fail(error) },
        )
    }

    private fun downloadAndInstall(engine: Engine) {
        installPending = true
        mutableStatus.value = Status.DOWNLOADING
        // Gecko downloads the complete XPI, verifies its signature, requests permission,
        // and only then returns the installed extension. No artificial percent is shown.
        engine.installWebExtension(
            url = EXTENSION_URL,
            onSuccess = { extension ->
                installPending = false
                if (extension.id != EXTENSION_ID) {
                    engine.uninstallWebExtension(
                        extension,
                        onSuccess = { fail(IllegalStateException("Unexpected extension identity")) },
                        onError = { _, error -> fail(error) },
                    )
                } else {
                    applyDesiredState(engine, extension)
                }
            },
            onError = { error ->
                installPending = false
                if (!desiredEnabled) {
                    operationInProgress = false
                    mutableStatus.value = Status.DISABLED
                } else {
                    fail(error)
                }
            },
        )
    }

    /** The Settings switch is the opt-in for the pinned signed XPI, never other add-ons. */
    fun handleInstallPermission(extension: WebExtension, respond: (Boolean) -> Unit): Boolean {
        if (!installPending || extension.id != EXTENSION_ID) return false
        val approved = desiredEnabled && extension.getMetadata()?.version == EXTENSION_VERSION
        if (approved) mutableStatus.value = Status.INSTALLING
        respond(approved)
        return true
    }

    private fun fail(error: Throwable) {
        installPending = false
        operationInProgress = false
        failureDetail = "${mutableStatus.value.name}: ${describeFailure(error)}"
        mutableStatus.value = Status.ERROR
        Log.e(TAG, "EyeBlock operation failed: $failureDetail", error)
    }

    /** Retry disabled-state reconciliation, but never repeat a failed download on foreground return. */
    fun retryIfNeeded(engine: Engine, enabled: Boolean) {
        if ((!enabled && mutableStatus.value == Status.ERROR) ||
            (enabled && mutableStatus.value == Status.PRIVATE_UNAVAILABLE)
        ) {
            setEnabled(engine, enabled)
        }
    }

    /** Reduce restored-tab startup races without waiting indefinitely. */
    suspend fun awaitInitialState() {
        withTimeoutOrNull(RESTORE_WAIT_MS) {
            status.first { !it.busy }
        }
    }

    private fun applyDesiredState(
        engine: Engine,
        extension: WebExtension,
        privatePermissionAttempted: Boolean = false,
    ) {
        val target = desiredEnabled
        mutableStatus.value = if (target) Status.ENABLING else Status.DISABLING
        if (target && !privatePermissionAttempted && !extension.isAllowedInPrivateBrowsing()) {
            engine.setAllowedInPrivateBrowsing(
                extension,
                allowed = true,
                onSuccess = { updated -> applyDesiredState(engine, updated, true) },
                onError = { error ->
                    Log.w(TAG, "EyeBlock private permission failed", error)
                    applyDesiredState(engine, extension, true)
                },
            )
            return
        }
        if (extension.isEnabled() == target) {
            operationInProgress = false
            reportState(extension, target)
            Log.i(TAG, "EyeBlock enabled=$target; private=${extension.isAllowedInPrivateBrowsing()}")
            return
        }

        val success: (WebExtension) -> Unit = { updated ->
            Log.i(TAG, "EyeBlock enabled=${updated.isEnabled()}; private=${updated.isAllowedInPrivateBrowsing()}")
            if (desiredEnabled != target) {
                applyDesiredState(engine, updated, privatePermissionAttempted)
            } else {
                operationInProgress = false
                reportState(updated, target)
                if (updated.isEnabled() != target) {
                    Log.e(TAG, "EyeBlock did not reach the requested state")
                }
            }
        }
        val failure: (Throwable) -> Unit = { error ->
            fail(error)
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
