package com.logflare.android.push

import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.model.ProjectData
import com.logflare.android.data.DeviceRepository
import com.logflare.android.data.LegacyProjectSettingsReader
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

/**
 * Legacy projects defaulted to "Warning", so that default (and an unset legacy global) becomes a
 * WarningAndAbove global instead of a per-project override; projects then follow the global value.
 * No legacy data at all yields plain defaults.
 */
internal fun LegacyAlerts.toSettings(): AlertSettings {
    val global = globalLevel?.takeIf { it.isNotBlank() }
    if (global == null && projects.isEmpty()) return AlertSettings()
    return AlertSettings(
        global = global?.let(GlobalAlert::fromLegacyLabel) ?: GlobalAlert.WarningAndAbove,
        projects = projects
            .associate { p ->
                val level = p.alertLevel.takeUnless { it.trim().equals(LEGACY_DEFAULT_LEVEL, ignoreCase = true) }
                p.dto.id to ProjectAlert(level?.let(LfLevel::parse), p.excludeKeywords)
            }
            .filterValues { it != ProjectAlert() },
    )
}

private const val LEGACY_DEFAULT_LEVEL = "Warning"

/** [settings] together with the session they belong to; [session] null = none loaded. */
data class AlertSnapshot(val session: SessionKey?, val settings: AlertSettings)

/**
 * Push alert settings of the signed-in session, persisted per [SessionKey] in [CacheStore].
 *
 * [settings] is hot and in memory so the messaging service can read `.value` without blocking.
 * It switches when the session key changes (defaults while signed out or still loading). A
 * session without saved settings gets the legacy values migrated once, on its first load.
 * [snapshot] pairs the settings with their session so both are read in one atomic step.
 */
@Singleton
class AlertSettingsRepository internal constructor(
    private val sessions: StateFlow<SessionState>,
    private val store: CacheStore,
    private val legacy: suspend () -> LegacyAlerts,
    scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    /** Deletes the legacy stores after their values were copied, so they migrate only once per install. */
    private val clearLegacy: suspend () -> Unit = {},
) {
    @Inject
    constructor(
        sessionRepository: SessionRepository,
        store: CacheStore,
        deviceRepository: DeviceRepository,
        legacyProjects: LegacyProjectSettingsReader,
        @ApplicationScope scope: CoroutineScope,
    ) : this(
        sessions = sessionRepository.state,
        store = store,
        legacy = { LegacyAlerts(deviceRepository.getAlertLevel(), legacyProjects.readAll()) },
        scope = scope,
        clearLegacy = {
            deviceRepository.clearLegacyAlertLevel()
            legacyProjects.clear()
        },
    )

    private val _snapshot = MutableStateFlow(AlertSnapshot(null, AlertSettings()))
    val snapshot: StateFlow<AlertSnapshot> = _snapshot.asStateFlow()

    private val _settings = MutableStateFlow(AlertSettings())
    val settings: StateFlow<AlertSettings> = _settings.asStateFlow()

    /** Serializes loads and read-modify-write updates. */
    private val lock = Mutex()

    init {
        scope.launch {
            sessions.map { (it as? SessionState.SignedIn)?.key }
                .distinctUntilChanged()
                .collectLatest { key ->
                    lock.withLock {
                        publish(AlertSnapshot(null, AlertSettings()))
                        if (key != null) publish(AlertSnapshot(key, loadOrMigrate(key)))
                    }
                }
        }
    }

    /**
     * The settings that belong to [session], or null when they aren't loaded yet (or the session
     * is still [SessionState.Loading]). Signed out has nothing to wait for.
     */
    fun settingsFor(session: SessionState, snapshot: AlertSnapshot = _snapshot.value): AlertSettings? =
        when (session) {
            SessionState.Loading -> null
            SessionState.SignedOut -> AlertSettings()
            is SessionState.SignedIn -> snapshot.settings.takeIf { snapshot.session == session.key }
        }

    /** Waits until the session is known and its settings are loaded; returns both. */
    suspend fun awaitReady(): Pair<SessionState, AlertSettings> {
        val (state, settings) = combine(sessions, _snapshot) { s, snap -> s to settingsFor(s, snap) }
            .first { it.second != null }
        return state to checkNotNull(settings)
    }

    suspend fun setGlobal(v: GlobalAlert) = update { it.copy(global = v) }

    suspend fun setQuietHours(on: Boolean) = update { it.copy(quietHours = on) }

    /** [level] null returns the project to the global threshold. */
    suspend fun setProjectLevel(projectId: Int, level: LfLevel?) = updateProject(projectId) { it.copy(level = level) }

    suspend fun clearProjectLevel(projectId: Int) = setProjectLevel(projectId, null)

    suspend fun setMutedKeywords(projectId: Int, keywords: Set<String>) =
        updateProject(projectId) { it.copy(mutedKeywords = keywords) }

    /** An entry back at [ProjectAlert] defaults is removed: the project follows the global value. */
    private suspend fun updateProject(projectId: Int, transform: (ProjectAlert) -> ProjectAlert) = update {
        val next = transform(it.projects[projectId] ?: ProjectAlert())
        it.copy(projects = if (next == ProjectAlert()) it.projects - projectId else it.projects + (projectId to next))
    }

    /**
     * Applies [transform] to the signed-in session's settings and saves them. No-op when signed
     * out. If the session's cache was cleared meanwhile, the write is rejected and memory is kept.
     */
    private suspend fun update(transform: (AlertSettings) -> AlertSettings) {
        val key = (sessions.value as? SessionState.SignedIn)?.key ?: return
        lock.withLock {
            val generation = store.generation(key)
            val loaded = _snapshot.value.takeIf { it.session == key }
            val next = transform(loaded?.settings ?: loadOrMigrate(key))
            store.write(key, KEY, next, AlertSettings.serializer(), clock(), generation)
            // Generations only grow, so a changed one means the write was dropped (or undone).
            val accepted = store.generation(key) == generation
            if (accepted && loaded != null) publish(AlertSnapshot(key, next))
        }
    }

    private fun publish(snapshot: AlertSnapshot) {
        _snapshot.value = snapshot
        _settings.value = snapshot.settings
    }

    private suspend fun loadOrMigrate(key: SessionKey): AlertSettings {
        store.read(key, KEY, AlertSettings.serializer())?.let { return it.value }
        val generation = store.generation(key)
        // A legacy store that can't be read yields defaults, unsaved, so the next load retries.
        val migrated = attempt { legacy().toSettings() }.getOrElse { return AlertSettings() }
        val persisted = store.write(key, KEY, migrated, AlertSettings.serializer(), clock(), generation)
        // Only once the copy is on disk (a dropped or failed write keeps the legacy values for the
        // next load to retry); a failed clear just retries (and re-copies) next load.
        if (persisted) attempt { clearLegacy() }
        return migrated
    }

    private companion object {
        const val KEY = "alert-settings"
    }
}
