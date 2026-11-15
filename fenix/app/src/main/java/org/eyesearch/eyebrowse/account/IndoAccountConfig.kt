/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.account

import java.net.URLEncoder

/**
 * Everything the Indo Account handoff needs, in one place, so that when the other side freezes the
 * exchange endpoint it is one constant to change and not a refactor.
 *
 * The exchange path is the one value here that is not confirmed yet: Indo Account has no app handoff
 * endpoint at the time of writing, so this is the shape we proposed and nothing else in the app
 * depends on it being right.
 */
object IndoAccountConfig {

    const val API_BASE = "https://ig4s3omb7uwdfwsl64u7fhw3ta0ahxnb.lambda-url.ap-southeast-2.on.aws"

    const val SIGN_IN_URL = "https://indoworkspace.github.io/Indo-Account/Sign%20in/index.html"

    const val MANAGE_URL =
        "https://indoworkspace.github.io/Indo-Account/management/apps-data/eyebrowse/index.html"

    const val APP_ID = "EYEBROWSE"

    const val DEEPLINK = "indobrowse://auth"

    /** Unconfirmed: proposed shape, flagged in the pull request. */
    const val EXCHANGE_PATH = "/api/v1/apps/eyebrowse/exchange"

    const val PROFILE_PATH = "/api/v1/me"

    fun signInUrl(state: String): String {
        val redirect = URLEncoder.encode(DEEPLINK, "UTF-8")
        return "$SIGN_IN_URL?app=$APP_ID&redirect=$redirect&state=$state"
    }
}
