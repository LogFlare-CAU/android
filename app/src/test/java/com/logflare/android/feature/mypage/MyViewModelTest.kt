package com.logflare.android.feature.mypage

import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.model.UserResponse
import com.example.logflare.core.model.UserSequenceResponse
import com.logflare.android.data.ThemePreference
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.AccountStore
import com.logflare.android.data.store.CacheStore
import com.logflare.android.data.store.FakeLogflareApi
import com.logflare.android.data.store.FileCacheStore
import com.logflare.android.data.store.MemberStore
import com.logflare.android.push.AlertSettingsRepository
import com.logflare.android.push.GlobalAlert
import com.logflare.android.push.LegacyAlerts
import com.logflare.android.ui.locale.AppLanguage
import com.logflare.android.ui.locale.LanguageController
import com.logflare.android.ui.theme.ThemeController
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class MyViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val api = FakeLogflareApi().apply {
        me = { UserResponse(true, "ok", data = UserDTO(7, "me", 80)) }
        users = {
            UserSequenceResponse(
                true,
                "ok",
                data = listOf(
                    UserDTO(1, "admin", 100),
                    UserDTO(7, "me", 80),
                    UserDTO(8, "minsu", 0),
                    UserDTO(9, "jihyun", 80),
                    UserDTO(10, "yuna", 0),
                ),
            )
        }
    }
    private val sessions = MutableStateFlow<SessionState>(ADMIN)
    private val main = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(main)

    @After fun tearDown() = Dispatchers.resetMain()

    private class Env(
        val vm: MyViewModel,
        val alerts: AlertSettingsRepository,
        val saved: MutableList<ThemePreference>,
        val events: MutableList<MyEvent>,
        /** Locale tags handed to the platform, in order ("" = system default). */
        val appliedLocales: MutableList<String>,
    )

    /** Lets a test make cache writes fail (a full disk, say). */
    private class FailingCache(private val inner: CacheStore) : CacheStore by inner {
        var failWrites = false

        override suspend fun <T> write(
            session: SessionKey,
            key: String,
            value: T,
            serializer: KSerializer<T>,
            fetchedAt: Long,
            generation: Long?,
        ) {
            if (failWrites) throw IOException("disk full")
            inner.write(session, key, value, serializer, fetchedAt, generation)
        }
    }

    private lateinit var cache: FailingCache

    private fun TestScope.newEnv(
        saveTheme: suspend (ThemePreference) -> Unit = {},
        signOut: suspend () -> Unit = {},
        storedLocales: String = "",
    ): Env {
        cache = FailingCache(FileCacheStore(tmp.root, Json, StandardTestDispatcher(testScheduler)))
        val alerts = AlertSettingsRepository(sessions, cache, { LegacyAlerts(null, emptyList()) }, backgroundScope)
        val account = AccountStore(api, sessions, cache, backgroundScope, onProfile = { _, _ -> }) { NOW }
        val saved = mutableListOf<ThemePreference>()
        val theme = ThemeController(emptyFlow(), { saved += it; saveTheme(it) }, backgroundScope)
        val appliedLocales = mutableListOf<String>()
        val language = LanguageController(read = { storedLocales }, apply = { appliedLocales += it })
        val vm = MyViewModel(
            account,
            MemberStore(api, sessions, cache, backgroundScope, account) { NOW },
            alerts,
            theme,
            language,
            signOut,
            sessions,
            "2.0.0",
        )
        val events = mutableListOf<MyEvent>()
        backgroundScope.launch { vm.state.collect {} }
        backgroundScope.launch { vm.events.collect { events += it } }
        return Env(vm, alerts, saved, events, appliedLocales)
    }

    private fun TestScope.settle() = repeat(3) {
        advanceUntilIdle()
        runCurrent()
    }

    @Test
    fun profileAndServer_fromAccountAndSession() = runTest {
        val vm = newEnv().vm
        settle()

        val s = vm.state.value
        assertEquals("me", s.me?.username)
        assertEquals(80, s.me?.permission)
        assertEquals("logs.example.com", s.serverHost)
        assertEquals("2.0.0", s.version)
        assertTrue(s.canManageMembers)
    }

    @Test
    fun theme_appliesImmediately() = runTest {
        // The save never finishes: the theme must apply before it is persisted.
        val env = newEnv(saveTheme = { CompletableDeferred<Unit>().await() })
        settle()
        env.vm.openSheet(MySheet.Appearance)
        env.vm.setTheme(ThemePreference.DARK)
        runCurrent()

        assertEquals(ThemePreference.DARK, env.vm.state.value.theme)
        assertEquals(listOf(ThemePreference.DARK), env.saved)
    }

    @Test
    fun language_reflectsStoredAppLocale() = runTest {
        assertEquals(AppLanguage.SYSTEM, newEnv().vm.state.value.language)
        val korean = newEnv(storedLocales = "ko-KR").vm
        settle()
        assertEquals(AppLanguage.KOREAN, korean.state.value.language)
        val english = newEnv(storedLocales = "en-US").vm
        settle()
        assertEquals(AppLanguage.ENGLISH, english.state.value.language)
    }

    @Test
    fun language_selectAppliesAtOnce() = runTest {
        val env = newEnv()
        settle()
        env.vm.openSheet(MySheet.Language)
        env.vm.setLanguage(AppLanguage.KOREAN)
        runCurrent()

        assertEquals(AppLanguage.KOREAN, env.vm.state.value.language)
        assertEquals(listOf("ko"), env.appliedLocales)
        assertNull(env.vm.state.value.sheet)

        env.vm.setLanguage(AppLanguage.SYSTEM)
        runCurrent()
        assertEquals(AppLanguage.SYSTEM, env.vm.state.value.language)
        assertEquals(listOf("ko", ""), env.appliedLocales)
    }

    @Test
    fun language_reselectingCurrentDoesNothing() = runTest {
        // Re-applying the same locales would recreate the screen for nothing.
        val env = newEnv(storedLocales = "ko")
        settle()
        env.vm.setLanguage(AppLanguage.KOREAN)
        runCurrent()
        assertEquals(emptyList<String>(), env.appliedLocales)
    }

    @Test
    fun globalAlert_persists() = runTest {
        val env = newEnv()
        settle()
        assertEquals(GlobalAlert.ErrorAndAbove, env.vm.state.value.globalAlert)

        env.vm.setGlobalAlert(GlobalAlert.WarningAndAbove)
        settle()

        assertEquals(GlobalAlert.WarningAndAbove, env.alerts.settings.value.global)
        assertEquals(GlobalAlert.WarningAndAbove, env.vm.state.value.globalAlert)
        assertEquals(listOf<MyEvent>(MyEvent.PushAlertsChanged(GlobalAlert.WarningAndAbove)), env.events)

        // A fresh screen reads the saved value back.
        val again = newEnv()
        settle()
        assertEquals(GlobalAlert.WarningAndAbove, again.vm.state.value.globalAlert)
    }

    @Test
    fun quietHours_toggle() = runTest {
        val env = newEnv()
        settle()
        assertFalse(env.vm.state.value.quietHours)

        env.vm.setQuietHours(true)
        settle()
        assertTrue(env.vm.state.value.quietHours)
        assertTrue(env.alerts.settings.value.quietHours)

        env.vm.setQuietHours(false)
        settle()
        assertFalse(env.vm.state.value.quietHours)
        assertEquals(listOf<MyEvent>(MyEvent.QuietHoursChanged(true), MyEvent.QuietHoursChanged(false)), env.events)
    }

    @Test
    fun alertSaveFailure_reportsAndKeepsValue() = runTest {
        val env = newEnv()
        settle()
        cache.failWrites = true

        env.vm.setGlobalAlert(GlobalAlert.Off)
        env.vm.setQuietHours(true)
        settle()

        assertEquals(listOf<MyEvent>(MyEvent.SaveFailed, MyEvent.SaveFailed), env.events)
        assertEquals(GlobalAlert.ErrorAndAbove, env.vm.state.value.globalAlert)
        assertFalse(env.vm.state.value.quietHours)
    }

    @Test
    fun logoutFailure_resetsAndReports() = runTest {
        var calls = 0
        val env = newEnv(signOut = { calls++; throw IllegalStateException("boom") })
        settle()
        env.vm.openSheet(MySheet.Logout)
        env.vm.confirmLogout()
        settle()

        assertFalse(env.vm.state.value.signingOut)
        assertEquals(listOf<MyEvent>(MyEvent.LogoutFailed), env.events)
        // It can be tried again.
        env.vm.confirmLogout()
        settle()
        assertEquals(2, calls)
    }

    @Test
    fun member_hidesAdminActions() = runTest {
        sessions.value = ADMIN.copy(permission = 0)
        api.me = { UserResponse(true, "ok", data = UserDTO(7, "me", 0)) }
        val vm = newEnv().vm
        settle()

        val s = vm.state.value
        assertFalse(s.canManageMembers)
        assertTrue(s.members.isEmpty())
        // Listing users is admin-only on the server: a member doesn't ask.
        assertFalse("getAllUsers" in api.calls)
    }

    @Test
    fun admin_membersExcludeSelf_adminsFirst() = runTest {
        val vm = newEnv().vm
        settle()

        assertEquals(listOf("admin", "jihyun", "minsu", "yuna"), vm.state.value.members.map { it.username })
    }

    @Test
    fun showAll_togglesMembers() = runTest {
        val vm = newEnv().vm
        settle()
        assertFalse(vm.state.value.showAllMembers)
        assertEquals(3, vm.state.value.visibleMembers.size)

        vm.toggleShowAll()
        settle()
        assertTrue(vm.state.value.showAllMembers)
        assertEquals(4, vm.state.value.visibleMembers.size)

        vm.toggleShowAll()
        settle()
        assertEquals(3, vm.state.value.visibleMembers.size)
    }

    @Test
    fun logout_callsSignOutThenEmitsNavigate() = runTest {
        val gate = CompletableDeferred<Unit>()
        var signOutCalls = 0
        val env = newEnv(signOut = { signOutCalls++; gate.await() })
        settle()
        env.vm.openSheet(MySheet.Logout)
        env.vm.confirmLogout()
        env.vm.confirmLogout() // a double tap signs out once
        runCurrent()

        assertEquals(1, signOutCalls)
        assertTrue(env.vm.state.value.signingOut)
        assertTrue(env.events.none { it == MyEvent.SignedOut })

        gate.complete(Unit)
        settle()
        assertEquals(listOf<MyEvent>(MyEvent.SignedOut), env.events)
    }

    @Test
    fun sheets_openAndDismiss() = runTest {
        val vm = newEnv().vm
        settle()
        vm.openSheet(MySheet.PushAlerts)
        settle()
        assertEquals(MySheet.PushAlerts, vm.state.value.sheet)
        vm.dismissSheet()
        settle()
        assertNull(vm.state.value.sheet)
    }

    private companion object {
        const val NOW = 1_000_000L
        val ADMIN = SessionState.SignedIn(
            serverUrl = "https://logs.example.com",
            token = "Bearer jwt",
            username = "me",
            userIdx = 7,
            permission = 80,
            key = SessionKey("bbbbbbbbbbbbbbbb"),
        )
    }
}
