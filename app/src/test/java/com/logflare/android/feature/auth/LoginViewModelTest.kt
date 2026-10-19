package com.logflare.android.feature.auth

import com.logflare.android.data.session.AuthModesResult
import com.logflare.android.data.session.SignInResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
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
    private val modeCalls = mutableListOf<String>()
    private var modesAnswer: suspend (String) -> AuthModesResult = { AuthModesResult.Ready(listOf("password")) }
    private val accessCalls = mutableListOf<List<Any>>()
    private var accessResult: SignInResult = SignInResult.Success

    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(saved: String? = null, recent: List<String> = emptyList()) =
        LoginViewModel(
            signIn = { url, user, pw -> calls += Triple(url, user, pw); result },
            loadModes = { url -> modeCalls += url; modesAnswer(url) },
            signInAccess = { url, id, secret, keep -> accessCalls += listOf(url, id, secret, keep); accessResult },
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

    // ---- sign-in methods -------------------------------------------------------------------

    private val both = AuthModesResult.Ready(listOf("password", "access"))
    private val qr = """{"v":1,"server":"https://logs.example.com","id":"abcdefghijklmnop","secret":"s3cret"}"""

    /** A form with Access selected and the modes loaded. */
    private fun TestScope.accessVm(): LoginViewModel {
        modesAnswer = { both }
        val vm = vm()
        vm.onUrl("https://logs.example.com")
        advanceUntilIdle()
        vm.selectMode(LoginMode.Access)
        return vm
    }

    @Test fun modes_loadForTheSavedUrl_withoutAnyKeyboardAction() = runTest {
        modesAnswer = { both }
        val vm = vm(saved = "https://logs.example.com/")
        advanceUntilIdle()

        assertEquals(listOf("https://logs.example.com/"), modeCalls)
        assertEquals(listOf(LoginMode.Password, LoginMode.Access), vm.state.value.modes)
        assertFalse(vm.state.value.modesLoading)
    }

    @Test fun modes_loadAfterTheDebounce_andRestartOnEveryKeystroke() = runTest {
        val vm = vm()
        vm.onUrl("https://a.example.com")
        advanceTimeBy(MODES_DEBOUNCE_MS - 1)
        runCurrent()
        assertTrue("still waiting for a pause in typing", modeCalls.isEmpty())

        vm.onUrl("https://b.example.com")
        advanceTimeBy(MODES_DEBOUNCE_MS - 1)
        runCurrent()
        assertTrue("the second keystroke restarted the wait", modeCalls.isEmpty())

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("https://b.example.com"), modeCalls)
    }

    @Test fun modes_notLoadedForUrlsThatCannotWorkYet() = runTest {
        val vm = vm()
        listOf("", "https://", "logs.example.com", "http://logs.example.com").forEach {
            vm.onUrl(it)
            advanceUntilIdle()
        }

        assertTrue(modeCalls.isEmpty())
        assertNull("typing never shows an error by itself", vm.state.value.urlError)
    }

    @Test fun modes_staleAnswerForAnEarlierUrl_isIgnored() = runTest {
        val first = CompletableDeferred<AuthModesResult>()
        val second = CompletableDeferred<AuthModesResult>()
        // NonCancellable: the answer for the first URL arrives even though that lookup was cancelled.
        modesAnswer = { url -> withContext(NonCancellable) { if (url.contains("first")) first.await() else second.await() } }
        val vm = vm()

        vm.onUrl("https://first.example.com")
        advanceTimeBy(MODES_DEBOUNCE_MS)
        runCurrent()
        assertTrue(vm.state.value.modesLoading)

        vm.onUrl("https://second.example.com")
        assertFalse("changing the URL ends the old lookup's spinner", vm.state.value.modesLoading)
        advanceTimeBy(MODES_DEBOUNCE_MS)
        runCurrent()
        first.complete(both)
        runCurrent()
        assertNull("the first URL's modes are not the second's", vm.state.value.modes)
        assertTrue("the second lookup is still the one loading", vm.state.value.modesLoading)

        second.complete(AuthModesResult.Ready(listOf("password")))
        advanceUntilIdle()
        assertEquals(listOf(LoginMode.Password), vm.state.value.modes)
        assertFalse(vm.state.value.modesLoading)
    }

    @Test fun modes_changingToAnUnusableUrlWhileLoading_doesNotLeaveSpinnerStuck() = runTest {
        val gate = CompletableDeferred<AuthModesResult>()
        modesAnswer = { gate.await() }
        val vm = vm()
        vm.onUrl("https://a.example.com")
        advanceTimeBy(MODES_DEBOUNCE_MS)
        runCurrent()
        assertTrue(vm.state.value.modesLoading)

        vm.onUrl("https://")
        advanceUntilIdle()

        assertFalse(vm.state.value.modesLoading)
        assertNull(vm.state.value.modes)
    }

    @Test fun modes_unreachable_staysQuiet_untilSignInIsTried() = runTest {
        modesAnswer = { AuthModesResult.Unreachable }
        val vm = vm()
        vm.fill()
        advanceUntilIdle()

        // Unknown modes never close a tab: the QR scan works whatever the lookup said.
        assertNull(vm.state.value.modes)
        vm.selectMode(LoginMode.Access)
        vm.scan()
        assertEquals(LoginStep.Scan, vm.state.value.step)
        vm.back()
        vm.selectMode(LoginMode.Password)
        assertNull(vm.state.value.formError)
        assertFalse(vm.state.value.modesLoading)

        vm.submit()
        advanceUntilIdle()
        assertEquals(LoginError.Unreachable, vm.state.value.formError)
        assertFalse(vm.state.value.submitting)
        assertTrue(calls.isEmpty())
    }

    @Test fun modes_pickRecent_loadsAtOnce() = runTest {
        val vm = vm(recent = listOf("https://a.example.com/"))
        advanceUntilIdle()
        vm.pickRecent("https://a.example.com/")
        runCurrent()

        assertEquals(listOf("https://a.example.com/"), modeCalls)
    }

    @Test fun tabs_startOnPassword_andNeverDependOnTheModesLookup() = runTest {
        val vm = vm()
        assertEquals(LoginMode.Password, vm.state.value.selectedMode)

        vm.selectMode(LoginMode.Access)
        assertEquals(LoginMode.Access, vm.state.value.selectedMode)
        vm.onUrl("https://logs.example.com")
        advanceUntilIdle()
        assertEquals("a lookup answer does not move the selection", LoginMode.Access, vm.state.value.selectedMode)
        vm.selectMode(LoginMode.Password)
        assertEquals(LoginMode.Password, vm.state.value.selectedMode)
    }

    @Test fun password_blockedWhenTheServerLacksIt_noRequestIsSent() = runTest {
        modesAnswer = { AuthModesResult.Ready(listOf("access")) }
        val vm = vm()
        vm.fill()
        advanceUntilIdle()

        assertTrue(vm.state.value.passwordBlocked)
        assertEquals(LoginMode.Password, vm.state.value.selectedMode)

        vm.submit()
        advanceUntilIdle()
        assertTrue(calls.isEmpty())
        assertFalse(vm.state.value.submitting)
    }

    @Test fun password_notBlocked_forLegacyEmptyOrUnknownModes() = runTest {
        modesAnswer = { AuthModesResult.Ready(listOf("password")) }
        val vm = vm()
        vm.fill()
        advanceUntilIdle()
        assertFalse(vm.state.value.passwordBlocked)

        vm.onUrl("https://empty.example.com")
        modesAnswer = { AuthModesResult.Ready(emptyList()) }
        advanceUntilIdle()
        assertFalse("an empty list is the no-methods box, not this error", vm.state.value.passwordBlocked)
    }

    @Test fun noModes_isAStateOfItsOwn_andSignInDoesNothing() = runTest {
        modesAnswer = { AuthModesResult.Ready(emptyList()) }
        val vm = vm()
        vm.fill()
        advanceUntilIdle()

        assertEquals(emptyList<LoginMode>(), vm.state.value.modes)
        assertNull(vm.state.value.formError)

        vm.submit()
        advanceUntilIdle()
        assertTrue(calls.isEmpty())
        assertFalse(vm.state.value.submitting)
    }

    @Test fun password_stillSignsIn_whenBothMethodsAreOffered() = runTest {
        modesAnswer = { both }
        val vm = vm()
        vm.fill()
        advanceUntilIdle()
        vm.submit()
        advanceUntilIdle()

        assertEquals(1, calls.size)
        assertEquals(LoginEvent.SignedIn, vm.events.first())
    }

    @Test fun passwordSignIn_isNotSentInAccessMode() = runTest {
        val vm = accessVm()
        vm.onUsername("admin")
        vm.onPassword("pw")
        vm.submit()
        advanceUntilIdle()

        assertTrue(calls.isEmpty())
    }

    // ---- Access: scan, confirm ---------------------------------------------------------------

    @Test fun access_scanThenConfirm_signsIn_withTheQrServer_keepLoggedInByDefault() = runTest {
        val vm = accessVm()
        vm.scan()
        assertEquals(LoginStep.Scan, vm.state.value.step)

        vm.onQr(qr)
        val s = vm.state.value
        assertEquals(LoginStep.Confirm, s.step)
        assertEquals("logs.example.com", s.accessServer)
        assertEquals("abcd…mnop", s.accessClientMasked)
        assertTrue(s.keepLoggedIn)
        assertFalse(s.toString().contains("s3cret"))

        vm.confirmAccess()
        advanceUntilIdle()
        assertEquals(listOf(listOf("https://logs.example.com", "abcdefghijklmnop", "s3cret", true)), accessCalls)
        assertEquals(LoginEvent.SignedIn, vm.events.first())
    }

    @Test fun access_keepLoggedInOff_isSentToTheServer() = runTest {
        val vm = accessVm()
        vm.scan()
        vm.onQr(qr)
        vm.setKeepLoggedIn(false)
        vm.confirmAccess()
        advanceUntilIdle()

        assertEquals(false, accessCalls.single()[3])
    }

    @Test fun access_wrongQr_thenValidQr_works() = runTest {
        val vm = accessVm()
        vm.scan()

        vm.onQr("not-a-qr")
        assertEquals(LoginError.QrInvalid, vm.state.value.scanError)
        assertEquals(LoginStep.Scan, vm.state.value.step)

        vm.onQr(qr)
        assertEquals(LoginStep.Confirm, vm.state.value.step)
        assertNull(vm.state.value.scanError)
    }

    @Test fun access_rejected_staysOnConfirm_withTheAlert() = runTest {
        accessResult = SignInResult.InvalidCredentials
        val vm = accessVm()
        vm.scan()
        vm.onQr(qr)
        vm.confirmAccess()
        advanceUntilIdle()

        assertEquals(LoginError.AccessRejected, vm.state.value.formError)
        assertEquals(LoginStep.Confirm, vm.state.value.step)
        assertFalse(vm.state.value.submitting)
    }

    @Test fun access_otherFailures_mapToTheirTexts() = runTest {
        val vm = accessVm()
        vm.scan()
        vm.onQr(qr)
        listOf(
            SignInResult.AccessUnavailable to LoginError.AccessUnavailable,
            SignInResult.RateLimited to LoginError.RateLimited,
            SignInResult.ServerUnreachable(RuntimeException()) to LoginError.Unreachable,
        ).forEach { (answer, error) ->
            accessResult = answer
            vm.confirmAccess()
            advanceUntilIdle()
            assertEquals(error, vm.state.value.formError)
        }
    }

    @Test fun access_scanAndManual_needTheAccessTab() = runTest {
        val vm = vm()
        vm.fill()
        advanceUntilIdle()
        vm.scan()
        vm.enterManual()

        assertEquals(LoginStep.Form, vm.state.value.step)
    }

    @Test fun access_scanWorks_withNoUrlTyped_noModesLookup_andUsesTheQrServer() = runTest {
        val vm = vm()
        vm.selectMode(LoginMode.Access)
        vm.scan()
        assertEquals(LoginStep.Scan, vm.state.value.step)

        vm.onQr(qr)
        assertEquals("logs.example.com", vm.state.value.accessServer)
        vm.confirmAccess()
        advanceUntilIdle()

        assertTrue("no lookup was needed", modeCalls.isEmpty())
        assertEquals("https://logs.example.com", accessCalls.single()[0])
        assertEquals(LoginEvent.SignedIn, vm.events.first())
    }

    @Test fun access_qrServerWins_overADifferentTypedUrl() = runTest {
        val vm = accessVm()
        vm.onUrl("https://typed.example.com")
        vm.selectMode(LoginMode.Access)
        vm.scan()
        vm.onQr(qr)
        vm.confirmAccess()
        advanceUntilIdle()

        assertEquals("https://logs.example.com", accessCalls.single()[0])
    }

    @Test fun access_qrOutsideTheScanStep_isIgnored() = runTest {
        val vm = accessVm()
        vm.onQr(qr)

        assertEquals(LoginStep.Form, vm.state.value.step)
    }

    // ---- Access: manual entry ----------------------------------------------------------------

    @Test fun manual_needsBothFields() = runTest {
        val vm = accessVm()
        vm.enterManual()
        assertEquals(LoginStep.Manual, vm.state.value.step)

        vm.submitManual("  ", "")
        assertEquals(LoginError.EnterClientId, vm.state.value.manualIdError)
        assertEquals(LoginError.EnterClientSecret, vm.state.value.manualSecretError)
        assertEquals(LoginStep.Manual, vm.state.value.step)

        vm.onManualEdit()
        assertNull(vm.state.value.manualIdError)
        assertNull(vm.state.value.manualSecretError)
    }

    @Test fun manual_serverIsPrefilledFromThePasswordTabUrl_orEmpty() = runTest {
        val typed = accessVm()
        typed.enterManual()
        assertEquals("https://logs.example.com", typed.state.value.manualServer)

        val blank = vm()
        blank.selectMode(LoginMode.Access)
        blank.enterManual()
        assertEquals("", blank.state.value.manualServer)
    }

    @Test fun manual_serverIsValidated_httpsOnly() = runTest {
        val vm = vm()
        vm.selectMode(LoginMode.Access)
        vm.enterManual()

        vm.submitManual("id-1", "secret-1")
        assertEquals(LoginError.EnterUrl, vm.state.value.manualServerError)
        vm.onManualServer("http://logs.example.com")
        vm.submitManual("id-1", "secret-1")
        assertEquals(LoginError.NeedsHttps, vm.state.value.manualServerError)
        vm.onManualServer("logs.example.com")
        vm.submitManual("id-1", "secret-1")
        assertEquals(LoginError.UrlScheme, vm.state.value.manualServerError)
        assertEquals(LoginStep.Manual, vm.state.value.step)

        vm.onManualServer("https://own.example.com/")
        assertNull(vm.state.value.manualServerError)
        vm.submitManual("id-1", "secret-1")
        assertEquals(LoginStep.Confirm, vm.state.value.step)
        assertEquals("own.example.com", vm.state.value.accessServer)
        vm.confirmAccess()
        advanceUntilIdle()
        assertEquals("https://own.example.com", accessCalls.single()[0])
    }

    @Test fun manual_continue_confirmsAgainstTheServerField_andKeepsTheSecretOutOfState() = runTest {
        val vm = accessVm()
        vm.enterManual()

        vm.submitManual(" client-id-1234 ", "manual-secret")
        val s = vm.state.value
        assertEquals(LoginStep.Confirm, s.step)
        assertEquals("logs.example.com", s.accessServer)
        assertEquals("clie…1234", s.accessClientMasked)
        assertFalse(s.toString().contains("manual-secret"))

        vm.confirmAccess()
        advanceUntilIdle()
        assertEquals(listOf(listOf("https://logs.example.com", "client-id-1234", "manual-secret", true)), accessCalls)
    }

    // ---- navigation ----------------------------------------------------------------------------

    @Test fun back_fromScanAndManual_returnsToTheForm() = runTest {
        val vm = accessVm()
        vm.scan()
        vm.onQr("nope")
        vm.back()
        assertEquals(LoginStep.Form, vm.state.value.step)
        assertNull(vm.state.value.scanError)

        vm.enterManual()
        vm.back()
        assertEquals(LoginStep.Form, vm.state.value.step)
    }

    @Test fun back_fromConfirm_returnsToWhereItCameFrom() = runTest {
        val vm = accessVm()
        vm.scan()
        vm.onQr(qr)
        vm.back()
        assertEquals("re-scan", LoginStep.Scan, vm.state.value.step)
        assertNull(vm.state.value.scanError)

        vm.back()
        assertEquals(LoginStep.Form, vm.state.value.step)

        vm.enterManual()
        vm.submitManual("id-1", "secret-1")
        vm.back()
        assertEquals(LoginStep.Manual, vm.state.value.step)
    }

    @Test fun back_afterGoingBack_cannotSignInWithTheOldToken() = runTest {
        val vm = accessVm()
        vm.scan()
        vm.onQr(qr)
        vm.back()
        vm.confirmAccess()
        advanceUntilIdle()

        assertTrue(accessCalls.isEmpty())
    }

    @Test fun back_onTheForm_changesNothing() = runTest {
        val vm = accessVm()
        val before = vm.state.value
        vm.back()
        assertEquals(before, vm.state.value)
    }

    @Test fun editingTheUrl_leavesTheAccessSteps() = runTest {
        val vm = accessVm()
        vm.scan()
        vm.onUrl("https://other.example.com")

        assertEquals(LoginStep.Form, vm.state.value.step)
        assertNull(vm.state.value.modes)
    }
}
