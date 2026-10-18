package com.logflare.android.feature.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.designsystem.v2.components.KeywordResult
import com.example.logflare.core.designsystem.v2.components.normalizeKeyword
import com.logflare.android.data.session.SessionRepository
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.ProjectStore
import com.logflare.android.push.AlertSettingsRepository
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
import kotlinx.coroutines.launch

data class CreatedProject(val id: Int, val name: String, val token: String)

enum class CreateSheet { Discard, Rotate }

data class ProjectCreateUiState(
    val name: String = "",
    val nameCheck: NameCheck = NameCheck.Empty,
    val level: LfLevel = DEFAULT_LEVEL,
    val keywords: List<String> = emptyList(),
    /** Something was changed from the empty form; closing then asks first. */
    val dirty: Boolean = false,
    val submitting: Boolean = false,
    /** Set once the server created the project: the screen then shows the token. */
    val created: CreatedProject? = null,
    val tokenRevealed: Boolean = false,
    val sheet: CreateSheet? = null,
    /** The signed-in server, for the example request. */
    val serverUrl: String = "",
)

sealed interface ProjectCreateEvent {
    data object NavigateBack : ProjectCreateEvent
    data object CreateFailed : ProjectCreateEvent
    data object TokenRotated : ProjectCreateEvent
    data object RotateFailed : ProjectCreateEvent
}

private val DEFAULT_LEVEL = LfLevel.Warning

private const val KEY_NAME = "name"
private const val KEY_LEVEL = "level"
private const val KEY_KEYWORDS = "keywords"
private const val KEY_CREATED_ID = "createdId"
private const val KEY_CREATED_NAME = "createdName"
private const val KEY_CREATED_TOKEN = "createdToken"

/**
 * New project: name, alert level and muted keywords, then the project token. The form (and the
 * created project, whose token is only shown here) lives in [savedState] so it survives rotation
 * and process death; sheets and the reveal toggle do not. The alert level and keywords are device
 * settings, saved to [AlertSettingsRepository] for the new id once the server created the project.
 */
