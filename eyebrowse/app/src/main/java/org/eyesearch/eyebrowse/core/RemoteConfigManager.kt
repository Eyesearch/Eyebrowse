/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.core

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.remoteConfig
import org.mozilla.fenix.BuildConfig

/**
 * Eyebrowse remote configuration.
 *
 * Backed by Firebase Remote Config, this holds the server-controlled kill-switches
 * (so a feature can be turned off without shipping an update) and the affiliate
 * links used by the app.
 *
 * Every getter falls back to the bundled defaults below, so the app behaves
 * correctly when Firebase is unavailable, when `google-services.json` has not been
 * added yet, or while the first fetch is still in flight.
 */
object RemoteConfigManager {

    private const val TAG = "RemoteConfigManager"

    const val KEY_ENABLE_ADBLOCKER = "enable_adblocker"
    const val KEY_ENABLE_VIDEO_PLAYER = "enable_video_player"
    const val KEY_MENU_SETTINGS_ENABLED = "menu_settings_enabled"
    const val KEY_SETTINGS_SEARCH_ENGINE_ENABLED = "settings_search_engine_enabled"
    const val KEY_APP_MIN_VERSION = "app_min_version"
    const val KEY_AMAZON_AFFILIATE_URL = "amazon_affiliate_url"

    /** Real-time updates while debugging, one hour of caching for release builds. */
    private const val MINIMUM_FETCH_INTERVAL_DEBUG_SECONDS = 0L
    private const val MINIMUM_FETCH_INTERVAL_RELEASE_SECONDS = 3600L

    private val defaultValues: Map<String, Any> = mapOf(
        KEY_ENABLE_ADBLOCKER to true,
        KEY_ENABLE_VIDEO_PLAYER to true,
        KEY_MENU_SETTINGS_ENABLED to true,
        KEY_SETTINGS_SEARCH_ENGINE_ENABLED to true,
        KEY_APP_MIN_VERSION to 1,
        KEY_AMAZON_AFFILIATE_URL to "https://www.amazon.in",
    )

    @Volatile
    private var remoteConfig: FirebaseRemoteConfig? = null

    /**
     * Initializes Remote Config. Safe to call more than once and safe to call when
     * Firebase has no configuration: in that case every getter keeps returning the
     * bundled default.
     */
    fun init(context: Context) {
        if (remoteConfig != null) {
            return
        }

        val applicationContext = context.applicationContext
        try {
            if (FirebaseApp.getApps(applicationContext).isEmpty()) {
                Log.w(TAG, "Firebase is not configured, using bundled defaults.")
                return
            }

            val config = Firebase.remoteConfig
            config.setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(minimumFetchIntervalSeconds())
                    .build(),
            )
            config.setDefaultsAsync(defaultValues).addOnCompleteListener {
                config.fetchAndActivate()
            }
            remoteConfig = config
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Remote Config unavailable, using bundled defaults.", e)
        }
    }

    private fun minimumFetchIntervalSeconds(): Long = if (BuildConfig.DEBUG) {
        MINIMUM_FETCH_INTERVAL_DEBUG_SECONDS
    } else {
        MINIMUM_FETCH_INTERVAL_RELEASE_SECONDS
    }

    /**
     * Returns the remote boolean for [key], falling back to the bundled default for
     * that key and finally to [default] when the key is unknown.
     */
    fun isFeatureEnabled(key: String, default: Boolean = true): Boolean {
        val fallback = defaultValues[key] as? Boolean ?: default
        return try {
            remoteConfig?.getBoolean(key) ?: fallback
        } catch (e: IllegalStateException) {
            Log.w(TAG, "No remote value for $key, using $fallback.", e)
            fallback
        }
    }

    /**
     * Returns the remote string for [key], falling back to the bundled default for
     * that key and finally to [default] when the key is unknown.
     */
    fun getString(key: String, default: String = ""): String {
        val fallback = defaultValues[key] as? String ?: default
        return try {
            remoteConfig?.getString(key) ?: fallback
        } catch (e: IllegalStateException) {
            Log.w(TAG, "No remote value for $key, using $fallback.", e)
            fallback
        }
    }

    /** Returns the remote integer for [key], falling back to [default]. */
    fun getInt(key: String, default: Int = 0): Int {
        val fallback = defaultValues[key] as? Int ?: default
        return try {
            remoteConfig?.getLong(key)?.toInt() ?: fallback
        } catch (e: IllegalStateException) {
            Log.w(TAG, "No remote value for $key, using $fallback.", e)
            fallback
        }
    }
}
