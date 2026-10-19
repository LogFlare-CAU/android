package com.logflare.qa.server

import java.time.Clock
import java.time.Duration
import java.time.Instant

data class QaUser(val idx: Int, val username: String, val permission: Int)

data class QaLogFile(
    val id: Int,
    val project_id: Int,
    val file_path: String,
    val file_name: String,
)

data class QaProject(val id: Int, val name: String, val logfiles: List<QaLogFile>)

data class QaProjectPerm(
    val id: Int,
    val project_id: Int,
    val user_id: Int,
    val view: Boolean,
)

/** One stored log row. Its time is an age before the server clock, not a fixed date. */
data class QaLogRow(
    val id: Int,
    val projectId: Int,
    val logfileId: Int?,
    val errortype: String?,
    val message: String,
    val level: String,
    val ageMinutes: Long,
    val raw: String?,
)

/** A row group as GET /log/events returns it: grouped by (project, errortype, message). */
data class QaEvent(
    val id: Int,
    val projectId: Int,
    val logfileId: Int?,
    val logfileName: String?,
    val errortype: String?,
    val message: String,
    val level: String,
    val occurrences: Int,
    val firstSeen: Instant,
    val lastSeen: Instant,
)

data class QaEventDetail(
    val event: QaEvent,
    val occurredAt: Instant,
    val rowLevel: String,
    val raw: String?,
    val recentIds: List<Int>,
    val recent: List<QaRecent>,
)

/** One entry of GET /log/events/{id}'s `recent[]`; [message] only when it differs from the group's. */
data class QaRecent(val id: Int, val at: Instant, val level: String, val message: String?)

data class QaProjectSummary(
    val projectId: Int,
    val errorCount: Int,
    val warningCount: Int,
    val lastErrorAt: Instant?,
)

data class QaSummary(
    val windowHours: Int,
    val since: Instant,
    val counts: Map<String, Int>,
    val projects: List<QaProjectSummary>,
)

data class QaEventFilter(
    val projectId: Int? = null,
    val minLevel: String? = null,
    val logfileId: Int? = null,
    val query: String? = null,
    val sort: String = "newest",
    val grouped: Boolean = true,
)

/**
 * The fixture mirrors the rework prototype: projects api-server, worker, web-frontend and
 * batch-jobs, members jihyun (Admin), devops-bot, minsu and yuna, and eleven log groups across
 * every level. Log times are ages before [clock], so relative labels ("1 min ago") and the 24h
 * summary window come out the same on every run.
 */
class FixtureState(private val clock: Clock = Clock.systemUTC()) {
    val users = linkedMapOf<Int, QaUser>()
    val projects = linkedMapOf<Int, QaProject>()
    val projectTokens = linkedMapOf<Int, String>()
    val perms = linkedMapOf<Int, QaProjectPerm>()
    val logRows = mutableListOf<QaLogRow>()
    private val fcmTokens = linkedMapOf<String, Int>()

    private var nextUserId = 6
    private var nextProjectId = 105
    private var nextLogFileId = 5001
    private var nextPermId = 1
    private var nextFcmTokenIdx = 1
    private var tokenRotations = 0

    init {
        reset()
    }

    fun now(): Instant = clock.instant()

    @Synchronized
    fun reset() {
        users.clear()
        projects.clear()
        projectTokens.clear()
        perms.clear()
        logRows.clear()
        fcmTokens.clear()

        listOf(
            QaUser(1, "qa-admin", 100),
            QaUser(2, "jihyun", 80),
            QaUser(3, "devops-bot", 0),
            QaUser(4, "minsu", 0),
            QaUser(5, "yuna", 0),
        ).forEach { users[it.idx] = it }

        project(101, "api-server", 1001 to "app.log", 1002 to "payment.log", 1003 to "db.log")
        project(102, "worker", 2001 to "worker.log")
        project(103, "web-frontend", 3001 to "frontend.log")
        project(104, "batch-jobs", 4001 to "jobs.log")

        nextPermId = 1
        grant(101, 2, 3, 4, 5)
        grant(102, 2, 3)
        grant(103, 2, 5)
        grant(104, 3)

        seedLogs()

        nextUserId = 6
        nextProjectId = 105
        nextLogFileId = 5001
        nextFcmTokenIdx = 1
        tokenRotations = 0
    }

    private fun project(id: Int, name: String, vararg files: Pair<Int, String>) {
        projects[id] = QaProject(
            id,
            name,
            files.map { (fileId, fileName) -> QaLogFile(fileId, id, "/var/log/$name/$fileName", fileName) },
        )
        projectTokens[id] = "qa-project-token-$id"
    }

