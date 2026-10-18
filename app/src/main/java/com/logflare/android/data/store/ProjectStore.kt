package com.logflare.android.data.store

import com.example.logflare.core.model.LogSummaryDTO
import com.example.logflare.core.model.ProjectCreateParams
import com.example.logflare.core.model.ProjectDTO
import com.example.logflare.core.model.ProjectPermsBatchParams
import com.example.logflare.core.network.LogflareApi
import com.logflare.android.data.session.SessionRepository
import com.logflare.android.data.session.SessionState
import com.logflare.android.data.store.model.HomeSummary
import com.logflare.android.data.store.model.ProjectItem
import com.logflare.android.data.store.model.ProjectWithToken
import com.logflare.android.data.store.model.mergeProjects
import com.logflare.android.data.store.model.toHomeSummary
import com.logflare.android.di.ApplicationScope
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.PairSerializer

/** Projects with their 24 h counts, the home summary, and project mutations. */
@Singleton
class ProjectStore internal constructor(
    private val api: LogflareApi,
    private val sessions: StateFlow<SessionState>,
    private val store: CacheStore,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
) {
    @Inject
    constructor(
        api: LogflareApi,
        sessionRepository: SessionRepository,
        store: CacheStore,
        @ApplicationScope scope: CoroutineScope,
    ) : this(api, sessionRepository.state, store, scope, System::currentTimeMillis)

    /**
     * Caches the raw (project list, summary) responses; they are merged on read. The fetch merges
     * once too, so a response that can't be mapped fails the fetch instead of the stream. The
     * summary half also backs `summary(null)`, so Home makes one summary call and every
     * mutation that refreshes the list refreshes the global counts too.
     */
    private val projectsResource = CachedResource(
        key = "projects",
        serializer = PairSerializer(ListSerializer(ProjectDTO.serializer()), LogSummaryDTO.serializer()),
        store = store,
        clock = clock,
        scope = scope,
    ) { session ->
        val cache = sessions.fetchAs(session) { token ->
            coroutineScope {
                val list = async { api.listProjects(token).let { requireData(it.success, it.message, it.error_code, it.data) } }
                val summary = async { api.getLogSummary(null).let { requireData(it.success, it.message, it.errorCode, it.data) } }
                list.await() to summary.await()
            }
        }
        cache.also { mergeProjects(it.first, it.second) }
    }

    /** Per-project summaries; the all-projects one lives in [projectsResource]. */
    private val summaries = ConcurrentHashMap<Int, CachedResource<LogSummaryDTO>>()

    fun projects(force: Boolean = false): Flow<Resource<List<ProjectItem>>> =
        sessions.whileSignedIn { session ->
            projectsResource.stream(session, force).map { r -> r.mapData { mergeProjects(it.first, it.second) } }
        }

    /** Counts for all of the user's projects, or for [projectId] only. */
    fun summary(projectId: Int? = null, force: Boolean = false): Flow<Resource<HomeSummary>> =
        sessions.whileSignedIn { session ->
            if (projectId == null) {
                projectsResource.stream(session, force).map { r -> r.mapData { it.second.toHomeSummary() } }
            } else {
                summaryResource(projectId).stream(session, force).map { r -> r.mapData { it.toHomeSummary() } }
            }
        }

    /**
     * Pull-to-refresh: fetches now, even when the cache is fresh, and returns once done with the
     * outcome. Without [projectId]: the project list and the all-projects summary; with it:
     * that project's summary.
     */
    suspend fun refresh(projectId: Int? = null): Result<Unit> = sessions.refreshSignedIn { session ->
        if (projectId == null) projectsResource.refresh(session) else summaryResource(projectId).refresh(session)
    }

    suspend fun create(name: String): Result<ProjectWithToken> = mutate {
        val dto = api.createProject(it, ProjectCreateParams(name))
            .let { r -> requireData(r.success, r.message, r.error_code, r.data) }
        ProjectWithToken(dto.id, dto.name, dto.token)
    }

    suspend fun rename(id: Int, name: String): Result<Unit> = mutate {
        api.changeProjectName(it, id, ProjectCreateParams(name)).let { r -> requireSuccess(r.success, r.message, r.error_code) }
    }

    suspend fun delete(id: Int): Result<Unit> = mutate {
        api.deleteProject(it, id).let { r -> requireSuccess(r.success, r.message, r.error_code) }
    }

    /** The project's ingest key (moderators and up). */
    suspend fun token(id: Int): Result<String> = attempt {
        sessions.signedIn()
        api.getProjectToken(id).let { requireData(it.success, it.message, it.error_code, it.data) }.token
    }

    suspend fun rotateToken(id: Int): Result<String> = attempt {
        val token = sessions.signedIn().token
        api.rotateProjectToken(token, id).let { requireData(it.success, it.message, it.error_code, it.data) }.token
    }

    /** User indexes that can view the project. */
    suspend fun members(id: Int): Result<Set<Int>> = attempt {
        val token = sessions.signedIn().token
        api.getProjectPermissions(token, id)
            .let { requireData(it.success, it.message, it.error_code, it.data) }
            .filter { it.view }
            .mapTo(mutableSetOf()) { it.user_id }
    }

    /** Replaces who can view the project. */
    suspend fun setMembers(id: Int, usernames: Set<String>): Result<Unit> = mutate {
        api.resetProjectPerms(it, ProjectPermsBatchParams(id, usernames))
            .let { r -> requireSuccess(r.success, r.message, r.error_code) }
    }

    private fun summaryResource(projectId: Int): CachedResource<LogSummaryDTO> =
        summaries.getOrPut(projectId) {
            CachedResource("summary:$projectId", LogSummaryDTO.serializer(), store, clock, scope) { session ->
                sessions.fetchAs(session) { api.getLogSummary(projectId) }
                    .let { requireData(it.success, it.message, it.errorCode, it.data) }
            }
        }

    /** Runs [block] with the session token, then refreshes the project list on success. */
    private suspend fun <T> mutate(block: suspend (token: String) -> T): Result<T> {
        val session = sessions.value as? SessionState.SignedIn ?: return Result.failure(NotSignedInException())
        return attempt { block(session.token) }.onSuccess { projectsResource.refresh(session.key) }
    }
}
