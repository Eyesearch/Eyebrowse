/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.vpn

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eyesearch.eyebrowse.BuildConfig

/** Production builds have no native OpenVPN backend. */
internal object OpenVpnExperiment {
    enum class Status { OFF, CONNECTING, CONNECTED, RECONNECTING, STOPPING, ERROR, STOP_FAILED }

    interface Backend {
        val status: StateFlow<Status>
        fun connect(context: Context, profile: OpenVpnProfilePolicy.ValidatedProfile): Boolean
        fun stop(context: Context)
    }

    val enabled: Boolean get() = BuildConfig.EXPERIMENTAL_OPENVPN

    private val backend: Backend? by lazy {
        if (!enabled) null else runCatching {
            Class.forName("org.eyesearch.eyebrowse.vpn.NativeOpenVpnBackend")
                .getDeclaredConstructor().newInstance() as Backend
        }.getOrNull()
    }

    private val unavailable = MutableStateFlow(Status.OFF)
    val status: StateFlow<Status> get() = backend?.status ?: unavailable
    val busy: Boolean get() = status.value !in setOf(Status.OFF, Status.ERROR)
    fun connect(context: Context, profile: OpenVpnProfilePolicy.ValidatedProfile): Boolean {
        if (VpnController.status.value !in setOf(
                VpnController.Status.OFF, VpnController.Status.ERROR, VpnController.Status.INVALID_PROFILE,
            )
        ) return false
        return backend?.connect(context.applicationContext, profile) == true
    }
    fun stop(context: Context) { backend?.stop(context.applicationContext) }
}
