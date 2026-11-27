/* Experimental adapter; no OpenVPN library code is bundled in this repository.
 * SPDX-License-Identifier: MPL-2.0
 */

package org.eyesearch.eyebrowse.vpn

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.IBinder
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.IOpenVPNServiceInternal
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VPNLaunchHelper
import de.blinkt.openvpn.core.VpnStatus
import java.io.StringReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Debug-only adapter for the pinned, source-built ics-openvpn AAR. */
internal class NativeOpenVpnBackend : OpenVpnExperiment.Backend, VpnStatus.StateListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutable = MutableStateFlow(OpenVpnExperiment.Status.OFF)
    override val status = mutable.asStateFlow()
    private var expectedUuid: String? = null
    private var observedUuid: String? = null
    private var deadline: Job? = null

    override fun connect(context: Context, profile: OpenVpnProfilePolicy.ValidatedProfile): Boolean {
        if (VpnService.prepare(context) != null || VpnStatus.isVPNActive() ||
            mutable.value !in setOf(OpenVpnExperiment.Status.OFF, OpenVpnExperiment.Status.ERROR)
        ) return false
        return try {
            val parsed = ConfigParser().run {
                parseConfig(StringReader(profile.content))
                convertProfile()
            }
            require(parsed.mExpectTLSCert && !parsed.mUseLzo)
            require(parsed.mAuthenticationType == VpnProfile.TYPE_CERTIFICATES ||
                parsed.mAuthenticationType == VpnProfile.TYPE_USERPASS ||
                parsed.mAuthenticationType == VpnProfile.TYPE_USERPASS_CERTIFICATES)
            // VPN Gate publicly documents these anonymous login credentials when required.
            parsed.mUsername = "vpn"
            parsed.mPassword = "vpn"
            parsed.mName = "Eyebrowse VPN Gate"
            parsed.mProfileCreator = context.packageName
            parsed.mAllowedAppsVpn = hashSetOf(context.packageName)
            parsed.mAllowedAppsVpnAreDisallowed = false
            parsed.mAllowAppVpnBypass = false
            parsed.mAllowLocalLAN = false
            parsed.mUseDefaultRoute = true
            parsed.mUseDefaultRoutev6 = true
            parsed.mPersistTun = true
            parsed.mBlockUnusedAddressFamilies = true
            expectedUuid = parsed.uuidString
            observedUuid = null
            mutable.value = OpenVpnExperiment.Status.CONNECTING
            VpnStatus.removeStateListener(this)
            VpnStatus.addStateListener(this)
            ProfileManager.setTemporaryProfile(context, parsed)
            VPNLaunchHelper.startOpenVpn(parsed, context, "Eyebrowse VPN Gate", false)
            deadline?.cancel()
            deadline = scope.launch {
                delay(45000)
                if (mutable.value == OpenVpnExperiment.Status.CONNECTING) stop(context)
            }
            true
        } catch (_: Exception) {
            // A service launch may already have been dispatched: do not assume it stopped.
            mutable.value = OpenVpnExperiment.Status.STOP_FAILED
            false
        } catch (_: LinkageError) {
            mutable.value = OpenVpnExperiment.Status.STOP_FAILED
            false
        }
    }

    override fun setConnectedVPN(uuid: String?) {
        scope.launch { observedUuid = uuid }
    }

    override fun updateState(
        state: String?,
        logmessage: String?,
        localizedResId: Int,
        level: ConnectionStatus?,
        intent: Intent?,
    ) {
        scope.launch {
            if (expectedUuid == null || observedUuid != expectedUuid) return@launch
            if (mutable.value == OpenVpnExperiment.Status.STOPPING ||
                mutable.value == OpenVpnExperiment.Status.STOP_FAILED
            ) {
                if (level != ConnectionStatus.LEVEL_NOTCONNECTED) return@launch
            }
            when (level) {
                ConnectionStatus.LEVEL_CONNECTED -> {
                    deadline?.cancel()
                    mutable.value = OpenVpnExperiment.Status.CONNECTED
                }
                ConnectionStatus.LEVEL_NOTCONNECTED -> {
                    deadline?.cancel()
                    expectedUuid = null
                    observedUuid = null
                    mutable.value = OpenVpnExperiment.Status.OFF
                    VpnStatus.removeStateListener(this@NativeOpenVpnBackend)
                }
                ConnectionStatus.LEVEL_AUTH_FAILED,
                ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT -> {
                    mutable.value = OpenVpnExperiment.Status.STOP_FAILED
                }
                else -> if (mutable.value == OpenVpnExperiment.Status.CONNECTED) {
                    mutable.value = OpenVpnExperiment.Status.RECONNECTING
                }
            }
        }
    }

    override fun stop(context: Context) {
        deadline?.cancel()
        mutable.value = OpenVpnExperiment.Status.STOPPING
        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                try {
                    val service = IOpenVPNServiceInternal.Stub.asInterface(binder)
                        ?: error("VPN control unavailable")
                    service.stopVPN(false)
                } catch (_: Exception) {
                    mutable.value = OpenVpnExperiment.Status.STOP_FAILED
                } finally {
                    runCatching { context.unbindService(this) }
                }
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                if (mutable.value == OpenVpnExperiment.Status.STOPPING) {
                    mutable.value = OpenVpnExperiment.Status.STOP_FAILED
                }
            }
        }
        val bound = runCatching {
            context.bindService(Intent(context, OpenVPNService::class.java).setAction(OpenVPNService.START_SERVICE), conn, 0)
        }.getOrDefault(false)
        if (!bound) {
            // Binding failure alone is not proof of disconnect.
            mutable.value = OpenVpnExperiment.Status.STOP_FAILED
        } else {
            deadline = scope.launch {
                delay(15000)
                if (mutable.value == OpenVpnExperiment.Status.STOPPING) {
                    mutable.value = OpenVpnExperiment.Status.STOP_FAILED
                    runCatching { context.unbindService(conn) }
                }
            }
        }
    }
}
