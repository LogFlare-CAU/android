package com.logflare.android.feature.mypage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logflare.android.data.session.SessionRepository
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.ApiException
import com.logflare.android.data.store.MemberStore
import com.logflare.android.feature.project.NameCheck
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddedMember(val name: String, val role: MemberRole)

data class AddMemberUiState(
    val name: String = "",
    val nameCheck: NameCheck = NameCheck.Empty,
    val password: String = "",
    val passwordRules: PasswordRules = passwordRules(""),
    val role: MemberRole = MemberRole.Member,
    val submitting: Boolean = false,
    /** Set once the server created the member: the screen then shows the success state. */
    val added: AddedMember? = null,
    /** The signed-in server's host, shown on the success state. */
    val serverHost: String = "",
    val discardOpen: Boolean = false,
) {
    val canAdd: Boolean get() = nameCheck == NameCheck.Valid && passwordRules.ok && !submitting && added == null

    /** Something was typed; closing then asks first. */
    val dirty: Boolean get() = added == null && (name.isNotEmpty() || password.isNotEmpty())
}

sealed interface AddMemberEvent {
    data object NavigateBack : AddMemberEvent
    data object AddFailed : AddMemberEvent
    /** The server already has that name (someone added it since the list was loaded). */
    data object NameTaken : AddMemberEvent
}

private const val KEY_NAME = "name"
private const val KEY_ROLE = "role"
private const val KEY_ADDED_NAME = "addedName"
private const val KEY_ADDED_ROLE = "addedRole"
private const val HTTP_CONFLICT = 409

/**
 * Add member: name (checked against the cached member list as typed), password with its rules,
 * and role. On success the member list is refreshed by [MemberStore] so My shows them at once.
 * Name, role and the added member survive process death; the password is never saved.
 */
@HiltViewModel
class AddMemberViewModel internal constructor(
    private val savedState: SavedStateHandle,
    private val memberStore: MemberStore,
    sessions: StateFlow<SessionState>,
) : ViewModel() {

    @Inject
    constructor(
        savedState: SavedStateHandle,
        memberStore: MemberStore,
        sessionRepository: SessionRepository,
    ) : this(savedState, memberStore, sessionRepository.state)

    private val local = MutableStateFlow(
        AddMemberUiState(
            name = savedState.get<String>(KEY_NAME).orEmpty(),
            role = savedState.get<String>(KEY_ROLE).toRole(),
            added = savedState.get<String>(KEY_ADDED_NAME)?.let { AddedMember(it, savedState.get<String>(KEY_ADDED_ROLE).toRole()) },
        ),
    )

    private val _events = Channel<AddMemberEvent>(Channel.BUFFERED)
    val events: Flow<AddMemberEvent> = _events.receiveAsFlow()

    /** Lower-cased names of every user, for the "Already taken" check. */
    private val takenNames: StateFlow<Set<String>> = memberStore.members()
        .map { r -> r.data.orEmpty().mapTo(HashSet()) { it.username.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val host = sessions.map { (it as? SessionState.SignedIn)?.serverUrl?.let(::serverHost).orEmpty() }

    val state: StateFlow<AddMemberUiState> = combine(local, takenNames, host) { l, taken, h ->
        l.resolved(taken).copy(serverHost = h)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value.resolved(emptySet()))

    fun setName(value: String) {
        if (local.value.added != null) return
        local.update { it.copy(name = value) }
        savedState[KEY_NAME] = value
    }

    fun setPassword(value: String) {
        if (local.value.added != null) return
        local.update { it.copy(password = value) }
    }

    fun setRole(role: MemberRole) {
        if (local.value.added != null) return
        local.update { it.copy(role = role) }
        savedState[KEY_ROLE] = role.name
    }

    fun add() {
        val current = local.value.resolved(takenNames.value)
        if (!current.canAdd) return
        local.update { it.copy(submitting = true) }
        viewModelScope.launch {
            memberStore.add(current.name, current.password, current.role.permission)
                .onSuccess { member ->
                    val added = AddedMember(member.username, current.role)
                    savedState[KEY_ADDED_NAME] = added.name
                    savedState[KEY_ADDED_ROLE] = added.role.name
                    local.update { it.copy(submitting = false, password = "", added = added) }
                }
                .onFailure { e ->
                    local.update { it.copy(submitting = false) }
                    val taken = e is ApiException && e.errorCode == HTTP_CONFLICT
                    _events.send(if (taken) AddMemberEvent.NameTaken else AddMemberEvent.AddFailed)
                }
        }
    }

    /** From the success state: an empty form again. */
    fun addAnother() {
        local.value = AddMemberUiState()
        listOf(KEY_NAME, KEY_ROLE, KEY_ADDED_NAME, KEY_ADDED_ROLE).forEach { savedState.remove<Any>(it) }
    }

    /** The close button / system Back: asks first when something was typed. */
    fun close() {
        if (local.value.dirty) {
            local.update { it.copy(discardOpen = true) }
        } else {
            _events.trySend(AddMemberEvent.NavigateBack)
        }
    }

    fun confirmDiscard() {
        local.update { it.copy(discardOpen = false) }
        _events.trySend(AddMemberEvent.NavigateBack)
    }

    fun dismissSheet() = local.update { it.copy(discardOpen = false) }

    private fun AddMemberUiState.resolved(taken: Set<String>) = copy(
        nameCheck = checkMemberName(name, { it.lowercase() in taken }),
        passwordRules = passwordRules(password),
    )
}

private fun String?.toRole(): MemberRole = MemberRole.entries.firstOrNull { it.name == this } ?: MemberRole.Member
