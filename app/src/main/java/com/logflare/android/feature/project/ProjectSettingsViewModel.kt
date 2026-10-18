package com.logflare.android.feature.project

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.data.session.SessionRepository
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.MemberStore
import com.logflare.android.data.store.ProjectStore
import com.logflare.android.data.store.model.Member
import com.logflare.android.data.store.model.ProjectItem
import com.logflare.android.push.AlertSettings
import com.logflare.android.push.AlertSettingsRepository
import com.logflare.android.push.GlobalAlert
import com.logflare.android.ui.nav.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A user in the Members sheet. Super Admins ([locked]) see every project and can't be unticked. */
data class MemberAccess(val member: Member, val hasAccess: Boolean, val locked: Boolean)

enum class SettingsSheet { Name, AlertLevel, Keywords, Members, Rotate, Delete }

data class ProjectSettingsUiState(
    /** Null while the project list loads for the first time, or once the project is gone. */
    val project: ProjectItem? = null,
    val loading: Boolean = true,
    val isAdmin: Boolean = false,
    /** The level that alerts on this device: the project's own, else the Push alerts default. */
    val alertLevel: LfLevel = DEFAULT_LEVEL,
    /** Push alerts are off on this device, so no level alerts. */
    val pushAlertsOff: Boolean = false,
    val mutedKeywords: List<String> = emptyList(),
    /** Users who can see the project; null until it's known. */
    val memberCount: Int? = null,
    val members: List<MemberAccess> = emptyList(),
    /** Fetched on the first reveal or copy. */
    val token: String? = null,
    val tokenRevealed: Boolean = false,
    val sheet: SettingsSheet? = null,
    val nameDraft: String = "",
    val nameCheck: NameCheck = NameCheck.Empty,
    val deleteConfirmText: String = "",
    /** A rename, delete or member change is running. */
    val busy: Boolean = false,
) {
    val canSaveName: Boolean get() = nameCheck == NameCheck.Valid && nameDraft != project?.name && !busy
    val canDelete: Boolean get() = project != null && deleteConfirmText == project.name && !busy
}

sealed interface ProjectSettingsEvent {
    data object Saved : ProjectSettingsEvent
    data object SaveFailed : ProjectSettingsEvent
    data class CopyToken(val token: String) : ProjectSettingsEvent
    data object TokenUnavailable : ProjectSettingsEvent
    data object TokenRotated : ProjectSettingsEvent
    data object RotateFailed : ProjectSettingsEvent
    data object Deleted : ProjectSettingsEvent
    data object DeleteFailed : ProjectSettingsEvent
}

private val DEFAULT_LEVEL: LfLevel = checkNotNull(AlertSettings().global.threshold())
private const val TAG = "ProjectSettings"
private const val ADMIN_PERMISSION = 80
private const val SUPER_ADMIN_PERMISSION = 100

