package com.logflare.qa.server

import kotlinx.serialization.Serializable

@Serializable
data class QaUser(val idx: Int, val username: String, val permission: Int)

@Serializable
data class QaLogFile(
    val id: Int,
    val project_id: Int,
    val file_path: String,
    val file_name: String,
)

@Serializable
data class QaProject(val id: Int, val name: String, val logfiles: List<QaLogFile>)

@Serializable
data class QaErrorLog(
    val id: Int,
    val project_id: Int,
    val errortype: String?,
    val message: String,
    val level: String,
    val timestamp: String,
)

@Serializable
data class QaProjectPerm(
    val id: Int,
    val project_id: Int,
    val user_id: Int,
    val view: Boolean,
)

class FixtureState {
    val users = linkedMapOf<Int, QaUser>()
    val projects = linkedMapOf<Int, QaProject>()
    val projectTokens = linkedMapOf<Int, String>()
    val perms = linkedMapOf<Int, QaProjectPerm>()
    val errorLogs = mutableListOf<QaErrorLog>()
    val logLines = linkedMapOf<Pair<Int, Int>, List<String>>()

    private var nextUserId = 3
    private var nextProjectId = 303
    private var nextLogFileId = 3001
    private var nextPermId = 1
    private var nextErrorId = 5003
    private var nextFcmTokenIdx = 1

    init {
        reset()
    }

    @Synchronized
    fun reset() {
        users.clear()
        projects.clear()
        projectTokens.clear()
        perms.clear()
        errorLogs.clear()
        logLines.clear()

        users[1] = QaUser(1, "qa-admin", 0)
        users[2] = QaUser(2, "qa-member", 2)
        projects[101] = QaProject(
            101,
            "Payments",
            listOf(QaLogFile(1001, 101, "/var/log/payments.log", "payments.log")),
        )
        projects[202] = QaProject(
            202,
            "Checkout",
            listOf(QaLogFile(2001, 202, "/var/log/checkout.log", "checkout.log")),
        )
        projectTokens[101] = "qa-project-token-101"
        projectTokens[202] = "qa-project-token-202"
        perms[1] = QaProjectPerm(1, 101, 1, true)
        perms[2] = QaProjectPerm(2, 101, 2, true)
        perms[3] = QaProjectPerm(3, 202, 1, true)
        errorLogs += QaErrorLog(
            id = 5001,
            project_id = 101,
            errortype = "Timeout",
            message = "payment gateway timeout",
            level = "ERROR",
            timestamp = "2026-01-01T00:00:00Z",
        )
        errorLogs += QaErrorLog(
            id = 5002,
            project_id = 202,
            errortype = "NullPointer",
            message = "checkout null cart",
            level = "ERROR",
            timestamp = "2026-01-01T00:01:00Z",
        )
        logLines[101 to 1001] = listOf(
            "2026-01-01T00:00:00Z INFO payments ready",
            "2026-01-01T00:00:01Z WARN retrying charge",
        )
        logLines[202 to 2001] = listOf(
            "2026-01-01T00:00:00Z INFO checkout ready",
        )

        nextUserId = 3
        nextProjectId = 303
        nextLogFileId = 3001
        nextPermId = 4
        nextErrorId = 5003
        nextFcmTokenIdx = 1
    }

    @Synchronized
    fun listUsers(): List<QaUser> = users.values.toList()

    @Synchronized
    fun createUser(username: String, permission: Int): QaUser {
        val id = nextUserId++
        val user = QaUser(id, username, permission)
        users[id] = user
        return user
    }

    @Synchronized
    fun getUser(id: Int): QaUser? = users[id]

    @Synchronized
    fun getUserByName(username: String): QaUser? =
        users.values.firstOrNull { it.username == username }

    @Synchronized
    fun updateUser(id: Int, username: String?, permission: Int?): QaUser? {
        val existing = users[id] ?: return null
        val updated = existing.copy(
            username = username ?: existing.username,
            permission = permission ?: existing.permission,
        )
        users[id] = updated
        return updated
    }

    @Synchronized
    fun deleteUser(id: Int): QaUser? = users.remove(id)

    @Synchronized
    fun listProjects(): List<QaProject> = projects.values.toList()

    @Synchronized
    fun createProject(name: String): Pair<QaProject, String> {
        val id = nextProjectId++
        val logId = nextLogFileId++
        val project = QaProject(
            id = id,
            name = name,
            logfiles = listOf(
                QaLogFile(logId, id, "/var/log/$name.log", "$name.log"),
            ),
        )
        val token = "qa-project-token-$id"
        projects[id] = project
        projectTokens[id] = token
        logLines[id to logId] = listOf("2026-01-01T00:00:00Z INFO $name ready")
        return project to token
    }

    @Synchronized
    fun renameProject(id: Int, name: String): QaProject? {
        val existing = projects[id] ?: return null
        val updated = existing.copy(name = name)
        projects[id] = updated
        return updated
    }

    @Synchronized
    fun deleteProject(id: Int): Boolean {
        val removed = projects.remove(id) != null
        if (removed) {
            projectTokens.remove(id)
            perms.entries.removeIf { it.value.project_id == id }
            logLines.keys.removeIf { it.first == id }
        }
        return removed
    }

    @Synchronized
    fun getPerms(projectId: Int): List<QaProjectPerm> =
        perms.values.filter { it.project_id == projectId }

    @Synchronized
    fun resetPerms(projectId: Int, usernames: Set<String>): List<QaProjectPerm> {
        perms.entries.removeIf { it.value.project_id == projectId }
        val created = mutableListOf<QaProjectPerm>()
        for (username in usernames.sorted()) {
            val user = getUserByName(username) ?: continue
            val id = nextPermId++
            val perm = QaProjectPerm(id, projectId, user.idx, true)
            perms[id] = perm
            created += perm
        }
        return created
    }

    @Synchronized
    fun listErrors(projectId: Int?): List<QaErrorLog> =
        if (projectId == null) errorLogs.toList()
        else errorLogs.filter { it.project_id == projectId }

    @Synchronized
    fun addError(projectId: Int, errortype: String?, level: String, message: String): QaErrorLog {
        val entry = QaErrorLog(
            id = nextErrorId++,
            project_id = projectId,
            errortype = errortype,
            message = message,
            level = level,
            timestamp = "2026-01-01T00:00:00Z",
        )
        errorLogs += entry
        return entry
    }

    @Synchronized
    fun getLogLines(projectId: Int, logFileId: Int): List<String> =
        logLines[projectId to logFileId].orEmpty()

    @Synchronized
    fun nextFcmTokenIdx(): Int = nextFcmTokenIdx++
}
