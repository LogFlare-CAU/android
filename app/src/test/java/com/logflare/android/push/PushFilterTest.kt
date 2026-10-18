package com.logflare.android.push

import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class PushFilterTest {

    private val signedIn = SessionState.SignedIn(
        serverUrl = "https://logs.example.com",
        token = "Bearer t",
        username = "me",
        userIdx = 7,
        permission = 0,
        key = SessionKey("aaaaaaaaaaaaaaaa"),
    )
    private val noon = LocalTime.of(12, 0)

    private fun msg(
        level: LfLevel = LfLevel.Error,
        projectId: Int? = 1,
        message: String = "something broke",
        isTest: Boolean = false,
    ) = PushMessage(
        errorId = 42,
        projectId = projectId,
        type = "ValueError",
        level = level,
        message = message,
        timestamp = "2026-10-18T09:30:00",
        isTest = isTest,
    )

    @Test
    fun signedOut_drops() {
        assertEquals(PushDecision.Drop, decidePush(msg(), SessionState.SignedOut, AlertSettings(), noon))
        assertEquals(PushDecision.Drop, decidePush(msg(), SessionState.Loading, AlertSettings(), noon))
        assertEquals(PushDecision.Drop, decidePush(msg(isTest = true), SessionState.SignedOut, AlertSettings(), noon))
    }

    @Test
    fun globalOff_drops() {
        val settings = AlertSettings(
            global = GlobalAlert.Off,
            projects = mapOf(1 to ProjectAlert(level = LfLevel.Debug)),
        )
        assertEquals(PushDecision.Drop, decidePush(msg(level = LfLevel.Critical), signedIn, settings, noon))
    }

    @Test
    fun test_alwaysNotifies_evenWhenOff() {
        val settings = AlertSettings(
            global = GlobalAlert.Off,
            quietHours = true,
            projects = mapOf(1 to ProjectAlert(level = LfLevel.Critical, mutedKeywords = setOf("broke"))),
        )
        val test = msg(level = LfLevel.Debug, isTest = true)
        assertEquals(PushDecision.Notify, decidePush(test, signedIn, settings, LocalTime.of(23, 30)))
    }

    @Test
    fun projectLevel_overridesGlobal() {
        val settings = AlertSettings(
            global = GlobalAlert.ErrorAndAbove,
            projects = mapOf(1 to ProjectAlert(level = LfLevel.Warning)),
        )
        assertEquals(PushDecision.Notify, decidePush(msg(level = LfLevel.Warning), signedIn, settings, noon))
        assertEquals(PushDecision.Drop, decidePush(msg(level = LfLevel.Info), signedIn, settings, noon))

        // A stricter project level wins over a looser global one, too.
        val strict = AlertSettings(
            global = GlobalAlert.Everything,
            projects = mapOf(1 to ProjectAlert(level = LfLevel.Critical)),
        )
        assertEquals(PushDecision.Drop, decidePush(msg(level = LfLevel.Error), signedIn, strict, noon))
    }

    @Test
    fun unknownProject_usesGlobalThreshold() {
        val settings = AlertSettings(
            global = GlobalAlert.ErrorAndAbove,
            projects = mapOf(1 to ProjectAlert(level = LfLevel.Debug)),
        )
        assertEquals(PushDecision.Notify, decidePush(msg(level = LfLevel.Error, projectId = 99), signedIn, settings, noon))
        assertEquals(PushDecision.Drop, decidePush(msg(level = LfLevel.Warning, projectId = 99), signedIn, settings, noon))
        // No project id at all, and a project entry with keywords but no own level.
        assertEquals(PushDecision.Notify, decidePush(msg(level = LfLevel.Error, projectId = null), signedIn, settings, noon))
        val keywordsOnly = AlertSettings(projects = mapOf(1 to ProjectAlert(mutedKeywords = setOf("x"))))
        assertEquals(PushDecision.Drop, decidePush(msg(level = LfLevel.Warning), signedIn, keywordsOnly, noon))
        assertEquals(PushDecision.Notify, decidePush(msg(level = LfLevel.Error), signedIn, keywordsOnly, noon))
    }

    @Test
    fun mutedKeyword_caseInsensitive_drops() {
        val settings = AlertSettings(projects = mapOf(1 to ProjectAlert(mutedKeywords = setOf("TimeOut"))))
        assertEquals(
            PushDecision.Drop,
            decidePush(msg(message = "upstream request timeout after 30s"), signedIn, settings, noon),
        )
        assertEquals(PushDecision.Notify, decidePush(msg(message = "disk full"), signedIn, settings, noon))
        // Keywords are per project.
        assertEquals(
            PushDecision.Notify,
            decidePush(msg(projectId = 2, message = "request timeout"), signedIn, settings, noon),
        )
    }

    @Test
    fun quietHours_wrapsMidnight() {
        val settings = AlertSettings(quietHours = true)
        val error = msg(level = LfLevel.Error)
        assertEquals(PushDecision.RecordOnly, decidePush(error, signedIn, settings, LocalTime.of(23, 30)))
        assertEquals(PushDecision.RecordOnly, decidePush(error, signedIn, settings, LocalTime.of(23, 0)))
        assertEquals(PushDecision.RecordOnly, decidePush(error, signedIn, settings, LocalTime.of(0, 0)))
        assertEquals(PushDecision.RecordOnly, decidePush(error, signedIn, settings, LocalTime.of(6, 59)))
        assertEquals(PushDecision.Notify, decidePush(error, signedIn, settings, LocalTime.of(7, 0)))
        assertEquals(PushDecision.Notify, decidePush(error, signedIn, settings, LocalTime.of(22, 59)))
        // Off: never quiet.
        assertEquals(PushDecision.Notify, decidePush(error, signedIn, AlertSettings(), LocalTime.of(23, 30)))
    }

    @Test
    fun quietHours_criticalNotifies() {
        val settings = AlertSettings(quietHours = true)
        assertEquals(
            PushDecision.Notify,
            decidePush(msg(level = LfLevel.Critical), signedIn, settings, LocalTime.of(2, 0)),
        )
        // Quiet hours never record something that is below the threshold anyway.
        assertEquals(
            PushDecision.Drop,
            decidePush(msg(level = LfLevel.Info), signedIn, settings, LocalTime.of(2, 0)),
        )
    }

    @Test
    fun globalThresholds() {
        assertEquals(null, GlobalAlert.Off.threshold())
        assertEquals(LfLevel.Critical, GlobalAlert.CriticalOnly.threshold())
        assertEquals(LfLevel.Error, GlobalAlert.ErrorAndAbove.threshold())
        assertEquals(LfLevel.Warning, GlobalAlert.WarningAndAbove.threshold())
        assertEquals(LfLevel.Debug, GlobalAlert.Everything.threshold())
    }

    @Test
    fun pushMessage_fromData() {
        val m = PushMessage.from(
            mapOf(
                "errorid" to "12",
                "projectid" to "3",
                "type" to "KeyError",
                "level" to "WARNING",
                "message" to "boom",
                "timestamp" to "2026-10-18T09:30:00",
                "test" to "true",
            ),
        )
        assertEquals(PushMessage(12, 3, "KeyError", LfLevel.Warning, "boom", "2026-10-18T09:30:00", true), m)

        val empty = PushMessage.from(emptyMap())
        assertEquals(PushMessage(0, null, "Unknown", LfLevel.Info, "", "", false), empty)
    }
}
