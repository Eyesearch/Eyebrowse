/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat

/**
 * The two things the rest of the app needs from the VPN: ask Android for permission, and start or
 * stop the service. Android's consent dialog is the only thing that can grant it, so [consentIntent]
 * returning null means we are already allowed.
 */
object VpnController {

    fun consentIntent(context: Context): Intent? = VpnService.prepare(context)

    fun start(context: Context) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, EyebrowseVpnService::class.java).setAction(EyebrowseVpnService.ACTION_START),
        )
    }

    fun stop(context: Context) {
        context.startService(
            Intent(context, EyebrowseVpnService::class.java).setAction(EyebrowseVpnService.ACTION_STOP),
        )
    }
}
