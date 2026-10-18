package com.logflare.android.data.session

import com.logflare.android.data.ServerConfigRepository
import java.security.MessageDigest

/**
 * Identifies whose data a cache entry belongs to: one server + one user.
 *
 * `sha256("$normalizedServerUrl|$userIdx")` as hex, first 16 chars. When the user index is not
 * known yet (profile never fetched), the username is used instead: `"$normalizedServerUrl|u:$username"`.
 */
@JvmInline
value class SessionKey(val value: String) {
    companion object {
        fun of(serverUrl: String, userIdx: Int?): SessionKey = of(serverUrl, userIdx, username = "")

        fun of(serverUrl: String, userIdx: Int?, username: String): SessionKey {
            val url = ServerConfigRepository.normalizeServerUrl(serverUrl)
            val input = if (userIdx != null) "$url|$userIdx" else "$url|u:$username"
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return SessionKey(digest.joinToString("") { "%02x".format(it) }.take(16))
        }
    }
}
