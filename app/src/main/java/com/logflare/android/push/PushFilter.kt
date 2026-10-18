package com.logflare.android.push

import com.example.logflare.core.designsystem.v2.LfLevel
import com.logflare.android.data.session.SessionState
import java.time.LocalTime

/** The data payload of a LogFlare FCM message. */
data class PushMessage(
    val errorId: Int,
    val projectId: Int?,
    val type: String,
    val level: LfLevel,
    val message: String,
    val timestamp: String,
    val isTest: Boolean,
) {
    companion object
}

fun PushMessage.Companion.from(data: Map<String, String>): PushMessage = PushMessage(
    errorId = data["errorid"]?.toIntOrNull() ?: 0,
    projectId = data["projectid"]?.toIntOrNull(),
    type = data["type"] ?: "Unknown",
    level = LfLevel.parse(data["level"] ?: "INFO"),
    message = data["message"].orEmpty(),
    timestamp = data["timestamp"].orEmpty(),
    isTest = data["test"]?.toBoolean() ?: false,
)

enum class PushDecision {
    /** Show a notification and record it in the history. */
    Notify,

    /** Quiet hours: no notification, but keep it in the history. */
    RecordOnly,

    /** Neither shown nor recorded. */
    Drop,
}

private val QUIET_START: LocalTime = LocalTime.of(23, 0)
private val QUIET_END: LocalTime = LocalTime.of(7, 0)

/** Quiet hours are 23:00 (inclusive) to 07:00 (exclusive) local time, wrapping midnight. */
internal fun isQuietTime(now: LocalTime): Boolean = !now.isBefore(QUIET_START) || now.isBefore(QUIET_END)

/**
 * Push precedence (spec §4/§5): signed out → never; `test` → always (when signed in);
 * global Off → never; threshold = project level ?: global level, below it → no; a muted keyword
 * (case-insensitive substring of the message) → no; quiet hours and below Critical → recorded
 * without a notification.
 */
fun decidePush(msg: PushMessage, session: SessionState, settings: AlertSettings, now: LocalTime): PushDecision {
    if (session !is SessionState.SignedIn) return PushDecision.Drop
    if (msg.isTest) return PushDecision.Notify
    val global = settings.global.threshold() ?: return PushDecision.Drop
    val project = msg.projectId?.let { settings.projects[it] }
    val threshold = project?.level ?: global
    if (msg.level < threshold) return PushDecision.Drop
    val muted = project?.mutedKeywords.orEmpty()
    if (muted.any { it.isNotBlank() && msg.message.contains(it, ignoreCase = true) }) return PushDecision.Drop
    if (settings.quietHours && isQuietTime(now) && msg.level < LfLevel.Critical) return PushDecision.RecordOnly
    return PushDecision.Notify
}

/**
 * [decidePush] for an incoming push whose settings may be unavailable: [settings] null means the
 * signed-in session's own settings aren't loaded (cold-start timeout). Never notify on settings
 * that aren't the session's: record only. Test pushes don't depend on settings and still notify.
 */
fun decideIncoming(msg: PushMessage, session: SessionState, settings: AlertSettings?, now: LocalTime): PushDecision =
    when {
        settings != null -> decidePush(msg, session, settings, now)
        session !is SessionState.SignedIn -> PushDecision.Drop
        msg.isTest -> PushDecision.Notify
        else -> PushDecision.RecordOnly
    }

fun PushMessage.toHistoryEntry(receivedAt: Long): HistoryEntry = HistoryEntry(
    errorId = errorId,
    projectId = projectId,
    type = type,
    level = level,
    message = message,
    receivedAt = receivedAt,
    read = false,
)
