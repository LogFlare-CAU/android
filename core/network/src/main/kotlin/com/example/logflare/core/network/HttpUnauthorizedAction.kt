package com.example.logflare.core.network

/**
 * Invoked when an authenticated API response returns HTTP 401.
 *
 * [rejectedAuthorization] is the `Authorization` header the rejected request carried (null when
 * it had none), so the handler can ignore a late 401 for a token that is no longer current.
 */
fun interface HttpUnauthorizedAction {
    fun onUnauthorized(rejectedAuthorization: String?)
}
