/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.onboarding

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import mozilla.components.support.utils.BrowsersCache
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.ext.openChromeAppInfo
import org.eyesearch.eyebrowse.ext.openSetDefaultBrowserOption
import org.eyesearch.eyebrowse.ext.settings

/**
 * The first screen a new user sees: make Eyebrowse the browser, and turn Chrome off.
 *
 * It is shown once. Both actions are things only the user can do, so the screen explains the two
 * steps and takes them straight to the screen that does each one, then gets out of the way.
 */
class EyebrowseSetupDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogContext = requireContext()
        val view = LayoutInflater.from(dialogContext).inflate(R.layout.eyebrowse_setup_dialog, null)

        view.findViewById<View>(R.id.setup_set_default).setOnClickListener {
            val host = activity
            dismiss()
            host?.openSetDefaultBrowserOption(useCustomTab = true)
        }

        view.findViewById<View>(R.id.setup_disable_chrome).setOnClickListener {
            val chromeContext = context
            dismiss()
            chromeContext?.openChromeAppInfo()
        }

        view.findViewById<View>(R.id.setup_later).setOnClickListener {
            dismiss()
        }

        return AlertDialog.Builder(dialogContext)
            .setView(view)
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        // One showing is enough, whatever the user chose; both actions live on in Settings.
        context?.settings()?.showEyebrowseSetup = false
        super.onDismiss(dialog)
    }

    companion object {
        const val TAG = "EyebrowseSetupDialogFragment"

        /**
         * @return true when the screen is worth showing: the user has not seen it, and this is not
         * already the browser that opens links.
         */
        fun shouldShow(context: Context): Boolean =
            context.settings().showEyebrowseSetup &&
                !BrowsersCache.all(context.applicationContext).isDefaultBrowser
    }
}
