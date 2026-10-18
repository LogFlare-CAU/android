package com.logflare.android.data.store

import com.example.logflare.core.model.ErrorParams
import com.example.logflare.core.model.ErrorSequenceResponse
import com.example.logflare.core.model.FcmConfigResponse
import com.example.logflare.core.model.FcmTokenParams
import com.example.logflare.core.model.FcmTokenResponse
import com.example.logflare.core.model.LogEventDTO
import com.example.logflare.core.model.LogEventDetailDTO
import com.example.logflare.core.model.LogEventDetailResponse
import com.example.logflare.core.model.LogEventPageDTO
import com.example.logflare.core.model.LogEventPageResponse
import com.example.logflare.core.model.LogSummaryDTO
import com.example.logflare.core.model.LogSummaryResponse
import com.example.logflare.core.model.ProjectCreateParams
import com.example.logflare.core.model.ProjectDTO
import com.example.logflare.core.model.ProjectDTOWithToken
import com.example.logflare.core.model.ProjectPermsBatchParams
import com.example.logflare.core.model.ProjectPermsSequenceResponse
import com.example.logflare.core.model.ProjectResponse
import com.example.logflare.core.model.ProjectResponseWithToken
import com.example.logflare.core.model.ProjectSequenceResponse
import com.example.logflare.core.model.RemovedResponse
import com.example.logflare.core.model.StringResponse
import com.example.logflare.core.model.StringSequenceResponse
import com.example.logflare.core.model.UserAuthParams
import com.example.logflare.core.model.UserCreateParams
import com.example.logflare.core.model.UserDTO
import com.example.logflare.core.model.UserResponse
import com.example.logflare.core.model.UserSequenceResponse
import com.example.logflare.core.model.UserUpdateParams
import com.example.logflare.core.network.LogflareApi
import retrofit2.Response

/** Arguments of one [LogflareApi.getLogEvents] call. */
data class EventsCall(
    val projectId: Int?,
    val minLevel: String?,
    val logfileId: Int?,
    val query: String?,
    val sort: String,
    val cursor: String?,
)

/**
 * Hand-written [LogflareApi] for store tests: canned responses that tests can swap, and a log of
 * every call (method name) plus the Authorization header the old endpoints were given.
 */
class FakeLogflareApi : LogflareApi {
    val calls = mutableListOf<String>()
    val tokens = mutableListOf<String>()
    val eventsCalls = mutableListOf<EventsCall>()

    fun count(method: String) = calls.count { it == method }

    var projects: suspend () -> ProjectSequenceResponse = { ok(projects = listOf(project(1, "alpha"))) }
    var summary: suspend (Int?) -> LogSummaryResponse = { summaryOf() }
    var events: suspend (EventsCall) -> LogEventPageResponse = { LogEventPageResponse(true, "ok", data = LogEventPageDTO()) }
    var users: suspend () -> UserSequenceResponse = { UserSequenceResponse(true, "ok", data = emptyList()) }
    var me: suspend () -> UserResponse = { UserResponse(true, "ok", data = UserDTO(7, "me", 100)) }
    var createProjectResult: suspend (String) -> ProjectResponseWithToken = { name ->
        ProjectResponseWithToken(true, "ok", data = ProjectDTOWithToken(id = 99, name = name, token = "tok-99"))
    }

    private fun record(method: String, token: String? = null) {
        calls += method
        if (token != null) tokens += token
    }

    override suspend fun listProjects(bearer: String): ProjectSequenceResponse {
        record("listProjects", bearer)
        return projects()
    }

    override suspend fun getLogSummary(projectId: Int?, hours: Int): LogSummaryResponse {
        record("getLogSummary")
        return summary(projectId)
    }

    override suspend fun getLogEvents(
        projectId: Int?,
        minLevel: String?,
        logfileId: Int?,
        query: String?,
        sort: String,
        grouped: Boolean,
        limit: Int,
        cursor: String?,
    ): LogEventPageResponse {
        record("getLogEvents")
        val call = EventsCall(projectId, minLevel, logfileId, query, sort, cursor)
        eventsCalls += call
        return events(call)
    }