    private fun grant(projectId: Int, vararg userIds: Int) {
        userIds.forEach { userId ->
            val id = nextPermId++
            perms[id] = QaProjectPerm(id, projectId, userId, true)
        }
    }

    /** Groups in the prototype's order; each age is one occurrence (newest first). */
    private fun seedLogs() {
        data class Seed(
            val projectId: Int,
            val logfileId: Int?,
            val errortype: String?,
            val level: String,
            val message: String,
            val ages: List<Long>,
            val raw: String? = null,
        )
        val seeds = listOf(
            Seed(101, 1002, "NullPointerException", "ERROR", "NullPointerException in PaymentService", listOf(1, 47, 128), NPE_RAW),
            Seed(102, 2001, "PoolExhausted", "ERROR", "Connection pool exhausted (max=20)", listOf(16)),
            Seed(101, 1003, "SlowQuery", "WARNING", "Slow query: SELECT * FROM logs (2.4s)", listOf(39, 95)),
            Seed(103, 3001, null, "INFO", "Deployed build 1.4.2", listOf(52)),
            Seed(102, 2001, "DiskFull", "CRITICAL", "Disk usage above 95% on /var/lib", listOf(98)),
            Seed(101, 1001, null, "DEBUG", "Cache miss ratio 0.42 for /projects", listOf(265, 280, 300, 320, 340, 360)),
            Seed(104, 4001, null, "INFO", "Nightly export finished in 4m12s", listOf(760)),
            Seed(101, 1001, "TimeoutError", "ERROR", "TimeoutError: upstream auth-service", listOf(949)),
            Seed(102, 2001, "ParseError", "ERROR", "Failed to parse webhook payload", listOf(1232, 1300)),
            Seed(103, 3001, "TypeError", "ERROR", "Uncaught TypeError in Checkout.tsx", listOf(1380, 1390, 1400, 1410)),
            Seed(104, 4001, "Retry", "WARNING", "Retrying job #8812 (attempt 2/5)", listOf(1810)),
        )
        // Ids grow with time like database rows: the oldest row is 1, the newest the highest.
        val rows = seeds.flatMap { seed -> seed.ages.map { age -> seed to age } }
            .sortedByDescending { (_, age) -> age }
        rows.forEachIndexed { index, (seed, age) ->
            logRows += QaLogRow(
                id = FIRST_LOG_ID + index,
                projectId = seed.projectId,
                logfileId = seed.logfileId,
                errortype = seed.errortype,
                message = seed.message,
                level = seed.level,
                ageMinutes = age,
                raw = seed.raw,
            )
        }
    }

    private fun QaLogRow.at(now: Instant): Instant = now.minus(Duration.ofMinutes(ageMinutes))

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
    fun getProject(id: Int): QaProject? = projects[id]

