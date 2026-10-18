package com.example.logflare.core.network.auth

/**
 * Synchronous source of the current Authorization header value (e.g. "Bearer xyz").
 * Implementations must not block; back them with an in-memory snapshot.
 */
fun interface AuthTokenProvider {
    fun currentToken(): String?
}