    override suspend fun createProject(bearer: String, body: ProjectCreateParams): ProjectResponseWithToken {
        record("createProject", bearer)
        return createProjectResult(body.name)
    }

    override suspend fun getAllUsers(token: String): UserSequenceResponse {
        record("getAllUsers", token)
        return users()
    }

    override suspend fun getme(token: String): UserResponse {
        record("getme", token)
        return me()
    }

    override suspend fun getFirebaseConfig(bearer: String): FcmConfigResponse {
        record("getFirebaseConfig", bearer)
        return FcmConfigResponse(true, "ok")
    }

    override suspend fun getFcmConfig(bearer: String): FcmConfigResponse {
        record("getFcmConfig", bearer)
        return FcmConfigResponse(true, "ok")
    }

    /** FCM tokens sent to [registerFcmToken] / [unregisterFcmToken], in call order. */
    val fcmTokens = mutableListOf<String>()
    var registerFcm: suspend (FcmTokenParams) -> FcmTokenResponse = { FcmTokenResponse(true, "ok") }
    var unregisterFcm: suspend (FcmTokenParams) -> RemovedResponse = { RemovedResponse(true, "ok") }
    var authenticateResult: suspend (UserAuthParams) -> StringResponse = { StringResponse(true, "ok", data = "jwt-${it.username}") }
    val authCalls = mutableListOf<UserAuthParams>()

    override suspend fun registerFcmToken(bearer: String, body: FcmTokenParams): FcmTokenResponse {
        record("registerFcmToken", bearer)
        fcmTokens += body.fcmToken
        return registerFcm(body)
    }

    override suspend fun unregisterFcmToken(body: FcmTokenParams): RemovedResponse {
        record("unregisterFcmToken")
        fcmTokens += body.fcmToken
        return unregisterFcm(body)
    }

    override suspend fun authenticate(body: UserAuthParams): StringResponse {
        record("authenticate")
        authCalls += body
        return authenticateResult(body)
    }

    /** When set, every mutation endpoint throws it (e.g. no network). */
    var mutationError: Exception? = null

    /** When true, every mutation endpoint answers `success: false` with [REJECTED]. */
    var mutationRejected = false

    private suspend fun <T> mutation(method: String, token: String?, ok: () -> T, rejected: () -> T): T {
        record(method, token)
        mutationError?.let { throw it }
        return if (mutationRejected) rejected() else ok()
    }

    override suspend fun changeProjectName(bearer: String, projectId: Int, body: ProjectCreateParams): ProjectResponse =
        mutation("changeProjectName", bearer, { ProjectResponse(true, "ok", data = ProjectDTO(projectId, body.name)) }) {
            ProjectResponse(false, REJECTED, 400)
        }

    override suspend fun deleteProject(bearer: String, projectId: Int): StringResponse =
        mutation("deleteProject", bearer, { StringResponse(true, "ok") }) { StringResponse(false, REJECTED, 404) }

    override suspend fun rotateProjectToken(bearer: String, projectId: Int): ProjectResponseWithToken =
        mutation(
            "rotateProjectToken",
            bearer,
            { ProjectResponseWithToken(true, "ok", data = ProjectDTOWithToken(projectId, "p$projectId", token = "rotated-$projectId")) },
        ) { ProjectResponseWithToken(false, REJECTED, 403) }

    /** Bodies of every [resetProjectPerms] call. */
    val permsResets = mutableListOf<ProjectPermsBatchParams>()

    override suspend fun resetProjectPerms(bearer: String, body: ProjectPermsBatchParams): ProjectPermsSequenceResponse =
        also { permsResets += body }.mutation("resetProjectPerms", bearer, { ProjectPermsSequenceResponse(true, "ok", data = emptyList()) }) {
            ProjectPermsSequenceResponse(false, REJECTED, 400)
        }

    /** Bodies of every [createUser] call, and `userId to body` of every [updateUser] call. */
    val userCreates = mutableListOf<UserCreateParams>()
    val userUpdates = mutableListOf<Pair<Int, UserUpdateParams>>()

