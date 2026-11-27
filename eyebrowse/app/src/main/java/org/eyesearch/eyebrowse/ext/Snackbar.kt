/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.ext

import android.view.View
import org.eyesearch.eyebrowse.components.EyebrowseSnackbar

/**
 * Shows the Fenix Snackbar in the given view along with the provided text.
 *
 * @param view A [View] used to determine a parent for the [EyebrowseSnackbar].
 * @param text The text to display in the [EyebrowseSnackbar].
 * @param duration The duration to show the [EyebrowseSnackbar] for.
 */
internal fun showSnackBar(view: View, text: String, duration: Int = EyebrowseSnackbar.LENGTH_SHORT) {
    EyebrowseSnackbar.make(
        view = view,
        duration = duration,
        isDisplayedWithBrowserToolbar = true,
    )
        .setText(text)
        .show()
}
