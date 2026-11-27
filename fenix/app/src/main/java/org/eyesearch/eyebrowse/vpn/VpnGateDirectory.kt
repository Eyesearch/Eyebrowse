/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.vpn

import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Opt-in, read-only VPN Gate discovery. Never installs or executes remote VPN profiles. */
internal object VpnGateDirectory {
    private const val ENDPOINT = "https://www.vpngate.net/api/iphone/"
    private const val MAX_BYTES = 8 * 1024 * 1024
    private const val MAX_RESULTS = 20
    private const val TIMEOUT_MS = 10000

    data class Server(
        val host: String,
        val country: String,
        val pingMs: Int,
        val score: Long,
        val logType: String,
    )

    suspend fun fetch(): List<Server> = withContext(Dispatchers.IO) {
        var connection: HttpsURLConnection? = null
        try {
            val request = (URL(ENDPOINT).openConnection() as HttpsURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                useCaches = false
                setRequestProperty("Accept", "text/plain, text/csv")
            }
            connection = request
            if (request.responseCode != 200 || request.contentLength > MAX_BYTES) {
                return@withContext emptyList()
            }
            val body = request.inputStream.use { input ->
                val buffer = ByteArray(8192)
                val output = java.io.ByteArrayOutputStream()
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > MAX_BYTES) return@withContext emptyList()
                    output.write(buffer, 0, count)
                }
                output.toString("UTF-8")
            }
            parse(body)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    /** Read only the documented metadata. Keep the OpenVPN config field opaque. */
    internal fun parse(body: String): List<Server> {
        val lines = body.lineSequence().iterator()
        if (!lines.hasNext() || lines.next().trim().removePrefix("\uFEFF") != "*vpn_servers") return emptyList()
        if (!lines.hasNext()) return emptyList()
        val header = lines.next().trim().removePrefix("#").split(',')
        val expected = listOf("HostName", "IP", "Score", "Ping", "Speed", "CountryLong", "CountryShort",
            "NumVpnSessions", "Uptime", "TotalUsers", "TotalTraffic", "LogType", "Operator", "Message",
            "OpenVPN_ConfigData_Base64")
        if (header != expected) return emptyList()
        val servers = mutableListOf<Server>()
        while (lines.hasNext()) {
            val line = lines.next()
            if (line.trim() == "*") break
            val fields = csvRow(line) ?: continue
            if (fields.size != expected.size || fields.last().isBlank()) continue
            val host = fields[0].trim()
            val country = fields[5].trim()
            val ping = fields[3].toIntOrNull() ?: continue
            val score = fields[2].toLongOrNull() ?: continue
            val logType = fields[11].trim()
            if (!host.matches(Regex("[A-Za-z0-9.-]{1,253}")) ||
                !safeLabel(country) || !safeLabel(logType) || ping !in 1..10000 || score < 0
            ) continue
            servers.add(Server(host, country, ping, score, logType))
        }
        return servers.sortedWith(compareByDescending<Server> { it.score }.thenBy { it.pingMs })
            .distinctBy { it.host.lowercase() }
            .take(MAX_RESULTS)
    }

    private fun safeLabel(value: String): Boolean = value.length in 1..80 &&
        value.none { it.isISOControl() || Character.getType(it) == Character.FORMAT.toInt() }

    /** CSV quoting, including escaped quote pairs; reject incomplete quoted rows. */
    private fun csvRow(line: String): List<String>? {
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var closedQuote = false
        var index = 0
        while (index < line.length) {
            val char = line[index++]
            when {
                quoted && char == '"' -> {
                    if (index < line.length && line[index] == '"') {
                        field.append('"')
                        index++
                    } else {
                        quoted = false
                        closedQuote = true
                    }
                }
                quoted -> field.append(char)
                char == ',' -> {
                    fields.add(field.toString())
                    field.setLength(0)
                    closedQuote = false
                    if (fields.size > 14) return null
                }
                closedQuote -> return null
                char == '"' -> {
                    if (field.isNotEmpty()) return null
                    quoted = true
                }
                else -> field.append(char)
            }
        }
        if (quoted) return null
        fields.add(field.toString())
        return fields
    }
}
