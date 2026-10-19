package com.logflare.android.feature.auth

import java.net.URI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Payload of an Access sign-in QR: `{"v":1,"server":"https://…","id":"…","secret":"…"}`.
 * [toString] leaves the secret out.
 */
class AccessQr(
    val server: String,
    val clientId: String,
    val secret: String,
) {
    fun maskedClientId(): String = maskClientId(clientId)

    override fun toString(): String = "AccessQr(server=$server, clientId=${maskedClientId()})"
}

/** First and last four characters. A short id is not shown in full. */
fun maskClientId(id: String): String = when {
    id.length > 8 -> id.take(4) + "…" + id.takeLast(4)
    id.length >= 4 -> id.take(4) + "…"
    else -> "••••"
}

private val qrJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class AccessQrJson(
    val v: Int? = null,
    val server: String? = null,
    val id: String? = null,
    val secret: String? = null,
)

/** Null when the text is not a v1 Access QR, or the server is not https. */
fun parseAccessQr(raw: String): AccessQr? {
    val parsed = try {
        qrJson.decodeFromString<AccessQrJson>(raw.trim())
    } catch (_: Exception) {
        return null
    }
    if (parsed.v != 1) return null
    val server = parsed.server?.trim()?.takeIf { isHttpsServer(it) } ?: return null
    val id = parsed.id?.trim().orEmpty()
    val secret = parsed.secret?.trim().orEmpty()
    if (id.isEmpty() || secret.isEmpty() || id.length > MAX_FIELD || secret.length > MAX_FIELD) return null
    return AccessQr(server, id, secret)
}

private fun isHttpsServer(value: String): Boolean {
    if (!value.startsWith("https://", ignoreCase = true)) return false
    return try {
        !URI(value).host.isNullOrBlank()
    } catch (_: Exception) {
        false
    }
}

private const val MAX_FIELD = 512
