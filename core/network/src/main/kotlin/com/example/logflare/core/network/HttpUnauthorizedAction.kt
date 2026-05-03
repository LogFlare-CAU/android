package com.example.logflare.core.network

/**
 * Invoked when an authenticated API response returns HTTP 401.
 * Typically clears the local session so the UI can route to login.
 */
fun interface HttpUnauthorizedAction {
    fun onUnauthorized()
}
