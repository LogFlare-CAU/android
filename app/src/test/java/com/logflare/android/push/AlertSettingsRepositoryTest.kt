package com.logflare.android.push

import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.model.ProjectDTO
import com.example.logflare.core.model.ProjectData
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.FileCacheStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AlertSettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val sessions = MutableStateFlow<SessionState>(signedIn(A))
    private var legacy = LegacyAlerts(globalLevel = null, projects = emptyList())
    private var legacyReads = 0

    private fun TestScope.newRepo(): AlertSettingsRepository = AlertSettingsRepository(
        sessions = sessions,
        store = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler)),
        legacy = { legacyReads++; legacy },
        scope = backgroundScope,
        clearLegacy = { legacy = LegacyAlerts(globalLevel = null, projects = emptyList()) },
    )

    @Test
    fun migratesLegacyGlobalAndProjectValues() = runTest {
        legacy = LegacyAlerts(
            globalLevel = "Error",
            projects = listOf(
                ProjectData(ProjectDTO(1, "alpha"), excludeKeywords = setOf("timeout"), alertLevel = "Error"),
                ProjectData(ProjectDTO(2, "beta"), alertLevel = "Critical"),
                // The old default level is not an override: these follow the global value.
                ProjectData(ProjectDTO(3, "gamma"), excludeKeywords = setOf("noise"), alertLevel = "Warning"),
                ProjectData(ProjectDTO(4, "delta"), alertLevel = "warning"),
            ),
        )
        val repo = newRepo()
        runCurrent()

        val expected = AlertSettings(
            global = GlobalAlert.ErrorAndAbove,
            quietHours = false,
            projects = mapOf(
                1 to ProjectAlert(LfLevel.Error, setOf("timeout")),
                2 to ProjectAlert(LfLevel.Critical, emptySet()),
                3 to ProjectAlert(null, setOf("noise")),
            ),
        )
        assertEquals(expected, repo.settings.value)

        // Migrated once: a later load reads the saved settings, not the legacy stores.
        legacy = LegacyAlerts(globalLevel = "Critical", projects = emptyList())
        val reads = legacyReads
        val again = newRepo()
        runCurrent()
        assertEquals(expected, again.settings.value)
        assertEquals(reads, legacyReads)
    }

    @Test
    fun legacyMigratesOncePerInstall_laterSessionStartsWithDefaults() = runTest {
        legacy = LegacyAlerts(globalLevel = "Error", projects = emptyList())
        val repo = newRepo()
        runCurrent()
        assertEquals(GlobalAlert.ErrorAndAbove, repo.settings.value.global)

        // A different user/server signs in later: the legacy values were consumed by the first.
        sessions.value = signedIn(B)
        runCurrent()
        assertEquals(AlertSettings(), repo.settings.value)
    }

    @Test
    fun unreadableLegacy_isNotCleared() = runTest {
        var cleared = false
        val repo = AlertSettingsRepository(
            sessions = sessions,
            store = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler)),
            legacy = { error("boom") },
            scope = backgroundScope,
            clearLegacy = { cleared = true },
        )
        runCurrent()
        assertEquals(AlertSettings(), repo.settings.value)
        assertFalse(cleared)
    }

    @Test
    fun migratesUnsetLegacyGlobal_asWarningAndAbove() = runTest {
        legacy = LegacyAlerts(
            globalLevel = null,
            projects = listOf(ProjectData(ProjectDTO(1, "alpha"), alertLevel = "Warning")),
        )
        val repo = newRepo()
        runCurrent()
        assertEquals(AlertSettings(global = GlobalAlert.WarningAndAbove), repo.settings.value)
    }

    @Test
    fun noLegacyData_keepsDefaults() = runTest {
        legacy = LegacyAlerts(globalLevel = null, projects = emptyList())
        val repo = newRepo()
        runCurrent()
        assertEquals(AlertSettings(), repo.settings.value)
        assertEquals(GlobalAlert.ErrorAndAbove, repo.settings.value.global)
    }

    @Test
    fun legacyGlobalLabels_mapToNearest() {
        assertEquals(GlobalAlert.Everything, GlobalAlert.fromLegacyLabel("Debug"))
        assertEquals(GlobalAlert.Everything, GlobalAlert.fromLegacyLabel("Info"))
        assertEquals(GlobalAlert.WarningAndAbove, GlobalAlert.fromLegacyLabel("Warning"))
        assertEquals(GlobalAlert.ErrorAndAbove, GlobalAlert.fromLegacyLabel("error"))
        assertEquals(GlobalAlert.CriticalOnly, GlobalAlert.fromLegacyLabel("Critical"))
        assertEquals(GlobalAlert.ErrorAndAbove, GlobalAlert.fromLegacyLabel(null))
    }

    @Test
    fun settingsIsolatedBySession() = runTest {
        val repo = newRepo()
        runCurrent()
        repo.setGlobal(GlobalAlert.Off)
        repo.setProjectLevel(1, LfLevel.Critical)
        repo.setMutedKeywords(1, setOf("noise"))
        val settingsA = AlertSettings(
            global = GlobalAlert.Off,
            projects = mapOf(1 to ProjectAlert(LfLevel.Critical, setOf("noise"))),
        )
        assertEquals(settingsA, repo.settings.value)

        sessions.value = signedIn(B)
        runCurrent()
        assertEquals(AlertSettings(), repo.settings.value)
        repo.setQuietHours(true)
        assertEquals(AlertSettings(quietHours = true), repo.settings.value)

        sessions.value = SessionState.SignedOut
        runCurrent()
        assertEquals(AlertSettings(), repo.settings.value)

        sessions.value = signedIn(A)
        runCurrent()
        assertEquals(settingsA, repo.settings.value)

        // Persisted per session across process restarts.
        sessions.value = signedIn(B)
        val restarted = newRepo()
        runCurrent()
        assertEquals(AlertSettings(quietHours = true), restarted.settings.value)
    }

    @Test
    fun awaitReady_waitsForSessionAndLoad() = runTest {
        sessions.value = SessionState.Loading
        val gate = CompletableDeferred<Unit>()
        legacy = LegacyAlerts(globalLevel = "Critical", projects = emptyList())
        val repo = AlertSettingsRepository(
            sessions = sessions,
            store = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler)),
            legacy = { gate.await(); legacy },
            scope = backgroundScope,
        )
        val ready = async { repo.awaitReady() }
        runCurrent()
        assertFalse(ready.isCompleted)
        assertNull(repo.settingsFor(sessions.value))

        sessions.value = signedIn(A)
        runCurrent()
        assertFalse(ready.isCompleted)

        gate.complete(Unit)
        runCurrent()
        assertTrue(ready.isCompleted)
        val (state, settings) = ready.await()
        assertEquals(signedIn(A), state)
        assertEquals(GlobalAlert.CriticalOnly, settings.global)
        assertEquals(settings, repo.settingsFor(signedIn(A)))
        assertEquals(AlertSnapshot(A, settings), repo.snapshot.value)
        assertNull(repo.settingsFor(signedIn(B)))
        assertEquals(AlertSettings(), repo.settingsFor(SessionState.SignedOut))
    }

    @Test
    fun clearProjectLevel_followsGlobalAgain() = runTest {
        val repo = newRepo()
        runCurrent()
        repo.setProjectLevel(1, LfLevel.Critical)
        repo.setProjectLevel(2, LfLevel.Debug)
        repo.setMutedKeywords(2, setOf("noise"))
        repo.clearProjectLevel(1)
        repo.setProjectLevel(2, null)
        assertEquals(AlertSettings(projects = mapOf(2 to ProjectAlert(null, setOf("noise")))), repo.settings.value)
    }

    @Test
    fun rejectedWrite_leavesMemoryUnchanged() = runTest {
        val store = ClearingCacheStore(FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler)))
        val repo = AlertSettingsRepository(sessions, store, { legacy }, backgroundScope)
        runCurrent()
        repo.setQuietHours(true)
        assertEquals(AlertSettings(quietHours = true), repo.settings.value)

        // The session's cache is cleared (sign-out cleanup) while the write is in flight.
        store.clearBeforeNextWrite = true
        repo.setGlobal(GlobalAlert.Off)
        assertEquals(AlertSettings(quietHours = true), repo.settings.value)
        assertEquals(AlertSnapshot(A, AlertSettings(quietHours = true)), repo.snapshot.value)
    }

    private companion object {
        val A = SessionKey("aaaaaaaaaaaaaaaa")
        val B = SessionKey("bbbbbbbbbbbbbbbb")

        fun signedIn(key: SessionKey) = SessionState.SignedIn(
            serverUrl = "https://logs.example.com",
            token = "Bearer t-${key.value}",
            username = "me",
            userIdx = if (key == A) 1 else 2,
            permission = 0,
            key = key,
        )
    }
}
