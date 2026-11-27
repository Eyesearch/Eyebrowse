/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.onboarding

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import mozilla.components.lib.state.ext.observeAsComposableState
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.components.accounts.EyebrowseFxAEntryPoint
import org.eyesearch.eyebrowse.components.components
import org.eyesearch.eyebrowse.ext.nav
import org.eyesearch.eyebrowse.ext.settings
import org.eyesearch.eyebrowse.onboarding.view.UpgradeOnboarding
import org.eyesearch.eyebrowse.theme.EyebrowseTheme

/**
 * Dialog displaying a welcome and sync sign in onboarding.
 */
class HomeOnboardingDialogFragment : DialogFragment() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.HomeOnboardingDialogStyle)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            EyebrowseTheme {
                val account =
                    components.backgroundServices.syncStore.observeAsComposableState { state -> state.account }

                UpgradeOnboarding(
                    isSyncSignIn = account.value != null,
                    onDismiss = ::onDismiss,
                    onSignInButtonClick = {
                        findNavController().nav(
                            R.id.homeOnboardingDialogFragment,
                            HomeOnboardingDialogFragmentDirections.actionGlobalTurnOnSync(
                                entrypoint = EyebrowseFxAEntryPoint.HomeOnboardingDialog,
                            ),
                        )
                        onDismiss()
                    },
                )
            }
        }
    }

    private fun onDismiss() {
        context?.settings()?.showHomeOnboardingDialog = false
        dismiss()
    }
}
