package com.example.logflare.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserAuthParams(
    val username: String,
    val password: String,
    val keep_logged_in: Boolean = false
)

@Serializable
data class UserCreateParams(
    val username: String,
    val permission: Int,
    val password: String,
)


@Serializable
data class UserUpdateParams(
    val username: String? = null,
    val permission: Int? = null,
    val password: String? = null,
)

@Serializable
data class UserResponse(
    val success: Boolean,
    val message: String,
    val error_code: Int? = null,
    val data: UserDTO? = null
)

@Serializable
data class UserSequenceResponse(
    val success: Boolean,
    val message: String,
    val error_code: Int? = null,
    val data: List<UserDTO>? = null
)

@Serializable
data class UserDTO(
    val idx: Int,
    val username: String,
    val permission: Int,
)

@Serializable
data class StringResponse(
    val success: Boolean,
    val message: String,
    val error_code: Int? = null,
    val data: String? = null
)

@Serializable
data class StringSequenceResponse(
    val success: Boolean,
    val message: String,
    val error_code: Int? = null,
    val data: List<String>? = null
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
data class ProjectDTOWithToken(
    val id: Int,
    val name: String,
    val alias: String? = null,
    val description: String? = null,
    val token: String,
    val logfiles: List<LogFileDTO>? = null
)

@Serializable
data class ProjectData(
    val dto: ProjectDTO,
    val excludeKeywords: Set<String> = emptySet(),
    val alertLevel: String,
)

@Serializable
data class ProjectSequenceResponse(
    val success: Boolean,
    val message: String,
    val error_code: Int? = null,
    val data: List<ProjectDTO>? = null
)

@Serializable
data class ProjectResponseWithToken(
    val success: Boolean,
    val message: String,
    val error_code: Int? = null,
    val data: ProjectDTOWithToken? = null
)

@Serializable
data class ProjectResponse(
    val success: Boolean,
    val message: String,
    val error_code: Int? = null,
    val data: ProjectDTO? = null
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
data class ErrorParams(
    val errortype: String? = null,
    val level: String,
    val message: String
)

@Serializable
data class FcmConfigResponse(
    val success: Boolean,
    val message: String,
    @SerialName("error_code") val errorCode: Int? = null,
    val data: FcmConfig? = null
)

/** Minimal Firebase client fields from GET /fcm/data (backend FCMClientConfig). */
@Serializable
data class FcmConfig(
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("messaging_sender_id") val messagingSenderId: String? = null,
    @SerialName("mobilesdk_app_id") val mobilesdkAppId: String? = null,
    @SerialName("package_name") val packageName: String? = null,
    @SerialName("api_key") val apiKey: String? = null,
)

@Serializable
data class FcmTokenParams(
    @SerialName("fcm_token") val fcmToken: String
)

@Serializable
data class FcmTokenResponse(
    val success: Boolean,
    val message: String,
    @SerialName("error_code") val errorCode: Int? = null,
    val data: FcmTokenDTO? = null
)

@Serializable
data class FcmTokenDTO(
    val idx: Int,
    @SerialName("user_idx") val userIdx: Int,
    @SerialName("last_delivery") val lastDelivery: String? = null
)

@Serializable
data class ProjectPermsBatchParams(
    val projectid: Int,
    val usernames: Set<String>
)

@Serializable
data class ProjectPermsSequenceResponse(
    val success: Boolean,
    val message: String,
    val error_code: Int? = null,
    val data: List<ProjectPermsDTO>? = null
)

@Serializable
data class ProjectPermsDTO(
    val id: Int,
    val project_id: Int,
    val user_id: Int,
    val view: Boolean,
    val project: ProjectDTO? = null,
)