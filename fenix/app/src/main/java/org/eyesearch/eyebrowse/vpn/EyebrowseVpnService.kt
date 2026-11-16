/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R

/**
 * The VPN, step one: the service, the consent, the notification and the interface.
 *
 * It establishes an interface with no routes and no exit yet, so nothing is captured and nothing on
 * the phone breaks while we are building it. The status line says exactly that, and the exit, the
 * routes and the kill switch land in the next change. Shipping a tunnel that silently ate the
 * phone's connectivity would be worse than shipping none.
 */
class EyebrowseVpnService : VpnService() {

    private var tunnel: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTunnel()
                return START_NOT_STICKY
            }
            else -> startTunnel()
        }
        return START_STICKY
    }

    private fun startTunnel() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )

        if (tunnel != null) {
            return
        }

        // Step one: an interface with an address and nothing routed through it.
        tunnel = Builder()
            .setSession(getString(R.string.vpn_session_name))
            .setMtu(MTU)
            .addAddress(LOCAL_ADDRESS, LOCAL_PREFIX)
            .setBlocking(false)
            .establish()
    }

    private fun stopTunnel() {
        tunnel?.close()
        tunnel = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_status_logo)
        .setContentTitle(getString(R.string.vpn_notification_title))
        .setContentText(getString(R.string.vpn_notification_text))
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, HomeActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .addAction(
            0,
            getString(R.string.vpn_notification_stop),
            PendingIntent.getService(
                this,
                1,
                Intent(this, EyebrowseVpnService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .build()

    override fun onDestroy() {
        tunnel?.close()
        tunnel = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "org.eyesearch.eyebrowse.vpn.START"
        const val ACTION_STOP = "org.eyesearch.eyebrowse.vpn.STOP"

        private const val CHANNEL_ID = "eyebrowse_vpn"
        private const val NOTIFICATION_ID = 7311
        private const val MTU = 1500
        private const val LOCAL_ADDRESS = "10.111.222.1"
        private const val LOCAL_PREFIX = 32

        /** Creates the channel once; harmless to call again. */
        fun ensureChannel(manager: NotificationManager) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return
            }
            if (manager.getNotificationChannel(CHANNEL_ID) != null) {
                return
            }
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "VPN",
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }
}
