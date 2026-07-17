package com.example.logflare.core.network.host

interface MutableBaseUrlProvider : BaseUrlProvider {
    suspend fun setBaseUrl(url: String)
}
