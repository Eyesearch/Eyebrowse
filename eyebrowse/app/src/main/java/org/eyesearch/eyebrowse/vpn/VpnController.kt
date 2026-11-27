/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.vpn

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.SystemClock
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import java.io.ByteArrayInputStream
import java.net.Inet4Address
import java.net.Inet6Address
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Browser-scoped VPN using only profiles issued by a user-authorized provider. */
object VpnController {
    enum class Status { OFF, IMPORTING, CONNECTING, WAITING, HANDSHAKE, RECONNECTING, DISCONNECTING, STOP_FAILED, ERROR, INVALID_PROFILE }

    private const val MAX_PROFILE_BYTES = 65536
    private const val MAX_PROFILES = 5
    private const val CHECK_INTERVAL_MS = 2000L
    private const val CONNECT_TIMEOUT_MS = 40000L
    private const val HANDSHAKE_WINDOW_MS = 180000L
    private const val MAX_RETRY_ROUNDS = 2
    private const val RECOVERY_DELAY_MS = 5000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableStatus = MutableStateFlow(Status.OFF)
    val status = mutableStatus.asStateFlow()

    private var backend: GoBackend? = null
    private var connection: Job? = null
    private var wanted = false
    private var memoryProfiles: List<Config>? = null
    private var preferredIndex = 0
    private val tunnel = object : Tunnel {
        override fun getName() = "Eyebrowse"
        override fun onStateChange(newState: Tunnel.State) = Unit
    }

    fun consentIntent(context: Context): Intent? = VpnService.prepare(context)

    /** Require full IPv4 coverage and either full IPv6 coverage or an unused IPv6 family. */
    internal fun browserProfile(config: Config, packageName: String): Config {
        val source = config.getInterface()
        val peer = config.peers.single()
        require(peer.endpoint.isPresent)
        require(source.addresses.isNotEmpty() && source.dnsServers.isNotEmpty())
        val ipv4Default = peer.allowedIps.any { it.mask == 0 && it.address is Inet4Address }
        val ipv6Default = peer.allowedIps.any { it.mask == 0 && it.address is Inet6Address }
        val usesIpv6 = source.addresses.any { it.address is Inet6Address } ||
            source.dnsServers.any { it is Inet6Address } ||
            peer.allowedIps.any { it.address is Inet6Address }
        require(source.addresses.any { it.address is Inet4Address })
        require(VpnPolicy.acceptRoutes(ipv4Default, ipv6Default, usesIpv6))
        // GoBackend does not call allowFamily for a single peer with a default route.
        // IPv4-only profiles therefore leave IPv6 blocked, rather than permitting a bypass.
        // Actual device leak tests remain mandatory.

        val scoped = Interface.Builder()
            .setKeyPair(source.keyPair)
            .addAddresses(source.addresses)
            .addDnsServers(source.dnsServers)
            .addDnsSearchDomains(source.dnsSearchDomains)
            .includeApplication(packageName)
        if (source.listenPort.isPresent) scoped.setListenPort(source.listenPort.get())
        if (source.mtu.isPresent) scoped.setMtu(source.mtu.get())
        // Initiate a handshake even before a user navigates, and keep NAT state alive.
        val remote = Peer.Builder()
            .setPublicKey(peer.publicKey)
            .setEndpoint(peer.endpoint.get())
            .addAllowedIps(peer.allowedIps)
            .setPersistentKeepalive(peer.persistentKeepalive.orElse(0).takeIf { it > 0 } ?: 25)
        if (peer.preSharedKey.isPresent) remote.setPreSharedKey(peer.preSharedKey.get())
        return Config.Builder().setInterface(scoped.build()).addPeer(remote.build()).build()
    }

    private fun profiles(context: Context): List<Config> {
        memoryProfiles?.let { return it }
        return VpnProfileStore.load(context).mapNotNull { text ->
            runCatching {
                browserProfile(Config.parse(text.byteInputStream()), context.packageName)
            }.getOrNull()
        }.also { memoryProfiles = it }
    }

    fun hasProfiles(context: Context): Boolean = memoryProfiles?.isNotEmpty() == true ||
        VpnProfileStore.hasSaved(context)

    fun importProfile(context: Context, uri: Uri, completed: (Boolean) -> Unit) {
        if (connection?.isActive == true || mutableStatus.value == Status.IMPORTING ||
            mutableStatus.value == Status.STOP_FAILED
        ) return
        val app = context.applicationContext
        mutableStatus.value = Status.IMPORTING
        scope.launch {
            val accepted = try {
                withContext(Dispatchers.IO) {
                    val config = app.contentResolver.openInputStream(uri)?.use { input ->
                        val buffer = ByteArray(MAX_PROFILE_BYTES + 1)
                        var size = 0
                        try {
                            while (size < buffer.size) {
                                val count = input.read(buffer, size, buffer.size - size)
                                if (count <= 0) break
                                size += count
                            }
                            require(size in 1..MAX_PROFILE_BYTES)
                            ByteArrayInputStream(buffer, 0, size).use {
                                browserProfile(Config.parse(it), app.packageName)
                            }
                        } finally {
                            buffer.fill(0)
                        }
                    } ?: error("Missing profile")
                    val updated = (listOf(config) + profiles(app).filterNot {
                        it.toWgQuickString() == config.toWgQuickString()
                    }).take(MAX_PROFILES)
                    VpnProfileStore.save(app, updated.map { it.toWgQuickString() })
                    memoryProfiles = updated
                    true
                }
            } catch (_: Exception) {
                false
            }
            preferredIndex = 0
            mutableStatus.value = if (accepted) Status.OFF else Status.INVALID_PROFILE
            completed(accepted)
        }
    }

