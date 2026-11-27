/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build

/**
 * Opt-in browser navigation guard. This is NOT a packet-level kill switch.
 * It does not cover already-running network requests, service workers or native clients.
 */
internal object VpnNavigationGuard {
    private const val PREFS = "eyebrowse_vpn_navigation_guard"
    private const val ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, enabled).commit()

    fun shouldBlock(context: Context, uri: String): Boolean {
        if (!isEnabled(context)) return false
        val scheme = Uri.parse(uri).scheme
        val handshake = VpnController.status.value == VpnController.Status.HANDSHAKE
        val vpnNetwork = if (handshake && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                manager.activeNetwork?.let { network ->
                    manager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                } == true
            } catch (_: Exception) {
                false
            }
        } else {
            // No reliable active-network signal on API 21/22: fail closed when explicitly enabled.
            false
        }
        return VpnPolicy.blockNavigation(true, scheme, handshake, vpnNetwork)
    }
}
