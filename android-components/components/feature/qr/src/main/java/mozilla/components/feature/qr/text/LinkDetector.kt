/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.qr.text

/**
 * Decides whether a recognised word or line is something the user can open.
 *
 * Deliberately conservative: a false positive shows a Website action that leads nowhere, which
 * is worse than asking the user to copy the text instead.
 */
object LinkDetector {

    private val scheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
    private val email = Regex("^[^@\\s]+@[^@\\s]+\\.[a-zA-Z]{2,}$")
    private val domain = Regex(
        "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}(?:[/?#][^\\s]*)?$",
    )
    private val www = Regex("^www\\.[^\\s]+$")

    /**
     * @return true when [text] looks like something that can be opened in the browser.
     */
    fun isOpenable(text: String): Boolean {
        val candidate = trimEdges(text)
        if (candidate.isEmpty()) return false
        if (email.matches(candidate)) return true
        if (scheme.containsMatchIn(candidate)) return isHttp(candidate) || candidate.startsWith("mailto:")
        if (www.matches(candidate)) return true
        return domain.matches(candidate)
    }

    /**
     * @return the web URL to open for [text], adding a scheme when the text omits it, or null when
     * there is nothing here the browser can open.
     *
     * A recogniser splits one address into several words as easily as it reads it whole, so
     * `www .eyebrowse. in` is joined back together before this gives up.
     *
     * Only http and https come back. An email address is a link in the everyday sense, but the
     * screen that shows the result only accepts web links and answers anything else with its own
     * "invalid" dialog, so an email is left to Copy instead of opening a dead end.
     */
    fun toUrl(text: String): String? {
        val candidate = trimEdges(text)
        if (candidate.isEmpty()) return null

        val url = when {
            isOpenable(candidate) -> withScheme(candidate)
            else -> {
                val joined = candidate.filterNot { it.isWhitespace() }
                if (joined != candidate && isOpenable(joined)) withScheme(joined) else null
            }
        }
        return url?.takeIf { isHttp(it) }
    }

    private fun trimEdges(text: String): String =
        text.trim().trim('.', ',', ';', ':', ')', ']', '}', '"', '\'')

    private fun withScheme(candidate: String): String = when {
        scheme.containsMatchIn(candidate) -> candidate
        // An email address has no web form, so this never produces a URL for one.
        email.matches(candidate) -> "mailto:$candidate"
        else -> "https://$candidate"
    }

    private fun isHttp(candidate: String): Boolean =
        candidate.startsWith("http://", ignoreCase = true) ||
            candidate.startsWith("https://", ignoreCase = true)
}