/**
 * Project settings. Alert level and muted keywords are this device's push settings (anyone can
 * change them, no server call); rename, members, the token and delete are for admins only.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProjectSettingsViewModel internal constructor(
    savedState: SavedStateHandle,
    private val projectStore: ProjectStore,
    memberStore: MemberStore,
    private val alerts: AlertSettingsRepository,
    sessions: StateFlow<SessionState>,
) : ViewModel() {

    @Inject
    constructor(
        savedState: SavedStateHandle,
        projectStore: ProjectStore,
        memberStore: MemberStore,
        alerts: AlertSettingsRepository,
        sessionRepository: SessionRepository,
    ) : this(savedState, projectStore, memberStore, alerts, sessionRepository.state)

    private val projectId: Int = checkNotNull(savedState[Destinations.ProjectIdArg]) { "project id missing" }

    private val local = MutableStateFlow(Local())

    private val _events = Channel<ProjectSettingsEvent>(Channel.BUFFERED)
    val events: Flow<ProjectSettingsEvent> = _events.receiveAsFlow()

    private val isAdmin: StateFlow<Boolean> = sessions
        .map { (it as? SessionState.SignedIn)?.permission?.let { p -> p >= ADMIN_PERMISSION } == true }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val projects = projectStore.projects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** All users, for the Members sheet; members can't list users, so they don't ask. */
    private val users: Flow<List<Member>?> = isAdmin.flatMapLatest { admin ->
        if (admin) memberStore.members().map { it.data } else flowOf(null)
    }

    val state: StateFlow<ProjectSettingsUiState> =
        combine(projects, isAdmin, users, alerts.settings, local) { resource, admin, allUsers, settings, l ->
            val list = resource?.data
            val project = list?.firstOrNull { it.id == projectId }
            val own = settings.projects[projectId]
            val members = if (admin) memberAccess(allUsers.orEmpty(), l.access) else emptyList()
            ProjectSettingsUiState(
                project = project,
                loading = resource == null || resource.isInitialLoad,
                isAdmin = admin,
                alertLevel = own?.level ?: settings.global.threshold() ?: DEFAULT_LEVEL,
                pushAlertsOff = settings.global == GlobalAlert.Off,
                mutedKeywords = own?.mutedKeywords.orEmpty().toList(),
                memberCount = when {
                    !admin -> null
                    members.isNotEmpty() && l.access != null -> members.count { it.hasAccess }
                    else -> l.access?.size
                },
                members = members,
                token = l.token.takeIf { admin },
                tokenRevealed = admin && l.tokenRevealed,
                sheet = l.sheet,
                nameDraft = l.nameDraft,
                nameCheck = checkProjectName(l.nameDraft, list.orEmpty().map { it.name }, project?.name),
                deleteConfirmText = l.deleteText,
                busy = l.busy,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectSettingsUiState())

    init {
        // Who can see the project: loaded once the user is known to be an admin.
        viewModelScope.launch {
            isAdmin.first { it }
            loadAccess()
        }
    }

    fun openSheet(sheet: SettingsSheet) {
        val adminOnly = sheet != SettingsSheet.AlertLevel && sheet != SettingsSheet.Keywords
        if (adminOnly && !isAdmin.value) return
        val name = state.value.project?.name.orEmpty()
        local.value = local.value.copy(sheet = sheet, nameDraft = name, deleteText = "")
        if (sheet == SettingsSheet.Members && local.value.access == null) viewModelScope.launch { loadAccess() }
    }

    fun dismissSheet() {
        local.value = local.value.copy(sheet = null)
    }

    fun setNameDraft(value: String) {
        local.value = local.value.copy(nameDraft = value)
    }

    fun saveName() {
        val list = projects.value?.data.orEmpty()
        val current = list.firstOrNull { it.id == projectId }?.name ?: return
        val draft = local.value.nameDraft
        val valid = checkProjectName(draft, list.map { it.name }, current) == NameCheck.Valid
        if (!isAdmin.value || !valid || draft == current || local.value.busy) return
        mutate(closeSheet = true) { projectStore.rename(projectId, draft) }
    }

    /** Choosing the Push alerts default again drops the project's own level, so it follows it. */
    fun saveAlertLevel(level: LfLevel) = saveLocally {
        if (level == alerts.settings.value.global.threshold()) {
            alerts.clearProjectLevel(projectId)
        } else {
            alerts.setProjectLevel(projectId, level)
        }
    }

    fun saveKeywords(keywords: List<String>) = saveLocally { alerts.setMutedKeywords(projectId, keywords.toSet()) }

    /** [selected] are user indexes; the server takes usernames. */
    fun saveMembers(selected: Set<Int>) {
        val s = state.value
        if (!s.isAdmin || s.busy) return
        val usernames = s.members.filter { it.member.idx in selected }.mapTo(mutableSetOf()) { it.member.username }
        mutate(closeSheet = true, onSuccess = { local.value = local.value.copy(access = selected) }) {
            projectStore.setMembers(projectId, usernames)
        }
    }

    fun toggleReveal() {
        if (!isAdmin.value) return
        val l = local.value
        if (l.tokenRevealed) {
            local.value = l.copy(tokenRevealed = false)
            return
        }
        withToken { local.value = local.value.copy(tokenRevealed = true) }
    }

    fun copyToken() {
        if (!isAdmin.value) return
        withToken { _events.send(ProjectSettingsEvent.CopyToken(it)) }
    }

    fun confirmRotate() {
        if (!isAdmin.value || local.value.sheet != SettingsSheet.Rotate) return
        local.value = local.value.copy(sheet = null)
        viewModelScope.launch {
            projectStore.rotateToken(projectId)
                .onSuccess { token ->
                    local.value = local.value.copy(token = token)
                    _events.send(ProjectSettingsEvent.TokenRotated)
                }
                .onFailure { _events.send(ProjectSettingsEvent.RotateFailed) }
        }
    }

    fun setDeleteConfirmText(value: String) {
        local.value = local.value.copy(deleteText = value)
    }

    /** Only when the typed text is exactly the project's name. */
    fun confirmDelete() {
        val name = projects.value?.data?.firstOrNull { it.id == projectId }?.name ?: return
        if (!isAdmin.value || local.value.deleteText != name || local.value.busy) return
        local.value = local.value.copy(busy = true)
        viewModelScope.launch {
            // The store refreshes the project list once the delete went through.
            projectStore.delete(projectId)
                .onSuccess {
                    local.value = local.value.copy(sheet = null)
                    _events.send(ProjectSettingsEvent.Deleted)
                }
                .onFailure {
                    local.value = local.value.copy(busy = false)
                    _events.send(ProjectSettingsEvent.DeleteFailed)
                }
        }
    }

    private data class Local(
        val access: Set<Int>? = null,
        val token: String? = null,
        val tokenRevealed: Boolean = false,
        val sheet: SettingsSheet? = null,
        val nameDraft: String = "",
        val deleteText: String = "",
        val busy: Boolean = false,
    )

    private suspend fun loadAccess() {
        projectStore.members(projectId).onSuccess { local.value = local.value.copy(access = it) }
    }

    private fun withToken(block: suspend (String) -> Unit) {
        viewModelScope.launch {
            val token = local.value.token ?: projectStore.token(projectId).getOrElse {
                // E.g. a 403: admins (80) only get tokens of projects they can see.
                _events.send(ProjectSettingsEvent.TokenUnavailable)
                return@launch
            }.also { local.value = local.value.copy(token = it) }
            block(token)
        }
    }

    private fun mutate(closeSheet: Boolean, onSuccess: () -> Unit = {}, block: suspend () -> Result<Unit>) {
        local.value = local.value.copy(busy = true)
        viewModelScope.launch {
            val result = block()
            local.value = local.value.copy(busy = false)
            if (result.isSuccess) {
                onSuccess()
                if (closeSheet) local.value = local.value.copy(sheet = null)
                _events.send(ProjectSettingsEvent.Saved)
            } else {
                _events.send(ProjectSettingsEvent.SaveFailed)
            }
        }
    }

    private fun saveLocally(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Saving alert settings for project $projectId failed", e)
                _events.send(ProjectSettingsEvent.SaveFailed)
                return@launch
            }
            local.value = local.value.copy(sheet = null)
            _events.send(ProjectSettingsEvent.Saved)
        }
    }

    private fun memberAccess(users: List<Member>, access: Set<Int>?): List<MemberAccess> = users.map {
        val locked = it.permission >= SUPER_ADMIN_PERMISSION
        MemberAccess(it, hasAccess = locked || access?.contains(it.idx) == true, locked = locked)
    }
}
