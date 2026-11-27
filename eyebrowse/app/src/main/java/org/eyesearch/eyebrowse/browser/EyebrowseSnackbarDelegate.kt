/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.browser

import android.view.View
import androidx.annotation.StringRes
import mozilla.components.ui.widgets.SnackbarDelegate
import org.eyesearch.eyebrowse.components.EyebrowseSnackbar

class EyebrowseSnackbarDelegate(private val view: View) : SnackbarDelegate {

    override fun show(
        snackBarParentView: View,
        @StringRes text: Int,
        duration: Int,
        @StringRes action: Int,
        listener: ((v: View) -> Unit)?,
    ) {
        if (listener != null && action != 0) {
            EyebrowseSnackbar.make(
                view = view,
                duration = EyebrowseSnackbar.LENGTH_SHORT,
                isDisplayedWithBrowserToolbar = true,
            )
                .setText(view.context.getString(text))
                .setAction(view.context.getString(action)) { listener.invoke(view) }
                .show()
        } else {
            EyebrowseSnackbar.make(
                view,
                duration = EyebrowseSnackbar.LENGTH_SHORT,
                isDisplayedWithBrowserToolbar = true,
            )
                .setText(view.context.getString(text))
                .show()
        }
    }
}
