package com.logflare.android.feature.mypage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logflare.android.data.session.SessionRepository
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.ApiException
import com.logflare.android.data.store.MemberStore
import com.logflare.android.data.store.Resource
import com.logflare.android.data.store.model.Member
import com.logflare.android.feature.project.NameCheck
import com.logflare.android.ui.nav.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EditLoad {
    /** The member list loads for the first time. */
    Loading,

    /** The first load failed and nothing is cached. */
    Failed,

    /** The list has no user with this idx (deleted elsewhere). */
    Missing,
    Ready,
}

/** Why the viewer can't change a member. */
enum class MemberLock { SuperAdmin, Self, Role }

data class EditMemberUiState(
    val load: EditLoad = EditLoad.Loading,
    /** The member as the server knows them: the baseline for [dirty]. */
    val member: Member? = null,
    /** Set when the viewer may not change this member; the screen then only shows who it is. */
    val lock: MemberLock? = null,
    val name: String = "",
    val nameCheck: NameCheck = NameCheck.Empty,
    /** A new password, or empty to keep the current one. */
    val password: String = "",
    val passwordRules: PasswordRules = passwordRules(""),
    val role: MemberRole = MemberRole.Member,
    val saving: Boolean = false,
    val deleting: Boolean = false,
    val deleteOpen: Boolean = false,
) {
    val locked: Boolean get() = lock != null

    val dirty: Boolean
        get() = member != null && !locked &&
            (name != member.username || role != MemberRole.of(member.permission) || password.isNotEmpty())

    val canSave: Boolean
        get() = dirty && nameCheck == NameCheck.Valid && (password.isEmpty() || passwordRules.ok) && !saving && !deleting
}

sealed interface EditMemberEvent {
    data object Saved : EditMemberEvent
    data object SaveFailed : EditMemberEvent
    /** The server already has the new name. */
    data object NameTaken : EditMemberEvent
    /** The member is gone: the screen leaves for My. */
    data object Deleted : EditMemberEvent
    data object DeleteFailed : EditMemberEvent
}

private const val KEY_NAME = "name"
private const val KEY_ROLE = "role"
private const val HTTP_CONFLICT = 409
private const val SUPER_ADMIN_PERMISSION = 100

/**
 * Edit member `my/members/{idx}`: rename, set a new password (optional) and change the role, or
 * delete. The member comes from [MemberStore]'s list, whose refresh after every mutation also
 * updates My. Unsaved name and role survive process death; the password is never saved.
 */
