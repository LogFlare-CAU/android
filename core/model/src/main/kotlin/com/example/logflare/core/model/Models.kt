package com.example.logflare.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserAuthParams(
    val username: String,
    val password: String,
    val keep_logged_in: Boolean = false
)

@Serializable
data class StringResponse(
    val success: Boolean,
    val message: String,
    val error_code: Int? = null,
    val data: String? = null
)

@Serializable
data class ProjectCreateParams(
    val name: String
)

@Serializable
data class LogFileDTO(
    val id: Int,
    val project_id: Int,
    val file_path: String,
    val file_name: String
)

@Serializable
data class ProjectDTO(
    val id: Int,
    val name: String,
    val alias: String? = null,
    val description: String? = null,
    val logfiles: List<LogFileDTO>? = null
)

@Serializable
data class ProjectSequenceResponse(
    val success: Boolean,
    val message: String,
    val error_code: Int? = null,
    val data: List<ProjectDTO>? = null
)

@Serializable
data class ErrorlogDTO(
    val id: Int,
    val project_id: Int,
    val errortype: String? = null,
    val message: String,
    val level: String,
    val timestamp: String
)

@Serializable
data class ErrorSequenceResponse(
    val success: Boolean,
    val message: String,
    val error_code: Int? = null,
    val data: List<ErrorlogDTO>? = null
)

@Serializable
data class DeviceRegisterParams(
    val token: String,
    val platform: String = "android",
    val metadata: Map<String, String>? = null
)
