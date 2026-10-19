package com.logflare.android.push

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.logflare.core.model.LogoutParams
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.FakeLogflareApi
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The persisted registration across an app update: legacy (unversioned) rows must be re-sent once. */
@OptIn(ExperimentalCoroutinesApi::class)
class DataStorePushRegistrationStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.dataStore() = PreferenceDataStoreFactory.create(
        scope = backgroundScope,
        produceFile = { File(tmp.root, "push.preferences_pb") },
    )

    @Test
    fun legacyRegistration_withoutVersion_isIgnored() = runTest {
        val ds = dataStore()
        ds.edit {
            it[stringPreferencesKey("session_key")] = A.value
            it[stringPreferencesKey("fcm_token")] = "fcm-1"
        }
        assertNull(DataStorePushRegistrationStore(ds).get())
    }

    @Test
    fun currentRegistration_roundTrips_andForgets() = runTest {
        val store = DataStorePushRegistrationStore(dataStore())
        store.set(PushRegistration(A, "fcm-1"))
        assertEquals(PushRegistration(A, "fcm-1"), store.get())
        store.set(null)
        assertNull(store.get())
    }

    @Test
    fun afterUpgrade_registersOnce_thenNotAgain() = runTest {
        val ds = dataStore()
        ds.edit {
            it[stringPreferencesKey("session_key")] = A.value
            it[stringPreferencesKey("fcm_token")] = "fcm-1"
        }
        val api = FakeLogflareApi()
        val sessions = MutableStateFlow<SessionState>(
            SessionState.SignedIn(
                serverUrl = "https://logs.example.com/",
                token = "Bearer a",
                username = "user",
                userIdx = 1,
                permission = 1,
                key = A,
            ),
        )
        val tokens = object : PushTokenSource {
            override suspend fun ensureFirebase() = true
            override suspend fun ensureFirebaseFromCache() = true
            override suspend fun getFcmTokenOrNull() = "fcm-1"
        }

        PushRegistrar(sessions, api, tokens, DataStorePushRegistrationStore(ds), backgroundScope).start()
        runCurrent()
        assertEquals("legacy row is re-registered to bind it", 1, api.count("registerFcmToken"))

        // Process restart after the update: already bound, nothing is sent.
        PushRegistrar(sessions, api, tokens, DataStorePushRegistrationStore(ds), backgroundScope).start()
        runCurrent()
        assertEquals(1, api.count("registerFcmToken"))
        assertEquals(emptyList<LogoutParams>(), api.logoutBodies)
    }

    private companion object {
        val A = SessionKey("aaaaaaaaaaaaaaaa")
    }
}
