package com.logflare.android.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logflare.android.BuildConfig
import com.logflare.android.data.ThemePreference
import com.logflare.android.data.session.SessionRepository
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.session.SignOutUseCase
import com.logflare.android.data.store.AccountStore
import com.logflare.android.data.store.MemberStore
import com.logflare.android.data.store.Resource
import com.logflare.android.data.store.model.Member
import com.logflare.android.push.AlertSettings
import com.logflare.android.push.AlertSettingsRepository
import com.logflare.android.push.GlobalAlert
import com.logflare.android.ui.locale.AppLanguage
import com.logflare.android.ui.locale.LanguageController
import com.logflare.android.ui.theme.ThemeController
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URI
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MySheet { Appearance, Language, PushAlerts, Logout }

data class MyUiState(
    /** The signed-in user: the fetched profile, else what the session knows. */
    val me: Member? = null,
    /** The server's host (and port), e.g. `logs.example.com`. */
    val serverHost: String = "",
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val globalAlert: GlobalAlert = AlertSettings().global,
    val quietHours: Boolean = false,
    /** Other users of the server, admins first; empty for members (they can't list users). */
    val members: List<Member> = emptyList(),
    val showAllMembers: Boolean = false,
    /** Admin (permission >= 80): sees the members, can add and edit them. */
    val canManageMembers: Boolean = false,
    val version: String = "",
    val sheet: MySheet? = null,
    val signingOut: Boolean = false,
    /** Admins only: the member list is loading for the first time. */
    val membersLoading: Boolean = false,
    /** Admins only: the first member load failed and nothing is cached. */
    val membersFailed: Boolean = false,
    val refreshing: Boolean = false,
) {
    val visibleMembers: List<Member> get() = if (showAllMembers) members else members.take(MEMBERS_PREVIEW)
    val hasMoreMembers: Boolean get() = members.size > MEMBERS_PREVIEW
}

sealed interface MyEvent {
    data class CopyServer(val url: String) : MyEvent
    data class PushAlertsChanged(val value: GlobalAlert) : MyEvent
    data class QuietHoursChanged(val on: Boolean) : MyEvent
    /** A Push alerts or Quiet hours change could not be saved. */
    data object SaveFailed : MyEvent
    data object RefreshFailed : MyEvent
    data object LogoutFailed : MyEvent
    data object SignedOut : MyEvent
}

internal const val MEMBERS_PREVIEW = 3
private const val ADMIN_PERMISSION = 80

/** "logs.example.com" from "https://logs.example.com/"; keeps a port ("10.0.2.2:8000"). */
internal fun serverHost(url: String): String {
    val uri = runCatching { URI(url.trim()) }.getOrNull()
    val host = uri?.host ?: return url.trim().removeSuffix("/")
    return if (uri.port != -1) "$host:${uri.port}" else host
}

