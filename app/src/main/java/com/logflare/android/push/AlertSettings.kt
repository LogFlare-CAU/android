package com.logflare.android.push

import com.example.logflare.core.designsystem.v2.LfLevel
import kotlinx.serialization.Serializable

/** The My > Push alerts default. [Off] silences every project, whatever its own level. */
@Serializable
enum class GlobalAlert {
    Off, CriticalOnly, ErrorAndAbove, WarningAndAbove, Everything;

    /** Lowest level that alerts, or null when alerts are off. */
    fun threshold(): LfLevel? = when (this) {
        Off -> null
        CriticalOnly -> LfLevel.Critical
        ErrorAndAbove -> LfLevel.Error
        WarningAndAbove -> LfLevel.Warning
        Everything -> LfLevel.Debug
    }

    companion object {
        /** Maps the old DataStore `fcm`/`alert_level` label ("Warning", …); null/unknown → default. */
        fun fromLegacyLabel(label: String?): GlobalAlert = when (label?.trim()?.lowercase()) {
            "debug", "info" -> Everything
            "warning" -> WarningAndAbove
            "error" -> ErrorAndAbove
            "critical" -> CriticalOnly
            else -> AlertSettings().global
        }
    }
}

/** A project's own alert settings. [level] null means "use the global threshold". */
@Serializable
data class ProjectAlert(
    val level: LfLevel? = null,
    val mutedKeywords: Set<String> = emptySet(),
)

/** This device's push alert settings for one session. Only affects push alerts. */
@Serializable
data class AlertSettings(
    val global: GlobalAlert = GlobalAlert.ErrorAndAbove,
    val quietHours: Boolean = false,
    val projects: Map<Int, ProjectAlert> = emptyMap(),
)
