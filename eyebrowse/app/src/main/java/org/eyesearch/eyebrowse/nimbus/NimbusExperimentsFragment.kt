/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.nimbus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.ext.components
import org.eyesearch.eyebrowse.ext.showToolbar
import org.eyesearch.eyebrowse.nimbus.view.NimbusExperiments
import org.eyesearch.eyebrowse.theme.EyebrowseTheme

/**
 * Fragment use for managing Nimbus experiments.
 */
class NimbusExperimentsFragment : Fragment() {

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_nimbus_experiments))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            EyebrowseTheme {
                val experiments =
                    requireContext().components.nimbus.sdk.getAvailableExperiments()

                NimbusExperiments(
                    experiments = experiments,
                    onExperimentClick = { experiment ->
                        val directions =
                            NimbusExperimentsFragmentDirections.actionNimbusExperimentsFragmentToNimbusBranchesFragment(
                                experimentId = experiment.slug,
                                experimentName = experiment.userFacingName,
                            )

                        findNavController().navigate(directions)
                    },
                )
            }
        }
    }
}
