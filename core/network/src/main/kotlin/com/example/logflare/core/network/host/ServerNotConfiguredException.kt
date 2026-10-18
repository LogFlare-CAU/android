package com.example.logflare.core.network.host

import java.io.IOException

/** Thrown instead of sending a request when no valid server URL has been configured. */
class ServerNotConfiguredException : IOException("Server URL is not configured")
