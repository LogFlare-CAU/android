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

/** Body of `POST /user/logout`; the token is optional (removes that FCM row too). */
@Serializable
data class LogoutParams(
    @SerialName("fcm_token") val fcmToken: String? = null
)

@Serializable
data class LogoutDTO(
    @SerialName("logged_out") val loggedOut: Boolean,
    @SerialName("fcm_removed") val fcmRemoved: Boolean = false
)

@Serializable
data class LogoutResponse(
    val success: Boolean,
    val message: String,
    @SerialName("error_code") val errorCode: Int? = null,
    val data: LogoutDTO? = null
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

@Serializable
data class ProjectSummaryDTO(
    @SerialName("project_id") val projectId: Int,
    @SerialName("error_count") val errorCount: Int,
    @SerialName("warning_count") val warningCount: Int,
    @SerialName("last_error_at") val lastErrorAt: String? = null,
)

@Serializable
data class LogSummaryDTO(
    @SerialName("window_hours") val windowHours: Int,
    val since: String,
    val counts: Map<String, Int> = emptyMap(),
    val projects: List<ProjectSummaryDTO> = emptyList(),
)

@Serializable
data class LogSummaryResponse(
    val success: Boolean,
    val message: String,
    @SerialName("error_code") val errorCode: Int? = null,
    val data: LogSummaryDTO? = null,
)

/** Row of GET /log/events (grouped by default; group-wide level/timestamp/occurrences). */
@Serializable
data class LogEventDTO(
    val id: Int,
    @SerialName("project_id") val projectId: Int,
    @SerialName("logfile_id") val logfileId: Int? = null,
    @SerialName("logfile_name") val logfileName: String? = null,
    val errortype: String? = null,
    val message: String,
    val level: String,
    val timestamp: String,
    val occurrences: Int,
    @SerialName("first_seen") val firstSeen: String,
    @SerialName("last_seen") val lastSeen: String,
)

@Serializable
data class LogEventPageDTO(
    val items: List<LogEventDTO> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class LogEventPageResponse(
    val success: Boolean,
    val message: String,
    @SerialName("error_code") val errorCode: Int? = null,
    val data: LogEventPageDTO? = null,
)

/**
 * GET /log/events/{id}. id, raw, occurredAt, rowLevel and logfile_* describe the requested row;
 * occurrences, firstSeen, lastSeen, level and timestamp describe the whole group.
 */
@Serializable
data class LogEventDetailDTO(
    val id: Int,
    @SerialName("project_id") val projectId: Int,
    @SerialName("logfile_id") val logfileId: Int? = null,
    @SerialName("logfile_name") val logfileName: String? = null,
    val errortype: String? = null,
    val message: String,
    val level: String,
    val timestamp: String,
    val occurrences: Int,
    @SerialName("first_seen") val firstSeen: String,
    @SerialName("last_seen") val lastSeen: String,
    val raw: String? = null,
    @SerialName("recent_ids") val recentIds: List<Int> = emptyList(),
    @SerialName("occurred_at") val occurredAt: String,
    @SerialName("row_level") val rowLevel: String,
)

@Serializable
data class LogEventDetailResponse(
    val success: Boolean,
    val message: String,
    @SerialName("error_code") val errorCode: Int? = null,
    val data: LogEventDetailDTO? = null,
)

@Serializable
data class RemovedDTO(val removed: Boolean)

@Serializable
data class RemovedResponse(
    val success: Boolean,
    val message: String,
    @SerialName("error_code") val errorCode: Int? = null,
    val data: RemovedDTO? = null,
)