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

@Serializable
data class FcmConfig(
    @SerialName("project_info") val projectInfo: FcmProjectInfo,
    val client: List<FcmClient>,
    @SerialName("configuration_version") val configurationVersion: String
)

@Serializable
data class FcmProjectInfo(
    @SerialName("project_number") val projectNumber: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("storage_bucket") val storageBucket: String? = null
)

@Serializable
data class FcmClient(
    @SerialName("client_info") val clientInfo: FcmClientInfo,
    @SerialName("api_key") val apiKey: List<FcmApiKey> = emptyList()
)

@Serializable
data class FcmClientInfo(
    @SerialName("mobilesdk_app_id") val mobileSdkAppId: String,
    @SerialName("android_client_info") val androidClientInfo: FcmAndroidClientInfo
)

@Serializable
data class FcmAndroidClientInfo(
    @SerialName("package_name") val packageName: String
)

@Serializable
data class FcmApiKey(
    @SerialName("current_key") val currentKey: String
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
    @SerialName("fcm_token") val fcmToken: String,
    @SerialName("user_idx") val userIdx: Int,
    @SerialName("last_delivery") val lastDelivery: String? = null
)
