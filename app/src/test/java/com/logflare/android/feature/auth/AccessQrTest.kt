package com.logflare.android.feature.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessQrTest {
    private val id = "abcdefghijklmnopqrstuvwxyz"
    private val secret = "super-secret-value"
    private val raw = """{"v":1,"server":"https://logs.example.com","id":"$id","secret":"$secret"}"""

    @Test fun parsesV1HttpsQr() {
        val qr = parseAccessQr(raw)!!
        assertEquals("https://logs.example.com", qr.server)
        assertEquals(id, qr.clientId)
        assertEquals(secret, qr.secret)
        assertEquals("abcd…wxyz", qr.maskedClientId())
        assertTrue(!qr.toString().contains(secret))
    }

    @Test fun rejectsWrongVersionBlankAndHttp() {
        assertNull(parseAccessQr("""{"v":2,"server":"https://logs.example.com","id":"abcdefghij","secret":"s"}"""))
        assertNull(parseAccessQr("""{"v":1,"server":"https://logs.example.com","id":"","secret":"s"}"""))
        assertNull(parseAccessQr("""{"v":1,"server":"http://logs.example.com","id":"abcdefghij","secret":"s"}"""))
        assertNull(parseAccessQr("not json"))
        assertNull(parseAccessQr(""))
    }

    @Test fun shortIdIsNotShownWhole() {
        assertEquals("abcd…", maskClientId("abcdefgh"))
        assertEquals("••••", maskClientId("ab"))
    }
}
