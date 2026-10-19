package com.logflare.android.feature.auth

import com.logflare.android.data.session.SignInResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val calls = mutableListOf<Triple<String, String, String>>()
    private var result: SignInResult = SignInResult.Success

    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(saved: String? = null, recent: List<String> = emptyList()) =
        LoginViewModel(
            signIn = { url, user, pw -> calls += Triple(url, user, pw); result },
            savedServerUrl = flowOf(saved),
            recentServers = flowOf(recent),
        )

    private fun LoginViewModel.fill(url: String = "https://logs.example.com", user: String = "admin", pw: String = "secret") {
        onUrl(url)
        onUsername(user)
        onPassword(pw)
    }

    @Test fun prefill_usesSavedServer_andRecentList() = runTest {
        val vm = vm(saved = "http://10.0.2.2:8000/", recent = listOf("http://a/", "http://b/"))
        advanceUntilIdle()

        assertEquals("http://10.0.2.2:8000/", vm.state.value.url)
        assertEquals(listOf("http://a/", "http://b/"), vm.state.value.recentServers)
    }

    @Test fun submit_empty_showsAllErrors_noCall() = runTest {
        val vm = vm()
        vm.submit()
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(LoginError.EnterUrl, s.urlError)
        assertEquals(LoginError.EnterUsername, s.usernameError)
        assertEquals(LoginError.EnterPassword, s.passwordError)
        assertTrue(calls.isEmpty())
    }

    @Test fun submit_badScheme_urlError() = runTest {
        val vm = vm()
        vm.fill(url = "logs.example.com")
        vm.submit()
        advanceUntilIdle()

        assertEquals(LoginError.UrlScheme, vm.state.value.urlError)
        assertNull(vm.state.value.usernameError)
        assertTrue(calls.isEmpty())
    }

    @Test fun submit_httpUrl_needsHttps_noCall() = runTest {
        val vm = vm()
        vm.fill(url = "http://logs.example.com")
        vm.submit()
        advanceUntilIdle()

        assertEquals(LoginError.NeedsHttps, vm.state.value.urlError)
        assertTrue(calls.isEmpty())
    }

    @Test fun submit_success_emitsSignedIn() = runTest {
        val vm = vm()
        vm.fill()
        vm.submit()
        assertTrue(vm.state.value.submitting)
        advanceUntilIdle()

        assertEquals(listOf(Triple("https://logs.example.com", "admin", "secret")), calls)
        assertEquals(LoginEvent.SignedIn, vm.events.first())
        assertFalse(vm.state.value.submitting)
        assertNull(vm.state.value.formError)
    }

    @Test fun submit_invalid_showsFormError_keepsInput() = runTest {
        result = SignInResult.InvalidCredentials
        val vm = vm()
        vm.fill()
        vm.submit()
        advanceUntilIdle()

        val s = vm.state.value
        assertEquals(LoginError.InvalidCredentials, s.formError)
        assertEquals("secret", s.password)
        assertEquals("admin", s.username)
        assertFalse(s.submitting)
    }

    @Test fun submit_rateLimitedAndUnreachable_mapToFormErrors() = runTest {
        val vm = vm()
        vm.fill()
        result = SignInResult.RateLimited
        vm.submit()
        advanceUntilIdle()
        assertEquals(LoginError.RateLimited, vm.state.value.formError)

        result = SignInResult.ServerUnreachable(RuntimeException())
        vm.submit()
        advanceUntilIdle()
        assertEquals(LoginError.Unreachable, vm.state.value.formError)
    }

    @Test fun pickRecent_fillsUrl_closesSheet() = runTest {
        val vm = vm(recent = listOf("http://a/"))
        advanceUntilIdle()
        vm.openRecent()
        assertTrue(vm.state.value.recentSheetOpen)

        vm.pickRecent("http://a/")

        assertEquals("http://a/", vm.state.value.url)
        assertFalse(vm.state.value.recentSheetOpen)
    }

    @Test fun editingField_clearsItsError() = runTest {
        val vm = vm()
        vm.submit()
        vm.onUrl("h")

        val s = vm.state.value
        assertNull(s.urlError)
        assertEquals(LoginError.EnterUsername, s.usernameError)
        assertEquals(LoginError.EnterPassword, s.passwordError)
    }

    @Test fun submit_bareScheme_noHost_urlError() = runTest {
        val vm = vm()
        vm.fill(url = "https://")
        vm.submit()
        advanceUntilIdle()

        assertEquals(LoginError.EnterUrl, vm.state.value.urlError)
        assertTrue(calls.isEmpty())
    }

    @Test fun submit_whileSubmitting_isIgnored() = runTest {
        val vm = vm()
        vm.fill()
        vm.submit()
        vm.submit()
        advanceUntilIdle()

        assertEquals(1, calls.size)
    }
}
