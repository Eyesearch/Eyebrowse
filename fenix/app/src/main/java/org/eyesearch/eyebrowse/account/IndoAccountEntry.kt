/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.account

import android.content.Context
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity

/**
 * The two ways out to the web panel. Both open in this browser, and both keep the truth on the
 * account side: the app only ever holds a token and reads the profile live.
 */
object IndoAccountEntry {

    /** Writes a fresh state, then opens the sign-in page with it. */
    fun startSignIn(context: Context) {
        val state = IndoAccountStorage(context).startSignIn()
        open(context, IndoAccountConfig.signInUrl(state))
    }

    fun openManagement(context: Context) {
        open(context, IndoAccountConfig.MANAGE_URL)
    }

    private fun open(context: Context, url: String) {
        (context as? HomeActivity)?.openToBrowserAndLoad(
            searchTermOrURL = url,
            newTab = true,
            from = BrowserDirection.FromSettings,
        )
    }
}
