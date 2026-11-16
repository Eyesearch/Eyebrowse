/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.account

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components

/**
 * Indo Account inside the app, so the sign-in finishes without the user tapping anything.
 *
 * The page runs in a WebView owned by this activity. The moment it tries to open `indobrowse://auth`
 * the load is stopped, the code and state are read, the state is checked against the stored one, the
 * WebView closes and the code is exchanged.
 *
 * The same activity shows the management panel, opened with the app token in the URL fragment so the
 * panel already knows who is asking. A fragment never reaches a server and is never logged here.
 *
 * The deep link activity remains the fallback for devices without a usable WebView.
 */
class IndoAccountWebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SIGN_IN
        val url = intent.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            finish()
            return
        }

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean = handleUri(request.url, mode)

                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                    handleUri(Uri.parse(url), mode)
            }
        }

        setContentView(webView)
        webView.loadUrl(url)
    }

    /** True means we handled it and the WebView must not load it. */
    private fun handleUri(uri: Uri, mode: String): Boolean {
        if (!uri.toString().startsWith(IndoAccountConfig.DEEPLINK)) {
            return false
        }

        if (mode != MODE_SIGN_IN) {
            return true
        }

        val code = uri.getQueryParameter(QUERY_CODE)
        val state = uri.getQueryParameter(QUERY_STATE)

        webView.stopLoading()
        finishWithExchange(code = code, state = state)
        return true
    }

    private fun finishWithExchange(code: String?, state: String?) {
        lifecycleScope.launch {
            val errorRes = IndoAccountHandoff(
                context = applicationContext,
                client = components.core.client,
            ).complete(code = code, state = state)

            if (errorRes == null) {
                Toast.makeText(this@IndoAccountWebViewActivity, R.string.indo_account_signed_in, Toast.LENGTH_SHORT)
                    .show()
                setResult(RESULT_OK)
            } else {
                Toast.makeText(this@IndoAccountWebViewActivity, errorRes, Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.loadUrl(BLANK_PAGE)
            webView.clearHistory()
            webView.clearCache(true)
            webView.destroy()
            // Nothing from the panel is kept on the device once this screen closes.
            CookieManager.getInstance().removeAllCookies(null)
        }
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_MODE = "indo_account_mode"
        private const val EXTRA_URL = "indo_account_url"
        private const val QUERY_CODE = "code"
        private const val QUERY_STATE = "state"
        private const val BLANK_PAGE = "about:blank"

        const val MODE_SIGN_IN = "sign_in"
        const val MODE_MANAGE = "manage"

        fun intent(context: Context, url: String, mode: String): Intent =
            Intent(context, IndoAccountWebViewActivity::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_MODE, mode)
    }
}
