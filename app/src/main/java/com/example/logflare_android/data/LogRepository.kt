package com.example.logflare_android.data

import com.example.logflare.core.model.ErrorParams
import com.example.logflare.core.network.LogflareApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class LogRepository @Inject constructor(
    private val api: LogflareApi,
    private val projectAuth: ProjectAuthRepository
) {
    suspend fun postError(level: String, message: String, errortype: String? = null): Boolean {
        val key = projectAuth.projectKey.first() ?: return false
        val name = projectAuth.projectName.first() ?: return false
        return try {
            val body = ErrorParams(errortype = errortype, level = level, message = message)
            val res = api.postError(projectKey = key, projectName = name, body = body)
            res.isSuccessful
        } catch (_: Exception) {
            false
        }
    }
}