    fun clearProfiles(context: Context) {
        if (connection?.isActive == true || mutableStatus.value == Status.IMPORTING ||
            mutableStatus.value == Status.STOP_FAILED
        ) return
        VpnProfileStore.clear(context)
        memoryProfiles = emptyList()
        preferredIndex = 0
        mutableStatus.value = Status.OFF
    }

    fun start(context: Context) {
        if (OpenVpnExperiment.busy) return
        if (connection?.isActive == true || mutableStatus.value == Status.IMPORTING ||
            mutableStatus.value == Status.STOP_FAILED
        ) return
        val app = context.applicationContext
        wanted = true
        mutableStatus.value = Status.CONNECTING
        connection = scope.launch {
            var finalStatus = Status.ERROR
            try {
                val candidates = withContext(Dispatchers.IO) { profiles(app) }
                if (candidates.isEmpty()) {
                    finalStatus = Status.INVALID_PROFILE
                    return@launch
                }
                val active = withContext(Dispatchers.IO) {
                    backend ?: GoBackend(app).also { backend = it }
                }
                // Recovery is bounded per outage; a successful handshake resets the budget.
                // Never reconnect following Android consent revocation or service removal.
                var cursor = preferredIndex % candidates.size
                var failedAttempts = 0
                var recovering = false
                val maxAttempts = candidates.size * MAX_RETRY_ROUNDS
                while (wanted && failedAttempts < maxAttempts && consentIntent(app) == null) {
                    val index = cursor
                    cursor = (cursor + 1) % candidates.size
                    mutableStatus.value = if (recovering) Status.RECONNECTING else Status.CONNECTING
                    val started = withContext(Dispatchers.IO) {
                        runCatching {
                            active.setState(tunnel, Tunnel.State.UP, candidates[index]) == Tunnel.State.UP
                        }.getOrDefault(false)
                    }
                    if (!started) {
                        failedAttempts++
                        recovering = true
                        if (wanted) {
                            mutableStatus.value = Status.RECONNECTING
                            delay(RECOVERY_DELAY_MS)
                        }
                        continue
                    }
                    if (wanted) mutableStatus.value = Status.WAITING
                    val deadline = SystemClock.elapsedRealtime() + CONNECT_TIMEOUT_MS
                    var connected = false
                    while (wanted) {
                        val (running, recent) = withContext(Dispatchers.IO) {
                            val up = active.getState(tunnel) == Tunnel.State.UP
                            up to (up && recentHandshake(active))
                        }
                        if (!running || consentIntent(app) != null) {
                            wanted = false
                            finalStatus = Status.ERROR
                            break
                        }
                        if (recent) {
                            preferredIndex = index
                            connected = true
                            failedAttempts = 0
                            if (wanted) mutableStatus.value = Status.HANDSHAKE
                        } else {
                            if (wanted) mutableStatus.value = Status.WAITING
                            if (connected || SystemClock.elapsedRealtime() >= deadline) break
                        }
                        delay(CHECK_INTERVAL_MS)
                    }
                    if (!wanted) break
                    val down = withContext(Dispatchers.IO) {
                        runCatching {
                            active.setState(tunnel, Tunnel.State.DOWN, null) == Tunnel.State.DOWN
                        }.getOrDefault(false)
                    }
                    if (!down) break
                    failedAttempts++
                    recovering = true
                    if (wanted && failedAttempts < maxAttempts) {
                        mutableStatus.value = Status.RECONNECTING
                        delay(RECOVERY_DELAY_MS)
                    }
                }
            } catch (_: Exception) {
                finalStatus = Status.ERROR
            } catch (_: LinkageError) {
                finalStatus = Status.ERROR
            } finally {
                val requestedStop = mutableStatus.value == Status.DISCONNECTING
                val stopped = withContext(Dispatchers.IO) {
                    runCatching {
                        backend?.setState(tunnel, Tunnel.State.DOWN, null) != Tunnel.State.UP
                    }.getOrDefault(false)
                }
                wanted = false
                mutableStatus.value = when {
                    !stopped -> Status.STOP_FAILED
                    requestedStop -> Status.OFF
                    else -> finalStatus
                }
            }
        }
    }

    private fun recentHandshake(active: GoBackend): Boolean {
        val stats = active.getStatistics(tunnel)
        val now = System.currentTimeMillis()
        return stats.peers().any { key ->
            val time = stats.peer(key)?.latestHandshakeEpochMillis() ?: 0L
            VpnPolicy.recentHandshake(time, now, HANDSHAKE_WINDOW_MS)
        }
    }

    fun stop() {
        wanted = false
        mutableStatus.value = Status.DISCONNECTING
        if (connection?.isActive == true) return
        connection = scope.launch {
            val stopped = withContext(Dispatchers.IO) {
                runCatching {
                    backend?.setState(tunnel, Tunnel.State.DOWN, null) != Tunnel.State.UP
                }.getOrDefault(false)
            }
            mutableStatus.value = if (stopped) Status.OFF else Status.STOP_FAILED
        }
    }
}
