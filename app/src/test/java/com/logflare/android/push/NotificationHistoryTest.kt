package com.logflare.android.push

import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.FileCacheStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationHistoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val sessions = MutableStateFlow<SessionState>(signedIn(A))

    private fun TestScope.newHistory() = NotificationHistory(
        sessions = sessions,
        store = FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler)),
    )

    private fun entry(errorId: Int, read: Boolean = false) = HistoryEntry(
        errorId = errorId,
        projectId = 1,
        type = "ValueError",
        level = LfLevel.Error,
        message = "boom $errorId",
        receivedAt = 1_000L + errorId,
        read = read,
    )

    @Test
    fun capsAt50_newestFirst() = runTest {
        val history = newHistory()
        for (id in 1..60) history.add(entry(id))

        val entries = history.entries.first()
        assertEquals(50, entries.size)
        assertEquals((60 downTo 11).toList(), entries.map { it.errorId })
        assertEquals(50, history.unreadCount.first())

        // Persisted.
        assertEquals(entries, newHistory().entries.first())
    }

    @Test
    fun markAllRead_zeroUnread() = runTest {
        val history = newHistory()
        history.add(entry(1))
        history.add(entry(2))
        history.add(entry(3))
        assertEquals(3, history.unreadCount.first())

        history.markRead(2)
        assertEquals(2, history.unreadCount.first())
        assertTrue(history.entries.first().single { it.errorId == 2 }.read)

        history.markAllRead()
        assertEquals(0, history.unreadCount.first())
        assertEquals(listOf(3, 2, 1), history.entries.first().map { it.errorId })
    }

    @Test
    fun historyIsPerSession_andClearable() = runTest {
        val history = newHistory()
        history.add(entry(1))

        sessions.value = signedIn(B)
        assertEquals(emptyList<HistoryEntry>(), history.entries.first())
        history.add(entry(2))

        sessions.value = SessionState.SignedOut
        assertEquals(emptyList<HistoryEntry>(), history.entries.first())
        assertEquals(0, history.unreadCount.first())
        history.add(entry(3)) // ignored while signed out

        sessions.value = signedIn(A)
        assertEquals(listOf(1), history.entries.first().map { it.errorId })
        history.clear(A)
        assertEquals(emptyList<HistoryEntry>(), history.entries.first())

        sessions.value = signedIn(B)
        assertEquals(listOf(2), history.entries.first().map { it.errorId })
    }

    private companion object {
        val A = SessionKey("aaaaaaaaaaaaaaaa")
        val B = SessionKey("bbbbbbbbbbbbbbbb")

        fun signedIn(key: SessionKey) = SessionState.SignedIn(
            serverUrl = "https://logs.example.com",
            token = "Bearer t",
            username = "me",
            userIdx = if (key == A) 1 else 2,
            permission = 0,
            key = key,
        )
    }
}
