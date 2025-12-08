package com.example.logflare.core.network

import com.example.logflare.core.model.ErrorSequenceResponse
import com.example.logflare.core.model.FcmConfigResponse
import com.example.logflare.core.model.FcmTokenParams
import com.example.logflare.core.model.FcmTokenResponse
import com.example.logflare.core.model.ProjectCreateParams
import com.example.logflare.core.model.ProjectSequenceResponse
import com.example.logflare.core.model.StringResponse
import com.example.logflare.core.model.UserAuthParams
import com.example.logflare.core.model.ErrorParams
import com.example.logflare.core.model.ProjectPermsBatchParams
import com.example.logflare.core.model.ProjectPermsSequenceResponse
import com.example.logflare.core.model.ProjectResponse
import com.example.logflare.core.model.ProjectResponseWithToken
import com.example.logflare.core.model.StringSequenceResponse
import com.example.logflare.core.model.UserResponse
import com.example.logflare.core.model.UserSequenceResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LogflareApi {
    @GET("/user/")
    suspend fun getAllUsers(@Header("Authorization") token: String): UserSequenceResponse

    @POST("/user/auth")
    suspend fun authenticate(@Body body: UserAuthParams): StringResponse

    @GET("/user/me")
    suspend fun getme(@Header("Authorization") token: String): UserResponse

    @GET("/fcm/data")
    suspend fun getFirebaseConfig(
        @Header("Authorization") bearer: String
    ): FcmConfigResponse

    @GET("/project/")
    suspend fun listProjects(
        @Header("Authorization") bearer: String
    ): ProjectSequenceResponse

    @POST("/project/")
    suspend fun createProject(
        @Header("Authorization") bearer: String,
        @Body body: ProjectCreateParams
    ): ProjectResponseWithToken

    @DELETE("/project/{projectid}")
    suspend fun deleteProject(
        @Header("Authorization") bearer: String,
        @Path("projectid") projectId: Int
    ): StringResponse

    @PATCH("/project/{projectid}")
    suspend fun changeProjectName(
        @Header("Authorization") bearer: String,
        @Path("projectid") projectId: Int,
        @Body body: ProjectCreateParams
    ): ProjectResponse

    @GET("/project/{projectid}/perm")
    suspend fun getProjectPermissions(
        @Header("Authorization") bearer: String,
        @Path("projectid") projectId: Int
    ): ProjectPermsSequenceResponse

    @GET("/log/error")
    suspend fun getErrors(
        @Header("Authorization") bearer: String,
        @Query("project_id") projectId: Int? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("sortby") sortby: String? = null
    ): ErrorSequenceResponse

    @POST("/log/error")
    suspend fun postError(
        @Header("ProjectKey") projectKey: String,
        @Header("Project") projectName: String,
        @Body body: ErrorParams
    ): Response<Unit>

    @GET("/log/{projectid}/{logfileid}")
    suspend fun getProjectLogFile(
        @Header("Authorization") bearer: String,
        @Path("projectid") projectId: Int,
        @Path("logfileid") logfileid: Int,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("sortby") sortBy: String? = null
    ): StringSequenceResponse

    @POST("/project/perm/batch/reset")
    suspend fun resetProjectPerms(
        @Header("Authorization") bearer: String,
        @Body body: ProjectPermsBatchParams
    ): ProjectPermsSequenceResponse

    @GET("/fcm/data")
    suspend fun getFcmConfig(
        @Header("Authorization") bearer: String
    ): FcmConfigResponse

    @POST("/fcm/token")
    suspend fun registerFcmToken(
        @Header("Authorization") bearer: String,
        @Body body: FcmTokenParams
    ): FcmTokenResponse

}
