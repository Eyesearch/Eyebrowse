/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.vpn

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.Interface
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** In-memory browser-only WireGuard client. Never logs or persists profile credentials. */
object VpnController {
    enum class Status { OFF, CONNECTING, WAITING, HANDSHAKE, DISCONNECTING, ERROR, INVALID_PROFILE }

    private const val MAX_PROFILE_BYTES = 65536
    private const val CHECK_INTERVAL_MS = 5000L
    private const val HANDSHAKE_WINDOW_MS = 180000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()
    private val mutableStatus = MutableStateFlow(Status.OFF)
    val status = mutableStatus.asStateFlow()

    private var backend: GoBackend? = null
    private var pendingProfile: Config? = null
    private var monitor: Job? = null
    private val tunnel = object : Tunnel {
        override fun getName() = "Eyebrowse"
        override fun onStateChange(newState: Tunnel.State) {
            if (newState == Tunnel.State.DOWN) {
                scope.launch {
                    if (mutableStatus.value == Status.WAITING ||
                        mutableStatus.value == Status.HANDSHAKE
                    ) {
                        mutableStatus.value = Status.ERROR
                    }
                }
            }
        }
    }

    fun consentIntent(context: Context): Intent? = VpnService.prepare(context)

    /** Accept only a full-route single-peer config; override any app scope with this package. */
    internal fun browserProfile(config: Config, packageName: String): Config {
        val source = config.getInterface()
        val peer = config.peers.single()
        require(peer.endpoint.isPresent)
        require(source.addresses.isNotEmpty() && source.dnsServers.isNotEmpty())
        require(peer.allowedIps.any { it.mask == 0 && it.address is Inet4Address })
        require(peer.allowedIps.any { it.mask == 0 && it.address is Inet6Address })

        val scoped = Interface.Builder()
            .setKeyPair(source.keyPair)
            .addAddresses(source.addresses)
            .addDnsServers(source.dnsServers)
            .addDnsSearchDomains(source.dnsSearchDomains)
            .includeApplication(packageName)
        if (source.listenPort.isPresent) scoped.setListenPort(source.listenPort.get())
        if (source.mtu.isPresent) scoped.setMtu(source.mtu.get())
        return Config.Builder().setInterface(scoped.build()).addPeer(peer).build()
    }

    fun importProfile(context: Context, uri: Uri, completed: (Boolean) -> Unit) {
        if (mutableStatus.value == Status.CONNECTING ||
            mutableStatus.value == Status.DISCONNECTING ||
            mutableStatus.value == Status.WAITING ||
            mutableStatus.value == Status.HANDSHAKE
        ) return
        val app = context.applicationContext
        mutableStatus.value = Status.CONNECTING
        scope.launch {
            val profile = try {
                withContext(Dispatchers.IO) {
                    val bytes = app.contentResolver.openInputStream(uri)?.use { input ->
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
                    }
                    bytes
                }
            } catch (_: Exception) {
                null
            }
            pendingProfile = profile
            if (profile == null) mutableStatus.value = Status.INVALID_PROFILE
            completed(profile != null)
        }
    }

    fun cancelProfile() {
        pendingProfile = null
        mutableStatus.value = Status.OFF
    }

    fun start(context: Context) {
        val profile = pendingProfile ?: run {
            mutableStatus.value = Status.INVALID_PROFILE
            return
        }
        pendingProfile = null
        val app = context.applicationContext
        scope.launch {
            mutex.withLock {
                try {
                    val started = withContext(Dispatchers.IO) {
                        val active = backend ?: GoBackend(app).also { backend = it }
                        active.setState(tunnel, Tunnel.State.UP, profile) == Tunnel.State.UP
                    }
                    mutableStatus.value = if (started) Status.WAITING else Status.ERROR
                    if (started) monitorTunnel()
                } catch (_: Exception) {
                    mutableStatus.value = Status.ERROR
                } catch (_: LinkageError) {
                    mutableStatus.value = Status.ERROR
                }
            }
        }
    }

    private fun monitorTunnel() {
        monitor?.cancel()
        monitor = scope.launch {
            while (true) {
                val next = mutex.withLock {
                    withContext(Dispatchers.IO) {
                        try {
                            val active = backend
                            if (active == null || active.getState(tunnel) != Tunnel.State.UP) {
                                Status.ERROR
                            } else {
                                val stats = active.getStatistics(tunnel)
                                val now = System.currentTimeMillis()
                                val recent = stats.peers().any { key ->
                                    val time = stats.peer(key)?.latestHandshakeEpochMillis() ?: 0L
                                    time > 0 && now - time in 0..HANDSHAKE_WINDOW_MS
                                }
                                if (recent) Status.HANDSHAKE else Status.WAITING
                            }
                        } catch (_: Exception) {
                            Status.WAITING
                        }
                    }
                }
                mutableStatus.value = next
                if (next == Status.ERROR) break
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        pendingProfile = null
        monitor?.cancel()
        mutableStatus.value = Status.DISCONNECTING
        scope.launch {
            mutex.withLock {
                mutableStatus.value = try {
                    val stopped = withContext(Dispatchers.IO) {
                        backend?.setState(tunnel, Tunnel.State.DOWN, null) != Tunnel.State.UP
                    }
                    if (stopped) Status.OFF else Status.WAITING
                } catch (_: Exception) {
                    // Do not claim disconnected if the backend failed to stop.
                    val running = withContext(Dispatchers.IO) {
                        runCatching { backend?.getState(tunnel) == Tunnel.State.UP }.getOrDefault(true)
                    }
                    if (running) Status.WAITING else Status.ERROR
                }
                if (mutableStatus.value == Status.WAITING) monitorTunnel()
            }
        }
    }
}
