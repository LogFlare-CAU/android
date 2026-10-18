package com.logflare.android.data.store.model

import com.example.logflare.core.designsystem.v2.LfLevel
import com.example.logflare.core.model.LogSummaryDTO
import com.example.logflare.core.model.ProjectDTO
import java.time.Instant

data class LogFileRef(val id: Int, val name: String)

/** A project with its last-24h counts (from `GET /log/summary`). */
data class ProjectItem(
    val id: Int,
    val name: String,
    val logFiles: List<LogFileRef>,
    /** ERROR + CRITICAL rows in the last 24 h. */
    val errorCount24h: Int,
    val warningCount24h: Int,
    /** Latest ERROR/CRITICAL row of all time, or null if there is none. */
    val lastErrorAt: Instant?,
)

data class HomeSummary(val errors24h: Int, val warnings24h: Int, val projectCount: Int)

data class ProjectWithToken(val id: Int, val name: String, val token: String)

internal fun mergeProjects(projects: List<ProjectDTO>, summary: LogSummaryDTO): List<ProjectItem> {
    val byId = summary.projects.associateBy { it.projectId }
    return projects.map { dto ->
        val counts = byId[dto.id]
        ProjectItem(
            id = dto.id,
            name = dto.name,
            logFiles = dto.logfiles.orEmpty().map { LogFileRef(it.id, it.file_name) },
            errorCount24h = counts?.errorCount ?: 0,
            warningCount24h = counts?.warningCount ?: 0,
            lastErrorAt = counts?.lastErrorAt?.let(::parseServerTime),
        )
    }
}

internal fun LogSummaryDTO.toHomeSummary(): HomeSummary {
    val byLevel = counts.entries.groupBy({ LfLevel.parse(it.key) }, { it.value })
    fun sum(vararg levels: LfLevel) = levels.sumOf { byLevel[it].orEmpty().sum() }
    return HomeSummary(
        errors24h = sum(LfLevel.Error, LfLevel.Critical),
        warnings24h = sum(LfLevel.Warning),
        projectCount = projects.size,
    )
}
