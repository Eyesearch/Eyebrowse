/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.account

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.eyesearch.eyebrowse.account.IndoAccountEntry

/**
 * Eyebrowse: this used to be Mozilla's sign-in screen, with its camera pairing and its email
 * fallback. It is not ours, so it is gone: the screen draws nothing and exists only as a door.
 *
 * Every place in the app that still sends a user here, bookmarks, the toolbar menu, the home menu,
 * autofill, saved logins, the share sheet, the tabs tray and onboarding, now lands on the Indo
 * Account handoff instead, and this closes itself behind it.
 */
class TurnOnSyncFragment : Fragment() {

    override fun onStart() {
        super.onStart()
        IndoAccountEntry.startSignIn(requireContext())
        findNavController().popBackStack()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Nothing is read from the arguments: this screen has no content of its own any more.
    }
}
