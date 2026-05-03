package com.logflare.android.data

import com.example.logflare.core.model.ErrorlogDTO
import com.example.logflare.core.network.LogflareApi
import com.logflare.android.enums.LogSort
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

@Singleton
class LogsRepository @Inject constructor(
    private val api: LogflareApi,
    private val auth: AuthRepository
) {
    suspend fun getErrors(
        projectId: Int? = null,
        limit: Int = 50,
        offset: Int = 0,
        sortBy: LogSort = LogSort.NEWEST,
    ): Result<List<ErrorlogDTO>> = runCatching {
        val token = auth.token.first() ?: throw IllegalStateException("No token")
        val res = api.getErrors(
            bearer = token,
            projectId = projectId,
            limit = limit,
            offset = offset,
            sortby = sortBy.label,
        )
        if (!res.success) {
            val detail = res.message.ifBlank { "getErrors failed" }
            throw IllegalStateException(detail)
        }
        res.data ?: emptyList()
    }.recoverCatching { e ->
        if (e is HttpException && e.code() == 401) {
            throw IllegalStateException("Unauthorized")
        } else throw e
    }
}
