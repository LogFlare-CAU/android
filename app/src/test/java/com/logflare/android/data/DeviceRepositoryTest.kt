package com.logflare.android.data

import androidx.test.core.app.ApplicationProvider
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.FakeLogflareApi
import com.logflare.android.data.store.ProjectStoreTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceRepositoryTest {

    private val sessions = MutableStateFlow<SessionState>(SessionState.Loading)
    private val api = FakeLogflareApi().apply { authHeader = { (sessions.value as? SessionState.SignedIn)?.token } }
    private val repo = DeviceRepository(api, sessions, ApplicationProvider.getApplicationContext(), Json)

    @Test
    fun registerDevice_signedOut_sendsNothing() = runTest {
        sessions.value = SessionState.SignedOut
        repo.registerDevice("fcm-1")
        assertEquals(0, api.count("registerFcmToken"))
    }

    @Test
    fun registerDevice_coldStart_waitsForSessionThenSendsWithItsToken() = runTest {
        launch { repo.registerDevice("fcm-1") }
        runCurrent()
        assertEquals("nothing goes out while the session is loading", 0, api.count("registerFcmToken"))

        sessions.value = ProjectStoreTest.signedIn()
        runCurrent()
        assertEquals(1, api.count("registerFcmToken"))
        assertEquals(listOf(ProjectStoreTest.TOKEN), api.tokens)
        assertEquals(listOf("fcm-1"), api.fcmTokens)
    }
}
