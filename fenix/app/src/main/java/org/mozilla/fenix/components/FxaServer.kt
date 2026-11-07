/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import mozilla.appservices.fxaclient.FxaServer
import mozilla.components.service.fxa.ServerConfig
import org.mozilla.fenix.ext.settings

/**
 * Utility to configure Firefox Account servers.
 */

object FxaServer {
    /**
     * Eyebrowse accounts server.
     *
     * Mozilla's account servers (Release and China) are no longer used: nothing in the app talks to
     * accounts.firefox.com. Replace this with the real Eyebrowse backend once it is live, or set
     * the server override in the secret settings for testing.
     */
    private const val EYEBROWSE_SERVER = "https://accounts.eyebrowse.in"

    // OAuth client id for the Eyebrowse backend. A placeholder until the backend issues one.
    private const val CLIENT_ID = "eyebrowse"

    const val REDIRECT_URL = "urn:ietf:wg:oauth:2.0:oob:oauth-redirect-webchannel"

    fun config(context: Context): ServerConfig {
        val serverOverride = context.settings().overrideFxAServer
        val tokenServerOverride = context.settings().overrideSyncTokenServer.ifEmpty { null }
        val server = serverOverride.ifEmpty { EYEBROWSE_SERVER }
        return ServerConfig(FxaServer.Custom(server), CLIENT_ID, REDIRECT_URL, tokenServerOverride)
    }
}
