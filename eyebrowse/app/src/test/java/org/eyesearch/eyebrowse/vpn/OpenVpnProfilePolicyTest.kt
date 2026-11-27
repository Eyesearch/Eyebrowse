/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.vpn

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OpenVpnProfilePolicyTest {
    private val host = "relay.example"
    private val base = """
        client
        dev tun
        proto udp
        remote relay.example 1194
        remote-cert-tls server
        tls-version-min 1.2
        cipher AES-256-GCM
        auth SHA256
        <ca>
        -----BEGIN CERTIFICATE-----
        TEST
        -----END CERTIFICATE-----
        </ca>
    """.trimIndent()

    @Test
    fun `accept narrow profile grammar without asserting valid certificate`() {
        assertNotNull(OpenVpnProfilePolicy.validate(base, host))
    }

    @Test
    fun `reject scripts plugins external files and unsafe crypto`() {
        listOf(
            "up /tmp/script", "plugin evil.so", "script-security 3",
            "ca external.pem", "config other.ovpn", "compress lz4",
            "data-ciphers NONE", "auth-user-pass credentials.txt",
            "route-nopull", "pull-filter ignore redirect-gateway",
            "remote other.example 443", "verb 9", "auth SHA1",
        ).forEach { option ->
            assertNull(option, OpenVpnProfilePolicy.validate(base + "\n" + option, host))
        }
    }

    @Test
    fun `require verification and endpoint match`() {
        assertNull(OpenVpnProfilePolicy.validate(base.replace("remote-cert-tls server", ""), host))
        assertNull(OpenVpnProfilePolicy.validate(base.replace("tls-version-min 1.2", ""), host))
        assertNull(OpenVpnProfilePolicy.validate(base, "other.example"))
        assertNull(OpenVpnProfilePolicy.validate(base.replace("</ca>", ""), host))
        assertNull(OpenVpnProfilePolicy.validate(base.replace("1194", "99999"), host))
    }
}
