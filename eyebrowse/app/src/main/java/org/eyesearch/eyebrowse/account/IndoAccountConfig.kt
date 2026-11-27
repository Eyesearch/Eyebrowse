/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.account

import java.net.URLEncoder

/**
 * The handoff, frozen against `Indoworkspace/Indo-Account: docs/app-data-contract.md`.
 *
 * Sign-in carries a PKCE S256 challenge; the code is exchanged at /api/v1/app/session for an app
 * token; identity is read at /api/v1/app/me. The user session endpoint /api/v1/me is refused for an
 * app token by design, so it is never called from here.
 */
object IndoAccountConfig {

    const val API_BASE = "https://ig4s3omb7uwdfwsl64u7fhw3ta0ahxnb.lambda-url.ap-southeast-2.on.aws"

    const val SIGN_IN_URL = "https://indoworkspace.github.io/Indo-Account/Sign%20in/index.html"

    const val MANAGE_URL =
        "https://indoworkspace.github.io/Indo-Account/management/apps-data/eyebrowse/index.html"

    const val APP_ID = "EYEBROWSE"

    const val DEEPLINK = "indobrowse://auth"

    const val EXCHANGE_PATH = "/api/v1/app/session"

    const val IDENTITY_PATH = "/api/v1/app/me"

    const val PANEL_SESSION_PATH = "/api/v1/app/panel-session"

    /** The panel at Home, which is the account as a browser sees it. */
    const val PANEL_URL = "https://indoworkspace.github.io/Indo-Account/management/home/index.html"

    /** The panel, opened with a short lived panel token in the fragment and nothing else. */
    fun panelUrl(panelToken: String): String =
        PANEL_URL + "#panel_token=" + URLEncoder.encode(panelToken, "UTF-8")

    fun signInUrl(state: String, codeChallenge: String): String {
        val redirect = URLEncoder.encode(DEEPLINK, "UTF-8")
        return buildString {
            append(SIGN_IN_URL)
            append("?app=").append(APP_ID)
            append("&redirect=").append(redirect)
            append("&state=").append(URLEncoder.encode(state, "UTF-8"))
            append("&code_challenge=").append(URLEncoder.encode(codeChallenge, "UTF-8"))
            append("&code_challenge_method=S256")
        }
    }
}
