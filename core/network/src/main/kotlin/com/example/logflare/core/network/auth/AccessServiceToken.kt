package com.example.logflare.core.network.auth

/**
 * Cloudflare Access service token (client id + secret) for the server the app is talking to.
 * [toString] omits both values so a log of the object cannot leak the secret.
 */
class AccessServiceToken(
    val clientId: String,
    val secret: String,
) {
    override fun toString(): String = "AccessServiceToken"
}

/** Supplies the token for the active server, or null when this session does not use Access. */
fun interface AccessServiceTokenProvider {
    fun current(): AccessServiceToken?
}
