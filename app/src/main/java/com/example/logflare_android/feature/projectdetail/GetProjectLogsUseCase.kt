package com.example.logflare_android.feature.projectdetail

import com.example.logflare.core.network.LogflareApi
import com.example.logflare_android.data.AuthRepository
import com.example.logflare_android.enums.LogLevel
import com.example.logflare_android.enums.LogSort
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetProjectLogsUseCase @Inject constructor(
    private val api: LogflareApi,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        projectId: Int,
        projectName: String,
        logfileId: Int,
        fileName: String,
        limit: Int = 50,
        offset: Int = 0,
        sortBy: LogSort = LogSort.NEWEST
    ): List<ProjectDetailLog>? {

        val token = authRepository.getToken()
        val res = runCatching {
            api.getProjectLogFile(
                token,
                projectId = projectId,
                logfileid = logfileId,
                limit = limit,
                offset = offset,
                sortBy = sortBy.label
            )
        }.getOrElse { return null }

        val rawLogs = res.data ?: return null

        return rawLogs.mapIndexed { idx, raw ->
            // raw 형식: "timestamp | level | message"
            val parts = raw.split(" | ", limit = 3)
            val timestamp = parts.getOrNull(0) ?: ""
            val levelStr = parts.getOrNull(1) ?: "INFO"
            val message = parts.getOrNull(2) ?: ""

            ProjectDetailLog(
                id = idx,
                timestamp = timestamp,
                level = LogLevel.valueOf(levelStr.uppercase()),
                message = message,
                projectName = projectName,
                fileName = fileName
            )
        }
    }
}
