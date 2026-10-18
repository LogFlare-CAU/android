package com.logflare.android.data.store

import com.example.logflare.core.model.ErrorParams
import com.example.logflare.core.model.ErrorSequenceResponse
import com.example.logflare.core.model.FcmConfigResponse
import com.example.logflare.core.model.FcmTokenParams
import com.example.logflare.core.model.FcmTokenResponse
import com.example.logflare.core.model.LogEventDTO
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

    override suspend fun registerFcmToken(bearer: String, body: FcmTokenParams): FcmTokenResponse {
        record("registerFcmToken", bearer)
        return FcmTokenResponse(true, "ok")
    }

    override suspend fun unregisterFcmToken(body: FcmTokenParams): RemovedResponse {
        record("unregisterFcmToken")
        return RemovedResponse(true, "ok")
    }

    override suspend fun createUser(token: String, body: UserCreateParams): UserResponse = TODO()
    override suspend fun authenticate(body: UserAuthParams): StringResponse = TODO()
    override suspend fun getUserByUsername(token: String, username: String): UserResponse = TODO()
    override suspend fun updateUser(token: String, userId: Int, body: UserUpdateParams): UserResponse = TODO()
    override suspend fun deleteUser(token: String, userId: Int): UserResponse = TODO()
    override suspend fun deleteProject(bearer: String, projectId: Int): StringResponse = TODO()
    override suspend fun changeProjectName(bearer: String, projectId: Int, body: ProjectCreateParams): ProjectResponse = TODO()
    override suspend fun rotateProjectToken(bearer: String, projectId: Int): ProjectResponseWithToken = TODO()
    override suspend fun getProjectPermissions(bearer: String, projectId: Int): ProjectPermsSequenceResponse = TODO()
    override suspend fun getErrors(bearer: String, projectId: Int?, limit: Int, offset: Int, sortby: String?): ErrorSequenceResponse = TODO()
    override suspend fun postError(projectKey: String, projectName: String, body: ErrorParams): Response<Unit> = TODO()
    override suspend fun getProjectLogFile(bearer: String, projectId: Int, logfileid: Int, limit: Int, offset: Int, sortBy: String?): StringSequenceResponse = TODO()
    override suspend fun resetProjectPerms(bearer: String, body: ProjectPermsBatchParams): ProjectPermsSequenceResponse = TODO()
    override suspend fun getLogEvent(id: Int): LogEventDetailResponse = TODO()
    override suspend fun getProjectToken(projectId: Int): ProjectResponseWithToken = TODO()

    companion object {
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

        fun page(vararg ids: Int, cursor: String? = null) =
            LogEventPageResponse(true, "ok", data = LogEventPageDTO(ids.map { event(it) }, cursor))
    }
}
