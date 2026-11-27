/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.vpn

internal object VpnPolicy {
    fun acceptRoutes(ipv4Default: Boolean, ipv6Default: Boolean, usesIpv6: Boolean): Boolean =
        ipv4Default && (ipv6Default || !usesIpv6)

    fun blockNavigation(enabled: Boolean, scheme: String?, handshake: Boolean, vpnNetwork: Boolean): Boolean =
        enabled && (scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)) &&
            (!handshake || !vpnNetwork)

    fun recentHandshake(time: Long, now: Long, window: Long): Boolean =
        time > 0 && time <= now && now - time <= window
}