@HiltViewModel
class EditMemberViewModel internal constructor(
    private val savedState: SavedStateHandle,
    private val memberStore: MemberStore,
    private val sessions: StateFlow<SessionState>,
) : ViewModel() {

    @Inject
    constructor(
        savedState: SavedStateHandle,
        memberStore: MemberStore,
        sessionRepository: SessionRepository,
    ) : this(savedState, memberStore, sessionRepository.state)

    private val idx: Int = checkNotNull(savedState[Destinations.MemberIdxArg]) { "member idx missing" }

    private val local = MutableStateFlow(
        Local(
            name = savedState.get<String>(KEY_NAME),
            role = savedState.get<String>(KEY_ROLE)?.let { r -> MemberRole.entries.firstOrNull { it.name == r } },
        ),
    )

    private val _events = Channel<EditMemberEvent>(Channel.BUFFERED)
    val events: Flow<EditMemberEvent> = _events.receiveAsFlow()

    private val members: StateFlow<Resource<List<Member>>?> = memberStore.members()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The member as last seen, so the screen keeps showing them while they are being deleted. */
    private var lastSeen: Member? = null

    val state: StateFlow<EditMemberUiState> = combine(members, sessions, local) { res, session, l ->
        resolve(res, session as? SessionState.SignedIn, l)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EditMemberUiState())

    /** The state for the latest input, without waiting for [state] to be recomputed. */
    private fun current(): EditMemberUiState =
        resolve(members.value, sessions.value as? SessionState.SignedIn, local.value)

    fun setName(value: String) = edit {
        savedState[KEY_NAME] = value
        it.copy(name = value)
    }

    fun setPassword(value: String) = edit { it.copy(password = value) }

    fun setRole(role: MemberRole) = edit {
        savedState[KEY_ROLE] = role.name
        it.copy(role = role)
    }

    fun save() {
        val s = current()
        val member = s.member
        if (!s.canSave || member == null) return
        local.update { it.copy(saving = true) }
        viewModelScope.launch {
            memberStore.update(
                idx = member.idx,
                username = s.name.takeIf { it != member.username },
                password = s.password.ifEmpty { null },
                permission = s.role.permission.takeIf { s.role != MemberRole.of(member.permission) },
            ).onSuccess {
                // The store already reloaded the list: it is the new baseline.
                savedState.remove<String>(KEY_NAME)
                savedState.remove<String>(KEY_ROLE)
                local.update { it.copy(name = null, role = null, password = "", saving = false) }
                _events.send(EditMemberEvent.Saved)
            }.onFailure { e ->
                local.update { it.copy(saving = false) }
                _events.send(if (e.isConflict()) EditMemberEvent.NameTaken else EditMemberEvent.SaveFailed)
            }
        }
    }

    fun openDelete() {
        val s = current()
        if (s.member == null || s.locked || s.deleting) return
        local.update { it.copy(deleteOpen = true) }
    }

    fun dismissSheet() = local.update { it.copy(deleteOpen = false) }

    fun confirmDelete() {
        val s = current()
        val member = s.member
        if (!s.deleteOpen || member == null || s.locked || s.deleting) return
        local.update { it.copy(deleteOpen = false, deleting = true) }
        viewModelScope.launch {
            memberStore.delete(member.idx)
                .onSuccess {
                    local.update { it.copy(deleted = true) }
                    _events.send(EditMemberEvent.Deleted)
                }
                .onFailure {
                    local.update { it.copy(deleting = false) }
                    _events.send(EditMemberEvent.DeleteFailed)
                }
        }
    }

    /** The first load failed: try again. */
    fun retry() {
        viewModelScope.launch { memberStore.refresh() }
    }

    private fun edit(change: (Local) -> Local) {
        val s = current()
        if (s.member == null || s.locked || s.saving || s.deleting) return
        local.update(change)
    }

    private fun resolve(res: Resource<List<Member>>?, session: SessionState.SignedIn?, l: Local): EditMemberUiState {
        val found = res?.data?.firstOrNull { it.idx == idx }
        if (found != null) lastSeen = found
        // Once deleted (or while deleting), the refreshed list no longer has them: keep the last view.
        val member = found ?: lastSeen?.takeIf { l.deleting || l.deleted }
        val load = when {
            member != null -> EditLoad.Ready
            res == null || res.data == null && res.refreshing -> EditLoad.Loading
            res.data == null -> if (res.error != null) EditLoad.Failed else EditLoad.Loading
            else -> EditLoad.Missing
        }
        if (member == null) return EditMemberUiState(load = load)

        val lock = when {
            canEditMember(session?.userIdx, session?.permission ?: 0, member) -> null
            member.permission >= SUPER_ADMIN_PERMISSION -> MemberLock.SuperAdmin
            member.idx == session?.userIdx -> MemberLock.Self
            else -> MemberLock.Role
        }
        val name = l.name ?: member.username
        val others = res?.data.orEmpty().filter { it.idx != member.idx }.mapTo(HashSet()) { it.username.lowercase() }
        return EditMemberUiState(
            load = load,
            member = member,
            lock = lock,
            name = name,
            nameCheck = checkMemberName(name, { it.lowercase() in others }, current = member.username),
            password = l.password,
            passwordRules = passwordRules(l.password),
            role = l.role ?: MemberRole.of(member.permission),
            saving = l.saving,
            deleting = l.deleting || l.deleted,
            deleteOpen = l.deleteOpen && lock == null,
        )
    }

    /** Unsaved input; null [name] / [role] means unchanged from the member. */
    private data class Local(
        val name: String? = null,
        val role: MemberRole? = null,
        val password: String = "",
        val saving: Boolean = false,
        val deleting: Boolean = false,
        val deleted: Boolean = false,
        val deleteOpen: Boolean = false,
    )
}

private fun Throwable.isConflict() = this is ApiException && errorCode == HTTP_CONFLICT
