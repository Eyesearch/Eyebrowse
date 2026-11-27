/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.settings.account

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mozilla.components.concept.fetch.Client
import mozilla.components.concept.sync.Profile
import mozilla.components.service.fxa.manager.FxaAccountManager
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.ext.bitmapForUrl
import org.eyesearch.eyebrowse.settings.requirePreference

class AccountUiView(
    fragment: PreferenceFragmentCompat,
    private val scope: CoroutineScope,
    private val accountManager: FxaAccountManager,
    private val httpClient: Client,
    private val updateFxAAllowDomesticChinaServerMenu: () -> Unit,
) {

    private val preferenceSignIn =
        fragment.requirePreference<Preference>(R.string.pref_key_sign_in)
    private val preferenceEyebrowseAccount =
        fragment.requirePreference<AccountPreference>(R.string.pref_key_account)
    private val preferenceEyebrowseAccountAuthError =
        fragment.requirePreference<AccountAuthErrorPreference>(R.string.pref_key_account_auth_error)
    private val accountPreferenceCategory =
        fragment.requirePreference<PreferenceCategory>(R.string.pref_key_account_category)

    private var avatarJob: Job? = null

    /**
     * Updates the UI to reflect current account state.
     * Possible conditions are logged-in without problems, logged-out, and logged-in but needs to re-authenticate.
     */
    fun updateAccountUIState(context: Context, profile: Profile?) {
        val account = accountManager.authenticatedAccount()

        updateFxAAllowDomesticChinaServerMenu()

        // Signed-in, no problems.
        if (account != null && !accountManager.accountNeedsReauth()) {
            preferenceSignIn.isVisible = false

            avatarJob?.cancel()
            val avatarUrl = profile?.avatar?.url
            if (avatarUrl != null) {
                avatarJob = scope.launch {
                    val roundedAvatarDrawable = toRoundedDrawable(avatarUrl, context)
                    preferenceEyebrowseAccount.icon = roundedAvatarDrawable ?: genericAvatar(context)
                }
            } else {
                avatarJob = null
                preferenceEyebrowseAccount.icon = genericAvatar(context)
            }

            preferenceSignIn.onPreferenceClickListener = null
            preferenceEyebrowseAccountAuthError.isVisible = false
            preferenceEyebrowseAccount.isVisible = true
            accountPreferenceCategory.isVisible = true

            preferenceEyebrowseAccount.displayName = profile?.displayName
            preferenceEyebrowseAccount.email = profile?.email

            // Signed-in, need to re-authenticate.
        } else if (account != null && accountManager.accountNeedsReauth()) {
            preferenceEyebrowseAccount.isVisible = false
            preferenceEyebrowseAccountAuthError.isVisible = true
            accountPreferenceCategory.isVisible = true

            preferenceSignIn.isVisible = false
            preferenceSignIn.onPreferenceClickListener = null

            preferenceEyebrowseAccountAuthError.email = profile?.email

            // Signed-out.
        } else {
            preferenceSignIn.isVisible = true
            preferenceEyebrowseAccount.isVisible = false
            preferenceEyebrowseAccountAuthError.isVisible = false
            accountPreferenceCategory.isVisible = false
        }
    }

    /**
     * Cancel any running coroutine jobs for loading account images.
     */
    fun cancel() {
        scope.cancel()
    }

    /**
     * Returns generic avatar for accounts.
     */
    private fun genericAvatar(context: Context) =
        AppCompatResources.getDrawable(context, R.drawable.ic_account)

    /**
     * Gets a rounded drawable from a URL if possible, else null.
     */
    private suspend fun toRoundedDrawable(
        url: String,
        context: Context,
    ) = httpClient.bitmapForUrl(url)?.let { bitmap ->
        RoundedBitmapDrawableFactory.create(context.resources, bitmap).apply {
            isCircular = true
            setAntiAlias(true)
        }
    }
}
