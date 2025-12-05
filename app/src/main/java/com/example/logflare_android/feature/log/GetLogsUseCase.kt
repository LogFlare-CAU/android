package com.example.logflare_android.feature.log

import com.example.logflare.core.model.ErrorlogDTO
import com.example.logflare.core.network.LogflareApi
import com.example.logflare_android.data.AuthRepository
import com.example.logflare_android.enums.LogSort
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetLogsUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val api: LogflareApi
) {
    suspend operator fun invoke(
        limit: Int,
        offset: Int = 0,
        projectId: Int? = null,
        sortBy: LogSort = LogSort.NEWEST,
    ): Result<List<ErrorlogDTO>> {
        return try {
            val token = authRepository.getToken()
            val response = api.getErrors(
                token,
                projectId = projectId,
                limit = limit,
                offset = offset,
                sortby = sortBy.label
            )
            Result.success(response.data ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


}