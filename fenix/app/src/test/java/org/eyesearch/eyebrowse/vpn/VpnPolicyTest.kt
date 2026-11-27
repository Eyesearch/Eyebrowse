/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.vpn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnPolicyTest {
    @Test
    fun `accept IPv4 only when IPv6 is unused`() {
        assertTrue(VpnPolicy.acceptRoutes(true, false, false))
        assertFalse(VpnPolicy.acceptRoutes(true, false, true))
    }

    @Test
    fun `require full IPv4 route even with IPv6`() {
        assertFalse(VpnPolicy.acceptRoutes(false, true, true))
        assertTrue(VpnPolicy.acceptRoutes(true, true, true))
    }

    @Test
    fun `only recent real handshakes count`() {
        assertFalse(VpnPolicy.recentHandshake(0, 200000, 180000))
        assertFalse(VpnPolicy.recentHandshake(20001, 20000, 180000))
        assertFalse(VpnPolicy.recentHandshake(1, 200000, 180000))
        assertTrue(VpnPolicy.recentHandshake(20000, 200000, 180000))
    }
}
