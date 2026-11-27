/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnGateDirectoryTest {
    private val header = "*vpn_servers\n#HostName,IP,Score,Ping,Speed,CountryLong,CountryShort," +
        "NumVpnSessions,Uptime,TotalUsers,TotalTraffic,LogType,Operator,Message,OpenVPN_ConfigData_Base64\n"

    private fun row(host: String, score: Long, ping: String = "12", config: String = "YQ==") =
        "$host,192.0.2.1,$score,$ping,1000,Japan,JP,1,2,3,4,2weeks,operator,\"hello, world\",$config"

    @Test
    fun `sort by score and then ping and discard duplicate hosts`() {
        val rows = listOf(row("relay-a", 10), row("relay-b", 20, "9"), row("relay-a", 30))
        val result = VpnGateDirectory.parse(header + rows.joinToString("\n") + "\n*")
        assertEquals(listOf("relay-a", "relay-b"), result.map { it.host })
        assertEquals("2weeks", result.first().logType)
    }

    @Test
    fun `reject invalid schema and malformed entries`() {
        assertTrue(VpnGateDirectory.parse("not a directory").isEmpty())
        val rows = listOf(row("bad host", 10), row("relay-a", 20, "bad"), row("relay-b", 30, config = ""))
        assertTrue(VpnGateDirectory.parse(header + rows.joinToString("\n")).isEmpty())
    }

    @Test
    fun `limit displayed servers and never return configuration contents`() {
        val rows = (1..30).map { row("relay-$it", it.toLong()) }
        val result = VpnGateDirectory.parse(header + rows.joinToString("\n"))
        assertEquals(20, result.size)
        assertEquals("relay-30", result.first().host)
    }
}
