package com.example.logflare_android.data

import com.example.logflare.core.model.ErrorSequenceResponse
import com.example.logflare.core.model.ErrorlogDTO
import com.example.logflare.core.network.LogflareApi
import com.example.logflare_android.ui.common.LogCardInfo
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

@Singleton
class LogsRepository @Inject constructor(
    private val api: LogflareApi,
    private val auth: AuthRepository
) {
    suspend fun getErrors(projectId: Int, limit: Int = 50, offset: Int = 0): Result<List<ErrorlogDTO>> = runCatching {
        val token = auth.token.first() ?: throw IllegalStateException("No token")
        val res: ErrorSequenceResponse = api.getErrors(token, projectId, limit, offset)
        if (!res.success) throw IllegalStateException("getErrors failed")
        res.data ?: emptyList()
    }.recoverCatching { e ->
        if (e is HttpException && e.code() == 401) {
            auth.clearToken()
            throw IllegalStateException("Unauthorized")
        } else throw e
    }

    private val selectedLog = MutableStateFlow<LogCardInfo?>(null)

    fun selectLog(log: LogCardInfo) {
        selectedLog.value = log
    }

    fun clearSelectedLog() {
        selectedLog.value = null
    }

    fun getSelectedLog(): LogCardInfo? {
        return selectedLog.value
    }

}
