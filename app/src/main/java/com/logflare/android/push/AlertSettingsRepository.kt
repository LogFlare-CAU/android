package com.logflare.android.push

import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.model.ProjectData
import com.logflare.android.data.DeviceRepository
import com.logflare.android.data.ProjectsRepository
import com.logflare.android.data.session.SessionKey
import com.logflare.android.data.session.SessionRepository
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.CacheStore
import com.logflare.android.data.store.attempt
import com.logflare.android.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Pre-rework alert settings: DataStore `fcm`/`alert_level` and the `projects` DataStore list. */
data class LegacyAlerts(val globalLevel: String?, val projects: List<ProjectData>)

internal fun LegacyAlerts.toSettings(): AlertSettings = AlertSettings(
    global = GlobalAlert.fromLegacyLabel(globalLevel),
    projects = projects.associate { it.dto.id to ProjectAlert(LfLevel.parse(it.alertLevel), it.excludeKeywords) },
)

/**
 * Push alert settings of the signed-in session, persisted per [SessionKey] in [CacheStore].
 *
 * [settings] is hot and in memory so the messaging service can read `.value` without blocking.
 * It switches when the session key changes (defaults while signed out or still loading). A
 * session without saved settings gets the legacy values migrated once, on its first load.
 */
@Singleton
class AlertSettingsRepository internal constructor(
    private val sessions: StateFlow<SessionState>,
    private val store: CacheStore,
    private val legacy: suspend () -> LegacyAlerts,
    scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    @Inject
    constructor(
        sessionRepository: SessionRepository,
        store: CacheStore,
        deviceRepository: DeviceRepository,
        projectsRepository: ProjectsRepository,
        @ApplicationScope scope: CoroutineScope,
    ) : this(
        sessions = sessionRepository.state,
        store = store,
        legacy = { LegacyAlerts(deviceRepository.getAlertLevel(), projectsRepository.getAll()) },
        scope = scope,
    )

    private val _settings = MutableStateFlow(AlertSettings())
    val settings: StateFlow<AlertSettings> = _settings.asStateFlow()

    /** Session whose settings [settings] currently holds; null while switching or signed out. */
    private val loadedFor = MutableStateFlow<SessionKey?>(null)

    /** Serializes loads and read-modify-write updates. */
    private val lock = Mutex()

    init {
        scope.launch {
            sessions.map { (it as? SessionState.SignedIn)?.key }
                .distinctUntilChanged()
                .collectLatest { key ->
                    lock.withLock {
                        loadedFor.value = null
                        _settings.value = AlertSettings()
                        if (key != null) {
                            _settings.value = loadOrMigrate(key)
                            loadedFor.value = key
                        }
                    }
                }
        }
    }

    /** True when [settings] belongs to [session] (always for signed out; never while loading). */
    fun isReadyFor(session: SessionState): Boolean = when (session) {
        SessionState.Loading -> false
        SessionState.SignedOut -> true
        is SessionState.SignedIn -> loadedFor.value == session.key
    }

    /** Waits until the session is known and its settings are loaded; returns both. */
    suspend fun awaitReady(): Pair<SessionState, AlertSettings> {
        val state = combine(sessions, loadedFor) { s, _ -> s }.first { isReadyFor(it) }
        return state to _settings.value
    }

    suspend fun setGlobal(v: GlobalAlert) = update { it.copy(global = v) }

    suspend fun setQuietHours(on: Boolean) = update { it.copy(quietHours = on) }

    suspend fun setProjectLevel(projectId: Int, level: LfLevel) = updateProject(projectId) { it.copy(level = level) }

    suspend fun setMutedKeywords(projectId: Int, keywords: Set<String>) =
        updateProject(projectId) { it.copy(mutedKeywords = keywords) }

    private suspend fun updateProject(projectId: Int, transform: (ProjectAlert) -> ProjectAlert) = update {
        it.copy(projects = it.projects + (projectId to transform(it.projects[projectId] ?: ProjectAlert())))
    }

    /** Applies [transform] to the signed-in session's settings and saves them. No-op when signed out. */
    private suspend fun update(transform: (AlertSettings) -> AlertSettings) {
        val key = (sessions.value as? SessionState.SignedIn)?.key ?: return
        lock.withLock {
            val generation = store.generation(key)
            val current = if (loadedFor.value == key) _settings.value else loadOrMigrate(key)
            val next = transform(current)
            store.write(key, KEY, next, AlertSettings.serializer(), clock(), generation)
            if (loadedFor.value == key) _settings.value = next
        }
    }

    private suspend fun loadOrMigrate(key: SessionKey): AlertSettings {
        store.read(key, KEY, AlertSettings.serializer())?.let { return it.value }
        val generation = store.generation(key)
        // A legacy store that can't be read yields defaults, unsaved, so the next load retries.
        val migrated = attempt { legacy().toSettings() }.getOrElse { return AlertSettings() }
        store.write(key, KEY, migrated, AlertSettings.serializer(), clock(), generation)
        return migrated
    }

    private companion object {
        const val KEY = "alert-settings"
    }
}