@HiltViewModel
class ProjectCreateViewModel internal constructor(
    private val savedState: SavedStateHandle,
    private val projectStore: ProjectStore,
    private val alerts: AlertSettingsRepository,
    sessions: StateFlow<SessionState>,
) : ViewModel() {

    @Inject
    constructor(
        savedState: SavedStateHandle,
        projectStore: ProjectStore,
        alerts: AlertSettingsRepository,
        sessionRepository: SessionRepository,
    ) : this(savedState, projectStore, alerts, sessionRepository.state)

    private val form = MutableStateFlow(readForm())
    private val local = MutableStateFlow(Local(created = readCreated()))

    private val _events = Channel<ProjectCreateEvent>(Channel.BUFFERED)
    val events: Flow<ProjectCreateEvent> = _events.receiveAsFlow()

    private val existingNames: StateFlow<List<String>> = projectStore.projects()
        .map { r -> r.data.orEmpty().map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val serverUrl = sessions.map { (it as? SessionState.SignedIn)?.serverUrl.orEmpty() }

    val state: StateFlow<ProjectCreateUiState> =
        combine(form, local, existingNames, serverUrl) { f, l, names, url ->
            ProjectCreateUiState(
                name = f.name,
                nameCheck = checkProjectName(f.name, names),
                level = f.level,
                keywords = f.keywords,
                dirty = isDirty(f, l),
                submitting = l.submitting,
                created = l.created,
                tokenRevealed = l.tokenRevealed,
                sheet = l.sheet,
                serverUrl = url,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState())

    fun setName(value: String) = updateForm { it.copy(name = value) }

    fun setLevel(level: LfLevel) = updateForm { it.copy(level = level) }

    fun addKeyword(raw: String) = updateForm {
        val r = normalizeKeyword(raw, it.keywords)
        if (r is KeywordResult.Added) it.copy(keywords = it.keywords + r.value) else it
    }

    fun removeKeyword(keyword: String) = updateForm { it.copy(keywords = it.keywords - keyword) }

    /** Creates the project when the name is valid; the level and keywords follow on success. */
    fun create() {
        val f = form.value
        val l = local.value
        if (checkProjectName(f.name, existingNames.value) != NameCheck.Valid || l.submitting || l.created != null) return
        local.value = l.copy(submitting = true)
        viewModelScope.launch {
            val result = projectStore.create(f.name)
            val project = result.getOrNull()
            if (project == null) {
                local.value = local.value.copy(submitting = false)
                _events.send(ProjectCreateEvent.CreateFailed)
                return@launch
            }
            // The server side is done; a failed local save only loses the (default) alert setup.
            runCatching {
                alerts.setProjectLevel(project.id, f.level)
                alerts.setMutedKeywords(project.id, f.keywords.toSet())
            }
            val created = CreatedProject(project.id, project.name, project.token)
            writeCreated(created)
            local.value = local.value.copy(submitting = false, created = created)
        }
    }

    /** The close button / system Back: asks first when the form was changed. */
    fun close() {
        if (isDirty(form.value, local.value)) {
            local.value = local.value.copy(sheet = CreateSheet.Discard)
        } else {
            _events.trySend(ProjectCreateEvent.NavigateBack)
        }
    }

    fun confirmDiscard() {
        local.value = local.value.copy(sheet = null)
        _events.trySend(ProjectCreateEvent.NavigateBack)
    }

    fun dismissSheet() {
        local.value = local.value.copy(sheet = null)
    }

    fun toggleReveal() {
        local.value = local.value.copy(tokenRevealed = !local.value.tokenRevealed)
    }

    fun openRotate() {
        if (local.value.created != null) local.value = local.value.copy(sheet = CreateSheet.Rotate)
    }

    fun confirmRotate() {
        val created = local.value.created ?: return
        if (local.value.sheet != CreateSheet.Rotate) return
        local.value = local.value.copy(sheet = null)
        viewModelScope.launch {
            projectStore.rotateToken(created.id)
                .onSuccess { token ->
                    val next = created.copy(token = token)
                    writeCreated(next)
                    local.value = local.value.copy(created = next)
                    _events.send(ProjectCreateEvent.TokenRotated)
                }
                .onFailure { _events.send(ProjectCreateEvent.RotateFailed) }
        }
    }

    private data class Form(
        val name: String = "",
        val level: LfLevel = DEFAULT_LEVEL,
        val keywords: List<String> = emptyList(),
    )

    private data class Local(
        val created: CreatedProject? = null,
        val submitting: Boolean = false,
        val tokenRevealed: Boolean = false,
        val sheet: CreateSheet? = null,
    )

    private fun isDirty(f: Form, l: Local) = l.created == null && f != Form()

    private fun initialState() = form.value.let {
        ProjectCreateUiState(
            name = it.name,
            nameCheck = checkProjectName(it.name, emptyList()),
            level = it.level,
            keywords = it.keywords,
            dirty = isDirty(it, local.value),
            created = local.value.created,
        )
    }

    private fun updateForm(change: (Form) -> Form) {
        if (local.value.created != null) return
        val next = change(form.value)
        form.value = next
        savedState[KEY_NAME] = next.name
        savedState[KEY_LEVEL] = next.level.name
        savedState[KEY_KEYWORDS] = ArrayList(next.keywords)
    }

    private fun readForm() = Form(
        name = savedState.get<String>(KEY_NAME).orEmpty(),
        level = savedState.get<String>(KEY_LEVEL)?.let { runCatching { LfLevel.valueOf(it) }.getOrNull() } ?: DEFAULT_LEVEL,
        keywords = savedState.get<ArrayList<String>>(KEY_KEYWORDS)?.toList().orEmpty(),
    )

    private fun readCreated(): CreatedProject? {
        val id = savedState.get<Int>(KEY_CREATED_ID) ?: return null
        return CreatedProject(
            id,
            savedState.get<String>(KEY_CREATED_NAME).orEmpty(),
            savedState.get<String>(KEY_CREATED_TOKEN).orEmpty(),
        )
    }

    private fun writeCreated(created: CreatedProject) {
        savedState[KEY_CREATED_ID] = created.id
        savedState[KEY_CREATED_NAME] = created.name
        savedState[KEY_CREATED_TOKEN] = created.token
    }
}
