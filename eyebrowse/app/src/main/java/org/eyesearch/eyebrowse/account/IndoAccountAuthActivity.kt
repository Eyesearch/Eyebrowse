/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.account

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.ext.components

/**
 * The door the account comes back through: indobrowse://auth?code=...&state=...
 *
 * It checks the state against the one we wrote before leaving, refuses anything that does not match,
 * exchanges the code once, stores the token, and closes. It never keeps the code, never logs it, and
 * never shows it.
 */
class IndoAccountAuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent?.data
        val code = data?.getQueryParameter(QUERY_CODE)
        val state = data?.getQueryParameter(QUERY_STATE)

        if (code.isNullOrBlank() || state.isNullOrBlank()) {
            finishWithMessage(R.string.indo_account_error_missing_code)
            return
        }

        lifecycleScope.launch {
            val errorRes = IndoAccountHandoff(
                context = applicationContext,
                client = components.core.client,
            ).complete(code = code, state = state)

            if (errorRes == null) {
                Toast.makeText(
                    this@IndoAccountAuthActivity,
                    R.string.indo_account_signed_in,
                    Toast.LENGTH_SHORT,
                ).show()
                setResult(RESULT_OK)
                finish()
            } else {
                finishWithMessage(errorRes)
            }
        }
    }

    private fun finishWithMessage(messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        private const val QUERY_CODE = "code"
        private const val QUERY_STATE = "state"
    }
}