    @Synchronized
    fun createProject(name: String): Pair<QaProject, String> {
        val id = nextProjectId++
        val logId = nextLogFileId++
        val project = QaProject(
            id = id,
            name = name,
            logfiles = listOf(QaLogFile(logId, id, "/var/log/$name/app.log", "app.log")),
        )
        val token = "qa-project-token-$id"
        projects[id] = project
        projectTokens[id] = token
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
            logRows.removeIf { it.projectId == id }
        }
        return removed
    }

    @Synchronized
    fun getProjectToken(id: Int): String? = projectTokens[id]

    @Synchronized
    fun rotateProjectToken(id: Int): String? {
        if (id !in projects) return null
        val token = "qa-project-token-$id-r${++tokenRotations}"
        projectTokens[id] = token
        return token
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

    /** Level counts in the last [hours] (1..168) and per-project error/warning statistics. */
    @Synchronized
    fun summary(projectId: Int?, hours: Int): QaSummary {
        val now = now()
        val window = hours.coerceIn(1, 168)
        val since = now.minus(Duration.ofHours(window.toLong()))
        val projectIds = projectId?.let(::listOf) ?: projects.keys.toList()
        val rows = logRows.filter { it.projectId in projectIds }
        val inWindow = rows.filter { !it.at(now).isBefore(since) }
        val counts = LEVELS.associateWith { level -> inWindow.count { it.level == level } }
        val perProject = projectIds.map { id ->
            QaProjectSummary(
                projectId = id,
                errorCount = inWindow.count { it.projectId == id && it.level in ERROR_LEVELS },
                warningCount = inWindow.count { it.projectId == id && it.level == "WARNING" },
                lastErrorAt = rows.filter { it.projectId == id && it.level in ERROR_LEVELS }
                    .minOfOrNull { it.ageMinutes }
                    ?.let { now.minus(Duration.ofMinutes(it)) },
            )
        }
        return QaSummary(window, since, counts, perProject)
    }

    /** Every event matching [filter], ordered; the server pages through the result. */
    @Synchronized
    fun listEvents(filter: QaEventFilter): List<QaEvent> {
        val now = now()
        val floor = filter.minLevel?.let { LEVELS.indexOf(it.uppercase()) } ?: -1
        val needle = filter.query?.lowercase()
        val rows = logRows.filter { row ->
            (filter.projectId == null || row.projectId == filter.projectId) &&
                LEVELS.indexOf(row.level) >= floor &&
                (filter.logfileId == null || row.logfileId == filter.logfileId) &&
                (
                    needle.isNullOrEmpty() ||
                        row.message.lowercase().contains(needle) ||
                        row.errortype.orEmpty().lowercase().contains(needle)
                    )
        }
        val events = if (filter.grouped) {
            rows.groupBy { Triple(it.projectId, it.errortype.orEmpty(), it.message) }.values.map { toEvent(it, now) }
        } else {
            rows.map { toEvent(listOf(it), now) }
        }
        val newest = compareByDescending<QaEvent> { it.lastSeen }.thenByDescending { it.id }
        return when (filter.sort) {
            "oldest" -> events.sortedWith(compareBy<QaEvent> { it.lastSeen }.thenBy { it.id })
            "highest" -> events.sortedWith(compareByDescending<QaEvent> { LEVELS.indexOf(it.level) }.then(newest))
            "lowest" -> events.sortedWith(compareBy<QaEvent> { LEVELS.indexOf(it.level) }.then(newest))
            else -> events.sortedWith(newest)
        }
    }

    /** The row [id] plus statistics of its whole group, like GET /log/events/{id}. */
    @Synchronized
    fun eventDetail(id: Int): QaEventDetail? {
        val row = logRows.firstOrNull { it.id == id } ?: return null
        val now = now()
        val group = logRows.filter {
            it.projectId == row.projectId && it.errortype.orEmpty() == row.errortype.orEmpty() && it.message == row.message
        }
        val others = group.filter { it.id != row.id }.sortedByDescending { it.id }.take(10)
        return QaEventDetail(
            event = toEvent(group, now).copy(id = row.id, logfileId = row.logfileId, logfileName = fileName(row.logfileId)),
            occurredAt = row.at(now),
            rowLevel = row.level,
            raw = row.raw,
            recentIds = others.map { it.id },
            recent = others.map { QaRecent(it.id, it.at(now), it.level, it.message.takeIf { m -> m != row.message }) },
        )
    }

    private fun toEvent(group: List<QaLogRow>, now: Instant): QaEvent {
        val latest = group.maxBy { it.id }
        return QaEvent(
            id = latest.id,
            projectId = latest.projectId,
            logfileId = latest.logfileId,
            logfileName = fileName(latest.logfileId),
            errortype = latest.errortype,
            message = latest.message,
            level = group.maxBy { LEVELS.indexOf(it.level) }.level,
            occurrences = group.size,
            firstSeen = group.minOf { it.at(now) },
            lastSeen = group.maxOf { it.at(now) },
        )
    }

    private fun fileName(logfileId: Int?): String? =
        logfileId?.let { id -> projects.values.flatMap { it.logfiles }.firstOrNull { it.id == id }?.file_name }

    @Synchronized
    fun registerFcmToken(token: String): Int =
        fcmTokens.getOrPut(token) { nextFcmTokenIdx++ }

    @Synchronized
    fun unregisterFcmToken(token: String): Boolean = fcmTokens.remove(token) != null

    companion object {
        /** Backend level order, lowest first. */
        val LEVELS = listOf("DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL")
        private val ERROR_LEVELS = setOf("ERROR", "CRITICAL")
        const val FIRST_LOG_ID = 1

        private const val NPE_RAW =
            "java.lang.NullPointerException: Cannot invoke \"Card.getToken()\" because \"card\" is null\n" +
                "  at com.shop.payment.PaymentService.charge(PaymentService.kt:142)\n" +
                "  at com.shop.order.OrderController.checkout(OrderController.kt:88)\n" +
                "  at com.shop.order.OrderController\$routes\$1.invokeSuspend(OrderController.kt:41)\n" +
                "  at io.ktor.server.routing.Route.handle(Route.kt:120)\n" +
                "  at io.ktor.server.engine.BaseApplicationEngine.execute(BaseApplicationEngine.kt:66)\n" +
                "  at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:108)\n" +
                "  at java.base/java.lang.Thread.run(Thread.java:1583)\n" +
                "\n" +
                "request_id=7f3a91c2 user_id=10482 amount=39000 currency=KRW"
    }
}
