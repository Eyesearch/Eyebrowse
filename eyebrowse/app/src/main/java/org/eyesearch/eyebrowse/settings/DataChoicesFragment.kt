/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.ext.showToolbar

/**
 * Eyebrowse: this screen held Mozilla's usage data and marketing switches. Both are gone and the
 * upload is forced off in code, so the screen is now empty on purpose.
 *
 * It still exists because two places can navigate here, the settings list and the Nimbus branches
 * screen, and a route that leads to nothing is better than a route that crashes.
 */
class DataChoicesFragment : PreferenceFragmentCompat() {

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_data_collection))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.data_choices_preferences, rootKey)
    }
}
