package com.example.logflare.core.network

import com.example.logflare.core.model.ErrorSequenceResponse
import com.example.logflare.core.model.ProjectCreateParams
import com.example.logflare.core.model.ProjectSequenceResponse
import com.example.logflare.core.model.StringResponse
import com.example.logflare.core.model.UserAuthParams
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LogflareApi {
    @POST("/user/auth")
    suspend fun authenticate(@Body body: UserAuthParams): StringResponse

    @GET("/project/")
    suspend fun listProjects(
        @Header("Authorization") bearer: String
    ): ProjectSequenceResponse

    @POST("/project/")
    suspend fun createProject(
        @Header("Authorization") bearer: String,
        @Body body: ProjectCreateParams
    ): StringResponse

    @DELETE("/project/{projectid}")
    suspend fun deleteProject(
        @Header("Authorization") bearer: String,
        @Path("projectid") projectId: Int
    ): StringResponse

    @GET("/log/error")
    suspend fun getErrors(
        @Header("Authorization") bearer: String,
        @Query("project_id") projectId: Int,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): ErrorSequenceResponse
}