    override suspend fun createUser(token: String, body: UserCreateParams): UserResponse =
        also { userCreates += body }.mutation("createUser", token, { UserResponse(true, "ok", data = UserDTO(50, body.username, body.permission)) }) {
            UserResponse(false, REJECTED, 409)
        }

    override suspend fun updateUser(token: String, userId: Int, body: UserUpdateParams): UserResponse =
        also { userUpdates += userId to body }.mutation(
            "updateUser",
            token,
            { UserResponse(true, "ok", data = UserDTO(userId, body.username ?: "unchanged", body.permission ?: 0)) },
        ) { UserResponse(false, REJECTED, 404) }

    override suspend fun deleteUser(token: String, userId: Int): UserResponse =
        mutation("deleteUser", token, { UserResponse(true, "ok") }) { UserResponse(false, REJECTED, 404) }

    override suspend fun getUserByUsername(token: String, username: String): UserResponse = TODO()
    var projectPerms: suspend (Int) -> ProjectPermsSequenceResponse = { ProjectPermsSequenceResponse(true, "ok", data = emptyList()) }

    override suspend fun getProjectPermissions(bearer: String, projectId: Int): ProjectPermsSequenceResponse {
        record("getProjectPermissions", bearer)
        return projectPerms(projectId)
    }

    override suspend fun getErrors(bearer: String, projectId: Int?, limit: Int, offset: Int, sortby: String?): ErrorSequenceResponse = TODO()
    override suspend fun postError(projectKey: String, projectName: String, body: ErrorParams): Response<Unit> = TODO()
    override suspend fun getProjectLogFile(bearer: String, projectId: Int, logfileid: Int, limit: Int, offset: Int, sortBy: String?): StringSequenceResponse = TODO()
    var eventDetail: suspend (Int) -> LogEventDetailResponse = { LogEventDetailResponse(true, "ok", data = detail(it)) }

    override suspend fun getLogEvent(id: Int): LogEventDetailResponse {
        record("getLogEvent")
        return eventDetail(id)
    }

    var projectToken: suspend (Int) -> ProjectResponseWithToken = { id ->
        ProjectResponseWithToken(true, "ok", data = ProjectDTOWithToken(id, "p$id", token = "tok-$id"))
    }

    override suspend fun getProjectToken(projectId: Int): ProjectResponseWithToken {
        record("getProjectToken")
        return projectToken(projectId)
    }

    companion object {
        const val REJECTED = "rejected by server"

        fun project(id: Int, name: String) = ProjectDTO(id = id, name = name)

        fun ok(projects: List<ProjectDTO>) = ProjectSequenceResponse(true, "ok", data = projects)

        fun summaryOf(
            counts: Map<String, Int> = emptyMap(),
            projects: List<com.example.logflare.core.model.ProjectSummaryDTO> = emptyList(),
        ) = LogSummaryResponse(
            true,
            "ok",
            data = LogSummaryDTO(windowHours = 24, since = "2026-10-17T10:00:00", counts = counts, projects = projects),
        )

        fun event(id: Int, level: String = "ERROR", message: String = "boom $id") = LogEventDTO(
            id = id,
            projectId = 1,
            logfileName = "app.log",
            errortype = "RuntimeError",
            message = message,
            level = level,
            timestamp = "2026-10-18T09:00:00",
            occurrences = 2,
            firstSeen = "2026-10-18T08:00:00.5",
            lastSeen = "2026-10-18T09:00:00",
        )

        fun detail(id: Int) = LogEventDetailDTO(
            id = id,
            projectId = 1,
            logfileName = "app.log",
            errortype = "RuntimeError",
            message = "boom $id",
            level = "ERROR",
            timestamp = "2026-10-18T09:00:00",
            occurrences = 2,
            firstSeen = "2026-10-18T08:00:00",
            lastSeen = "2026-10-18T09:00:00",
            occurredAt = "2026-10-18T09:00:00",
            rowLevel = "ERROR",
        )

        fun page(vararg ids: Int, cursor: String? = null) =
            LogEventPageResponse(true, "ok", data = LogEventPageDTO(ids.map { event(it) }, cursor))
    }
}
