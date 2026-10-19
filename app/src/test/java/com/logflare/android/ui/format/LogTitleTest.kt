package com.logflare.android.ui.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogTitleTest {
    private fun check(expected: String, message: String) = assertEquals(expected, logTitle(message))

    @Test
    fun traceback_usesFinalExceptionLine() {
        val tb = "Traceback (most recent call last):\n" +
            "  File \"/app/src/market/candles.py\", line 42, in fetch\n" +
            "    raise ValueError(\"bad candle\")\n" +
            "ValueError: bad candle"
        check("ValueError: bad candle", tb)
        check("ValueError: bad candle", tb + "\n")
        check("ValueError: bad candle", tb.replace("\n", "\r\n"))
    }

    @Test
    fun chainedTraceback_usesLastException() {
        val tb = "Traceback (most recent call last):\n  File \"a.py\", line 1\nKeyError: 'x'\n\n" +
            "During handling of the above exception, another exception occurred:\n\n" +
            "Traceback (most recent call last):\n  File \"b.py\", line 2\n    ^^^^\nRuntimeError: wrapped"
        check("RuntimeError: wrapped", tb)
    }

    @Test
    fun tracebackAfterLogLine_stillUsesException() {
        check("OSError: disk full", "2026-10-19 13:15:27 - app - ERROR - save failed\nTraceback (most recent call last):\n  File \"x.py\", line 1\nOSError: disk full")
    }

    @Test
    fun tracebackWithoutExceptionLine_fallsBackToFirstLine() {
        check("Traceback (most recent call last):", "Traceback (most recent call last):\n  File \"x.py\", line 1\n    foo()")
    }

    @Test
    fun pythonLoggingPrefix_isStripped() {
        check(
            "Toss minute candles failed for WKEY/us",
            "2026-10-19 13:15:27 - src.market.candles - WARNING - Toss minute candles failed for WKEY/us",
        )
        check("started", "2026-10-19 13:15:27,123 - app - INFO - started")
        check("x", "2026-10-19 13:15:27.123456 - a.b_c - CRITICAL - x")
    }

    @Test
    fun goStylePrefix_isStripped() {
        check("connection refused", "2026-10-19 02:26:47 ERROR  to/to.go:214 connection refused")
        check("message", "2026-10-19 02:26:47 ERROR  to/to.go:2... message")
        check("boom", "2026/10/19 02:26:47 WARN main.go:12:5: boom")
    }

    @Test
    fun isoTimestamps_areStripped() {
        check("hello", "2026-10-19T13:15:27Z hello")
        check("hello", "2026-10-19T13:15:27.123+09:00 hello")
        check("hello", "[2026-10-19 13:15:27] hello")
        check("hello", "2026-10-19 hello")
        check("hello", "2026-10-19 13:15:27 - hello")
    }

    @Test
    fun levelForms_areStripped() {
        check("disk full", "[ERROR] disk full")
        check("disk full", "ERROR: disk full")
        check("disk full", "WARNING - disk full")
        check("disk full", "[warn] disk full".replace("warn", "WARN"))
        check("disk full", "CRITICAL disk full")
        check("disk full", "2026-10-19 13:15:27 [ERROR] disk full")
        check("disk full", "2026-10-19 13:15:27 ERROR: disk full")
    }

    @Test
    fun plainMessages_areUntouched() {
        check("Error handling failed", "Error handling failed")
        check("Connection to db lost", "Connection to db lost")
        check("2026 was a year", "2026 was a year")
        check("main.go:12 is mentioned", "main.go:12 is mentioned")
        check("INFORMATION leaked", "INFORMATION leaked")
    }

    @Test
    fun multiLine_usesFirstNonEmptyLine() {
        check("first", "\n  \nfirst\nsecond")
        check("first", "first\r\nsecond\r\n")
        check("NullPointerException in PaymentService", "NullPointerException in PaymentService\n\tat a.b.C(C.java:1)")
    }

    @Test
    fun neverEmpty_fallsBackToTrimmedFirstLine() {
        check("[ERROR]", "[ERROR]")
        check("2026-10-19 13:15:27", "  2026-10-19 13:15:27  ")
        check("ERROR:", "ERROR:")
        check("", "")
        check("", " \n \r\n")
    }

    @Test
    fun unicode_isKept() {
        check("토스 분봉 조회 실패: 시장 없음", "2026-10-19 13:15:27 - src.market - ERROR - 토스 분봉 조회 실패: 시장 없음")
        check("ValueError: 잘못된 캔들 💥", "Traceback (most recent call last):\n  File \"a.py\", line 1\nValueError: 잘못된 캔들 💥")
    }

    @Test
    fun veryLong_isCappedWithEllipsis() {
        val long = "x".repeat(5_000)
        val title = logTitle(long)
        assertEquals(LOG_TITLE_MAX, title.length)
        assertTrue(title.endsWith("…"))
        check("short", "short")
    }

    @Test
    fun longTraceback_stillFindsTheEndQuickly() {
        val frames = (1..2_000).joinToString("\n") { "  File \"f$it.py\", line $it, in g\n    call()" }
        check("TimeoutError: slow", "Traceback (most recent call last):\n$frames\nTimeoutError: slow")
    }
}