/**
 * The My tab: profile, this device's appearance and push settings, the server's members (admins)
 * and log out. Settings are per device and apply at once; logging out goes through [signOut].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyViewModel internal constructor(
    private val accountStore: AccountStore,
    private val memberStore: MemberStore,
    private val alerts: AlertSettingsRepository,
    private val theme: ThemeController,
    private val language: LanguageController,
    private val signOut: suspend () -> Unit,
    sessions: StateFlow<SessionState>,
    version: String,
) : ViewModel() {

    @Inject
    constructor(
        accountStore: AccountStore,
        memberStore: MemberStore,
        alerts: AlertSettingsRepository,
        theme: ThemeController,
        language: LanguageController,
        signOutUseCase: SignOutUseCase,
        sessionRepository: SessionRepository,
    ) : this(
        accountStore,
        memberStore,
        alerts,
        theme,
        language,
        { signOutUseCase() },
        sessionRepository.state,
        BuildConfig.VERSION_NAME,
    )

    private val local = MutableStateFlow(Local())

    private val _events = Channel<MyEvent>(Channel.BUFFERED)
    val events: Flow<MyEvent> = _events.receiveAsFlow()

    private val signedIn: StateFlow<SessionState.SignedIn?> = sessions
        .map { it as? SessionState.SignedIn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), sessions.value as? SessionState.SignedIn)

    private val isAdmin: Flow<Boolean> = signedIn
        .map { (it?.permission ?: 0) >= ADMIN_PERMISSION }
        .distinctUntilChanged()

    /** Members can't list users (the server answers 403), so only admins ask. */
    private val users: Flow<Resource<List<Member>>?> = isAdmin
        .flatMapLatest { admin -> if (admin) memberStore.members() else flowOf(null) }

    private val account: Flow<Member?> = accountStore.me().map { it.data }

    private val profile = combine(signedIn, account, users) { session, fetched, res -> Profile(session, fetched, res) }

    private val settings = combine(alerts.settings, theme.preference, language.language, ::DeviceSettings)

    val state: StateFlow<MyUiState> = combine(profile, settings, local) { p, (alert, themePref, lang), l ->
        val session = p.session
        val me = p.me ?: session?.let { Member(it.userIdx ?: 0, it.username, it.permission ?: 0) }
        val admin = (session?.permission ?: 0) >= ADMIN_PERMISSION
        val res = p.users.takeIf { admin }
        MyUiState(
            me = me,
            serverHost = session?.serverUrl?.let(::serverHost).orEmpty(),
            theme = themePref,
            language = lang,
            globalAlert = alert.global,
            quietHours = alert.quietHours,
            members = res?.data.orEmpty().otherMembers(me),
            showAllMembers = l.showAll,
            canManageMembers = admin,
            version = version,
            sheet = l.sheet,
            signingOut = l.signingOut,
            membersLoading = admin && (res == null || res.isInitialLoad),
            membersFailed = admin && res != null && res.data == null && res.error != null && !res.refreshing,
            refreshing = l.refreshing,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MyUiState(version = version, theme = theme.preference.value, language = language.language.value),
    )

    fun openSheet(sheet: MySheet) = local.update { it.copy(sheet = sheet) }

    fun dismissSheet() = local.update { it.copy(sheet = null) }

    /** Applies at once; saving happens in the background. */
    fun setTheme(preference: ThemePreference) = theme.set(preference)

    /**
     * Applies at once: Android recreates the screen in the new language and keeps the choice. The
     * sheet closes first, so the recreated screen doesn't open it again.
     */
    fun setLanguage(value: AppLanguage) {
        local.update { it.copy(sheet = null) }
        language.set(value)
    }

    fun setGlobalAlert(value: GlobalAlert) = saveAlerts(MyEvent.PushAlertsChanged(value)) { alerts.setGlobal(value) }

    fun setQuietHours(on: Boolean) = saveAlerts(MyEvent.QuietHoursChanged(on)) { alerts.setQuietHours(on) }

    /** Runs [save]; reports [done] on success, [MyEvent.SaveFailed] otherwise. */
    private fun saveAlerts(done: MyEvent, save: suspend () -> Unit) {
        viewModelScope.launch {
            val ok = try {
                save()
                true
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
            _events.send(if (ok) done else MyEvent.SaveFailed)
        }
    }

    fun toggleShowAll() = local.update { it.copy(showAll = !it.showAll) }

    fun copyServer() {
        val url = signedIn.value?.serverUrl ?: return
        _events.trySend(MyEvent.CopyServer(url))
    }

    /** Pull-to-refresh: reloads the profile and (admins) the members, then reports a failure once. */
    fun refresh() {
        if (local.value.refreshing) return
        local.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            try {
                val admin = (signedIn.value?.permission ?: 0) >= ADMIN_PERMISSION
                val results = listOf(accountStore.refresh()) + if (admin) listOf(memberStore.refresh()) else emptyList()
                if (results.any { it.isFailure }) _events.send(MyEvent.RefreshFailed)
            } finally {
                local.update { it.copy(refreshing = false) }
            }
        }
    }

    fun retryMembers() {
        viewModelScope.launch { memberStore.refresh() }
    }

    /** Signs out once; the screen then leaves for Login on [MyEvent.SignedOut]. */
    fun confirmLogout() {
        if (local.value.signingOut) return
        local.update { it.copy(signingOut = true) }
        viewModelScope.launch {
            try {
                signOut()
                _events.send(MyEvent.SignedOut)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                local.update { it.copy(signingOut = false) }
                _events.send(MyEvent.LogoutFailed)
            }
        }
    }

    private data class DeviceSettings(val alerts: AlertSettings, val theme: ThemePreference, val language: AppLanguage)

    private data class Profile(val session: SessionState.SignedIn?, val me: Member?, val users: Resource<List<Member>>?)

    private data class Local(
        val sheet: MySheet? = null,
        val showAll: Boolean = false,
        val signingOut: Boolean = false,
        val refreshing: Boolean = false,
    )
}

/** Everyone but [me], admins first, then by name. */
private fun List<Member>.otherMembers(me: Member?): List<Member> =
    filterNot { me != null && (it.idx == me.idx || it.username == me.username) }
        .sortedWith(
            compareByDescending<Member> { it.permission >= ADMIN_PERMISSION }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.username },
        )
