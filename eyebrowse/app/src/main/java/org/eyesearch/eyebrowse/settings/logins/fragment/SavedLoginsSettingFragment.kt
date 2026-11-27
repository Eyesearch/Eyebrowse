/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.settings.logins.fragment

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.eyesearch.eyebrowse.GleanMetrics.Logins
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.ext.components
import org.eyesearch.eyebrowse.ext.showToolbar
import org.eyesearch.eyebrowse.settings.RadioButtonPreference
import org.eyesearch.eyebrowse.settings.SharedPreferenceUpdater
import org.eyesearch.eyebrowse.settings.requirePreference
import org.eyesearch.eyebrowse.utils.view.addToRadioGroup

class SavedLoginsSettingFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.save_logins_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_passwords_save_logins))
        val save = bindSave()
        val neverSave = bindNeverSave()
        addToRadioGroup(save, neverSave)
    }

    private fun bindSave(): RadioButtonPreference {
        val preferenceSave = requirePreference<RadioButtonPreference>(R.string.pref_key_save_logins)
        preferenceSave.onPreferenceChangeListener = object : SharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                if (newValue == true) {
                    Logins.saveLoginsSettingChanged.record(
                        Logins.SaveLoginsSettingChangedExtra(
                            Setting.ASK_TO_SAVE.name,
                        ),
                    )
                }
                // We want to reload the current session here so we can try to fill the current page
                context?.components?.useCases?.sessionUseCases?.reload?.invoke()
                return super.onPreferenceChange(preference, newValue)
            }
        }
        return preferenceSave
    }

    private fun bindNeverSave(): RadioButtonPreference {
        val preferenceNeverSave = requirePreference<RadioButtonPreference>(R.string.pref_key_never_save_logins)
        preferenceNeverSave.onPreferenceChangeListener = object : SharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                if (newValue == true) {
                    Logins.saveLoginsSettingChanged.record(
                        Logins.SaveLoginsSettingChangedExtra(
                            Setting.NEVER_SAVE.name,
                        ),
                    )
                }
                // We want to reload the current session here so we don't save any currently inserted login
                context?.components?.useCases?.sessionUseCases?.reload?.invoke()
                return super.onPreferenceChange(preference, newValue)
            }
        }
        return preferenceNeverSave
    }

    companion object {
        // Setting describing the approach of saving logins, used for telemetry
        enum class Setting { NEVER_SAVE, ASK_TO_SAVE }
    }
}
