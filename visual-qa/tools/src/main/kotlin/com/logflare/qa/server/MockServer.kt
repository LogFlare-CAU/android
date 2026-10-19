package com.logflare.qa.server

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean

class MockServer(
    private val host: String = "127.0.0.1",
    private val port: Int = 8000,
    clock: Clock = Clock.systemUTC(),
) {
    private val state = FixtureState(clock)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var executor: ExecutorService? = null
    private var server: HttpServer? = null
    private val started = AtomicBoolean(false)

    val boundPort: Int
        get() = server?.address?.port ?: error("Server not started")

    @Synchronized
    fun start() {
        check(started.compareAndSet(false, true)) { "Server already started" }
        val newExecutor = Executors.newSingleThreadExecutor(
            ThreadFactory { runnable ->
                Thread(runnable, "qa-mock-server").apply { isDaemon = false }
            },
        )
        try {
            val httpServer = HttpServer.create(InetSocketAddress(host, port), 0)
            httpServer.executor = newExecutor
            httpServer.createContext("/") { exchange ->
                try {
                    handle(exchange)
                } catch (t: Throwable) {
                    writeJson(
                        exchange,
                        500,
                        envelope(success = false, message = t.message ?: "internal error"),
                    )
                }
            }
            httpServer.start()
            executor = newExecutor
            server = httpServer
        } catch (t: Throwable) {
            newExecutor.shutdownNow()
            started.set(false)
            throw t
        }
    }

    @Synchronized
    fun stop() {
        server?.stop(0)
        server = null
        executor?.shutdownNow()
        executor = null
        started.set(false)
    }

    private fun handle(exchange: HttpExchange) {
        val method = exchange.requestMethod.uppercase()
        val rawPath = exchange.requestURI.path ?: "/"
        val path = normalizePath(rawPath)
        val query = parseQuery(exchange.requestURI.rawQuery)

        when {
            path == "/__qa/health" -> {
                if (method == "GET") {
                    writeRawJson(exchange, 200, """{"status":"ok"}""")
                } else {
                    methodNotAllowed(exchange)
                }
            }
            path == "/__qa/reset" -> {
                if (method == "POST") {
                    state.reset()
                    writeJson(exchange, 200, envelope(success = true, message = "reset"))
                } else {
                    methodNotAllowed(exchange)
                }
            }
            path == "/user/auth" -> {
                if (method == "POST") {
                    handleAuth(exchange)
                } else {
                    methodNotAllowed(exchange)
                }
            }
            path == "/user/auth/modes" -> {
                if (method == "GET") {
                    writeJson(
                        exchange,
                        200,
                        envelope(
                            success = true,
                            message = "ok",
                            data = buildJsonObject {
                                put("modes", buildJsonArray {
                                    add(JsonPrimitive("password"))
                                })
                            },
                        ),
                    )
                } else {
                    methodNotAllowed(exchange)
                }
            }
            path == "/user/" || path == "/user" -> handleUsersCollection(exchange, method)
            path == "/user/me" -> {
                if (method != "GET") {
                    methodNotAllowed(exchange)
                } else {
                    requireAuth(exchange) {
                        val user = state.getUser(1)!!
                        writeJson(exchange, 200, userEnvelope(user))
                    }
                }
            }
            USER_ID_PATH.matches(path) -> handleUserById(exchange, method, path)
            path == "/project/" || path == "/project" -> handleProjectsCollection(exchange, method)
            path == "/project/perm/batch/reset" -> requireAuth(exchange) {
                if (method != "POST") {
                    writeJson(exchange, 405, envelope(false, "method not allowed"))
                    return@requireAuth
                }
                handlePermBatchReset(exchange)
            }
            PROJECT_PERM_PATH.matches(path) -> requireAuth(exchange) {
                if (method != "GET") {
                    writeJson(exchange, 405, envelope(false, "method not allowed"))
                    return@requireAuth
                }
                val projectId = PROJECT_PERM_PATH.matchEntire(path)!!.groupValues[1].toInt()
                val perms = state.getPerms(projectId)
                writeJson(
                    exchange,
                    200,
                    envelope(
                        success = true,
                        message = "ok",
                        data = JsonArray(perms.map { permToJson(it) }),
                    ),
                )
            }
            PROJECT_TOKEN_PATH.matches(path) -> requireAuth(exchange) {
                if (method != "GET") {
                    writeJson(exchange, 405, envelope(false, "method not allowed"))
                    return@requireAuth
                }
                val id = PROJECT_TOKEN_PATH.matchEntire(path)!!.groupValues[1].toInt()
                writeProjectWithToken(exchange, id, state.getProjectToken(id))
            }
            PROJECT_ROTATE_PATH.matches(path) -> requireAuth(exchange) {
                if (method != "POST") {
                    writeJson(exchange, 405, envelope(false, "method not allowed"))
                    return@requireAuth
                }
                val id = PROJECT_ROTATE_PATH.matchEntire(path)!!.groupValues[1].toInt()
                writeProjectWithToken(exchange, id, state.rotateProjectToken(id))
            }
            PROJECT_ID_PATH.matches(path) -> handleProjectById(exchange, method, path)
            path == "/log/summary" -> requireAuth(exchange) {
                if (method != "GET") {
                    writeJson(exchange, 405, envelope(false, "method not allowed"))
                    return@requireAuth
                }
                handleSummary(exchange, query)
            }
            path == "/log/events" -> requireAuth(exchange) {
                if (method != "GET") {
                    writeJson(exchange, 405, envelope(false, "method not allowed"))
                    return@requireAuth
                }
                handleEvents(exchange, query)
            }
            LOG_EVENT_PATH.matches(path) -> requireAuth(exchange) {
                if (method != "GET") {
                    writeJson(exchange, 405, envelope(false, "method not allowed"))
                    return@requireAuth
                }
                val id = LOG_EVENT_PATH.matchEntire(path)!!.groupValues[1].toInt()
                val detail = state.eventDetail(id)
                if (detail == null) {
                    writeJson(exchange, 404, envelope(false, "Error not found"))
                } else {
                    writeJson(exchange, 200, envelope(success = true, message = "ok", data = eventDetailToJson(detail)))
                }
            }
            path == "/fcm/data" -> requireAuth(exchange) {
                if (method != "GET") {
                    writeJson(exchange, 405, envelope(false, "method not allowed"))
                    return@requireAuth
                }
                writeJson(
                    exchange,
                    200,
                    envelope(success = false, message = "fcm disabled for qa", data = null),
                )
            }
            path == "/fcm/token" -> requireAuth(exchange) {
                if (method != "POST" && method != "DELETE") {
                    writeJson(exchange, 405, envelope(false, "method not allowed"))
                    return@requireAuth
                }
                val token = runCatching {
                    json.parseToJsonElement(readBody(exchange)).jsonObject["fcm_token"]?.jsonPrimitive?.contentOrNull
                }.getOrNull()
                if (token.isNullOrBlank()) {
                    writeJson(exchange, 400, envelope(false, "fcm_token required"))
                    return@requireAuth
                }
                val data = if (method == "POST") {
                    buildJsonObject {
                        put("idx", JsonPrimitive(state.registerFcmToken(token)))
                        put("user_idx", JsonPrimitive(1))
                        put("last_delivery", JsonNull)
                    }
                } else {
                    buildJsonObject { put("removed", JsonPrimitive(state.unregisterFcmToken(token))) }
                }
                writeJson(exchange, 200, envelope(success = true, message = "ok", data = data))
            }
            else -> writeJson(exchange, 404, envelope(false, "not found"))
        }
    }

    private fun handleAuth(exchange: HttpExchange) {
        val body = readBody(exchange)
        val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        if (obj == null) {
            writeJson(exchange, 400, envelope(false, "invalid json"))
            return
        }
        val username = obj["username"]?.jsonPrimitive?.contentOrNull
        val password = obj["password"]?.jsonPrimitive?.contentOrNull
        if (username == "qa-admin" && password == "qa-password") {
            writeJson(
                exchange,
                200,
                envelope(success = true, message = "ok", data = JsonPrimitive("qa-token-fixed")),
            )
        } else {
            writeJson(exchange, 401, envelope(false, "invalid credentials"))
        }
    }

    private fun handleUsersCollection(exchange: HttpExchange, method: String) {
        when (method) {
            "GET" -> requireAuth(exchange) {
                val users = state.listUsers()
                writeJson(
                    exchange,
                    200,
                    envelope(
                        success = true,
                        message = "ok",
                        data = JsonArray(users.map { userToJson(it) }),
                    ),
                )
            }
            "POST" -> requireAuth(exchange) {
                val body = readBody(exchange)
                val obj = json.parseToJsonElement(body).jsonObject
                val username = obj["username"]?.jsonPrimitive?.contentOrNull
                val permission = obj["permission"]?.jsonPrimitive?.intOrNull
                if (username.isNullOrBlank() || permission == null) {
                    writeJson(exchange, 400, envelope(false, "invalid body"))
                    return@requireAuth
                }
                val user = state.createUser(username, permission)
                writeJson(exchange, 200, userEnvelope(user))
            }
            else -> writeJson(exchange, 405, envelope(false, "method not allowed"))
        }
    }

    private fun handleUserById(exchange: HttpExchange, method: String, path: String) {
        val id = USER_ID_PATH.matchEntire(path)!!.groupValues[1].toInt()
        requireAuth(exchange) {
            when (method) {
                "PATCH" -> {
                    val body = readBody(exchange)
                    val obj = json.parseToJsonElement(body).jsonObject
                    val username = obj["username"]?.let {
                        if (it is JsonNull) null else it.jsonPrimitive.contentOrNull
                    }
                    val permission = obj["permission"]?.let {
                        if (it is JsonNull) null else it.jsonPrimitive.intOrNull
                    }
                    val updated = state.updateUser(id, username, permission)
                    if (updated == null) {
                        writeJson(exchange, 404, envelope(false, "user not found"))
                    } else {
                        writeJson(exchange, 200, userEnvelope(updated))
                    }
                }
                "DELETE" -> {
                    val deleted = state.deleteUser(id)
                    if (deleted == null) {
                        writeJson(exchange, 404, envelope(false, "user not found"))
                    } else {
                        writeJson(exchange, 200, userEnvelope(deleted))
                    }
                }
                else -> writeJson(exchange, 405, envelope(false, "method not allowed"))
            }
        }
    }

    private fun handleProjectsCollection(exchange: HttpExchange, method: String) {
        when (method) {
            "GET" -> requireAuth(exchange) {
                val projects = state.listProjects()
                writeJson(
                    exchange,
                    200,
                    envelope(
                        success = true,
                        message = "ok",
                        data = JsonArray(projects.map { projectToJson(it) }),
                    ),
                )
            }
            "POST" -> requireAuth(exchange) {
                val body = readBody(exchange)
                val name = json.parseToJsonElement(body).jsonObject["name"]?.jsonPrimitive?.contentOrNull
                if (name.isNullOrBlank()) {
                    writeJson(exchange, 400, envelope(false, "invalid body"))
                    return@requireAuth
                }
                val (project, token) = state.createProject(name)
                writeJson(
                    exchange,
                    200,
                    envelope(
                        success = true,
                        message = "ok",
                        data = projectToJsonWithToken(project, token),
                    ),
                )
            }
            else -> writeJson(exchange, 405, envelope(false, "method not allowed"))
        }
    }

    private fun handleProjectById(exchange: HttpExchange, method: String, path: String) {
        val id = PROJECT_ID_PATH.matchEntire(path)!!.groupValues[1].toInt()
        requireAuth(exchange) {
            when (method) {
                "PATCH" -> {
                    val body = readBody(exchange)
                    val name = json.parseToJsonElement(body).jsonObject["name"]?.jsonPrimitive?.contentOrNull
                    if (name.isNullOrBlank()) {
                        writeJson(exchange, 400, envelope(false, "invalid body"))
                        return@requireAuth
                    }
                    val updated = state.renameProject(id, name)
                    if (updated == null) {
                        writeJson(exchange, 404, envelope(false, "project not found"))
                    } else {
                        writeJson(
                            exchange,
                            200,
                            envelope(success = true, message = "ok", data = projectToJson(updated)),
                        )
                    }
                }
                "DELETE" -> {
                    val removed = state.deleteProject(id)
                    if (!removed) {
                        writeJson(exchange, 404, envelope(false, "project not found"))
                    } else {
                        writeJson(
                            exchange,
                            200,
                            envelope(success = true, message = "ok", data = JsonPrimitive("deleted")),
                        )
                    }
                }
                else -> writeJson(exchange, 405, envelope(false, "method not allowed"))
            }
        }
    }

    private fun handlePermBatchReset(exchange: HttpExchange) {
        val body = readBody(exchange)
        val obj = json.parseToJsonElement(body).jsonObject
        val projectId = obj["projectid"]?.jsonPrimitive?.intOrNull
        val usernames = obj["usernames"]?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.toSet()
            .orEmpty()
        if (projectId == null) {
            writeJson(exchange, 400, envelope(false, "invalid body"))
            return
        }
        val perms = state.resetPerms(projectId, usernames)
        writeJson(
            exchange,
            200,
            envelope(
                success = true,
                message = "ok",
                data = JsonArray(perms.map { permToJson(it) }),
            ),
        )
    }

    private fun handleSummary(exchange: HttpExchange, query: Map<String, String>) {
        val projectId = query["project_id"]?.toIntOrNull()
        if (projectId != null && state.getProject(projectId) == null) {
            writeJson(exchange, 403, envelope(false, "You do not have access to this project"))
            return
        }
        val summary = state.summary(projectId, query["hours"]?.toIntOrNull() ?: 24)
        val data = buildJsonObject {
            put("window_hours", JsonPrimitive(summary.windowHours))
            put("since", time(summary.since))
            put("counts", buildJsonObject { summary.counts.forEach { (level, n) -> put(level, JsonPrimitive(n)) } })
            put(
                "projects",
                buildJsonArray {
                    summary.projects.forEach { p ->
                        add(
                            buildJsonObject {
                                put("project_id", JsonPrimitive(p.projectId))
                                put("error_count", JsonPrimitive(p.errorCount))
                                put("warning_count", JsonPrimitive(p.warningCount))
                                put("last_error_at", p.lastErrorAt?.let(::time) ?: JsonNull)
                            },
                        )
                    }
                },
            )
        }
        writeJson(exchange, 200, envelope(success = true, message = "ok", data = data))
    }

    /** Offset-cursor pages, like the backend's (minus its time ceiling: the fixture never grows mid-run). */
    private fun handleEvents(exchange: HttpExchange, query: Map<String, String>) {
        val projectId = query["project_id"]?.toIntOrNull()
        if (projectId != null && state.getProject(projectId) == null) {
            writeJson(exchange, 403, envelope(false, "You do not have access to this project"))
            return
        }
        val cursor = query["cursor"]
        val offset = if (cursor == null) 0 else decodeCursor(cursor)
        if (offset == null) {
            writeJson(exchange, 400, envelope(false, "Invalid cursor"))
            return
        }
        val limit = (query["limit"]?.toIntOrNull() ?: 50).coerceIn(1, 200)
        val events = state.listEvents(
            QaEventFilter(
                projectId = projectId,
                minLevel = query["min_level"],
                logfileId = query["logfile_id"]?.toIntOrNull(),
                query = query["q"],
                sort = query["sort"] ?: "newest",
                grouped = query["grouped"]?.toBooleanStrictOrNull() ?: true,
            ),
        )
        val page = events.drop(offset).take(limit)
        val next = if (offset + limit < events.size) encodeCursor(offset + limit) else null
        val data = buildJsonObject {
            put("items", JsonArray(page.map { eventToJson(it) }))
            put("next_cursor", next?.let(::JsonPrimitive) ?: JsonNull)
        }
        writeJson(exchange, 200, envelope(success = true, message = "ok", data = data))
    }

    private fun writeProjectWithToken(exchange: HttpExchange, id: Int, token: String?) {
        val project = state.getProject(id)
        if (project == null || token == null) {
            writeJson(exchange, 404, envelope(false, "project not found"))
        } else {
            writeJson(exchange, 200, envelope(success = true, message = "ok", data = projectToJsonWithToken(project, token)))
        }
    }

    private fun methodNotAllowed(exchange: HttpExchange) {
        writeJson(exchange, 405, envelope(false, "method not allowed"))
    }

    private inline fun requireAuth(exchange: HttpExchange, block: () -> Unit) {
        val auth = exchange.requestHeaders.getFirst("Authorization")
        if (auth != "Bearer qa-token-fixed") {
            writeJson(exchange, 401, envelope(false, "unauthorized"))
            return
        }
        block()
    }

    private fun readBody(exchange: HttpExchange): String =
        exchange.requestBody.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

    private fun writeJson(exchange: HttpExchange, status: Int, body: JsonObject) {
        writeRawJson(exchange, status, json.encodeToString(JsonObject.serializer(), body))
    }

    private fun writeRawJson(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun writeEmpty(exchange: HttpExchange, status: Int) {
        exchange.sendResponseHeaders(status, -1)
        exchange.responseBody.close()
    }

    private fun envelope(
        success: Boolean,
        message: String,
        data: JsonElement? = null,
        errorCode: Int? = null,
    ): JsonObject = buildJsonObject {
        put("success", JsonPrimitive(success))
        put("message", JsonPrimitive(message))
        put("error_code", errorCode?.let { JsonPrimitive(it) } ?: JsonNull)
        put("data", data ?: JsonNull)
    }

    private fun userEnvelope(user: QaUser): JsonObject =
        envelope(success = true, message = "ok", data = userToJson(user))

    private fun userToJson(user: QaUser): JsonObject = buildJsonObject {
        put("idx", JsonPrimitive(user.idx))
        put("username", JsonPrimitive(user.username))
        put("permission", JsonPrimitive(user.permission))
    }

    private fun projectToJson(project: QaProject): JsonObject = buildJsonObject {
        put("id", JsonPrimitive(project.id))
        put("name", JsonPrimitive(project.name))
        put("alias", JsonNull)
        put("description", JsonNull)
        put(
            "logfiles",
            buildJsonArray {
                for (log in project.logfiles) {
                    add(
                        buildJsonObject {
                            put("id", JsonPrimitive(log.id))
                            put("project_id", JsonPrimitive(log.project_id))
                            put("file_path", JsonPrimitive(log.file_path))
                            put("file_name", JsonPrimitive(log.file_name))
                        },
                    )
                }
            },
        )
    }

    private fun projectToJsonWithToken(project: QaProject, token: String): JsonObject =
        buildJsonObject {
            put("id", JsonPrimitive(project.id))
            put("name", JsonPrimitive(project.name))
            put("alias", JsonNull)
            put("description", JsonNull)
            put("token", JsonPrimitive(token))
            put(
                "logfiles",
                buildJsonArray {
                    for (log in project.logfiles) {
                        add(
                            buildJsonObject {
                                put("id", JsonPrimitive(log.id))
                                put("project_id", JsonPrimitive(log.project_id))
                                put("file_path", JsonPrimitive(log.file_path))
                                put("file_name", JsonPrimitive(log.file_name))
                            },
                        )
                    }
                },
            )
        }

    private fun permToJson(perm: QaProjectPerm): JsonObject = buildJsonObject {
        put("id", JsonPrimitive(perm.id))
        put("project_id", JsonPrimitive(perm.project_id))
        put("user_id", JsonPrimitive(perm.user_id))
        put("view", JsonPrimitive(perm.view))
        put("project", JsonNull)
    }

    private fun eventToJson(event: QaEvent): JsonObject = buildJsonObject { putEvent(event) }

    private fun eventDetailToJson(detail: QaEventDetail): JsonObject = buildJsonObject {
        putEvent(detail.event)
        put("occurred_at", time(detail.occurredAt))
        put("row_level", JsonPrimitive(detail.rowLevel))
        put("raw", detail.raw?.let(::JsonPrimitive) ?: JsonNull)
        put("recent_ids", JsonArray(detail.recentIds.map(::JsonPrimitive)))
        put(
            "recent",
            JsonArray(
                detail.recent.map { r ->
                    buildJsonObject {
                        put("id", JsonPrimitive(r.id))
                        put("timestamp", time(r.at))
                        put("level", JsonPrimitive(r.level))
                        put("message", r.message?.let(::JsonPrimitive) ?: JsonNull)
                    }
                },
            ),
        )
    }

    private fun JsonObjectBuilder.putEvent(event: QaEvent) {
        put("id", JsonPrimitive(event.id))
        put("project_id", JsonPrimitive(event.projectId))
        put("logfile_id", event.logfileId?.let(::JsonPrimitive) ?: JsonNull)
        put("logfile_name", event.logfileName?.let(::JsonPrimitive) ?: JsonNull)
        put("errortype", event.errortype?.let(::JsonPrimitive) ?: JsonNull)
        put("message", JsonPrimitive(event.message))
        put("level", JsonPrimitive(event.level))
        put("timestamp", time(event.lastSeen))
        put("occurrences", JsonPrimitive(event.occurrences))
        put("first_seen", time(event.firstSeen))
        put("last_seen", time(event.lastSeen))
    }

    /** The backend's naive-UTC form, e.g. `2026-10-19T11:59:00`. */
    private fun time(at: Instant): JsonPrimitive = JsonPrimitive(SERVER_TIME.format(at.atOffset(ZoneOffset.UTC)))

    companion object {
        private val USER_ID_PATH = Regex("""^/user/(\d+)$""")
        private val PROJECT_ID_PATH = Regex("""^/project/(\d+)$""")
        private val PROJECT_PERM_PATH = Regex("""^/project/(\d+)/perm$""")
        private val PROJECT_TOKEN_PATH = Regex("""^/project/(\d+)/token$""")
        private val PROJECT_ROTATE_PATH = Regex("""^/project/(\d+)/rotate-token$""")
        private val LOG_EVENT_PATH = Regex("""^/log/events/(\d+)$""")
        private val SERVER_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        private fun encodeCursor(offset: Int): String =
            Base64.getUrlEncoder().encodeToString("""{"o":$offset}""".toByteArray(StandardCharsets.UTF_8))

        /** The offset in [cursor], or null when it is not one this server issued. */
        private fun decodeCursor(cursor: String): Int? = runCatching {
            val decoded = String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
            Json.parseToJsonElement(decoded).jsonObject["o"]?.jsonPrimitive?.intOrNull
        }.getOrNull()?.takeIf { it >= 0 }

        private fun normalizePath(path: String): String {
            if (path.length > 1 && path.endsWith('/')) {
                // Keep collection trailing slash semantics for /user/ and /project/.
                val trimmed = path.trimEnd('/')
                return when (trimmed) {
                    "/user", "/project" -> "$trimmed/"
                    else -> trimmed.ifEmpty { "/" }
                }
            }
            return path
        }

        private fun parseQuery(raw: String?): Map<String, String> {
            if (raw.isNullOrBlank()) return emptyMap()
            return raw.split('&').mapNotNull { part ->
                if (part.isBlank()) return@mapNotNull null
                val idx = part.indexOf('=')
                if (idx < 0) {
                    decode(part) to ""
                } else {
                    decode(part.substring(0, idx)) to decode(part.substring(idx + 1))
                }
            }.toMap()
        }

        private fun decode(value: String): String =
            URLDecoder.decode(value, StandardCharsets.UTF_8)
    }
}
