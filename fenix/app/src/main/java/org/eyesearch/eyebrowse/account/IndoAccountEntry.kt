/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.account

import android.content.Context
import android.os.Build
import android.webkit.WebView
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import java.net.URLEncoder

/**
 * The two ways out to Indo Account. Both prefer the in-app WebView, so the handoff finishes without
 * the user tapping anything; a device without a usable WebView falls back to the browser and the
 * deep link, which still works.
 *
 * The management panel is opened with the app token in the URL fragment, never in the query string,
 * so it never reaches a server and never appears in a log.
 */
object IndoAccountEntry {

    fun startSignIn(context: Context) {
        val start = IndoAccountStorage(context).startSignIn()
        val url = IndoAccountConfig.signInUrl(state = start.state, codeChallenge = start.challenge)

        if (hasWebView()) {
            context.startActivity(
                IndoAccountWebViewActivity.intent(
                    context = context,
                    url = url,
                    mode = IndoAccountWebViewActivity.MODE_SIGN_IN,
                ),
            )
        } else {
            openInBrowser(context, url)
        }
    }

    fun openManagement(context: Context) {
        val token = IndoAccountStorage(context).token()
        val url = if (token.isNullOrBlank()) {
            IndoAccountConfig.MANAGE_URL
        } else {
            IndoAccountConfig.MANAGE_URL + "#app_token=" + URLEncoder.encode(token, "UTF-8")
        }

        if (hasWebView()) {
            context.startActivity(
                IndoAccountWebViewActivity.intent(
                    context = context,
                    url = url,
                    mode = IndoAccountWebViewActivity.MODE_MANAGE,
                ),
            )
        } else {
            openInBrowser(context, url)
        }
    }

    private fun hasWebView(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return true
        }
        return runCatching { WebView.getCurrentWebViewPackage() != null }.getOrDefault(false)
    }

    private fun openInBrowser(context: Context, url: String) {
        (context as? HomeActivity)?.openToBrowserAndLoad(
            searchTermOrURL = url,
            newTab = true,
            from = BrowserDirection.FromSettings,
        )
    }
}
