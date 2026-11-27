/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.vpn

/**
 * Conservative OpenVPN input gate for a future in-process backend.
 *
 * A remote profile is untrusted input, never a command line. Reject anything
 * outside the deliberately narrow grammar rather than silently repairing it.
 * This does not authenticate a relay, establish a tunnel, or prevent leaks.
 */
internal object OpenVpnProfilePolicy {
    private const val MAX_PROFILE_CHARS = 64 * 1024
    private const val MAX_LINE_CHARS = 4096
    private val remoteHost = Regex("[A-Za-z0-9.-]{1,253}")
    private val safeCiphers = setOf("AES-128-GCM", "AES-256-GCM", "AES-128-CBC", "AES-256-CBC")
    private val safeAuth = setOf("SHA256", "SHA384", "SHA512")
    private val inlineTags = setOf("ca", "cert", "key", "tls-auth", "tls-crypt")

    /** Never print or persist this object: the profile can contain private keys. */
    class ValidatedProfile internal constructor(internal val content: String)

    fun validate(profile: String, expectedHost: String): ValidatedProfile? {
        if (profile.length !in 1..MAX_PROFILE_CHARS || !remoteHost.matches(expectedHost)) return null
        if (profile.any { it.isISOControl() && it != '\r' && it != '\n' && it != '\t' }) return null
        val lines = profile.lineSequence()
        val seen = mutableSetOf<String>()
        val tags = mutableSetOf<String>()
        var inline: String? = null
        var inlineHasData = false
        var validRemote = false
        for (raw in lines) {
            if (raw.length > MAX_LINE_CHARS) return null
            val line = raw.trim()
            if (inline != null) {
                if (line == "</$inline>") {
                    if (!inlineHasData) return null
                    inline = null
                    continue
                }
                // No nested tags, option syntax, comments, or external file paths.
                if (line.isNotEmpty()) {
                    if (!line.matches(Regex("[A-Za-z0-9+/= -]+"))) return null
                    inlineHasData = true
                }
                continue
            }
            if (line.isBlank() || line.startsWith("#") || line.startsWith(";")) continue
            if (line.startsWith("<") && line.endsWith(">")) {
                val tag = line.substring(1, line.length - 1)
                if (tag !in inlineTags || !tags.add(tag)) return null
                inline = tag
                inlineHasData = false
                continue
            }
            // Reject quoting, escape sequences, continuations and inline comments;
            // prevents differences between this validator and the native parser.
            if (line.any { it == '"' || it == '\'' || it == '\\' || it == '#' || it == ';' }) return null
            val parts = line.split(Regex("\\s+"))
            val option = parts.first()
            val args = parts.drop(1)
            if (!seen.add(option)) return null
            when (option) {
                "client", "nobind", "persist-key", "persist-tun", "auth-nocache", "pull" ->
                    if (args.isNotEmpty()) return null
                "dev" -> if (args != listOf("tun")) return null
                "proto" -> if (args.singleOrNull() !in setOf("udp", "tcp-client", "tcp")) return null
                "remote" -> {
                    if (args.size != 2 || !args[0].equals(expectedHost, ignoreCase = true) ||
                        (args[1].toIntOrNull() ?: -1) !in 1..65535
                    ) return null
                    validRemote = true
                }
                "remote-cert-tls" -> if (args != listOf("server")) return null
                "tls-version-min" -> if (args != listOf("1.2") && args != listOf("1.3")) return null
                "cipher" -> if (args.singleOrNull() !in safeCiphers) return null
                "data-ciphers" -> if (args.size != 1 || args[0].split(':').any { it !in safeCiphers }) return null
                "auth" -> if (args.singleOrNull() !in safeAuth) return null
                "resolv-retry" -> if (args != listOf("infinite") && (args.singleOrNull()?.toIntOrNull() ?: -1) !in 0..600) return null
                "verb" -> if ((args.singleOrNull()?.toIntOrNull() ?: -1) !in 0..3) return null
                "auth-user-pass" -> if (args.isNotEmpty()) return null
                "key-direction" -> if (args.singleOrNull() !in setOf("0", "1")) return null
                "redirect-gateway" -> if (args != listOf("def1")) return null
                else -> return null
            }
        }
        if (inline != null || !validRemote || "ca" !in tags ||
            !seen.containsAll(setOf("client", "dev", "proto", "remote-cert-tls", "tls-version-min"))
        ) return null
        if (("cert" in tags) != ("key" in tags)) return null
        if ("tls-auth" in tags && "tls-crypt" in tags) return null
        if ("key-direction" in seen && "tls-auth" !in tags) return null
        return ValidatedProfile(profile)
    }
}
