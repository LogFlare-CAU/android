package com.example.logflare.core.network

import com.example.logflare.core.model.LogEventDetailResponse
import com.example.logflare.core.model.LogEventPageResponse
import com.example.logflare.core.model.LogSummaryResponse
import com.example.logflare.core.model.RemovedResponse
import com.example.logflare.core.model.FcmConfigResponse
import com.example.logflare.core.model.FcmTokenParams
import com.example.logflare.core.model.FcmTokenResponse
import com.example.logflare.core.model.ProjectCreateParams
import com.example.logflare.core.model.ProjectSequenceResponse
import com.example.logflare.core.model.StringResponse
import com.example.logflare.core.model.UserAuthParams
import com.example.logflare.core.model.ProjectPermsBatchParams
import com.example.logflare.core.model.ProjectPermsSequenceResponse
import com.example.logflare.core.model.ProjectResponse
import com.example.logflare.core.model.ProjectResponseWithToken
import com.example.logflare.core.model.UserCreateParams
import com.example.logflare.core.model.UserResponse
import com.example.logflare.core.model.UserSequenceResponse
import com.example.logflare.core.model.UserUpdateParams
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The backend API. `Authorization` is added by [com.example.logflare.core.network.auth.AuthHeaderInterceptor]
 * from the signed-in session; only [getMeAs] sends a token of its own.
 */
interface LogflareApi {
    @GET("/user/")
    suspend fun getAllUsers(): UserSequenceResponse

    @POST("/user/")
    suspend fun createUser(
        @Body body: UserCreateParams
    ): UserResponse

    @POST("/user/auth")
    suspend fun authenticate(@Body body: UserAuthParams): StringResponse

    @GET("/user/me")
    suspend fun getme(): UserResponse

    /**
     * [getme] with an explicit [bearer]: sign-in loads the profile before the session is stored,
     * so there is no session token for the interceptor to add yet.
     */
    @GET("/user/me")
    suspend fun getMeAs(@Header("Authorization") bearer: String): UserResponse

    @PATCH("/user/{userid}")
    suspend fun updateUser(
        @Path("userid") userId: Int,
        @Body body: UserUpdateParams
    ): UserResponse

    @DELETE("/user/{userid}")
    suspend fun deleteUser(
        @Path("userid") userId: Int
    ): UserResponse

    @GET("/fcm/data")
    suspend fun getFirebaseConfig(): FcmConfigResponse

    @GET("/project/")
    suspend fun listProjects(): ProjectSequenceResponse

    @POST("/project/")
    suspend fun createProject(
        @Body body: ProjectCreateParams
    ): ProjectResponseWithToken

    @DELETE("/project/{projectid}")
    suspend fun deleteProject(
        @Path("projectid") projectId: Int
    ): StringResponse

    @PATCH("/project/{projectid}")
    suspend fun changeProjectName(
        @Path("projectid") projectId: Int,
        @Body body: ProjectCreateParams
    ): ProjectResponse

    @POST("/project/{projectid}/rotate-token")
    suspend fun rotateProjectToken(
        @Path("projectid") projectId: Int
    ): ProjectResponseWithToken

    @GET("/project/{projectid}/perm")
    suspend fun getProjectPermissions(
        @Path("projectid") projectId: Int
    ): ProjectPermsSequenceResponse

    @POST("/project/perm/batch/reset")
    suspend fun resetProjectPerms(
        @Body body: ProjectPermsBatchParams
    ): ProjectPermsSequenceResponse

    @POST("/fcm/token")
    suspend fun registerFcmToken(
        @Body body: FcmTokenParams
    ): FcmTokenResponse

    @GET("/log/summary")
    suspend fun getLogSummary(
        @Query("project_id") projectId: Int? = null,
        @Query("hours") hours: Int = 24
    ): LogSummaryResponse

    @GET("/log/events")
    suspend fun getLogEvents(
        @Query("project_id") projectId: Int?,
        @Query("min_level") minLevel: String?,
        @Query("logfile_id") logfileId: Int?,
        @Query("q") query: String?,
        @Query("sort") sort: String = "newest",
        @Query("grouped") grouped: Boolean = true,
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null
    ): LogEventPageResponse

    @GET("/log/events/{id}")
    suspend fun getLogEvent(@Path("id") id: Int): LogEventDetailResponse

    @GET("/project/{projectid}/token")
    suspend fun getProjectToken(@Path("projectid") projectId: Int): ProjectResponseWithToken

    @HTTP(method = "DELETE", path = "/fcm/token", hasBody = true)
    suspend fun unregisterFcmToken(@Body body: FcmTokenParams): RemovedResponse
}
