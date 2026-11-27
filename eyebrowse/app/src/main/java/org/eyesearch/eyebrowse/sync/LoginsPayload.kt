/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.sync

import org.json.JSONObject

/**
 * A saved login, on its way to the server and back.
 *
 * Nothing here is stored in the clear once it leaves this object: the JSON is encrypted with the
 * Keystore key before it becomes a payload, so what the server holds is unreadable even to us.
 * The reverse only works on the phone that made it, which is the whole point.
 */
object LoginsPayload {

    /** Turns a login into the opaque string that is uploaded. */
    fun seal(host: String, username: String, password: String): String {
        val json = JSONObject()
            .put("host", host)
            .put("username", username)
            .put("password", password)
            .toString()
        return LoginVault.encrypt(json)
    }

    /** Reads a payload back, or null when it was not made by this phone. */
    fun open(payload: String): JSONObject? = runCatching {
        JSONObject(LoginVault.decrypt(payload))
    }.getOrNull()
}
